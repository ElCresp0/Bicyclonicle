#include "BluetoothSerial.h"
// #include "driver/spi_master.h"

// #define SPI_SHIFT_DATA(data, len) __builtin_bswap32((uint32_t)data<<(32-len))
// #define SPI_REARRANGE_DATA(data, len) (__builtin_bswap32(data)>>(32-len))
BluetoothSerial SerialBT;

void setup()
{
    SerialBT.begin("Bicyclonicle");
    Serial.begin(115200);
    Serial.println("Starting esp");
}

void loop()
{
    if (SerialBT.available())
    {
        char c = SerialBT.read();
        Serial.print(c);
        if (c == ';') {
          Serial.print("\n");
        }
    }
    if (Serial.available())
    {
        char c = Serial.read();
        // if (c == 'k') SerialBT.print(" hello;");
        SerialBT.write(c);
        // SerialBT.flush();
        // String s = "ab" + String(c) + "defg;";
        // SerialBT.print(s.c_str());
        Serial.println("sending: " + String(c));
        // Serial.println(String(c) + "\n");
    }
    delay(20);
}