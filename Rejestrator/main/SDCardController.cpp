#include "SDCardController.h"

// TODO check if changing for SDMCC library wont be a problem
void SDCardController::initialize()
{

  Serial.println("Initialising SD card");

  sdmmc_host_t host = SDMMC_HOST_DEFAULT();
  sdmmc_slot_config_t slot_config = SDMMC_SLOT_CONFIG_DEFAULT();

  // Changing bus width to enable using gpio 4, 12 and 13
  slot_config.width = 1;
  host.flags = SDMMC_HOST_FLAG_1BIT;

  // Set up the mount configuration for the FAT filesystem on the SD card
  esp_vfs_fat_sdmmc_mount_config_t mount_config =
      {
          .format_if_mount_failed = false, // Do not format the card if mounting fails
          .max_files = 2,                  // Maximum number of files that can be opened at the same time
      };

  // Pointer to the SDMMC card structure
  sdmmc_card_t *card;

  // Mount the FAT filesystem from the SD card to the "/sdcard" directory
  esp_err_t ret = esp_vfs_fat_sdmmc_mount("/sdcard", &host, &slot_config, &mount_config, &card);

  // Check if the mounting was successful
  if (ret == ESP_OK)
  {
    Serial.println("SD card mounted");
  }
  else
  {
    Serial.print("SD card initialisation error ");
    Serial.println(esp_err_to_name(ret));
    fatalError();
  }

  if (SD_MMC.begin("/sdcard", true))
  {
    Serial.println("SD card ready");
  }
  else
  {
    Serial.println("Could not initialize SD_MMC - some SD card functions may not work");
  }
}

bool SDCardController::startFile()
{
#ifdef RTC_CLOCK
//   DateTime now = rtc.now();
#endif
  char currentDateTime[11] = "YYMMDDhhmm";

#ifdef RTC_CLOCK
  // now.toString(currentDateTime);
#endif

  char AVIFilename[AVI_NAME_LENGTH] = "/sdcard/VID";
  strcat(AVIFilename, currentDateTime);
  strcat(AVIFilename, ".avi");

  aviFile = fopen(AVIFilename, "w");
  if (aviFile == NULL)
  {
    Serial.print("Unable to open AVI file ");
    Serial.println(AVIFilename);
    Serial.println(strerror(errno));
    return false;
  }

  Serial.print(AVIFilename);
  Serial.println(" opened.");

  size_t written = fwrite(aviHeader, 1, sizeof(aviHeader), aviFile);
  if (written != sizeof(aviHeader))
  {
    Serial.println("Unable to write header to AVI file");
    fclose(aviFile);
    return false;
  }

  idx1File = fopen("/sdcard/idx1.tmp", "w+");
  if (idx1File == NULL)
  {
    Serial.println("Unable to open idx1 file for read/write");
    fclose(aviFile);
    return false;
  }

  return true;
}

void SDCardController::closeFile(bool buttonPressed, uint32_t fileFramesTotalSize)
{

  // Calculate how long the AVI file runs for.
  unsigned long fileDuration = (millis() - fileStartTime) / 1000UL;

  // Update AVI header with fps info.
  calculateFps();

  // Update AVI header with resolution info.
  updateResolution();

  // Update AVI header with total file size. This is the sum of:
  //   AVI header (without first 8 bytes)
  //   fileFramesWritten * 8 (extra chunk bytes for each frame)
  //   fileFramesTotalSize (frames from the camera)
  //   filePadding
  //   idx1 section (8 + 16 * fileFramesWritten)
  writeLittleEndian((AVI_HEADER_SIZE - 8) + fileFramesWritten * 8 + fileFramesTotalSize + filePadding + (8 + 16 * fileFramesWritten), aviFile, 0x04, FROM_START);

  // Update the AVI header with maximum bytes per second.
  uint32_t maxBytes = fileFramesTotalSize / fileDuration;
  writeLittleEndian(maxBytes, aviFile, 0x24, FROM_START);

  // Update AVI header with total number of frames.
  writeLittleEndian(fileFramesWritten, aviFile, 0x30, FROM_START);

  // Update stream header with total number of frames.
  writeLittleEndian(fileFramesWritten, aviFile, 0x8C, FROM_START);

  // Update info section if saving button was pressed during this recording
  if (buttonPressed)
  {
    writeLittleEndian(1, aviFile, 0x110, FROM_START);
    buttonPressed = false;
  }

  // Update movi section with total size of frames.  This is the sum of:
  //   fileFramesWritten * 8 (extra chunk bytes for each frame)
  //   fileFramesTotalSize (frames from the camera)
  //   filePadding
  writeLittleEndian(fileFramesWritten * 8 + fileFramesTotalSize + filePadding, aviFile, 0x115, FROM_START);

  // Move the write head back to the end of the AVI file.
  fseek(aviFile, 0, SEEK_END);

  // Add the idx1 section to the end of the AVI file
  writeIdx1Chunk();

  fclose(aviFile);

  Serial.print("File closed, size: ");
  Serial.println(AVI_HEADER_SIZE + fileFramesWritten * 8 + fileFramesTotalSize + filePadding + (8 + 16 * fileFramesWritten));
}

// idx1 chunk is optional, but it is facilitating efficient navigation and synchronization of audio and video streams
void SDCardController::writeIdx1Chunk()
{
  size_t bytesWritten = 0;

  bytesWritten = fwrite(bufferidx1, 1, 4, aviFile);
  if (bytesWritten != 4)
  {
    Serial.println("Unable to write idx1 chunk header to AVI file");
    return;
  }

  bytesWritten = writeLittleEndian((uint32_t)fileFramesWritten * 16, aviFile, 0x00, FROM_CURRENT);
  if (bytesWritten != 4)
  {
    Serial.println("Unable to write idx1 size to AVI file");
    return;
  }

  // We need to read the idx1 file back in, so move the read head to the start of the idx1 file.
  fseek(idx1File, 0, SEEK_SET);

  // For each frame, write a sub chunk to the AVI file (offset & size are read from the idx file)
  char readBuffer[8];
  for (uint32_t x = 0; x < fileFramesWritten; x++)
  {
    // Read the offset & size from the idx file.
    bytesWritten = fread(readBuffer, 1, 8, idx1File);
    if (bytesWritten != 8)
    {
      Serial.println("Unable to read from idx file");
      return;
    }

    bytesWritten = fwrite(buffer00dc, 1, 4, aviFile);
    if (bytesWritten != 4)
    {
      Serial.println("Unable to write 00dc to AVI file idx");
      return;
    }

    bytesWritten = fwrite(buffer0000, 1, 4, aviFile);
    if (bytesWritten != 4)
    {
      Serial.println("Unable to write flags to AVI file idx");
      return;
    }

    bytesWritten = fwrite(readBuffer, 1, 8, aviFile);
    if (bytesWritten != 8)
    {
      Serial.println("Unable to write offset & size to AVI file idx");
      return;
    }
  }

  fclose(idx1File);
}

void SDCardController::addToFile(camera_fb_t *frame)
{
  size_t bytesWritten;

  // Calculate if a padding byte is required (frame chunks need to be an even number of bytes).
  uint8_t paddingByte = frame->len & 0x00000001;

  // Keep track of the current position in the file relative to the start of the movi section.  This is used to update the idx1 file.
  uint32_t frameOffset = ftell(aviFile) - 285;

  // Add the chunk header "00dc" to the file.
  bytesWritten = fwrite(buffer00dc, 1, 4, aviFile);
  if (bytesWritten != 4)
  {
    Serial.println("Unable to write 00dc header to AVI file");
    return;
  }

  // Add the frame size to the file (including padding).
  uint32_t frameSize = frame->len + paddingByte;
  bytesWritten = writeLittleEndian(frameSize, aviFile, 0x00, FROM_CURRENT);
  if (bytesWritten != 4)
  {
    Serial.println("Unable to write frame size to AVI file");
    return;
  }

  // Write the frame from the camera.
  bytesWritten = fwrite(frame->buf, 1, frame->len, aviFile);
  if (bytesWritten != frame->len)
  {
    Serial.println("Unable to write frame to AVI file");
    return;
  }

  // Release this frame from memory.
  esp_camera_fb_return(frame);

  // The frame from the camera contains a chunk header of JFIF (bytes 7-10) that we want to replace with AVI1.
  // So we move the write head back to where the frame was just written + 6 bytes.
  fseek(aviFile, (bytesWritten - 6) * -1, SEEK_END);

  // Then overwrite with the new chunk header value of AVI1.
  bytesWritten = fwrite(bufferAVI1, 1, 4, aviFile);
  if (bytesWritten != 4)
  {
    Serial.println("Unable to write AVI1 to AVI file");
    return;
  }

  // Move the write head back to the end of the file.
  fseek(aviFile, 0, SEEK_END);

  // If required, add the padding to the file.
  if (paddingByte > 0)
  {
    bytesWritten = fwrite(buffer0000, 1, paddingByte, aviFile);
    if (bytesWritten != paddingByte)
    {
      Serial.println("Unable to write padding to AVI file");
      return;
    }
  }

  // Write the frame offset to the idx1 file for this frame (used later).
  bytesWritten = writeLittleEndian(frameOffset, idx1File, 0x00, FROM_CURRENT);
  if (bytesWritten != 4)
  {
    Serial.println("Unable to write frame offset to idx1 file");
    return;
  }

  // Write the frame size to the idx1 file for this frame (used later).
  bytesWritten = writeLittleEndian(frameSize - paddingByte, idx1File, 0x00, FROM_CURRENT);
  if (bytesWritten != 4)
  {
    Serial.println("Unable to write frame size to idx1 file");
    return;
  }

  // Increment the frames written count, and keep track of total padding.
  fileFramesWritten++;
  filePadding = filePadding + paddingByte;
}

void SDCardController::calculateFps()
{

  uint32_t fps = vid_config.fps;
  uint32_t fpms = 1000000 / fps;

  writeLittleEndian(fpms, aviFile, 0x20, FROM_START);
  writeLittleEndian(fps, aviFile, 0x84, FROM_START);
}

void SDCardController::updateResolution()
{

  uint32_t width = resolution[vid_config.resolution].width;
  uint32_t height = resolution[vid_config.resolution].height;

  writeLittleEndian(width, aviFile, 0x40, FROM_START);
  writeLittleEndian(width, aviFile, 0xA8, FROM_START);
  writeLittleEndian(height, aviFile, 0x44, FROM_START);
  writeLittleEndian(height, aviFile, 0xAC, FROM_START);
}

void SDCardController::deleteOldFiles()
{
  float empty_space_kB = 1.0 * (SD_MMC.totalBytes() - SD_MMC.usedBytes()) / 1024;
  Serial.printf("empty_space_MB = %.2f\n", empty_space_kB / 1024);

  while (empty_space_kB <= vid_config.video_length * 375 * 6)
  { // 375 kB -  UXGA jpeg frame at highest quality rounded up, 6 fps - max possible framerate at this quality
    Serial.printf("Not enough space. Free %.1f MB of %.1f MB ... Deleting oldest file.\n", empty_space_kB / 1024, 1.0 * SD_MMC.totalBytes() / 1024 / 1024);

    char oldestFileName[] = "VID9999999999.avi";
    File root = SD_MMC.open("/");
    findOldestFile(root, oldestFileName);

    Serial.print("Deleting ");
    Serial.println(oldestFileName);

    SD_MMC.remove(oldestFileName);
    root.close();
    empty_space_kB = 1.0 * (SD_MMC.totalBytes() - SD_MMC.usedBytes()) / 1024;
  }
}

std::string SDCardController::listFiles()
{
  /****
   * returns semicolon separated list of .avi files
   */
  std::string result = "";
  File root = SD_MMC.open("/sdcard");
  String file = root.getNextFileName();
  while (!file.isEmpty())
  {
    if (file.endsWith(".avi") || file.endsWith(".mp4") || file.endsWith(".png"))
    // TODO: leave only .avi, other formats are for tests
    {
      result.append(file.c_str());
      result.append(";");
    }
    file = root.getNextFileName();
  }
  return (result);
}

FILE *SDCardController::getFileStream(std::string name, std::string mode)
{
  return fopen(name.c_str(), mode.c_str());
}

void SDCardController::findOldestFile(File dir, char *oldestFileName)
{
  File file = dir.openNextFile();

  while (file)
  {
    if (file.isDirectory())
    {
      findOldestFile(file, oldestFileName);
    }
    else
    {
      const char *fileName = file.name();

      if (strlen(fileName) == 17)
      {

        if (strtoll(&fileName[3], NULL, 10) != 0 && strtoll(&fileName[3], NULL, 10) < strtoll(&oldestFileName[3], NULL, 10))
        {
          char buffer[4];
          file.seek(0x00);
          file.readBytes(buffer, 4);
          file.seek(0x110);
          char saveByte = file.peek();
          if (strcmp(buffer, "RIFF") == 0 && saveByte == 0x01)
          {
            strcpy(oldestFileName, fileName);
          }
        }
      }
    }

    file.close();
    file = dir.openNextFile();
  }
}