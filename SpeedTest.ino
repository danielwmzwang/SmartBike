int sensor1 = 3; //assign first hall sensor to pin 3
int val1 = 0; //holds sensor data
float speed = 0; //speed in MPH 
float RPM = 0; //RPM of the wheel
float rps = 0;
float distance = 0;
int count = 0;
float sum = 0;
float avgSpeed = 0.00;
float totalTime = 0.00;
bool started = false;


void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
	pinMode(sensor1, INPUT);
}

void loop() {
  // put your main code here, to run repeatedly:
  val1 = digitalRead(sensor1);
  float time1 = millis();

  while (totalTime<=1.00) {
    int tempval = digitalRead(sensor1);
    if (tempval != val1) {
      float ft = millis();
      float et = (ft-time1)/60000.00;
      Serial.print("time = ");
      Serial.println(et, 4);
      RPM = 1.00/2.00 / et;
      rps = RPM * (2.00 * PI / 60);
      speed = rps * 0.34925 * 2.23694; // rad/sec * radius(meters) * conversion to mph
      distance += speed * (et/60);   
      Serial.print("rpm = ");
      Serial.println(RPM);
      Serial.print("speed = ");
      Serial.println(speed);
      Serial.print("distance = ");
      Serial.println(distance, 4);
      time1 = millis();
      val1 = 1 - val1;
      sum += speed;
      count++;
      avgSpeed = sum / count;
      Serial.print("avgspeed = ");
      Serial.println(avgSpeed);
      if (started) {
        totalTime += et;
      }
      Serial.print("Total Time = ");
      Serial.println(totalTime);
      started = true;
    }
  }
}
