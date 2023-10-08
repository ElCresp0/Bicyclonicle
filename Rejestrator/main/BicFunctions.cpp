#include "BicFunctions.h"

video_config_t vid_config;

void fatalError()
{
    Serial.println("Fatal error - restarting.");
    delay(1000);

    ESP.restart();
}

uint8_t writeLittleEndian(uint32_t value, FILE *file, int32_t offset, relative position)
{
    uint8_t digit[1];
    uint8_t writeCount = 0;


    // Set position within file.  Either relative to: SOF, current position, or EOF.
    if (position == FROM_START)
        fseek(file, offset, SEEK_SET);    // offset >= 0
    else if (position == FROM_CURRENT)
        fseek(file, offset, SEEK_CUR);    // Offset > 0, < 0, or 0
    else if (position == FROM_END)
        fseek(file, offset, SEEK_END);    // offset <= 0 ??
    else
        return 0;


    // Write the value to the file a byte at a time (LSB first).
    for (uint8_t x = 0; x < 4; x++)
    {
        digit[0] = value % 0x100;
        writeCount = writeCount + fwrite(digit, 1, 1, file);
        value = value >> 8;
    }


    // Return the number of bytes written to the file.
    return writeCount;
}
