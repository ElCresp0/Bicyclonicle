#include "BluetoothController.h"

void BluetoothController::initialize()
{
    /****
     * initialize serial bluetooth stream
     * */
    Serial.println("Initializing bluetooth controller");
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
    SerialBT.printf("%s", msg.c_str());
    // TODO: co ze spacja z przodu?
}

void BluetoothController::sendVideo(std::string name)
{
    // test results:
    // started sending an 40MB video at 23:23:16.335
    // finished at 23:29:43.931
    // which gives speed of 40MB/(23:29:43.931 - 23:23:16.335) = 103200 B/s = 103 kB/s
    char buff[BUFF_SIZE + 1] = "";
    int count = 0;
    bool breakFlag = false;
    fs::File f = sdCardController->getFileStream(name, "r");
    input.clear();
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
        while (SerialBT.available()) {
            char in = SerialBT.read();
            if (in == ':' || in == '_' || in == '-' || in == '.' || isalnum(in))
                input.append({in});
            else if (in == ';')
            {
                if (input.find("error") != std::string::npos)
                {
                    // rollback
                    Serial.println("android error: rolling back");
                    input.clear();
                    breakFlag = true;
                    break;
                }
                else {
                    Serial.printf("unexpected input: %s\n", input.c_str());
                }
                input.clear();
            }
        }
        if (breakFlag) break;
        count = f.readBytes(buff, BUFF_SIZE);
        SerialBT.write((uint8_t *)buff, count);
    }
    f.close();
    // TODO? set savedByte of the video to false after downloading?
    // sendMessage("executed;"); <- that causes problems on application side
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
            // Serial.printf("received: %1c\n", in);
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
                    if (key.compare("key_resolution") == 0)
                    {
                        Serial.printf("received key_resolution of value: %s\n", value.c_str());
                        if (!value.empty()) vid_config.resolution = (framesize_t)std::stoi(value);
                        else Serial.println("resolution value EMPTY");
                    }
                    else if (key.compare("key_duration") == 0)
                    {
                        Serial.printf("received key_duration of value: %s\n", value.c_str());
                        if (!value.empty()) vid_config.video_length = std::stoi(value);
                        else Serial.println("duration value EMPTY");
                    }
                    else if (key.compare("key_mute_sound") == 0)
                    {
                        Serial.printf("received key_mute_sound of value: %s\n", value.c_str());
                        // if (!value.empty()) vid_config = (value.compare("true") == 0); // there is no sound in videos yet
                        if (value.empty()) Serial.println("sound value EMPTY");
                    }
                    else if (key.compare("key_show_date") == 0)
                    {
                        Serial.printf("received key_show_date of value: %s\n", value.c_str());
                        if (!value.empty()) vid_config.video_date = (value.compare("true") == 0);
                        else Serial.println("date value EMPTY");
                        writeVideoConfigToMemory();
                        sendMessage("executed;");
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
                        readVideoConfigFromMemory(); // for debugging purposes, doesn't affect anything
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