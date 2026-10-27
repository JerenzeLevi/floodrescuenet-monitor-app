#include "../FloodRescueNetESP32/Controller.h"
#include <cassert>
#include <iostream>
using namespace flood;
int main(){
 Inputs in;in.calibrated=true;
 {Controller c;in.raw=2200;c.update(1,in);assert(c.net==Net::Unarmed);assert(!c.moving());}
 {assert(Controller::tier(879)==0);assert(Controller::tier(880)==1);assert(Controller::tier(1999)==1);assert(Controller::tier(2000)==2);}
 {Controller c;assert(c.arm(0,in));c.update(1,in);assert(c.net==Net::Deploying);c.update(3000,in);assert(c.moving());c.update(3001,in);assert(c.net==Net::Deployed);c.update(6000,in);assert(c.net==Net::Deployed);}
 {Controller c;in.raw=0;c.arm(0,in);c.deploy(0,1000,50,in);c.update(1000,in);c.update(1001,in);c.update(6000,in);assert(c.net==Net::Deployed);c.update(6001,in);assert(c.net==Net::Retracting);c.update(9001,in);assert(c.net==Net::Ready);}
 {Controller c;in.raw=0;c.arm(0,in);c.deploy(0,1000,100,in);c.update(1000,in);c.update(1001,in);in.raw=880;c.update(5000,in);in.raw=0;c.update(6000,in);c.update(10000,in);assert(c.net==Net::Deployed);c.update(11000,in);assert(c.net==Net::Retracting);}
 {Controller c;in.raw=2200;c.arm(0,in);c.setAutomatic(false);c.update(1,in);assert(c.net==Net::Ready);assert(c.deploy(2,2000,35,in));assert(c.power==35);in.stop=true;c.update(3,in);assert(c.net==Net::Fault&&!c.moving());assert(!c.arm(4,in));in.stop=false;}
 {Controller c;in.raw=0;in.limits=true;in.retracted=true;c.arm(0,in);c.deploy(1,1000,100,in);in.retracted=false;c.update(1001,in);assert(c.net==Net::Fault);assert(!c.moving());}
 {Controller c;in.retracted=true;in.deployed=false;c.arm(0,in);c.deploy(1,3000,100,in);in.retracted=false;in.deployed=true;c.update(50,in);assert(c.net==Net::Deployed&&!c.moving());}
 {Controller c;in.deployed=false;in.retracted=true;c.arm(0,in);assert(c.diagnostic(1,in));in.retracted=false;c.update(100,in);c.update(401,in);assert(!c.moving());assert(!c.arm(450,in));c.update(501,in);assert(c.direction==-1);in.retracted=true;c.update(600,in);assert(c.diagnosticDone&&c.diagnosticPassed&&c.net==Net::Ready);}
 {Controller c;in.retracted=true;c.arm(0,in);c.diagnostic(1,in);c.update(401,in);c.update(501,in);c.update(1301,in);assert(c.diagnosticDone&&!c.diagnosticPassed&&c.net==Net::Fault);}
 {Controller c;in.limits=false;in.retracted=false;c.arm(0,in);assert(!c.deploy(0,999,100,in));assert(!c.deploy(0,1000,0,in));assert(!c.diagnostic(0,in));}
 {Controller c;in.raw=0;c.arm(0xfffffff0u,in);c.deploy(0xfffffff0u,1000,100,in);c.update(984u,in);assert(c.net==Net::Deployed);}
 {Controller c;Inputs d;d.calibrated=true;d.limits=true;d.retracted=true;c.arm(0,d);c.diagnostic(1,d);d.retracted=false;c.update(401,d);d.aligned=false;c.update(501,d);assert(c.net==Net::Fault&&!c.moving()&&c.diagnosticDone);}
 {Controller c;Inputs d;d.calibrated=true;d.limits=true;d.retracted=true;c.arm(0,d);c.diagnostic(1,d);d.retracted=false;d.deployed=true;c.update(50,d);assert(!c.moving());d.deployed=false;c.update(150,d);assert(c.direction==-1);d.retracted=true;c.update(200,d);assert(c.diagnosticPassed);}
 std::cout<<"PASS: 14 controller scenarios (boundaries, timing, retraction reset, interlocks, feedback, diagnostics, rollover)\n";
}
