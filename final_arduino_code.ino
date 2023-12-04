#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>
#include <SoftwareSerial.h>

SoftwareSerial BTserial(10, 11); // TX pin | RX pin

//Set up variables for accelerometer/gyroscope
Adafruit_MPU6050 mpu;
float AccX, AccY, AccZ;
float GyroX, GyroY, GyroZ;
float accAngleX, accAngleY, gyroAngleX, gyroAngleY, gyroAngleZ;
float roll, yaw, pitch;
float elapsedTime, currentTime, previousTime;
float changePitch;
float op = 0;
float crankArm = 0.170;
float rps = 0;
float distance = 0;

int sensor1 = 3; //assign first hall sensor to pin 3
int val1 = 0; //holds sensor data
int val2 = 0; //holds sensor2 data
float speed = 0; //speed in MPH 
float speedC = 0; //Cadence (RPM of the crank)
float RPM = 0; //RPM of the wheel
int sensor2 = 5; //assign second hall sensor to pin 5
bool flip = false; //bool used for activating pitch printing
float errorSum=0; //used to correct MPU error
float pedAcc = 0.00; 
float tempCad = 1.00;
float crps = 0.00; //cadence rad/s
float forceG = 0.00;
float workG = 0.00; //work to overcome Fg
float tempForce = 0.00;
float inertia = 0.00;
float torque = 0.00;
float disInc = 0.00;
float power = 0.00;
float d1 = 0.01;
float d2 = 0.01;
bool S2 = true;

void setup(void) {
	BTserial.begin(9600);

	//Initialize the mpu
	if (!mpu.begin()) {
		BTserial.println("Failed to find MPU6050 chip");
		while (1) {
		  delay(10);
		}
	}
	BTserial.println("MPU6050 Found!");
  pinMode(sensor1, INPUT); //set the hall sensors as input
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
      float et = (ft-time1)/60000.00; //change in time since last switch
      RPM = 1.00/2.00 / et; //wheel rpm
      rps = RPM * (2.00 * PI / 60); //rad/s
      speed = rps * 0.34925 * 2.23694; //mph
      distance += speed * (et/60);  //distance in miles
      BTserial.print("R");
      BTserial.print(RPM);
      delay(10);
      BTserial.print("S");
      BTserial.print(speed);
      delay(10);
      BTserial.print("D");
      BTserial.print(distance, 4);
      time1 = millis(); //reset time variable
      val1 = 1 - val1; //flip to the correct value of switch
    }

    sensors_event_t a, g, temp;
	    mpu.getEvent(&a, &g, &temp); //gets data from mpu
      AccX = a.acceleration.x; //stores accels in variables
      AccY = a.acceleration.y;
      AccZ = a.acceleration.z;
      GyroX = g.gyro.x; //stores gyro values
      GyroY = g.gyro.y;
      GyroZ = g.gyro.z;
      GyroX = GyroX + 0.087; //all 3 corrected for error 
      GyroY = GyroY + 0.01462;
      GyroZ = GyroZ + 0.02;
      
      accAngleY = (atan(-1 * AccX / sqrt(pow(AccY, 2) + pow(AccZ, 2))) * 180 / PI) + 6.40;
      
      previousTime = currentTime; //move current time to previous
      currentTime = millis(); //reset current time
      elapsedTime = (currentTime - previousTime) / 1000; //calc elapsed time
      
      gyroAngleY = gyroAngleY + GyroY * elapsedTime; //Calculation for gyro angle
      
      pitch = 0.96 * gyroAngleY + 0.04 * accAngleY; //accounts for gyro since the bike will be moving, which throws off the angle of accel in y
    
    tempval = digitalRead(sensor2);
    if (tempval != val2) {
      float ft = millis();
      float et = (ft-time2)/60000.00;

      //These conditionals will track the distance so it can be used for work calculation for power
      if (S2) {
        S2 = false;
        d1 = distance;   
      }
      else if (!S2) {
        S2 = true;
        d2 = distance;
      }
      
      speedC = 1.00/2.00 / et; //Cadence RPM
      crps = speedC * (2.00 * PI / 60); //Rad/s
      pedAcc = (crps - tempCad) / (et * 60); //Rad/s/s
      tempCad = crps; //update temp value
      //inertia = (1.00/3.00) * 0.227 * crankArm * crankArm; wrong value, need inertial load. Using 20kgm^2 for average load
      inertia = 1.50;
      torque = abs(inertia * pedAcc); //torque = I*angularAccel
      forceG = sin(pitch * PI / 180) * 84.3 * 9.81; //force of gravity
      disInc = abs(d1 - d2) * 1609.34; //change in distance converted to meters from miles
      workG = forceG * disInc; //work due to incline
      power = (torque * crps) + workG; //power = torque * angular speed

      // BTserial.print("C");
      // BTserial.println(speedC);
      // BTserial.print("P");
      // BTserial.println(power);
      BTserial.print("C");
      BTserial.print(speedC);
      delay(10);
      BTserial.print("P");
      BTserial.print(power);
      time2 = millis();
      val2 = 1 - val2;
      flip = true;
    }


      errorSum += .000679; //value calculated to prevent incrementing 0.000649 -> 0.000709 decrease hovering around 0, then increase
      //712 decreased to -3 degrees in 8 minutes
    if(flip) {  
      delay(10);
      BTserial.print("I");
      //adjust pitch by 11 degrees to offset the incline that the sensor is at while mounted on the bike.
      changePitch = (pitch*90.00/5.50) - 11; //change pitch to be correct units since it gets altered when adjusting to use gyro as well
      BTserial.print(changePitch - errorSum); //subtract the running error to correct incrementing
    }
    flip = false;


  }

}

