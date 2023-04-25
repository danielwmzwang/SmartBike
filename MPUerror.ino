#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>

Adafruit_MPU6050 mpu;
float AccX, AccY, AccZ;
float GyroX, GyroY, GyroZ;
float accAngleX, accAngleY, gyroAngleX, gyroAngleY, gyroAngleZ;
float AccErrorX, AccErrorY, GyroErrorX, GyroErrorY, GyroErrorZ;
float roll, yaw, pitch;
float elapsedTime, currentTime, previousTime;
int c=0;

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

	// set accelerometer range to +-8G
	mpu.setAccelerometerRange(MPU6050_RANGE_8_G);

	// set gyro range to +- 500 deg/s
	mpu.setGyroRange(MPU6050_RANGE_500_DEG);

	// set filter bandwidth to 21 Hz
	mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);

	delay(100);
}

void loop() {
	sensors_event_t a, g, temp;
	mpu.getEvent(&a, &g, &temp);
  AccX = a.acceleration.x;
  AccY = a.acceleration.y;
  AccZ = a.acceleration.z;
  GyroX = g.gyro.x;
  GyroY = g.gyro.y;
  GyroZ = g.gyro.z;
  GyroX = GyroX; //add error value here to test
  GyroY = GyroY; //add error value here to test
  GyroZ = GyroZ; //add error value here to test
       
  c=0;
  while (c < 1000) {
    // Sum all readings
    AccErrorX = AccErrorX + ((atan((AccY) / sqrt(pow((AccX), 2) + pow((AccZ), 2))) * 180 / PI)); //add error value here to test
    AccErrorY = AccErrorY + ((atan(-1 * (AccX) / sqrt(pow((AccY), 2) + pow((AccZ), 2))) * 180 / PI)); //add error value here to test
    c++;
  }
  AccErrorX = AccErrorX / 1000;
  AccErrorY = AccErrorY / 1000;
  c = 0;

  GyroErrorX = 0;
  while (c < 1000) {
    // Sum all readings
    GyroErrorX = GyroErrorX + (GyroX);
    GyroErrorY = GyroErrorY + (GyroY);
    GyroErrorZ = GyroErrorZ + (GyroZ);
    c++;
  }
  GyroErrorX = GyroErrorX / 1000;
  GyroErrorY = GyroErrorY / 1000;
  GyroErrorZ = GyroErrorZ / 1000;

  Serial.print("AccErrorX= ");
  Serial.println(AccErrorX);
  Serial.print("AccErrorY= ");
  Serial.println(AccErrorY);
  Serial.print("GyroErrorX= ");
  Serial.println(GyroErrorX, 6);
  Serial.print("GyroErrorY= ");
  Serial.println(GyroErrorY, 6);
  Serial.print("GyroErrorZ= ");
  Serial.println(GyroErrorZ, 6);
  //delay(10000);	
}