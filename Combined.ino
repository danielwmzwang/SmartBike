#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>
//#include <cstdlib>

Adafruit_MPU6050 mpu;
float AccX, AccY, AccZ;
float GyroX, GyroY, GyroZ;
float accAngleX, accAngleY, gyroAngleX, gyroAngleY, gyroAngleZ;
float AccErrorX, AccErrorY, GyroErrorX, GyroErrorY, GyroErrorZ;
float roll, yaw, pitch;
float elapsedTime, currentTime, previousTime;
float changePitch;
float op = 0;
float crankArm = 0.170;
float rps = 0;
float distance = 0;

int sensor1 = 3;
int val1 = 0;
int val2 = 0;
float speed = 0;
float speedC = 0;
float RPM = 0;
int sensor2 = 5;
float CRPM = 0;
bool flip = false;
int i = 0;
float errorSum=0;
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

void setup(void) {
	Serial.begin(115200);

	// Try to initialize!
	if (!mpu.begin()) {
		Serial.println("Failed to find MPU6050 chip");
		while (1) {
		  delay(10);
		}
	}
	Serial.println("MPU6050 Found!");
  pinMode(sensor1, INPUT);
  pinMode(sensor2, INPUT);  

	// set accelerometer range to +-8G
	mpu.setAccelerometerRange(MPU6050_RANGE_8_G);

	// set gyro range to +- 500 deg/s
	mpu.setGyroRange(MPU6050_RANGE_500_DEG);

	// set filter bandwidth to 21 Hz
	mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);

	delay(100);
}


void loop()
{
	val1 = digitalRead(sensor1);
  val2 = digitalRead(sensor2);
  float time1 = millis();
  float time2 = millis();

  while (1) {
    int tempval = digitalRead(sensor1);
    if (tempval != val1) {
      float ft = millis();
      float et = (ft-time1)/60000.00;
      RPM = 1.00/2.00 / et;
      rps = RPM * (2.00 * PI / 60);
      speed = rps * 0.34925 * 2.23694;
      distance += speed * (et/60);   
      Serial.print("rpm = ");
      Serial.println(RPM);
      Serial.print("speed = ");
      Serial.println(speed, 4);
      Serial.print("distance = ");
      Serial.println(distance, 4);
      time1 = millis();
      val1 = 1 - val1;
      //flip = true;
    }

    sensors_event_t a, g, temp;
	    mpu.getEvent(&a, &g, &temp);
      AccX = a.acceleration.x;
      AccY = a.acceleration.y;
      AccZ = a.acceleration.z;
      GyroX = g.gyro.x;
      GyroY = g.gyro.y;
      GyroZ = g.gyro.z;
      GyroX = GyroX + 0.087;
      GyroY = GyroY + 0.015;
      GyroZ = GyroZ + 0.02;
      
      accAngleX = (atan(AccY / sqrt(pow(AccX, 2) + pow(AccZ, 2))) * 180 / PI) - 0.66;
      accAngleY = (atan(-1 * AccX / sqrt(pow(AccY, 2) + pow(AccZ, 2))) * 180 / PI) + 6.40;
      
      previousTime = currentTime;
      currentTime = millis();
      elapsedTime = (currentTime - previousTime) / 1000;
      
      gyroAngleX = gyroAngleX + GyroX * elapsedTime;
      gyroAngleY = gyroAngleY + GyroY * elapsedTime;
      
      yaw =  yaw + GyroZ * elapsedTime;
      roll = 0.96 * gyroAngleX + 0.04 * accAngleX;
      pitch = 0.96 * gyroAngleY + 0.04 * accAngleY;

    tempval = digitalRead(sensor2);
    if (tempval != val2) {
      float ft = millis();
      float et = (ft-time2)/60000.00;
      if (S2) {
        S2 = false;
        d1 = distance;        
      }
      if (!S2) {
        S2 = true;
        d2 = distance;
      }
      speedC = 1.00/2.00 / et;
      crps = speedC * (2.00 * PI / 60);
      pedAcc = (crps - tempCad) / (et * 60);
      tempCad = crps;
      //inertia = (1.00/3.00) * 0.227 * crankArm * crankArm;
      inertia = 20.00;
      torque = abs(inertia * pedAcc);
      forceG = sin(pitch * PI / 180) * 84.3 * 9.81; //force of gravity
      disInc = abs(d1 - d2) * 1609.34;
      workG = forceG * disInc;
      power = (torque * crps) + workG;

      Serial.print("crpm = ");
      Serial.println(speedC);
      Serial.print("power = ");
      Serial.println(power);
      time2 = millis();
      val2 = 1 - val2;
      flip = true;
    }

    errorSum += .0006070; 

    if(flip) {  
      Serial.print("incline angle = ");
      changePitch = pitch*90.00/5.50;
      Serial.println(changePitch - errorSum);
    }
    flip = false;


  }

}

