#include "BluetoothController.h"

void BluetoothController::initialize()
{
    /****
     * initialize serial bluetooth stream
     * */
    Serial.println("Initializing blueatooth controller");
    SerialBT.begin("Bicyclonicle");
}

void BluetoothController::listFiles()
{
    Serial.println("received command to list files");
    std::string out = sdCardController->listFiles();
    out.append(";executed;");
    sendMessage(out);
}

void BluetoothController::sendMessage(std::string msg)
{
    Serial.printf("sending message: %s", msg);
    SerialBT.printf(" %s", msg);
}

void BluetoothController::sendVideo(std::string name)
{
    sendMessage("sending;");
    FILE *f = sdCardController->getFileStream(name, "r");
    if (!f)
    {
        sendMessage("failed;");
        return;
    }
    byte b = '0';
    b = fgetc(f);
    while (b != EOF)
    {
        SerialBT.write(b);
        b = fgetc(f);
    }
    fclose(f);
    sendMessage("executed;");
}

void BluetoothController::run()
{
    /****
     *
     * Read bytes from bluetooth serial if available,
     * when functions or keywords are recognized run special tasks
     * */
    while (true)
    {
        if (SerialBT.available())
        {
            char in = SerialBT.read();
            if (in == ';')
            {
                size_t strpos = input.find(':', 0);
                if (strpos != std::string::npos)
                {
                    // input contains a key-value pair
                    std::string key = input.substr(0, strpos);
                    std::string value = input.substr(strpos + 1);
                    if (key == "key1")
                    {
                        Serial.printf("received key1 of value: %s\n", value);
                    }
                    else if (key == "key2")
                    {
                        Serial.printf("received key2 of value: %s\n", value);
                    }
                    else if (key == "sendVideo")
                    {
                        sendVideo(value);
                    }
                    else
                    {
                        Serial.printf("reseived unexpected key: %s of value: %s\n", key, value);
                    }
                }
                else
                {
                    // input contains a single command
                    if (input == "ls")
                    {
                        listFiles();
                    }
                    else
                    {
                        Serial.printf("received unknown command: %s\n", input);
                    }
                }
            }
            else if (in == ':' || isalpha(in))
                input.append({in});
            // else: it's some noise
        }
    }
}