#ifndef SDCardController_h
#define SDCardController_h

#include "BicFunctions.h"

// sd & memory
#include <string>
#include "driver/sdmmc_host.h"
#include <SD_MMC.h>
#include "esp_vfs_fat.h"
#include "FS.h" // File system for SD Card ESP32
#include <EEPROM.h>

#define AVI_NAME_LENGTH 30

class SDCardController
{
public:
    void initialize();
    void addToFile(camera_fb_t *frame);
    bool startFile();
    void closeFile(bool buttonPressed, uint32_t fileFramesTotalSize);
    void deleteOldFiles();
    std::string listFiles();
    FILE *getFileStream(std::string name, std::string mode);
    // RTC_DS3231 rtc;

private:
    // The following relate to the AVI file that gets created.
    uint16_t fileFramesWritten = 0; // Number of frames written to the AVI file.
    uint32_t fileStartTime = 0;     // Used to calculate FPS.
    uint32_t filePadding = 0;       // Total padding in the file.

    FILE *aviFile;
    FILE *idx1File; // Temporary file used to hold the index information
    void writeIdx1Chunk();
    void calculateFps();
    void updateResolution();
    void findOldestFile(File dir, char *oldestFileName);
};

#endif
