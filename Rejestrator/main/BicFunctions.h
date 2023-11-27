#ifndef BicFunctions_H
#define BicFunctions_H

#include "BicVariables.h"

extern video_config_t vid_config;

void fatalError();

uint8_t writeLittleEndian(uint32_t value, FILE *file, int32_t offset, relative position);

#endif