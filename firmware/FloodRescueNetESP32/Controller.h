#pragma once
#include <stdint.h>
namespace flood {
enum class Net { Unarmed, Ready, Deploying, Deployed, Retracting, Fault, Diagnosing };
struct Inputs { int raw=0; bool stop=false, limits=false, retracted=false, deployed=false, aligned=true, calibrated=false; };
class Controller {
 public:
  Net net=Net::Unarmed;
  bool automatic=true, diagnosticPassed=false, diagnosticDone=false;
  int direction=0, power=0;
  unsigned long transitions=0;
  const char* error="Local arming required";
  static const char* name(Net n){switch(n){case Net::Unarmed:return "unarmed";case Net::Ready:return "ready";case Net::Deploying:return "deploying";case Net::Deployed:return "deployed";case Net::Retracting:return "retracting";case Net::Diagnosing:return "diagnosing";default:return "fault";}}
  static int tier(int raw){return raw>=2000?2:raw>=880?1:0;}
  bool moving()const{return direction!=0;}
  bool arm(uint32_t now,const Inputs& in){
    if(in.stop||!in.aligned||!in.calibrated||(in.limits&&(!in.retracted||in.deployed))||moving()||net==Net::Diagnosing||net==Net::Deploying||net==Net::Retracting)return false;
    direction=power=0;normalTiming=false;error="";set(Net::Ready,now);return true;
  }
  bool deploy(uint32_t now,uint32_t duration,int thrust,const Inputs& in){
    if(net!=Net::Ready||in.stop||!in.aligned||!in.calibrated||(in.limits&&in.deployed)||duration<1000||duration>10000||thrust<10||thrust>100)return false;
    runMs=duration;power=thrust;direction=1;error="";set(Net::Deploying,now);return true;
  }
  bool diagnostic(uint32_t now,const Inputs& in){
    diagnosticDone=false;diagnosticPassed=false;
    if(net!=Net::Ready||in.stop||!in.aligned||!in.calibrated||!in.limits||!in.retracted||in.deployed)return false;
    phase=0;leftHome=false;direction=1;power=40;set(Net::Diagnosing,now);return true;
  }
  void update(uint32_t now,const Inputs& in){
    if(in.stop){fail("Emergency shutdown open",now);return;}
    if(in.limits&&in.retracted&&in.deployed){fail("Conflicting limit switches",now);return;}
    if((moving()||net==Net::Diagnosing)&&(!in.aligned||!in.calibrated)){fail("Alignment/calibration interlock lost",now);return;}
    if(net==Net::Diagnosing){
      if(phase==0){if(!in.retracted)leftHome=true;if(in.deployed||now-start>=400){direction=power=0;phase=1;start=now;}}
      else if(phase==1&&now-start>=100){direction=-1;power=40;phase=2;start=now;}
      else if(phase==2){
        if(leftHome&&in.retracted){direction=power=0;diagnosticPassed=true;diagnosticDone=true;error="";set(Net::Ready,now);}
        else if(now-start>=800){diagnosticDone=true;fail("Motor test did not leave and return to home switch",now);}
      }
      return;
    }
    if(net==Net::Deploying||net==Net::Retracting){
      bool forward=net==Net::Deploying;
      bool atLimit=in.limits&&(forward?in.deployed:in.retracted);
      if(atLimit||now-start>=runMs){
        direction=power=0;
        if(in.limits&&!atLimit){fail("Motor travel timeout: endpoint not confirmed",now);return;}
        set(forward?Net::Deployed:Net::Ready,now);normalTiming=false;
      }
      return;
    }
    if(!automatic)return;
    if(net==Net::Ready&&tier(in.raw)==2){deploy(now,3000,100,in);return;}
    if(net==Net::Deployed){
      if(tier(in.raw)==0){
        if(!normalTiming){normalTiming=true;normalSince=now;}
        if(now-normalSince>=5000){direction=-1;power=100;runMs=3000;set(Net::Retracting,now);normalTiming=false;}
      } else normalTiming=false;
    }
  }
  void setAutomatic(bool enabled){automatic=enabled;normalTiming=false;}
  void fail(const char* why,uint32_t now){
    if(net==Net::Diagnosing)diagnosticDone=true;
    direction=power=0;error=why;normalTiming=false;
    if(net!=Net::Fault)set(Net::Fault,now);
  }
 private:
  uint32_t start=0,runMs=3000,normalSince=0;bool normalTiming=false,leftHome=false;int phase=0;
  void set(Net value,uint32_t now){net=value;start=now;transitions++;}
};
}
