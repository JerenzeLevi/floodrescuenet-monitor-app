#include <Arduino.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <Preferences.h>
#include <time.h>
#include "Config.h"
#include "secrets.h"
#include "firebase_roots.h"
#include "Controller.h"
#include "Lcd16x2.h"

using namespace flood;
struct Request { char json[1536],id[81],type[20],location[81]; uint32_t duration;int power;bool automatic;int64_t expires; };
struct Record { char path[120],json[2304];bool acknowledgement; };
struct Snapshot { int raw,power;float cm,temperature,battery;int rain;bool aligned,sensorReady,lcd,limits,home,end,stop;char net[20],mode[12];uint32_t activity; };
Controller controller;Lcd16x2 lcd;
QueueHandle_t commands,records;SemaphoreHandle_t snapshotMutex;
Snapshot snapshot{};Request running{};bool hasRunning=false,pwmOK=false;
uint32_t lastDisplay=0,lastReading=0,armSince=0;bool armHeld=false,armConsumed=false;
unsigned long previousTransition=0;int previousTier=-1;int64_t lastActivityAt=0;
String token;uint32_t tokenDeadline=0;Preferences storage;
uint32_t droppedRecords=0,lastLatencyMs=0;
int64_t epochMs(){return (int64_t)time(nullptr)*1000;}
bool clockReady(){return time(nullptr)>1700000000;}
float waterCm(int raw){if(FULL_ADC<=DRY_ADC||FULL_DEPTH_CM<=0)return 0;return constrain((raw-DRY_ADC)*FULL_DEPTH_CM/(FULL_ADC-DRY_ADC),0.0f,FULL_DEPTH_CM);}
void enqueue(const String& path,JsonDocument& doc,bool ack=false){
 Record record{};path.toCharArray(record.path,sizeof(record.path));record.acknowledgement=ack;
 if(serializeJson(doc,record.json,sizeof(record.json))>=sizeof(record.json)-1 || xQueueSend(records,&record,0)!=pdTRUE)__atomic_fetch_add(&droppedRecords,1,__ATOMIC_RELAXED);
}
String recordId(){return String((uint32_t)esp_random(),HEX)+String((uint32_t)esp_random(),HEX)+String(millis(),HEX);}
void event(const char* title,const char* message,const char* severity,const char* category,const char* type="STATUS"){
 JsonDocument d;d["timestamp"][".sv"]="timestamp";d["title"]=title;d["description"]=message;d["severity"]=severity;d["category"]=category;d["type"]=type;
 d["resolution"]=(String(severity)=="WARNING"||String(severity)=="DANGER")?"Pending":"Passed";
 enqueue("events/"+recordId(),d);
}
void ack(const Request& r,const char* status,const char* message){JsonDocument d;if(deserializeJson(d,r.json))return;d["status"]=status;d["message"]=message;d["acknowledgedAt"][".sv"]="timestamp";enqueue("command",d,true);}
void deployment(const Request* request,bool success,const char* error,uint32_t cycle){
 JsonDocument d;d["timestamp"][".sv"]="timestamp";d["location"]=request?request->location:DEFAULT_LOCATION;d["initiator"]=request?"Manual":"System";d["unitId"]=UNIT_ID;d["status"]=success?"Success":"Failed";d["cycleMs"]=cycle;d["thrustPercent"]=request?request->power:100;if(!success)d["error"]=error;
 enqueue("deployments/"+(request?String(request->id):recordId()),d);
}
void diagnosticResult(bool passed,const char* explanation){
 JsonDocument d;d["timestamp"][".sv"]="timestamp";d["title"]=passed?"Hardware diagnostic passed":"Hardware diagnostic incomplete/failed";d["description"]=explanation;d["severity"]=passed?"SYSTEM":"WARNING";d["category"]="System";d["type"]="DIAGNOSTIC";d["resolution"]=passed?"Passed":"Pending";
 enqueue("diagnostic",d);enqueue("events/"+recordId(),d);
}
// All network calls run in a separate task. They never delay the motor stop loop.
int https(const String& url,const char* method,const String& body,String& response,const char* match=nullptr,String* etag=nullptr){
 WiFiClientSecure tls;tls.setCACert(FIREBASE_ROOTS);HTTPClient http;http.setConnectTimeout(3000);http.setTimeout(3000);
 if(!http.begin(tls,url))return -1;
 http.addHeader("Content-Type","application/json");
 if(etag){http.addHeader("X-Firebase-ETag","true");const char* headers[]={"ETag"};http.collectHeaders(headers,1);}
 if(match)http.addHeader("if-match",match);
 uint32_t start=millis();int code=http.sendRequest(method,body);lastLatencyMs=millis()-start;
 response=http.getString();if(etag)*etag=http.header("ETag");http.end();return code;
}
String rootPath(){return String("deviceSessions/")+VIEWER_UID+"/";}
int db(const String& path,const char* method,const String& body,String& response,const char* match=nullptr,String* etag=nullptr){
 return https(String(DATABASE_URL)+"/"+rootPath()+path+".json?auth="+token,method,body,response,match,etag);
}
bool authenticate(){
 JsonDocument d;d["email"]=DEVICE_EMAIL;d["password"]=DEVICE_PASSWORD;d["returnSecureToken"]=true;String body,response;serializeJson(d,body);
 if(https(String("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=")+FIREBASE_API_KEY,"POST",body,response)!=200)return false;
 JsonDocument auth;if(deserializeJson(auth,response))return false;token=auth["idToken"].as<String>();tokenDeadline=millis()+3000000;return !token.isEmpty();
}
void network(void*){
 WiFi.mode(WIFI_STA);WiFi.begin(WIFI_SSID,WIFI_PASSWORD);configTime(0,0,"pool.ntp.org","time.google.com");storage.begin("flood",false);
 String lastId=storage.getString("lastId","");Record pending{};bool hasPending=false;uint32_t reconnect=0,published=0,polled=0;int priorWater=-1;
 for(;;){
  if(WiFi.status()!=WL_CONNECTED){if(millis()-reconnect>10000){WiFi.reconnect();reconnect=millis();}vTaskDelay(pdMS_TO_TICKS(250));continue;}
  if(!clockReady()){vTaskDelay(pdMS_TO_TICKS(250));continue;}
  if(token.isEmpty()||(int32_t)(millis()-tokenDeadline)>=0){if(!authenticate()){token="";vTaskDelay(pdMS_TO_TICKS(3000));continue;}}
  String response;
  if(!hasPending)hasPending=xQueueReceive(records,&pending,0)==pdTRUE;
  if(hasPending){
   int code=-1;
   if(pending.acknowledgement){
    String tag;int readCode=db("command","GET","",response,nullptr,&tag);JsonDocument current,desired;
    if(readCode==200&&!deserializeJson(current,response)&&!deserializeJson(desired,pending.json)){
     String status=current["status"]|"";
     if(current["id"].as<String>()!=desired["id"].as<String>() || current["expiresAt"].as<int64_t>()<=epochMs() || (status!="pending"&&status!="accepted")){hasPending=false;}
     else if(!tag.isEmpty())code=db("command","PUT",pending.json,response,tag.c_str());
    } else code=readCode;
   } else code=db(pending.path,"PUT",pending.json,response);
   if(code==200||code==204)hasPending=false;
   if(code==401||code==403)token="";
  }
  if(millis()-published>=2000){
   published=millis();Snapshot s{};if(xSemaphoreTake(snapshotMutex,pdMS_TO_TICKS(20))==pdTRUE){s=snapshot;xSemaphoreGive(snapshotMutex);}
   JsonDocument d;d["schemaVersion"]=1;d["source"]="esp32";d["unitId"]=UNIT_ID;d["waterCm"]=s.cm;d["warningCm"]=waterCm(880);d["dangerCm"]=waterCm(2000);d["net"]=s.net;d["mode"]=s.mode;d["updatedAt"][".sv"]="timestamp";
   // Convert elapsed monotonic activity time to epoch without relying on clock during boot.
   d["lastActivityAt"]=epochMs()-(uint32_t)(millis()-s.activity);d["sensorReady"]=s.sensorReady;d["motorPowerPercent"]=s.power;
   d["signal"]=String(WiFi.RSSI())+" dBm";d["latencyMs"]=min(lastLatencyMs,(uint32_t)60000);if(isfinite(s.temperature)&&s.temperature>=-50&&s.temperature<=150)d["coreTempC"]=s.temperature;
   if(USE_BATTERY)d["batteryPercent"]=constrain((s.battery-BATTERY_EMPTY_V)*100/(BATTERY_FULL_V-BATTERY_EMPTY_V),0.0f,100.0f);
   if(USE_RAIN){float wet=constrain((s.rain-RAIN_DRY_ADC)*100.0f/(RAIN_WET_ADC-RAIN_DRY_ADC),0.0f,100.0f);d["rain"]=String(wet,0)+"% wetness (calibrated)";}
   if(USE_ALIGNMENT)d["alignment"]=s.aligned?"Aligned":"Misaligned";
   d["positionFeedback"]=USE_LIMITS?"limit switches":"timed estimate";d["droppedRecords"]=__atomic_load_n(&droppedRecords,__ATOMIC_RELAXED);
   String body;serializeJson(d,body);int code=db("telemetry","PUT",body,response);if(code==401||code==403)token="";
   JsonDocument reading;reading["timestamp"][".sv"]="timestamp";reading["waterCm"]=s.cm;reading["trend"]=priorWater<0?"STABLE":s.raw>priorWater?"RISING":s.raw<priorWater?"RECEDING":"STABLE";priorWater=s.raw;body="";serializeJson(reading,body);db("readings/"+recordId(),"PUT",body,response);
  }
  if(millis()-polled>=1000){
   polled=millis();if(db("command","GET","",response)==200){
    JsonDocument d;if(!deserializeJson(d,response)&&d.is<JsonObject>()){
     String id=d["id"]|"",type=d["type"]|"",status=d["status"]|"";
     int64_t created=d["createdAt"]|0LL,expires=d["expiresAt"]|0LL;
     if(status=="pending"&&id.length()>=16&&id.length()<=80&&id!=lastId&&d["target"]=="esp32"&&d["requestedBy"]==VIEWER_UID&&created<=epochMs()+5000&&expires>epochMs()&&expires-created<=35000){
      Request request{};id.toCharArray(request.id,sizeof(request.id));type.toCharArray(request.type,sizeof(request.type));String(d["location"]|DEFAULT_LOCATION).toCharArray(request.location,sizeof(request.location));request.duration=d["durationMs"]|3000;request.power=d["thrustPercent"]|100;request.automatic=d["mode"]=="automatic";request.expires=expires;
      size_t length=serializeJson(d,request.json,sizeof(request.json));
      if(length<sizeof(request.json)-1&&uxQueueSpacesAvailable(commands)>0){
       // Persist before enqueue: a reset between receipt and actuation cannot replay this ID.
       storage.putString("lastId",id);lastId=id;
       if(xQueueSend(commands,&request,0)!=pdTRUE)ack(request,"failed","Controller request queue unavailable");
      }
     }
    }
   }
  }
  vTaskDelay(pdMS_TO_TICKS(30));
 }
}
uint32_t motionStart=0;Net previousNet=Net::Unarmed;uint32_t activityMillis=0;
void setup(){
 pinMode(IN1_PIN,OUTPUT);pinMode(IN2_PIN,OUTPUT);digitalWrite(IN1_PIN,LOW);digitalWrite(IN2_PIN,LOW);
 pinMode(BUZZER_PIN,OUTPUT);digitalWrite(BUZZER_PIN,LOW);pinMode(ESTOP_PIN,INPUT_PULLUP);pinMode(ARM_PIN,INPUT_PULLUP);
 if(USE_LIMITS){pinMode(HOME_LIMIT_PIN,INPUT_PULLUP);pinMode(END_LIMIT_PIN,INPUT_PULLUP);}if(USE_ALIGNMENT)pinMode(ALIGNMENT_PIN,INPUT);
 analogReadResolution(12);pwmOK=ledcAttach(ENA_PIN,1000,8);ledcWrite(ENA_PIN,0);lcd.begin();
 commands=xQueueCreate(1,sizeof(Request));records=xQueueCreate(32,sizeof(Record));snapshotMutex=xSemaphoreCreateMutex();
 if(!commands||!records||!snapshotMutex){while(true){digitalWrite(IN1_PIN,LOW);digitalWrite(IN2_PIN,LOW);delay(1000);}}
 xTaskCreatePinnedToCore(network,"firebase",16384,nullptr,1,nullptr,0);
 event("ESP32 started","Motor stopped; local arming and calibration required.","SYSTEM","System");
}
void loop(){
 uint32_t now=millis();Inputs in;in.raw=analogRead(WATER_PIN);in.stop=digitalRead(ESTOP_PIN)==HIGH;in.limits=USE_LIMITS;in.retracted=USE_LIMITS&&digitalRead(HOME_LIMIT_PIN)==LOW;in.deployed=USE_LIMITS&&digitalRead(END_LIMIT_PIN)==LOW;in.aligned=!USE_ALIGNMENT||digitalRead(ALIGNMENT_PIN)==LOW;
 in.calibrated=WATER_CALIBRATED&&pwmOK&&FULL_ADC>2000&&DRY_ADC<880&&FULL_ADC>DRY_ADC&&FULL_DEPTH_CM>0&&FULL_DEPTH_CM<=20;
 bool arm=digitalRead(ARM_PIN)==LOW;
 if(arm&&!armHeld){armHeld=true;armSince=now;}if(!arm){armHeld=false;armConsumed=false;}
 if(armHeld&&!armConsumed&&now-armSince>=1000){armConsumed=true;if(controller.arm(now,in))event("Device armed","Operator confirmed the net is repacked.","SYSTEM","System");}
 Request request{};
 if(!hasRunning&&xQueueReceive(commands,&request,0)==pdTRUE){
  if(request.expires<=epochMs()){ack(request,"failed","Request expired before execution");}
  else if(String(request.type)=="SET_MODE"){controller.setAutomatic(request.automatic);ack(request,"completed","Controller mode changed");event("Mode changed",request.automatic?"automatic":"manual","SYSTEM","System");}
  else if(String(request.type)=="DEPLOY"){
   if(controller.deploy(now,request.duration,request.power,in)){running=request;hasRunning=true;ack(request,"accepted","Motor deployment started");}
   else{ack(request,"failed","Net not ready, interlock open, or invalid settings");deployment(&request,false,"Deployment preconditions failed",0);}
  }else if(String(request.type)=="DIAGNOSTIC"){
   if(lcd.probe()&&controller.diagnostic(now,in)){running=request;hasRunning=true;ack(request,"accepted","Short motor out-and-back test started");}
   else{const char* reason="Motion test requires a ready, calibrated, aligned rig, LCD and both limit switches; not all checks passed";diagnosticResult(false,reason);ack(request,"failed",reason);}
  }else ack(request,"failed","Unsupported command");
 }
 controller.update(now,in);
 // The 10ms control loop stops PWM independently of network I/O.
 if(controller.direction==0){ledcWrite(ENA_PIN,0);digitalWrite(IN1_PIN,LOW);digitalWrite(IN2_PIN,LOW);}
 else {digitalWrite(IN1_PIN,controller.direction>0?HIGH:LOW);digitalWrite(IN2_PIN,controller.direction<0?HIGH:LOW);ledcWrite(ENA_PIN,controller.power*255/100);}
 int tier=Controller::tier(in.raw);digitalWrite(BUZZER_PIN,tier==2||(tier==1&&(now/400)%2==0)?HIGH:LOW);
 if(tier!=previousTier){event(tier==2?"Critical water level":tier==1?"Rising water alert":"Water level normal","Reported from the physical ADC",tier==2?"DANGER":tier==1?"WARNING":"NORMAL",tier==2?"Danger":"System");previousTier=tier;}
 if(controller.transitions!=previousTransition){
  previousTransition=controller.transitions;activityMillis=now;
  if(controller.net==Net::Deploying)motionStart=now;
  if(previousNet==Net::Deploying&&(controller.net==Net::Deployed||controller.net==Net::Fault)){
   bool success=controller.net==Net::Deployed;deployment(hasRunning?&running:nullptr,success,controller.error,now-motionStart);
   event(success?"Net deployed":"Deployment failed",success?(USE_LIMITS?"Endpoint switch confirmed":"Timed actuation ended; endpoint is estimated"):controller.error,success?"DEPLOYED":"WARNING","Net Deployments");
   if(hasRunning&&String(running.type)=="DEPLOY"){ack(running,success?"completed":"failed",success?(USE_LIMITS?"Net endpoint confirmed":"Motor cycle complete; position estimated"):controller.error);hasRunning=false;}
  }
  if(previousNet==Net::Retracting)event(controller.net==Net::Ready?"Net retracted":"Retraction failed",controller.net==Net::Ready?"Retraction cycle completed":controller.error,controller.net==Net::Ready?"SYSTEM":"WARNING","Net Deployments");
  previousNet=controller.net;
 }
 if(hasRunning&&String(running.type)=="DIAGNOSTIC"&&controller.diagnosticDone){bool passed=controller.diagnosticPassed;const char* detail=passed?"ADC sampling, interlocks, LCD and motor leave/return feedback passed":controller.error;diagnosticResult(passed,detail);ack(running,passed?"completed":"failed",detail);hasRunning=false;controller.diagnosticDone=false;}
 if(now-lastDisplay>=250){lastDisplay=now;char line[24];snprintf(line,sizeof(line),"%4d %5.1f cm",in.raw,waterCm(in.raw));lcd.line(0,line);snprintf(line,sizeof(line),"%s %s",tier==2?"DANGER":tier==1?"WARN":"NORMAL",Controller::name(controller.net));lcd.line(1,line);}
 Snapshot s{};s.raw=in.raw;s.cm=waterCm(in.raw);s.power=controller.power;s.temperature=temperatureRead();s.battery=USE_BATTERY?analogReadMilliVolts(BATTERY_PIN)*BATTERY_DIVIDER/1000.0f:0;s.rain=USE_RAIN?analogRead(RAIN_PIN):0;s.aligned=in.aligned;s.sensorReady=in.calibrated;s.lcd=lcd.present;s.limits=USE_LIMITS;s.home=in.retracted;s.end=in.deployed;s.stop=in.stop;s.activity=activityMillis;
 strncpy(s.net,Controller::name(controller.net),sizeof(s.net)-1);strncpy(s.mode,controller.automatic?"automatic":"manual",sizeof(s.mode)-1);
 if(xSemaphoreTake(snapshotMutex,pdMS_TO_TICKS(1))==pdTRUE){snapshot=s;xSemaphoreGive(snapshotMutex);}delay(10);
}
