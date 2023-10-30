#ifndef BicFunctions_H
#define BicFunctions_H

#include "BicVariables.h"
#include "driver/sdmmc_host.h"
#include <SD_MMC.h>
#include "esp_vfs_fat.h"
#include "FS.h"
extern video_config_t vid_config;

void fatalError();

uint8_t writeLittleEndian(uint32_t value, File file, int32_t offset, relative position);

uint32_t getVideoLengthInSeconds(File file);

bool getSavedByte(File file);

void setSavedByte(File file, bool ifSaved);
#endif