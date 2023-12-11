//#define DEBUG_SAVING_TIME
#include "CameraController.h"

void CameraController::initialize() {
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sccb_sda = SIOD_GPIO_NUM;
  config.pin_sccb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.jpeg_quality = 16;
  config.fb_count = 1;

  if (psramFound()) {
    config.fb_count = 2;
    Serial.println("Using psram");
  }

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.print("Camera initialization error ");
    Serial.println(esp_err_to_name(err));
    fatalError();
  }

  Serial.println("Camera ready");
}

uint32_t CameraController::record() {
  sensor_t * s = esp_camera_sensor_get();
  s->set_framesize(s, vid_config.resolution);
  Serial.print("Resolution: ");
  Serial.println(vid_config.resolution);

  unsigned long currentMillis = 0;
  unsigned long lastPictureTaken = 0;

  // Start time of the recording
  unsigned long startTime = millis();

  // Length of video in milliseconds
  unsigned long recordingDuration = vid_config.video_length * 60 * 1000; // video_length is in minutes
  unsigned long frame_interval = 1000 / vid_config.fps;

  // Control video length
  while (millis() - startTime < recordingDuration) {
    currentMillis = millis();

    // Control video fps
    if (currentMillis - lastPictureTaken > frame_interval) {
      lastPictureTaken = currentMillis;

      captureFrame();

      #ifdef DEBUG_SAVING_TIME
        uint32_t savingMillis = millis() - lastPictureTaken;
        Serial.println("Saving time: ");
        Serial.println(savingMillis);
        float fps = 1000.0 / savingMillis;
        Serial.printf("FPS: %.2f\n", fps);
      #endif
    }

    delay(1);
  }

  uint32_t durationMillis = millis() - startTime;

  float fps = (float)fileFramesCaptured / (durationMillis / 1000.0);

  Serial.printf("Average FPS: %.2f\n", fps);
  Serial.println("Recording ended");
  return fileFramesTotalSize;
}

void CameraController::captureFrame() {
  // Take a picture and store a pointer to the frame in the buffer.
  camera_fb_t *frame = esp_camera_fb_get();
  if (frame->buf == NULL) {
    Serial.print("Frame capture failed.");
    return;
  }

  // Keep track of the total frames captured and total size of frames
  fileFramesCaptured++;
  fileFramesTotalSize += frame->len;

  sdCardController->addToFile(frame);
}
