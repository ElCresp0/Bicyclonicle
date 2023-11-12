#define BluetoothController_h

#include "BluetoothSerial.h"
#include "SDCardController.h"
// #include "BicFunctions.h"
#include <string>

#define BUFF_SIZE 1024

class BluetoothController
{
public:
    SDCardController *sdCardController;
    void initialize();
    void sendMessage(std::string message);
    // void receiveMessage();

    void run();

private:
    void listFiles();
    void sendVideo(std::string name);
    BluetoothSerial SerialBT;
    std::string input;
};
