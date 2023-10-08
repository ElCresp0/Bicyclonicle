#ifndef BicVariables_h
#define BicVariables_h

#include "Arduino.h"
// Esp32
#include "esp_camera.h"

// Disable brownour problems
#include "soc/soc.h"        
#include "soc/rtc_cntl_reg.h"

// RTC clock
#include "RTClib.h"

// Defines for debbuging
//#define DEBUG_WAIT
#define RTC_CLOCK

// Defines for EEPROM
#define EEPROM_SIZE          11
#define VIDEO_NUM_ADDRESS    1          // not used anymore
#define VIDEO_CONFIG_ADDRESS 3

// Pin definition for button
#define BUTTON_GPIO 4                   // cam 4, sense D1

// Pin definitions for I2C
#define I2C_SDA 13                      // cam - 13, camera uses GPIO 26 
#define I2C_SCL 3                      // cam - 3, camera uses GPIO 27

#define CAMERA_MODEL_AI_THINKER

#ifndef CAMERA_MODEL_AI_THINKER
// Pin definition for CAMERA_MODEL_AI_THINKER
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27

#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22
#endif

#ifndef CAMERA_MODEL_XIAO_ESP32S3
// Pin definition for CAMERA_MODEL_XIAO_ESP32S3
#define PWDN_GPIO_NUM     -1
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM     10
#define SIOD_GPIO_NUM     40
#define SIOC_GPIO_NUM     39

#define Y9_GPIO_NUM       48
#define Y8_GPIO_NUM       11
#define Y7_GPIO_NUM       12
#define Y6_GPIO_NUM       14
#define Y5_GPIO_NUM       16
#define Y4_GPIO_NUM       18
#define Y3_GPIO_NUM       17
#define Y2_GPIO_NUM       15
#define VSYNC_GPIO_NUM    38
#define HREF_GPIO_NUM     47
#define PCLK_GPIO_NUM     13
#endif

/*
 *  framesize_t (resolutions)
    0 FRAMESIZE_96X96,    // 96x96    ~60fps
    1 FRAMESIZE_QQVGA,    // 160x120
    2 FRAMESIZE_QCIF,     // 176x144
    3 FRAMESIZE_HQVGA,    // 240x176
    4 FRAMESIZE_240X240,  // 240x240
    5 FRAMESIZE_QVGA,     // 320x240
    6 FRAMESIZE_CIF,      // 400x296
    7 FRAMESIZE_HVGA,     // 480x320
    8 FRAMESIZE_VGA,      // 640x480
    9 FRAMESIZE_SVGA,     // 800x600  max: 25fps DEFAULT
    10 FRAMESIZE_XGA,      // 1024x768
    11 FRAMESIZE_HD,       // 1280x720
    12 FRAMESIZE_SXGA,     // 1280x1024
    13 FRAMESIZE_UXGA,     // 1600x1200 max: 6fps
*/
struct video_config_t{
  framesize_t resolution;
  int8_t fps;
  int8_t video_length;
  bool video_date;  
};


const byte buffer00dc   [4]  = {0x30, 0x30, 0x64, 0x63}; // "00dc"
const byte buffer0000   [4]  = {0x00, 0x00, 0x00, 0x00}; // 0x00000000
const byte bufferAVI1   [4]  = {0x41, 0x56, 0x49, 0x31}; // "AVI1"            
const byte bufferidx1   [4]  = {0x69, 0x64, 0x78, 0x31}; // "idx1" 

const long unsigned FRAME_INTERVAL  = 100;
const uint16_t      AVI_HEADER_SIZE = 285;   // Size of the AVI file header.
const byte aviHeader[AVI_HEADER_SIZE] =      // This is the AVI file header.  Some of these values get overwritten.
{
                0x52, 0x49, 0x46, 0x46,  // 0x00 "RIFF"
                0x00, 0x00, 0x00, 0x00,  // 0x04           Total file size less 8 bytes [gets updated later]
                0x41, 0x56, 0x49, 0x20,  // 0x08 "AVI "

                0x4C, 0x49, 0x53, 0x54,  // 0x0C "LIST"
                0x44, 0x00, 0x00, 0x00,  // 0x10 68        Structure length
                0x68, 0x64, 0x72, 0x6C,  // 0x04 "hdrl"

                0x61, 0x76, 0x69, 0x68,  // 0x08 "avih"    fcc
                0x38, 0x00, 0x00, 0x00,  // 0x0C 56        Structure length
                0x18, 0x6A, 0x00, 0x00,  // 0x20 250000    dwMicroSecPerFrame     [based on FRAME_INTERVAL]
                0x00, 0x00, 0x00, 0x00,  // 0x24           dwMaxBytesPerSec       [gets updated later]
                0x00, 0x00, 0x00, 0x00,  // 0x28 0         dwPaddingGranularity
                0x10, 0x00, 0x00, 0x00,  // 0x2C 0x10      dwFlags - AVIF_HASINDEX set.
                0x00, 0x00, 0x00, 0x00,  // 0x30           dwTotalFrames          [gets updated later]
                0x00, 0x00, 0x00, 0x00,  // 0x34 0         dwInitialFrames (used for interleaved files only)
                0x01, 0x00, 0x00, 0x00,  // 0x38 1         dwStreams (just video)
                0x00, 0x00, 0x00, 0x00,  // 0x3C 0         dwSuggestedBufferSize
                0x20, 0x03, 0x00, 0x00,  // 0x40 800       dwWidth - 800 (S-VGA)  [based on FRAMESIZE]
                0x58, 0x02, 0x00, 0x00,  // 0x44 600       dwHeight - 600 (S-VGA) [based on FRAMESIZE]
                0x00, 0x00, 0x00, 0x00,  // 0x48           dwReserved
                0x00, 0x00, 0x00, 0x00,  // 0x4C           dwReserved
                0x00, 0x00, 0x00, 0x00,  // 0x50           dwReserved
                0x00, 0x00, 0x00, 0x00,  // 0x54           dwReserved

                0x4C, 0x49, 0x53, 0x54,  // 0x58 "LIST"
                0x84, 0x00, 0x00, 0x00,  // 0x5C 144
                0x73, 0x74, 0x72, 0x6C,  // 0x60 "strl"

                0x73, 0x74, 0x72, 0x68,  // 0x64 "strh"    Stream header
                0x30, 0x00, 0x00, 0x00,  // 0x68  48       Structure length
                0x76, 0x69, 0x64, 0x73,  // 0x6C "vids"    fccType - video stream
                0x4D, 0x4A, 0x50, 0x47,  // 0x70 "MJPG"    fccHandler - Codec
                0x00, 0x00, 0x00, 0x00,  // 0x74           dwFlags - not set
                0x00, 0x00,              // 0x78           wPriority - not set
                0x00, 0x00,              // 0x7A           wLanguage - not set
                0x00, 0x00, 0x00, 0x00,  // 0x7C           dwInitialFrames
                0x01, 0x00, 0x00, 0x00,  // 0x80 1         dwScale
                0x0A, 0x00, 0x00, 0x00,  // 0x84 4         dwRate (frames per second)         [based on FRAME_INTERVAL]
                0x00, 0x00, 0x00, 0x00,  // 0x88           dwStart
                0x00, 0x00, 0x00, 0x00,  // 0x8C           dwLength (frame count)             [gets updated later]
                0x00, 0x00, 0x00, 0x00,  // 0x90           dwSuggestedBufferSize
                0x00, 0x00, 0x00, 0x00,  // 0x94           dwQuality
                0x00, 0x00, 0x00, 0x00,  // 0x98           dwSampleSize

                0x73, 0x74, 0x72, 0x66,  // 0x9C "strf"    Stream format header
                0x28, 0x00, 0x00, 0x00,  // 0xA0 40        Structure length
                0x28, 0x00, 0x00, 0x00,  // 0xA4 40        BITMAPINFOHEADER length (same as above)
                0x20, 0x03, 0x00, 0x00,  // 0xA8 800       Width                  [based on FRAMESIZE]
                0x58, 0x02, 0x00, 0x00,  // 0xAC 600       Height                 [based on FRAMESIZE]
                0x01, 0x00,              // 0xB0 1         Planes
                0x18, 0x00,              // 0xB2 24        Bit count (bit depth once uncompressed)
                0x4D, 0x4A, 0x50, 0x47,  // 0xB4 "MJPG"    Compression
                0x00, 0x00, 0x04, 0x00,  // 0xB8 262144    Size image (approx?)                              [what is this?]
                0x00, 0x00, 0x00, 0x00,  // 0xBC           X pixels per metre
                0x00, 0x00, 0x00, 0x00,  // 0xC0           Y pixels per metre
                0x00, 0x00, 0x00, 0x00,  // 0xC4           Colour indices used
                0x00, 0x00, 0x00, 0x00,  // 0xC8           Colours considered important (0 all important).

                0x49, 0x44, 0x49, 0x54, // 0xCB     "IDIT"     Date information
                0x18, 0x00, 0x00, 0x00, // 0xD0     24         Structure length
                0x53, 0x75, 0x6e, 0x20, // 0xD4                Date in string format Sun Aug 31 12:15:22 2008
                0x41, 0x75, 0x67, 0x20, // 0xD8
                0x33, 0x31, 0x20, 0x31, // 0xDC
                0x32, 0x3a, 0x31, 0x35, // 0xE0
                0x3a, 0x32, 0x32, 0x20, // 0xE4
                0x32, 0x30, 0x30, 0x38, // 0xE8

                0x49, 0x4E, 0x46, 0x4F, // 0xEC "INFO"     Additional informations
                0x1D, 0x00, 0x00, 0x00, // 0xF0 29         Structure length
                0x70, 0x61, 0x75, 0x6c, // 0xF4
                0x2e, 0x77, 0x2e, 0x69, // 0xF8
                0x62, 0x62, 0x6f, 0x74, // 0xFC
                0x73, 0x6f, 0x6e, 0x40, // 0x100
                0x67, 0x6d, 0x61, 0x69, // 0x104
                0x6c, 0x2e, 0x63, 0x6f, // 0x108
                0x6d, 0x00, 0x00, 0x00, // 0x10C
                0x00,                   // 0x110

                0x4C, 0x49, 0x53, 0x54, // 0x111 "LIST"
                0x00, 0x00, 0x00, 0x00, // 0x115           Total size of frames        [gets updated later]
                0x6D, 0x6F, 0x76, 0x69  // 0x119 "movi"
};

enum relative                              // Used when setting position within a file stream.
{
  FROM_START,
  FROM_CURRENT,
  FROM_END
};

#endif