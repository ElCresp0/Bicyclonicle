#define CameraController_h

#include "SDCardController.h"
#include "BicFunctions.h"

class CameraController {
public:
  SDCardController *sdCardController;
  void initialize();
  uint32_t record();
private:
  uint16_t fileFramesCaptured  = 0;          // Number of frames captured by camera.
  uint32_t fileFramesTotalSize = 0;          // Total size of frames in file.
  void captureFrame();
};
