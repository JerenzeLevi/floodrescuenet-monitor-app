#pragma once
#include <Arduino.h>
#include <Wire.h>
// PCF8574 backpack mapping: RS=0, RW=1, E=2, backlight=3, D4..D7=4..7.
class Lcd16x2 {
 public:
  bool probe(){Wire.beginTransmission(0x27);present=Wire.endTransmission()==0;return present;}
  bool begin(){Wire.begin(21,22);Wire.setTimeOut(20);probe();if(!present)return false;delay(50);nibble(0x30);delay(5);nibble(0x30);delay(1);nibble(0x30);nibble(0x20);send(0x28,false);send(0x0c,false);send(0x06,false);send(0x01,false);delay(2);return true;}
  void line(int row,const char* text){if(!present)return;send(row?0xc0:0x80,false);bool ended=false;for(int i=0;i<16;i++){if(!ended&&text[i]==0)ended=true;send(ended?' ':text[i],true);}}
  bool present=false;
 private:
  void bus(uint8_t b){Wire.beginTransmission(0x27);Wire.write(b|0x08);Wire.endTransmission();}
  void nibble(uint8_t b){bus(b|4);delayMicroseconds(2);bus(b&~4);delayMicroseconds(50);}
  void send(uint8_t value,bool rs){nibble((value&0xf0)|(rs?1:0));nibble(((value<<4)&0xf0)|(rs?1:0));}
};
