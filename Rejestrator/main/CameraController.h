#define CameraController_h

#ifndef SDCardController_H
#include "SDCardController.h"
#endif
#include "BicFunctions.h"

class CameraController {
public:
  SDCardController *sdCardController;
  void initialize();
  uint32_t record();
private:
  uint16_t fileFramesCaptured  = 0;          // Number of frames captured by camera.
  uint32_t fileFramesTotalSize = 0;          // Total size of frames in file.
  camera_config_t config;
  void captureFrame();
};
