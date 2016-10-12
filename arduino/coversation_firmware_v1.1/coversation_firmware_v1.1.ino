#include <TaskScheduler.h>
#include <SoftwareSerial.h>  
#include <Adafruit_NeoPixel.h>


int bluetoothTx = 3;  // TX-O pin of bluetooth mate, Arduino D2
int bluetoothRx = 2;  // RX-I pin of bluetooth mate, Arduino D3


int state1_led = 9;
int state2_led = 10;
int state3_led = 11;

boolean runningRainbow = false;
boolean pixelsOff = true;


int neoPin = 6;
int vibPin = 5;
int fsrPin = A0;

int pressHold = 950; // Press Threshold

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);
Adafruit_NeoPixel strip = Adafruit_NeoPixel(48, neoPin, NEO_GRB + NEO_KHZ800);

int state = 1; // 1 idle, 2 ringing, 3 offhook
int oldState = 0;

void stateCB();
void neoCB();
void fsrCB();
void vibCB();

boolean ANSWERCALL = false;

Task stateTask(1, TASK_FOREVER, &stateCB); // stateTask kører hver 5ms, forevigt!
Task neopixelTask(1, TASK_FOREVER, &neoCB); // neo call back - same deal!
Task fsrTask(1, TASK_FOREVER, &fsrCB); // fsr call back - same deal!
Task vibTask(1, TASK_FOREVER, &vibCB);

Scheduler runner;

void stateCB() {
    String btStr = bluetooth.readString();
  

    oldState = state;
    if(btStr.indexOf("RING") != -1){      
      state = 2;      
    } else if(btStr.indexOf("OFFH") != -1){
      state = 3;
    } else if(btStr.indexOf("IDLE") != -1){
      state = 1;
    }    


  digitalWrite(state1_led, LOW);
  digitalWrite(state2_led, LOW);
  digitalWrite(state3_led, LOW);
  switch (state) {
      case 2: // RINGING
        digitalWrite(state2_led, HIGH);        
       // Serial.println("RINGELING");        
        //rainbowCycle(20);
      break;
      case 3: // OFF HOOK / IN CALL
        digitalWrite(state3_led, HIGH);
       // Serial.println("OFF THE HOOK");
        // Strip OFF
        //strip.begin();
        //strip.show();       
      break;
      default:
        digitalWrite(state1_led, HIGH);
        // Serial.println("IDLE");        
        // Strip OFF
        //strip.begin();
        //strip.show();
        ANSWERCALL = false;
      break;
  }
  
}

void fsrCB(){  
  // A0 is reading from the FSR!
  int FSRReading = analogRead(fsrPin);  
  if(FSRReading > pressHold && state == 2 && !ANSWERCALL){ // if we are ringing and the fsr is squeezed
        bluetooth.write("PICKUP\n");        
        ANSWERCALL = true  ; // boolean
        state = 3;
  }
}

void neoCB(){
  if(state == 2){
  uint16_t i, j;
  
  // millis stuff!
  digitalWrite(vibPin, HIGH);        
    
  
  for(j=0; j<256*5; j++) { // 5 cycles of all colors on wheel       
      for(i=0; i< strip.numPixels(); i++) {
        strip.setPixelColor(i, Wheel(((i * 256 / strip.numPixels()) + j) & 255));
      }
      strip.setBrightness(100);
      strip.show();        
  }
    digitalWrite(vibPin, LOW);
  } else {    
    strip.setBrightness(0);
    strip.show();   
  }
}

void vibCB(){
    if(state == 2){
      digitalWrite(vibPin, HIGH);
      delay(400);
      digitalWrite(vibPin, LOW);
      delay(100);
      digitalWrite(vibPin, HIGH);
      delay(400);
      digitalWrite(vibPin, LOW);
      delay(10);
    } else {
      digitalWrite(vibPin, LOW);
    }
}

void setup()
{
  Serial.begin(9600);  // Begin the serial monitor at 9600bps

  bluetooth.begin(115200);  // The Bluetooth Mate defaults to 115200bps
  bluetooth.print("$");  // Print three times individually
  bluetooth.print("$");
  bluetooth.print("$");  // Enter command mode
  delay(100);  // Short delay, wait for the Mate to send back CMD
  bluetooth.println("U,9600,N");  // Temporarily Change the baudrate to 9600, no parity
  // 115200 can be too fast at times for NewSoftSerial to relay the data reliably
  bluetooth.begin(9600);  // Start bluetooth serial at 9600

  // setup leds
  pinMode(state1_led, OUTPUT);
  pinMode(state2_led, OUTPUT);
  pinMode(state3_led, OUTPUT);

  pinMode(vibPin, OUTPUT);

  strip.begin();
  strip.setBrightness(100);
  strip.show();

  Serial.println("Setup!");

  /* TASK SETUP */
  runner.init();
  runner.addTask(stateTask);
  runner.addTask(neopixelTask);
  runner.addTask(fsrTask);
  runner.addTask(vibTask);

  Serial.println("Task setup!");
  stateTask.enable();
  neopixelTask.enable();
  fsrTask.enable();
/*
  Serial.println("Waiting 5s for startup!");
  Serial.print("5 ");
  delay(1000);
  Serial.print("4 ");
  delay(1000);
  Serial.print("3 ");
  delay(1000);
  Serial.print("2 ");
  delay(1000);
  Serial.println("1");
  delay(1000);
  Serial.println("GO!");
*/
}

void loop() {
  runner.execute();
}


/* WHEEL TIL NEOPIXELS !! */

// Input a value 0 to 255 to get a color value.
// The colours are a transition r - g - b - back to r.
uint32_t Wheel(byte WheelPos) {
  if(WheelPos < 85) {
   return strip.Color(WheelPos * 3, 255 - WheelPos * 3, 0);
  } else if(WheelPos < 170) {
   WheelPos -= 85;
   return strip.Color(255 - WheelPos * 3, 0, WheelPos * 3);
  } else {
   WheelPos -= 170;
   return strip.Color(0, WheelPos * 3, 255 - WheelPos * 3);
  }
}
