package com.example.smartbike.ui.bluetooth

/*
Created By: Daniel Wang
Page Purpose:
This page was initially created to configure and test the Bluetooth data transmission/processing
All functions on here look similar to BluetoothService as this page is, in fact, the older version
This page was kept, however, as it contained less clutter and was much more reasonable for the
other subsystems to use to diagnose their own issues. Therefore, this page is kept and, on occasion,
re-initialized to the front screen for other systems to perform debugging.

Most functions here were eventually migrated to the Service and improved over there.
Therefore, as this file is kept for technical merit and the improved version is in BluetoothService,
not much comments will be included here.
 */

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanCallback.SCAN_FAILED_ALREADY_STARTED
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.smartbike.R
import com.example.smartbike.databinding.FragmentBluetoothBinding
import com.example.smartbike.databinding.FragmentDashboardBinding
import java.io.*
import java.nio.charset.Charset
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

class BluetoothFragment : Fragment() {

    private var _binding: FragmentBluetoothBinding? = null
    private val SCAN_PERIOD: Long = 10000
    private val BLEID = "64:69:4E:8C:95:97"

    private var connected = false;
    private var mpuFound = false;
    private var permissions = false;
    private var ready = false;
    private var start = false;

    private lateinit var btstatusText: TextView;
    private lateinit var btipText: TextView;
    private lateinit var btPermText: TextView;
    private lateinit var mpuText: TextView;
    private lateinit var dataStatTet: TextView;
    private lateinit var speedText: TextView;
    private lateinit var rpmText: TextView;
    private lateinit var cadText: TextView;
    private lateinit var incText: TextView;
    private lateinit var pwrText: TextView;
    private lateinit var distText: TextView;
    private lateinit var btn: Button;

    private var avgSpeed = 0.0;
    private var speedDP = 0;
    private var avgRPM = 0.0;
    private var rpmDP = 0;
    private var avgCad = 0.0;
    private var cadDP = 0;
    private var avgInc = 0.0;
    private var incDP = 0;
    private var avgPWR = 0.0;
    private var pwrDP = 0;
    private var totDistance = 0.0;

    private var startTime: Long = 0;
    private var endTime: Long = 0;

    private var mod: Double = 50.0; //modify reset multiplier
    private var crankTime: Long = 0;
    private var lastCrankTime: Long = 0;
    private var crankTimer: Timer? = null
    private var chainTime: Long = 0;
    private var lastChainTime: Long = 0;
    private var chainTimer: Timer? = null

    companion object {
        fun newInstance() = BluetoothFragment()
    }

    private lateinit var viewModel: BluetoothViewModel



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)

        val binding = _binding!!
        //displays that showed, on page, the values being given
        btstatusText = binding.textViewBluetoothStatus
        btipText = binding.textViewBluetoothConnector
        btPermText = binding.textViewBluetoothPermission
        mpuText = binding.textViewMPUFound
        dataStatTet = binding.textViewDataStatus
        speedText = binding.textViewRTDSpeed
        rpmText = binding.textViewRTDRPM
        cadText = binding.textViewRTDCadence
        incText = binding.textViewRTDIncline
        pwrText = binding.textViewRTDPower
        distText = binding.textViewRTDDistance
        btn = binding.startButton

        btn.setOnClickListener{

            //data ready locks and key
            if(connected && mpuFound && permissions)
            {
                ready = true
            }
            //start button
            if(!start)
            {
                Log.i("StartBtn", "If0")
                Log.i("StartBtn", "start, ready, connected, mpuFound, permissions")
                Log.i("StartBtn", "$start, $ready, $connected, $mpuFound, $permissions")
                //reset
                if(ready)
                {
                    Log.i("StartBtn", "Ready")
                    start = !start
                    btn.text = "Stop"
                    startTime = System.currentTimeMillis()

                    cadText.text = "Cadence: 0.0 RPM"
                    incText.text = "Incline: 0.0 Degrees"
                    pwrText.text = "Power: 0.0 Watts"
                    speedText.text = "Speed: 0.0 MPH"
                    rpmText.text = "RPM: 0.0 RPM"
                    distText.text = "Distance: 0.0 Miles"
                }
                else if(!connected)
                {
                    toaster("Not Connected!")
                }
                else if(!mpuFound)
                {
                    toaster("MPU Not Found!")
                }
                else if(!permissions)
                {
                    toaster("Permissions not granted!")
                }
            }
            else
            {
                //end button
                Log.i("StartBtn", "If1")
                start = !start
                btn.text = "Start"
                endTime = System.currentTimeMillis()
                //write to file
                writeToFile()

                startTime = 0
            }
        }

        //initiate bluetooth related variables/values

        val root: View = binding.root

        val bluetoothManager: BluetoothManager = requireActivity().getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.getAdapter()

        //safety
        if(bluetoothAdapter==null)
        {
            return root
        }
        //safety
        if(bluetoothAdapter?.isEnabled == false) {
            Log.i("isEnabled", "bluetooth is NOT enabled")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            var resultLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        //happynoises
                        Log.i("isEnabled", "bluetooth should? be enabled?")
                    }
                }

            Log.i("isEnabled", "launching resultlauncher")
            resultLauncher.launch(enableBtIntent)
            Log.i("isEnabled", "result launcher launched")
        }
        else
        {
            Log.i("isEnabled", "bluetooth should be enabled")
        }

        //update status
        btstatusText.text = "Bluetooth Status: Enabled"

        //begin conenction attempts
        var bluetoothGatt: BluetoothGatt? = null
        val deviceAddy = "64:69:4E:8C:95:97"
        Log.i("Connection", "Begin Connection attempt")

        //grab device
        val device = bluetoothAdapter.getRemoteDevice(deviceAddy)

        Log.i("Connection", "Got remote device")
        Log.i("Connection", device.toString())

        //check permissions
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("Bluetooth_Connect Permissions", "NEED PERMISSION")
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1234
            )
        }
        Log.i("Bluetooth_Connect Permissions", "PERMISSION GRANTED")

        permissions = true;
        btPermText.text = "Permission: Granted"

        Log.i("gattCallback", "Entering");

        //prepare for process data
        val arraysMap = mutableMapOf<Char, MutableList<String>>()
        var iSpeed = 0
        var iCad = 0
        var Speed = Array<Double>(5) { 0.0 } //mph
        var RPM = Array<Double>(5) { 0.0 }
        var Cad = Array<Double>(5) { 0.0 } //rpm
        var Inc = Array<Double>(5) { 0.0 } //deg
        var Power = Array<Double>(5) { 0.0 } //Watt
        var lastDistance = 0.0 //miles


        val gattCallback = object: BluetoothGattCallback(){
            //this section is nearly identical to BLuetoothService
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int){
                Log.i("onConnectionStateChange", "Entered")
                when(newState){
                    BluetoothProfile.STATE_CONNECTED ->{
                        Log.i("STATE_CONNECTED", "Connected")
                        if (ActivityCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {

                            Log.i("STATE_CONNECTED", "NEED PERMISSION")

                            ActivityCompat.requestPermissions(
                                requireActivity(),
                                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                                6969
                            )

                        }
                        btstatusText.text = "Bluetooth Status: Connected"
                        btipText.text = "Connected To: 64:69:4E:8C:95:97"
                        Log.i("STATE_CONNECTED", "PERMISSION GRANTED, DISCOVERING SERVICES")
                        connected = true;
                        gatt?.discoverServices();
                    }
                    BluetoothProfile.STATE_DISCONNECTED ->{
                        btstatusText.text = "Bluetooth Status: Disconnected"
                        btipText.text = "Connected To: N/A"
                        btPermText.text = "Permission: N/A"
                        dataStatTet.text = "Data Status: Not Ready"
                        connected = false;
                        mpuFound = false;
                        permissions = false;
                        Log.i("STATE_DISCONNECTED", "Disconnected")
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
                            //e.printStackTrace()
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
                        mpuFound = false;
                        mpuText.text = "MPU: LOST";
                    }
                    if(input.contains("MPU6050 Found!"))
                    {
                        Log.i("processData", "MPU Found")
                        mpuFound = true;
                        mpuText.text = "MPU: Found!"
                    }
                    if(!ready)
                    {

                        if(/*!mpuFound ||*/ !connected || !permissions)
                        {
                            return;
                        }
                        else if(/*mpuFound && */connected && permissions)
                        {
                            Log.i("processData", "DATA READY")
                            dataStatTet.text = "Data Status: Ready"
                            ready = true;
                        }
                    }
                    if(!start)
                    {
                        return;
                    }
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
                        //to screen
                        speedText.text = "Speed: " + String.format("%.1f",arrayAvg(Speed)) + " MPH"
                        //end
                        if (iSpeed == 4) {
                            iSpeed = 0
                        } else {
                            iSpeed++
                        }
                        speedDP++;
                        avgSpeed+=arrayAvg(Speed)
                    } else if (identifier == 'R') {
                        RPM[iSpeed] = data.toDouble()
                        print("RPM: ")
                        println(arrayAvg(RPM))
                        //to screen
                        rpmText.text = "RPM: " + String.format("%.1f",arrayAvg(RPM)) + " RPM"
                        //end
                        rpmDP++;
                        avgRPM+=arrayAvg(RPM)
                    } else if (identifier == 'C') {
                        Cad[iCad] = data.toDouble()
                        print("Cad: ")
                        println(arrayAvg(Cad))
                        //to screen
                        cadText.text = "Cadence: " + String.format("%.1f",arrayAvg(Cad)) + " RPM"
                        //end
                        cadDP++;
                        avgCad+=arrayAvg(Cad)
                    } else if (identifier == 'I') {
                        Inc[iCad] = data.toDouble()
                        print("Inc: ")
                        println(arrayAvg(Inc))
                        //to screen
                        incText.text = "Incline: " + String.format("%.1f",arrayAvg(Inc)) + " Degrees"
                        //end
                        if (iCad == 4) {
                            iCad = 0
                        } else {
                            iCad++
                        }
                        incDP++;
                        avgInc+=arrayAvg(Inc);
                    } else if (identifier == 'P') {
                        Power[iCad] = data.toDouble()
                        print("Power: ")
                        println(arrayAvg(Power))
                        //to screen
                        pwrText.text = "Power: " + String.format("%.1f",arrayAvg(Power)) + " Watts"
                        //end
                        pwrDP++
                        avgPWR+=arrayAvg(Power)
                    }
                    else if (identifier == 'D'){
                        lastDistance = data.toDouble()
                        print("Last Distance: ")
                        println(lastDistance)
                        //to screen
                        distText.text = "Distance: " +  String.format("%.1f",lastDistance) + " Miles"
                        totDistance = lastDistance;
                        //end
                    }

                }
            }

            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                //this section is completely identical to BluetoothService
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

                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN),
                            6969
                        )
                    }
                    gatt?.setCharacteristicNotification(character, true);


                    //successful

                    Log.i("[BLE Info] serviceUUID", service.toString());
                    Log.i("[BLE Info] characterUUID", character.toString());
                    Log.i("[BLE Info] descriptorUUID", descriptor.toString());

                    Log.i("[BLE Info] serviceUUID", service?.uuid.toString());
                    Log.i("[BLE Info] characterUUID", character?.uuid.toString());
                    Log.i("[BLE Info] descriptorUUID", descriptor?.uuid.toString());

                    //descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    //descriptor.

                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.i("STATE_CONNECTED", "NEED PERMISSION");

                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN),
                            6969
                        )

                    }
                    Log.i("PostPermission", "Permission Granted?");
                    permissions = true
                    btPermText.text = "Permission: Granted"
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

        //connection
        bluetoothGatt = device.connectGatt(requireContext(), false, gattCallback)

        return root
    }

    //toast messages
    private fun toaster(msg: String)
    {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    //this function is identical to BluetoothService
    private fun resetChainTimer()
    {
        Log.i("resetChainTimer", "CHAIN RESET")
        chainTimer?.cancel()
        chainTimer = Timer()
        chainTimer?.schedule(
            customTimer("chain"),
            (chainTime*mod).toLong()
        )
    }
    //this function is identical to BluetoothService
    private fun resetCrankTimer()
    {
        Log.i("resetCrankTimer", "CRANK RESET")
        crankTimer?.cancel()
        crankTimer = Timer()
        crankTimer?.schedule(
            customTimer("crank"),
            (crankTime*mod).toLong()
        )
    }

    //this function is identical to BluetoothService
    @RequiresApi(Build.VERSION_CODES.O)
    private fun writeToFile()
    {
        var cal = GregorianCalendar()
        val curDate = LocalDate.now()
        var year = curDate.year
        var month = curDate.monthValue
        var day = curDate.dayOfMonth
        var times = (endTime-startTime)
        var cad = avgCad/cadDP
        var pitch = avgInc/incDP
        var pwr = avgPWR/pwrDP
        var rpm = avgRPM/rpmDP
        var speed = avgSpeed/speedDP
        var distance = totDistance
        val dh = TimeUnit.MILLISECONDS.toHours(times)
        val dm = TimeUnit.MILLISECONDS.toMinutes(times) % 60
        val ds = TimeUnit.MILLISECONDS.toSeconds(times) % 60
        val durInSeconds = dh*60*60+dm*60+ds
        var duration = "" + dh + "h" + dm + "m" + ds + "s"
        val numIntervals = (durInSeconds/900).toInt()

        if(cad.isNaN() || pitch.isNaN() || pwr.isNaN() || rpm.isNaN() || speed.isNaN() || distance.isNaN())
        {
            Log.i("WriteToFile", "ISNAN - REJECTED")
            Log.i("WriteToFile", "$cad, $pitch, $pwr, $rpm, $speed, $distance")
            Log.i("WriteToFile", "${cad.isNaN()}, ${pitch.isNaN()}, ${pwr.isNaN()}, ${rpm.isNaN()}, ${speed.isNaN()}, ${distance.isNaN()}")
            return
        }

        var numLines = 0;
        val f = File(context?.filesDir, "data.csv")
        var exists = f.exists()
        var outStream = OutputStreamWriter(context?.openFileOutput("data.csv", Context.MODE_PRIVATE))
        if(!exists)
        {
            Log.i("writeToFile", "File does not exist")
            outStream.write("id, Date, Duration, Distance, Average Speed, Average Pedal Rate\n")
        }
        else
        {
            Log.i("writeToFile", "File Exists")
            try{
                val reader = BufferedReader(FileReader("data.csv"))
                // Count the number of lines (rows) in the CSV file
                val rowCount = reader.lines().count()
                numLines = rowCount.toInt()
                Log.i("writing", "Number of rows: $numLines")
                reader.close()
            }
            catch (e: IOException)
            {
                Log.i("writing","Error reading CSV file")
            }
        }
        numLines++;


        val tireCircumference = 86.3938/63360 //inches to miles
        val tireCircumferenceInch = 86.3938

        val mets = (6..10).random()
        var bmr = 0.0
        var userage = 21
        val userweight = 150
        val userfeet = 5
        val userinch = 8
        bmr = 88.362 + (13.397*userweight/2.205) + (4.799*(userfeet*12+userinch)*2.54) - (5.677*userage)
        val calories = bmr*mets/(24*0.25)

        val date = "$year-$month-$day"

        var outString = "$numLines, $date, $duration, $distance, $speed, $rpm, $cad, $pwr, $pitch, $calories"
        outString += "\n"
        Log.i("Writing","$outString")
        outStream.write(outString)
        outStream.close()
        Log.i("Writing","Complete")


    }

    //this function is identical to BluetoothService
    private inner class customTimer(private val id: String): TimerTask(){
        override fun run() {
            if(id=="crank")
            {
                Log.i("customTimer","Crank Triggered")
                cadText.text = "Cadence: 0.0 RPM"
                incText.text = "Incline: 0.0 Degrees"
                pwrText.text = "Power: 0.0 Watts"

            }
            if(id=="chain")
            {
                Log.i("customTimer","Chain Triggered")
                speedText.text = "Speed: 0.0 MPH"
                rpmText.text = "RPM: 0.0 RPM"
            }
        }

    }

}