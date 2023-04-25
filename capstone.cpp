#include <random>
#include <iostream>
#include <fstream>
#include <cmath>
using namespace std;


float crankArm = 0.170;
float rps = 0.00;
float dist = 0.00;
float speed = 0.00; //speed in MPH 
float speedC = 0.00; //Cadence (RPM of the crank)
float RPM = 0.00; //RPM of the wheel
float pedAcc = 0.00;
float tempCad = 1.00;
float crps = 0.00;
float forceG = 0.00;
float workG = 0.00;
float tempForce = 0.00;
float inertia = 0.00;
float torque = 0.00;
float disInc = 0.00;
float power = 0.00;
float d1 = 0.01;
float d2 = 0.01;
bool S2 = true;

int main() {
  ofstream et_file;
  ofstream et2_file;
  ofstream rpm_file;
  ofstream speed_file;
  ofstream dist_file;
  ofstream cad_file;
  ofstream power_file;
  ofstream pitch_file;
  ofstream combined_file;

  et_file.open("et_file.txt");
  et2_file.open("et2_file.txt");
  rpm_file.open("rpm_file.txt");
  speed_file.open("speed_file.txt");
  dist_file.open("dist_file.txt");
  cad_file.open("cad_file.txt");
  power_file.open("power_file.txt");
  pitch_file.open("pitch_file.txt");
  combined_file.open("combined_file.txt");



  for(int i = 0; i < 100; i++) {
    //float et = dis(gen);
    float et = 0.004 + static_cast <float> (rand()) /( static_cast <float> (RAND_MAX/(0.008-0.004)));
    //cout << et << "\n"; 
    RPM = 1.00/2.00 / et; 
    rps = RPM * (2.00 * 3.14159265 / 60);
    speed = rps * 0.34925 * 2.23694;
    dist += speed * (et/60);   
    cout << "time = " << et << "\n";
    cout << "rpm = " << RPM << "\n";
    cout << "speed = " << speed << "\n";
    cout << "dist = " << dist << "\n";

    et_file << "time = " << et << "\n";
    rpm_file << "rpm = " << RPM << "\n";
    speed_file << "speed = " << speed << "\n";
    dist_file << "dist = " << dist << "\n";
      

    
      float et2 = 0.008 + static_cast <float> (rand()) /( static_cast <float> (RAND_MAX/(0.020-0.008)));

      //These conditionals will track the dist so it can be used for work calculation for power
      if (S2) {
        S2 = false;
        d1 = dist;   
      }
      else if (!S2) {
        S2 = true;
        d2 = dist;
      }
      float pitch = 0.00 + static_cast <float> (rand()) /( static_cast <float> (RAND_MAX/(10.00-0.00)));
      
      speedC = 1.00/2.00 / et2;
      crps = speedC * (2.00 * 3.14159265 / 60); //rpm*0.1047198
      pedAcc = ((crps - tempCad) / (et2 * 60)) * crankArm; //25.6362(2.6846) - 41.821(4.3795) / 0.0195036(1.170216s) = 1.44831rad/s^2
      tempCad = crps;
      //inertia = (1.00/3.00) * 0.227 * crankArm * crankArm; // this is wrong
      inertia = 20.00; //average inertial load on crank
      torque = abs(inertia * pedAcc); //0.00218677*1.44831= 0.00316712, kg*m^2 * rad/s^2 = Nm
      forceG =  sin(pitch * 3.14159265 / 180) * 84.3 * 9.81; //826.983 N
      disInc = abs(d1 - d2) * 1609.34; //d1 = 0.0627228, d2 = 0.0647681 , 3.29159128 m
      workG = forceG * disInc; //827*0.949=784
      power = (torque * crps) + workG; //(0.00316712 * 2.6846) = 0.0085 + 
      

      cout << "time2 = " << et2 << "\n";
      cout << "Cadence = " << speedC << "\n";
      cout << "Power = " << power << "\n";
      cout << "Pitch = " << pitch << "\n";


      et2_file << "time2 = " << et2 << "\n";
      cad_file << "Cadence = " << speedC << "\n";
      power_file << "Power = " << power << "\n";
      pitch_file << "Pitch = " << pitch << "\n";
    

  }

  et_file.close();
  et2_file.close();
  rpm_file.close();
  speed_file.close();
  dist_file.close();
  cad_file.close();
  power_file.close();
  pitch_file.close();

}

