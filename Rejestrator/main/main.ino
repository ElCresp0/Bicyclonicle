#include "CameraController.h"
#include "SDCardController.h"
#include "BicVariables.h"
#include "Wire.h"

bool buttonPressed = false;

RTC_DS3231 rtc;

TaskHandle_t Core0Task;
TaskHandle_t Core1Task;

CameraController cameraController;
SDCardController sdCardController;

void writeVideoConfigToMemory(){
  EEPROM.put(VIDEO_CONFIG_ADDRESS, vid_config);
  EEPROM.commit();
}

void initialiseEeprom(){
  
  int8_t firstTime;
  EEPROM.get(0, firstTime);

  if(firstTime != 0){
    vid_config.resolution = FRAMESIZE_SVGA;
    vid_config.fps = 4;
    vid_config.video_length = 1;
    vid_config.video_date = false;

    EEPROM.write(0, 0x00);
    writeVideoConfigToMemory();

    Serial.println("Recording with default settings");
    return;
  }

  EEPROM.get(VIDEO_CONFIG_ADDRESS, vid_config);

  Serial.println("Recording settings: ");
  Serial.print("Resoution: ");
  Serial.println(vid_config.resolution);
  Serial.print("Fps: ");
  Serial.println(vid_config.fps);
  Serial.print("One video length: ");
  Serial.println(vid_config.video_length);
  Serial.print("Date on video: ");
  Serial.println(vid_config.video_date);
}

void IRAM_ATTR isr() {
  buttonPressed = true;
  Serial.println("Button pressed - current video will be saved.");
}

void setup() {
    WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0); // Disabling brownout detector

    Serial.begin(115200);  
    
    EEPROM.begin(EEPROM_SIZE);
    initialiseEeprom();

    #ifdef DEBUG_WAIT
        delay(5000);
    #endif

    cameraController.initialize();
    sdCardController.initialize();

    #ifdef RTC_CLOCK    
      Wire.begin(I2C_SDA, I2C_SCL);
      
      if (! rtc.begin()) {
        Serial.println("Could not find RTC.");
        fatalError();
      }
      else {
        rtc.adjust(DateTime(__DATE__, __TIME__));
        Serial.println("RTC clock ready");
      }
    #endif

    cameraController.sdCardController = &sdCardController;
    sdCardController.rtc = rtc;
   
    delay(10);

    #ifdef BUTTON  
      pinMode(BUTTON_GPIO, INPUT_PULLUP);
      attachInterrupt(BUTTON_GPIO, isr, FALLING);
    #endif

    xTaskCreatePinnedToCore(codeCore0Task, "Core0Task", 8192, NULL, 5, &Core0Task, 0);
    xTaskCreatePinnedToCore(codeCore1Task, "Core1Task", 8192, NULL, 5, &Core1Task, 1);
}

void loop() {}

void codeCore0Task(void *parameter){
  while(true){

    #ifdef DEBUG_WAIT
      delay(10000);
    #endif

    sdCardController.deleteOldFiles();
    
    bool fileOpen = sdCardController.startFile();

    if(fileOpen){
        uint32_t fileFramesTotalSize = cameraController.record();
        sdCardController.closeFile(buttonPressed, fileFramesTotalSize);
    }

    #ifdef DEBUG_WAIT
      delay(500000);
    #endif
  }
}

void codeCore1Task(void *parameter){

}
