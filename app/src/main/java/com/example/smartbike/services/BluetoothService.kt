package com.example.smartbike.services

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
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothGattCallback: BluetoothGattCallback? = null

    //
    //

    private var dataReady = false;
    private var transmit = false;

    private val arraysMap = mutableMapOf<Char, MutableList<String>>()
    private var iSpeed = 0
    private var iCad = 0
    private var Speed = Array<Double>(5) { 0.0 } //mph
    private var RPM = Array<Double>(5) { 0.0 }
    private var Cad = Array<Double>(5) { 0.0 } //rpm
    private var Inc = Array<Double>(5) { 0.0 } //deg
    private var Power = Array<Double>(5) { 0.0 } //Watt
    private var lastDistance = 0.0 //miles

    private val deviceAddy = "64:69:4E:8C:95:97"



    private var mod: Double = 50.0; //modify reset multiplier
    private var crankTime: Long = 0;
    private var lastCrankTime: Long = 0;
    private var crankTimer: Timer? = null
    private var chainTime: Long = 0;
    private var lastChainTime: Long = 0;
    private var chainTimer: Timer? = null

    inner class LocalBinder: Binder(){
        fun getService(): BluetoothService{
            return this@BluetoothService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        //Log.i("onBind", "Entered")
        return LocalBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Log.i("onStartCommand", "Entered")
        writeStatus("bluetooth", "Not Available")
        initializeBluetooth()
        return START_STICKY
    }


    private fun initializeBluetooth(){
        //Log.i("initializeBluetooth", "Entered")
        writeStatus("bluetooth", "Initializing")
        if(bluetoothManager == null)
        {
            bluetoothManager = getSystemService(BluetoothManager::class.java)
        }
        bluetoothAdapter = bluetoothManager!!.getAdapter()
        connectToDevice()
    }

    @SuppressLint("MissingPermission") //all permissions are handled elsewhere
    fun connectToDevice(){
        writeStatus("bluetooth", "Attempting Connection...")
        if(bluetoothAdapter == null)
        {
            Log.e("Device Connection", "An error has occured")
            return
        }

        //Log.i("connecToDevice", "Attempting Connection Now...")
        val device = bluetoothAdapter!!.getRemoteDevice(deviceAddy)
        //Log.i("connecToDevice", "Device Retrieved: $device")
        bluetoothGatt = device.connectGatt(this, false, createBluetoothGattCallback())
        //Log.i("connecToDevice", "Gatt Connected(?)")
    }



    @SuppressLint("MissingPermission") //all permissions are handled elsewhere
    private fun createBluetoothGattCallback(): BluetoothGattCallback{
        return object: BluetoothGattCallback(){
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int){
                //Log.i("[SERVICE]onConnectionStateChange", "Entered")
                when(newState){
                    BluetoothProfile.STATE_CONNECTED ->{
                        Log.i("STATE_CONNECTED", "Connected")

                        //fragments["settings"].onDataReceived()

                        //btstatusText.text = "Bluetooth Status: Connected"
                        //btipText.text = "Connected To: 64:69:4E:8C:95:97"
                        //Log.i("STATE_CONNECTED", "PERMISSION GRANTED, DISCOVERING SERVICES")
                        writeStatus("bluetooth", "Connected")
                        gatt?.discoverServices();
                    }
                    BluetoothProfile.STATE_DISCONNECTED ->{
                        //btstatusText.text = "Bluetooth Status: Disconnected"
                        //btipText.text = "Connected To: N/A"
                        //btPermText.text = "Permission: N/A"
                        //dataStatTet.text = "Data Status: Not Ready"
                        Log.i("STATE_DISCONNECTED", "Disconnected")
                        writeStatus("bluetooth", "Disconnected, Retrying...")
                        connectToDevice()
                    }
                }
                val filter = IntentFilter("StartTransmission")
                registerReceiver(receiver, filter)
            }

            private val receiver = object : BroadcastReceiver(){
                override fun onReceive(context: Context?, intent: Intent?){
                    val data = intent?.getStringExtra("data")
                    if(data!=null)
                    {
                        transmit = data=="STARTING"
                    }
                    else
                    {
                        Log.w("[BLUETOOTHSERVICE]", "broadcast receiver got NULL DATA")
                    }

                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                Log.i("onCharacteristicRead", "Entered")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Characteristic read successfully.
                    val data = characteristic.value
                    // Handle the data here.
                    Log.i("onCharacteristicRead",data.toString())
                }
            }


            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?){

                if (gatt != null) {
                    //val data = gatt.readCharacteristic(characteristic)
                    val data = characteristic!!.value;
                    val dataString = String(data, Charsets.UTF_8)
                    Log.i("onCharacteristicChanged", dataString);
                    if(dataString!=null && dataString!="")
                    {
                        //collect and hold times
                        if(dataString.first()=='S' || dataString.first()=='R')
                        {
                            dataReady = true
                            if(chainTimer!=null)
                            {
                                chainTime = System.currentTimeMillis() - lastChainTime
                            }
                            else
                            {
                                chainTime = 2000L
                            }
                            lastChainTime = System.currentTimeMillis()
                            Log.i("chainTime", "$chainTime")
                            Log.i("sysTime", "${System.currentTimeMillis()}")
                            resetChainTimer()

                        }
                        else if(dataString.first()=='C' || dataString.first()=='P' || dataString.first()=='I')
                        {
                            dataReady = true
                            if(crankTimer!=null)
                            {
                                crankTime = System.currentTimeMillis() - lastCrankTime
                            }
                            else
                            {
                                crankTime = 2000L
                            }
                            lastCrankTime = System.currentTimeMillis()
                            Log.i("crankTime", "$crankTime")
                            Log.i("sysTime", "${System.currentTimeMillis()}")
                            resetCrankTimer()

                        }
                        //process the data
                        try{
                            processData(dataString)
                        }catch(e: NumberFormatException)
                        {
                            e.printStackTrace()
                            Log.i("processData", "Number Format Exception for: $dataString")
                        }

                    }
                    else
                    {
                        //Log.i("onCharacteristicChanged", "ELSE")
                    }
                }


            }

            fun arrayAvg(Arr: Array<Double>): Double {
                var sum = 0.0
                for (x in 0..4) {
                    sum += Arr[x]
                }
                sum = sum / 5.0
                return sum
            }

            fun processData(input: String) {
                Log.i("ProcessingData", "Entered with {$input}")
                if (input.length != 0) {
                    if(input.contains("��"))
                    {
                        Log.i("processData", "MPU Lost")
                        dataReady = false;
                        writeStatus("data", "Not Ready")
                        //TODO
                        /*
                        mpuText.text = "MPU: LOST";

                         */
                    }
                    if(input.contains("MPU6050 Found!"))
                    {
                        Log.i("processData", "MPU Found")
                        dataReady = true;
                        writeStatus("data", "Ready")
                    }

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
                            writeData(String.format("%.1f",arrayAvg(Speed)), "s")
                        } else if (identifier == 'R') {
                            RPM[iSpeed] = data.toDouble()
                            Log.i("RPM","ENTERED RPM")
                            print("RPM: ")
                            println(arrayAvg(RPM))
                            writeData(String.format("%.1f",arrayAvg(RPM)), "r")
                        } else if (identifier == 'C') {
                            Cad[iCad] = data.toDouble()
                            print("Cad: ")
                            println(arrayAvg(Cad))
                            //to screen
                            writeData(String.format("%.1f",arrayAvg(Cad)), "c")
                        } else if (identifier == 'I') {
                            Inc[iCad] = data.toDouble()
                            print("Inc: ")
                            println(arrayAvg(Inc))
                            writeData(String.format("%.1f",arrayAvg(Inc)), "i")
                        } else if (identifier == 'P') {
                            Power[iCad] = data.toDouble()
                            print("Power: ")
                            println(arrayAvg(Power))
                            writeData(String.format("%.1f",arrayAvg(Power)), "p")
                        }
                        else if (identifier == 'D'){
                            lastDistance = data.toDouble()
                            print("Last Distance: ")
                            println(lastDistance)
                            writeData(String.format("%.1f",lastDistance), "d")
                        }
                    }
                }
            }



            @SuppressLint("MissingPermission") //all permissions are handled elsewhere
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                Log.i("OnServicesDiscovered", "Entered")
                if(status == BluetoothGatt.GATT_SUCCESS){
                    //val service = gatt?.getService(serviceUuid)
                    val serviceList = gatt?.services
                    if (serviceList != null) {
                        for(service in serviceList) {
                            val uuid = service.uuid;
                            Log.i("ServiceList", service.toString());
                            Log.i("ServiceListUUID", service.uuid.toString());
                        }
                    }

                    /*
                    val serviceUUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
                    val characterUUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
                    val char = gatt?.getService(serviceUUID)?.getCharacteristic(characterUUID);
                    */

                    val serviceUUIDSTR = "0000ffe0-0000-1000-8000-00805f9b34fb";
                    val characterUUIDSTR = "0000ffe1-0000-1000-8000-00805f9b34fb";
                    val descriptorUUIDSTR = "00002901-0000-1000-8000-00805f9b34fb";

                    val service = gatt?.getService(serviceList?.get(2)?.uuid)

                    if(service?.uuid.toString()!=serviceUUIDSTR)
                    {
                        Log.w("SERVICEUUID MISMATCH", service?.uuid.toString());
                        Log.w("SERVICEUUID MISMATCH", serviceUUIDSTR);
                        return;
                    }

                    val character = service?.characteristics?.get(0);



                    if(character?.uuid.toString()!=characterUUIDSTR)
                    {
                        Log.w("characterUUID MISMATCH", character?.uuid.toString());
                        Log.w("characterUUID MISMATCH", characterUUIDSTR);
                        return;
                    }



                    val descriptor = character?.descriptors?.get(1);
                    if(descriptor?.uuid.toString()!=descriptorUUIDSTR)
                    {
                        Log.w("descriptorUUID MISMATCH", descriptor?.uuid.toString());
                        Log.w("descriptorUUID MISMATCH", descriptorUUIDSTR);
                        return;
                    }

                    gatt?.setCharacteristicNotification(character, true);


                    //successful

                    Log.i("[BLE Info] serviceUUID", service.toString());
                    Log.i("[BLE Info] characterUUID", character.toString());
                    Log.i("[BLE Info] descriptorUUID", descriptor.toString());

                    Log.i("[BLE Info] serviceUUID", service?.uuid.toString());
                    Log.i("[BLE Info] characterUUID", character?.uuid.toString());
                    Log.i("[BLE Info] descriptorUUID", descriptor?.uuid.toString());

                    Log.i("PostPermission", "Permission Granted?");
                    if (gatt != null) {
                        if (descriptor != null) {
                            //gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            Log.i("OnServicesDiscoveredEnd", "Writing to descriptor")
                            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt?.writeDescriptor(descriptor)
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

    private fun resetChainTimer()
    {
        //Log.i("resetChainTimer", "CHAIN RESET")
        chainTimer?.cancel()
        chainTimer = Timer()
        chainTimer?.schedule(
            customTimer("chain"),
            (chainTime*mod).toLong()
        )
    }

    private fun resetCrankTimer()
    {
        //Log.i("resetCrankTimer", "CRANK RESET")
        crankTimer?.cancel()
        crankTimer = Timer()
        crankTimer?.schedule(
            customTimer("crank"),
            (crankTime*mod).toLong()
        )
    }

    @SuppressLint("MissingPermission") //all permissions are handled elsewhere
    override fun onDestroy(){
        super.onDestroy()
        Log.i("onDestroy", "Entered")
        disconnectDevice()
    }

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

    fun writeData(data: String, dataType: String)
    {
        //Log.i("writeData", "data: $data, dataType: $dataType")
        val intent = Intent("DataTransmission")
        intent.putExtra("data", "$dataType:$data")
        sendBroadcast(intent)
        //Log.i("writeData", "Complete")
        /*
        try{
            fragments["home"]?.onDataReceived(data, dataType)
        }
        catch (e: NullPointerException)
        {

        }

         */
    }

    fun writeStatus(status: String, value: String)
    {
        Log.i("writeStatus", "status: $status, value: $value")
        val intent = Intent("StatusTransmission")
        intent.putExtra("status", "$status:$value")
        sendBroadcast(intent)
        Log.i("writeStatus", "Complete")
    }


    private inner class customTimer(private val id: String): TimerTask(){
        override fun run() {
            if(id=="crank")
            {
                Log.i("customTimer","Crank Triggered")
                //TODO
                writeData("0.0", "c")
                //writeData("0.0", "i")
                writeData("0.0", "p")

                /*
                cadText.text = "Cadence: 0.0 RPM"
                incText.text = "Incline: 0.0 Degrees"
                pwrText.text = "Power: 0.0 Watts"
                */
            }
            if(id=="chain")
            {
                Log.i("customTimer","Chain Triggered")
                //TODO
                writeData("0.0", "s")
                writeData("0.0", "r")
            /*
                speedText.text = "Speed: 0.0 MPH"
                rpmText.text = "RPM: 0.0 RPM"

                 */
            }
        }

        fun fullReset(){

            //TODO
            Log.i("BLEService", "Full Reset")

            /*
            cadText.text = "Cadence: 0.0 RPM"
            incText.text = "Incline: 0.0 Degrees"
            pwrText.text = "Power: 0.0 Watts"
            speedText.text = "Speed: 0.0 MPH"
            rpmText.text = "RPM: 0.0 RPM"
            distText.text = "Distance: 0.0 Miles"
             */
        }

    }

    /*
    interface DataCallback{
        fun onDataReceived(data: String){

        }

        abstract fun setCallback(bluetoothService: BluetoothService) //TODO
    }
    */

}

/*
Data Delivery Structure:

string:
[data:code] IncomingData
where code:
 -
[
 */
