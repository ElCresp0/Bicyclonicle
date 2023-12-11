#include "BicFunctions.h"

video_config_t vid_config;

void fatalError() {
  Serial.println("Fatal error - restarting.");
  delay(1000);
  ESP.restart();
}

uint8_t writeLittleEndian(uint32_t value, File file, int32_t offset, relative position) {
  uint8_t digit[1];
  uint8_t writeCount = 0;

  // Set the position within the file, either relative to: SOF, current position, or EOF.
  if (position == FROM_START)
    file.seek(offset, SeekSet); // offset >= 0
  else if (position == FROM_CURRENT)
    file.seek(offset, SeekCur); // Offset > 0, < 0, or 0
  else if (position == FROM_END)
    file.seek(offset, SeekEnd); // offset <= 0 ??
  else
    return 0;

  // Write the value to the file a byte at a time (LSB first).
  for (uint8_t x = 0; x < 4; x++) {
    digit[0] = value % 0x100;
    writeCount = writeCount + file.write(digit, 1);
    value = value >> 8;
  }

  // Return the number of bytes written to the file.
  return writeCount;
}

uint32_t getVideoLengthInSeconds(File file){
  char framesBuffer[4];
  file.seek(0x30);
  file.readBytes(framesBuffer, 4);

  char fpsBuffer[4];
  file.seek(0x84);
  file.readBytes(fpsBuffer, 4);

  uint32_t frames = (uint32_t)(framesBuffer[0] << 24 | framesBuffer[1] << 16 | framesBuffer[2] << 8 | framesBuffer[3]);
  uint32_t fps = (uint32_t)(fpsBuffer[0] << 24 | fpsBuffer[1] << 16 | fpsBuffer[2] << 8 | fpsBuffer[3]);

  return frames/fps;
}

bool getSavedByte(File file){
  file.seek(0x110);
  return file.peek() == 0x01;
}

void setSavedByte(File file, bool ifSaved){
  writeLittleEndian(ifSaved, file, 0x110, FROM_START);
}

void writeVideoConfigToMemory()
{
  EEPROM.put(VIDEO_CONFIG_ADDRESS, vid_config);
  EEPROM.commit();
}

void readVideoConfigFromMemory()
{
  EEPROM.get(VIDEO_CONFIG_ADDRESS, vid_config);

  Serial.println("Recording settings: ");
  Serial.print("Resolution: ");
  Serial.println(vid_config.resolution);
  Serial.print("Fps: ");
  Serial.println(vid_config.fps);
  Serial.print("One video length: ");
  Serial.println(vid_config.video_length);
  Serial.print("Date on video: ");
  Serial.println(vid_config.video_date);
}
