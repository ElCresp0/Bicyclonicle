#include "BluetoothController.h"

void BluetoothController::initialize()
{
    /****
     * initialize serial bluetooth stream
     * */
    Serial.println("Initializing blueatooth controller");
    SerialBT.begin("Bicyclonicle");
    input = "";
}

void BluetoothController::listFiles()
{
    Serial.println("received command to list files");
    std::string out = sdCardController->listFiles();
    out.append(";");
    sendMessage(out);
}

void BluetoothController::sendMessage(std::string msg)
{
    Serial.printf("sending message: %s\n", msg.c_str());
    SerialBT.printf(" %s", msg.c_str());
    // TODO: co ze spacja z przodu?
}

void BluetoothController::sendVideo(std::string name)
{
    char buff[BUFF_SIZE + 1] = "";
    int count = 0;
    fs::File f = sdCardController->getFileStream(name, "r");
    if (f == NULL)
    {
        sendMessage("failed;");
        return;
    }
    sprintf(buff, "sending:%s:%d;", name.c_str(), f.size());
    sendMessage(std::string(buff));
    // char b = '0';
    f.seek(0);
    // b = fgetc(f);
    while (f.available() > 0)
    {
        count = f.readBytes(buff, BUFF_SIZE);
        SerialBT.write((uint8_t *)buff, count);
        // SerialBT.write(&b, 1);
        // b = fgetc(f);
    }
    // fclose(f);
    f.close();
    // sendMessage("executed;");
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
            Serial.printf("received: %1c\n", in);
            if (in == ';')
            {
                if (input.find("error") != std::string::npos)
                {
                    // rollback
                    Serial.println("android error: rolling back");
                    input.clear();
                    continue;
                }
                size_t keyVal = input.find(':', 0);
                if (keyVal != std::string::npos)
                {
                    // input contains a key-value pair
                    Serial.printf("received keyval: %s // ':' is at: %d\n", input.c_str(), keyVal);
                    Serial.println("received keyval:");
                    Serial.println(input.c_str());
                    std::string key = input.substr(0, keyVal);
                    std::string value = input.substr(keyVal + 1);
                    if (key.compare("key1") == 0)
                    {
                        Serial.printf("received key1 of value: %s\n", value.c_str());
                    }
                    else if (key.compare("key2") == 0)
                    {
                        Serial.printf("received key2 of value: %s\n", value.c_str());
                    }
                    else if (key.compare("sendVideo") == 0)
                    {
                        sendVideo(value);
                    }
                    else
                    {
                        Serial.printf("received unexpected key: %s of value: %s\n", key.c_str(), value.c_str());
                    }
                }
                else
                {
                    // input contains a single command
                    if (input.compare("ls") == 0)
                    {
                        listFiles();
                    }
                    else
                    {
                        Serial.printf("received unknown command: %s\n", input.c_str());
                    }
                }
                input.clear();
            }
            else if (in == ':' || in == '_' || in == '-' || in == '.' || isalnum(in))
                input.append({in});
            else
            {
                Serial.printf("not appending: %1c\n", in);
            }
            // else: it's some noise
        }
    }
}