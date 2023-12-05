package com.example.smartbike.services

/*
Created By: Daniel Wang
Page Purpose:
The purpose of this page is to define the Bluetooth Service
This service handles ALL bluetooth related functions including:
 - Data Reception (from bluetooth)
 - Data Processing (mostly Data Processing Subsystem with edits written by me to transmit data properly)
   > Data Override (in the event the user has stopped pedaling or is braking, etc.)
 - Data Transmission (post-processing, sending to fragments
 */

import android.app.Service
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.Activity
import androidx.core.app.ActivityCompat
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.smartbike.MainActivity
import java.util.*
import kotlin.collections.HashMap

class BluetoothService : Service() {
    //bluetooth related items
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothGattCallback: BluetoothGattCallback? = null

    //"keys" to ready data transmission
    private var dataReady = false;
    private var transmit = false;

    //global variables to store data
    private val arraysMap = mutableMapOf<Char, MutableList<String>>()
    private var iSpeed = 0
    private var iCad = 0
    private var Speed = Array<Double>(5) { 0.0 } //mph
    private var RPM = Array<Double>(5) { 0.0 }
    private var Cad = Array<Double>(5) { 0.0 } //rpm
    private var Inc = Array<Double>(5) { 0.0 } //deg
    private var Power = Array<Double>(5) { 0.0 } //Watt
    private var lastDistance = 0.0 //miles

    //our BLE devices unique and specific address
    private val deviceAddy = "64:69:4E:8C:95:97"

    private var mod: Double = 50.0; //modify reset multiplier - CALIBRATION COMPLETE
    private var crankTime: Long = 0; //stores time for timer to use
    private var lastCrankTime: Long = 0; //stores the last known crank time reset
    private var crankTimer: Timer? = null //the actual timer itself
    private var chainTime: Long = 0; //stores time for timer to use
    private var lastChainTime: Long = 0; //stores last known chain time reset
    private var chainTimer: Timer? = null //the actual timer itself

    //these are just initial functions that set things up
    inner class LocalBinder: Binder(){
        fun getService(): BluetoothService{
            return this@BluetoothService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return LocalBinder()
    }

    //the function called to start/initialize the entire service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //tell Settings that bluetooth is not yet available
        writeStatus("bluetooth", "Not Available")

        //begin initializing bluetooth
        initializeBluetooth()
        return START_STICKY
    }


    //begin initializing bluetooth
    private fun initializeBluetooth(){
        //tell Settings that bluetooth is initializing
        writeStatus("bluetooth", "Initializing")

        //emergency catcher in case the manager is not defined (first time start-up)
        if(bluetoothManager == null)
        {
            bluetoothManager = getSystemService(BluetoothManager::class.java)
        }
        //get the adapter set up from the manager
        bluetoothAdapter = bluetoothManager!!.getAdapter()

        //begin connection
        connectToDevice()
    }

    @SuppressLint("MissingPermission") //all permissions are handled elsewhere
    //this function handles connecting to a device
    fun connectToDevice(){
        //tell Settings that bluetooth is attempting to connect
        writeStatus("bluetooth", "Attempting Connection...")
        //near zero possibility but emergency case where the adapter is somehow undefined
        if(bluetoothAdapter == null)
        {
            Log.e("Device Connection", "An error has occured")
            //go back - DO NOT STOP
            initializeBluetooth()
        }

        //'device' stores the device instance of our BLE device
        val device = bluetoothAdapter!!.getRemoteDevice(deviceAddy)
        //the actual connection is now attempted using said instance using the 'GATT' library
        //please see 'createBluetoothCallback()' below to continue
        bluetoothGatt = device.connectGatt(this, false, createBluetoothGattCallback())
    }


    //all ACTIVE bluetooth actions, etc. are handled here
    @SuppressLint("MissingPermission") //all permissions are handled elsewhere
    private fun createBluetoothGattCallback(): BluetoothGattCallback{
        return object: BluetoothGattCallback(){
            //onConnectionStateChange: triggered when BluetoothService connects/disconnects
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int){
                //newState is the "new state" that the bluetooth connection has entered
                //it will only either be connected or disconnected as shown below
                when(newState){
                    BluetoothProfile.STATE_CONNECTED ->{
                        Log.i("STATE_CONNECTED", "Connected")
                        //tell Settings that bluetooth is connected
                        writeStatus("bluetooth", "Connected")
                        gatt?.discoverServices();
                    }
                    BluetoothProfile.STATE_DISCONNECTED ->{
                        Log.i("STATE_DISCONNECTED", "Disconnected")
                        //tell Settings that bluetooth has disconnected but it is now retrying
                        writeStatus("bluetooth", "Disconnected, Retrying...")
                        //we call the external function from above to start the entire process over
                        //we will also enter this function if connection fails in the first place
                        //therefore, as long as the bluetooth is not connected, we will continue
                        //to keep retrying until we succeed
                        connectToDevice()
                    }
                }
                //initiate the RECEPTION in BluetoothService - this is for the Service to know
                //when to STOP TRANSMITTING DATA due to the user stopping a workout
                //see receiver below to see how it ishandled
                //filter tells the program what to look for
                val filter = IntentFilter("StartTransmission")
                //register the receiver (initiate it)
                registerReceiver(receiver, filter)
            }

            //all incoming transmissions are handled here
            private val receiver = object : BroadcastReceiver(){
                //we only care about receiving thus there is only the reception function
                override fun onReceive(context: Context?, intent: Intent?){
                    //the specific data 'Header' we're looking for is called "data"
                    val data = intent?.getStringExtra("data")
                    //safety
                    if(data!=null)
                    {
                        //set the flag for transmit
                        //true = transmission is OK
                        //false = do NOT TRANSMIT
                        transmit = data=="STARTING"
                    }
                    else
                    {
                        Log.w("[BLUETOOTHSERVICE]", "broadcast receiver got NULL DATA")
                    }

                }
            }

            //this is to read the first "characteristic
            //'characteristics' oversimplified simply are data transmissions/updates
            //the data sent here is ALWAYS DUMPED - this is due to the BLE Device *always* transmitting
            //irrelevant device information at the beginning. therefore, storing this information is useless
            //the data is still output to console though for debugging purposes
            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                Log.i("onCharacteristicRead", "Entered")
                //safety
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    //data read successfully
                    val data = characteristic.value
                    //handle data
                    Log.i("onCharacteristicRead",data.toString())
                }
            }

            //after the first characteristic is read, all subsequent data transmissions are sent here
            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?){
                //safety
                if (gatt != null) {
                    //read value (comes in as a Byte Array
                    val data = characteristic!!.value;
                    //convert to readable/usable Strings
                    val dataString = String(data, Charsets.UTF_8)
                    Log.i("onCharacteristicChanged", dataString);
                    //safety
                    if(dataString!=null && dataString!="")
                    {
                        /*
                        the Chain and Crank timers are two separate independent entities that operate in parallel
                        furthermore, they may be triggered together or separately
                        therefore, there are two identical components but for handling each one
                         */

                        //collect and hold times for CHAIN TIMER
                        if(dataString.first()=='S' || dataString.first()=='R')
                        {
                            //if data is received in chain timer, the data is ready for collection
                            dataReady = true
                            //for subsequent runs
                            if(chainTimer!=null)
                            {
                                //calculates time between now and the last time the switch was hit
                                chainTime = System.currentTimeMillis() - lastChainTime
                            }
                            else //for first run -> 2 seconds
                            {
                                chainTime = 2000L
                            }
                            //record the new "last hit"
                            lastChainTime = System.currentTimeMillis()
                            Log.i("chainTime", "$chainTime")
                            Log.i("sysTime", "${System.currentTimeMillis()}")
                            //reset the timer as we've received data
                            resetChainTimer()

                        }
                        //collect and hold times for CRANK TIMER
                        else if(dataString.first()=='C' || dataString.first()=='P' || dataString.first()=='I')
                        {
                            //if data is received in crank timer, the data is ready for collection
                            dataReady = true
                            //for subsequent runs
                            if(crankTimer!=null)
                            {
                                //calculates time between now and the last time the switch was hit
                                crankTime = System.currentTimeMillis() - lastCrankTime
                            }
                            else //for first run -> 2 seconds
                            {
                                crankTime = 2000L
                            }
                            //record the new "last hit"
                            lastCrankTime = System.currentTimeMillis()
                            Log.i("crankTime", "$crankTime")
                            Log.i("sysTime", "${System.currentTimeMillis()}")
                            //reset the timer as we've received data
                            resetCrankTimer()

                        }
                        //process the data
                        //a try catch is used to prevent the program from ever crashing due to bad data
                        try{
                            processData(dataString)
                        }
                        catch(e: NumberFormatException) //need to track what bad data comes in
                        {
                            e.printStackTrace() //this just prints to console what the crash script *would* be
                            Log.i("processData", "Number Format Exception for: $dataString")
                        }

                    }
                    else
                    {
                        //just to notify if the safety every went off
                        Log.i("onCharacteristicChanged", "ELSE")
                    }
                }


            }

            //[DATA PROCESSING SUBSYSTEM]
            //calculates the average of an array
            fun arrayAvg(Arr: Array<Double>): Double {
                var sum = 0.0
                for (x in 0..4) {
                    sum += Arr[x]
                }
                sum = sum / 5.0
                return sum
            }

            //[MOSTLY DATA PROCESSING SUBSYSTEM]
            fun processData(input: String) {
                Log.i("ProcessingData", "Entered with {$input}")
                if (input.length != 0) {

                    //first "net" to try to catch an MPU reset
                    /*
                    this code has a high probability of failure but is still included as it would
                    (at least in computer terms) significantly expedite efficiency
                    in human terms, this code only makes <1 second of a difference to our eyes
                    the reasoning for the error is due to the MPU device transmitting random
                    symbols and signals whenever it resets. Therefore, significantly lowering
                    the probability of success here
                     */
                    if(input.contains("��"))
                    {
                        Log.i("processData", "MPU Lost")
                        dataReady = false;
                        //an MPU reset has occurred but MPU is STILL LOST
                        //tell settings data transmission is Not Ready
                        writeStatus("data", "Not Ready")
                    }
                    //second "net" to catch an MPU reset
                    /*
                    this code has a 100% probability of success
                    furthermore, this code demonstrates a successful reboot and data ready for transmission
                     */
                    if(input.contains("MPU6050 Found!"))
                    {
                        Log.i("processData", "MPU Found")
                        dataReady = true;
                        //tell status data transmission is ready
                        writeStatus("data", "Ready")
                    }

                    //IMPORTANT: all comments and code within JUST THIS IF STATEMENT
                    //that are marked with [APP] and [ENDAPP] are by me, Daniel Wang
                    //All other comments and code is by the Data Processing Subsystem
                    //[APP] keys necessary to begin transmission - both MUST be TRUE
                    if(transmit && dataReady)
                    {
                        //do stuff
                        val letters = listOf('R', 'S', 'D', 'C', 'P', 'I')
                        //val random = Random()
                        val array = mutableListOf<String>()

                        // Display the randomized array
                        array.forEach { println(it) }

                        val dataText = input
                        val identifier = dataText[0] // Get the first character as the identifier
                        val data = dataText.substring(1) // Get the data after the identifier

                        // Create or retrieve the list associated with the identifier
                        val dataList = arraysMap.getOrPut(identifier) { mutableListOf() }

                        dataList.add(data) // Add the data to the list
                        if (identifier == 'S') {
                            Speed[iSpeed] = data.toDouble()
                            print("Speed: ")
                            println(arrayAvg(Speed))
                            //[APP]: send post-processed data to the Workout Page down to 1 decimal place
                            writeData(String.format("%.1f",arrayAvg(Speed)), "s")
                            //[ENDAPP]
                        } else if (identifier == 'R') {
                            RPM[iSpeed] = data.toDouble()
                            Log.i("RPM","ENTERED RPM")
                            print("RPM: ")
                            println(arrayAvg(RPM))
                            //[APP]: send post-processed data to the Workout Page down to 1 decimal place
                            writeData(String.format("%.1f",arrayAvg(RPM)), "r")
                            //[ENDAPP]
                        } else if (identifier == 'C') {
                            Cad[iCad] = data.toDouble()
                            print("Cad: ")
                            println(arrayAvg(Cad))
                            //[APP]: send post-processed data to the Workout Page down to 1 decimal place
                            writeData(String.format("%.1f",arrayAvg(Cad)), "c")
                            //[ENDAPP]
                        } else if (identifier == 'I') {
                            Inc[iCad] = data.toDouble()
                            print("Inc: ")
                            println(arrayAvg(Inc))
                            //[APP]: send post-processed data to the Workout Page down to 1 decimal place
                            writeData(String.format("%.1f",arrayAvg(Inc)), "i")
                            //[ENDAPP]
                        } else if (identifier == 'P') {
                            Power[iCad] = data.toDouble()
                            print("Power: ")
                            println(arrayAvg(Power))
                            //[APP]: send post-processed data to the Workout Page down to 1 decimal place
                            writeData(String.format("%.1f",arrayAvg(Power)), "p")
                            //[ENDAPP]
                        }
                        else if (identifier == 'D'){
                            lastDistance = data.toDouble()
                            print("Last Distance: ")
                            println(lastDistance)
                            //[APP]: send post-processed data to the Workout Page down to 1 decimal place
                            writeData(String.format("%.1f",lastDistance), "d")
                            //[ENDAPP]
                        }
                    }
                }
            }



            @SuppressLint("MissingPermission") //all permissions are handled elsewhere
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            /*
            this function essentially takes the connected BLE device and starts looking through all of the details
            BLE standard UUID's (Universal Unique Identifier) are, as the name suggests, unique to all devices
            this function searches for the specific ID's needed to READ incoming data
             */
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                Log.i("OnServicesDiscovered", "Entered")
                //safety
                if(status == BluetoothGatt.GATT_SUCCESS){
                    //val service = gatt?.getService(serviceUuid)
                    val serviceList = gatt?.services
                    //safety
                    if (serviceList != null) {
                        //grab the service uuid object
                        for(service in serviceList) {
                            val uuid = service.uuid;
                            Log.i("ServiceList", service.toString());
                            Log.i("ServiceListUUID", service.uuid.toString());
                        }
                    }


                    //specific group of ID's necessary to READ data
                    val serviceUUIDSTR = "0000ffe0-0000-1000-8000-00805f9b34fb";
                    val characterUUIDSTR = "0000ffe1-0000-1000-8000-00805f9b34fb";
                    val descriptorUUIDSTR = "00002901-0000-1000-8000-00805f9b34fb";

                    //grab the service object
                    val service = gatt?.getService(serviceList?.get(2)?.uuid)

                    //safety
                    if(service?.uuid.toString()!=serviceUUIDSTR)
                    {
                        Log.w("SERVICEUUID MISMATCH", service?.uuid.toString());
                        Log.w("SERVICEUUID MISMATCH", serviceUUIDSTR);
                        return;
                    }

                    //grab the character object
                    val character = service?.characteristics?.get(0);

                    //safety
                    if(character?.uuid.toString()!=characterUUIDSTR)
                    {
                        Log.w("characterUUID MISMATCH", character?.uuid.toString());
                        Log.w("characterUUID MISMATCH", characterUUIDSTR);
                        return;
                    }

                    //grab the descriptor object
                    val descriptor = character?.descriptors?.get(1);

                    //safety
                    if(descriptor?.uuid.toString()!=descriptorUUIDSTR)
                    {
                        Log.w("descriptorUUID MISMATCH", descriptor?.uuid.toString());
                        Log.w("descriptorUUID MISMATCH", descriptorUUIDSTR);
                        return;
                    }

                    //enable notifications (aka: constant flow of data coming in
                    gatt?.setCharacteristicNotification(character, true);

                    //successful, print out data just to be safe and visually check
                    Log.i("[BLE Info] serviceUUID", service.toString());
                    Log.i("[BLE Info] characterUUID", character.toString());
                    Log.i("[BLE Info] descriptorUUID", descriptor.toString());

                    Log.i("[BLE Info] serviceUUID", service?.uuid.toString());
                    Log.i("[BLE Info] characterUUID", character?.uuid.toString());
                    Log.i("[BLE Info] descriptorUUID", descriptor?.uuid.toString());

                    //safety
                    if (gatt != null) {
                        //safety
                        if (descriptor != null) {
                            Log.i("OnServicesDiscoveredEnd", "Writing to descriptor")
                            //enable notifications for specific descriptor
                            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            //update the gatt telling it the new descriptor (with enabled notifications)
                            gatt?.writeDescriptor(descriptor)
                            //more debug info
                            Log.i("OnServicesDiscoveredEnd", "Wrote to descriptor")
                            Log.i("OnServicesDiscoveredEnd",descriptor.toString())
                            Log.i("OnServicesDiscoveredEnd",gatt.toString())
                        }
                    }

                }
                else
                {
                    Log.i("onServicesDiscovered", "NOT SUCCESS")
                    Log.i("onServicesDiscovered", BluetoothGatt.GATT_SUCCESS.toString())
                }
            }
        }
    }

    //this function just resets the countdown with whatever new value was provided
    private fun resetChainTimer()
    {
        //stop the old timer (if there is one)
        chainTimer?.cancel()
        //reinitialize the timer
        chainTimer = Timer()
        //give it the new time and an ID (see customTimer for more info
        chainTimer?.schedule(
            customTimer("chain"),
            (chainTime*mod).toLong() //function only accepts Long's
        )
    }

    //this function just resets the countdown with whatever new value was provided
    private fun resetCrankTimer()
    {
        //stop the old timer (if there is one)
        crankTimer?.cancel()
        //reinitialize the timer
        crankTimer = Timer()
        //give it the new time and an ID (see customTimer for more info
        crankTimer?.schedule(
            customTimer("crank"),
            (crankTime*mod).toLong() //function only accepts Long's
        )
    }

    //in the event the service is destroyed, disconnect the device
    @SuppressLint("MissingPermission") //all permissions are handled elsewhere
    override fun onDestroy(){
        super.onDestroy()
        Log.i("onDestroy", "Entered")
        disconnectDevice()
    }

    //function to disconnect the device safely
    @SuppressLint("MissingPermission") //all permissions are handled elsewhere
    fun disconnectDevice(){
        Log.i("disconnectDevice", "Entered")
        if(bluetoothGatt != null)
        {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        }
    }

    //BROADCASTING
    //this is where the Service broadcasts (sender) to other fragments (receiver)
    fun writeData(data: String, dataType: String)
    {
        //define a 'filter' for the fragments to look for
        val intent = Intent("DataTransmission")
        //LOAD the data, the 'name' defined as "data" here tells the receiver what to look for
        intent.putExtra("data", "$dataType:$data")
        //SEND the data
        sendBroadcast(intent)
    }

    //BROADCASTING
    //this is where the Service broadcasts (sender) to other fragments (receiver)
    fun writeStatus(status: String, value: String)
    {
        //define a 'filter' for the fragments to look for
        val intent = Intent("StatusTransmission")
        //LOAD the data, the 'name' defined as "status" here tells the receiver what to look for
        intent.putExtra("status", "$status:$value")
        //SEND the data
        sendBroadcast(intent)
    }

/*
This is a custom timer class i created for the sake of the Crank and Chain timers
all it does is define what to do when the time runs out
 */
    private inner class customTimer(private val id: String): TimerTask(){
        override fun run() {
            //id's define which one has run out of time
            if(id=="crank")
            {
                Log.i("customTimer","Crank Triggered")
                //reset the cadence and power to 0 as no work is being done
                writeData("0.0", "c")
                writeData("0.0", "p")
            }
            if(id=="chain")
            {
                Log.i("customTimer","Chain Triggered")
                //reset the speed and rpm to 0 as no work is being done
                writeData("0.0", "s")
                writeData("0.0", "r")
            }
        }
    }
}