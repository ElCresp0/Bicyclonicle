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
