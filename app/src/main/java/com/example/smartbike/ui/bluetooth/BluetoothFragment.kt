package com.example.smartbike.ui.bluetooth

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanCallback.SCAN_FAILED_ALREADY_STARTED
import android.bluetooth.le.ScanResult
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.smartbike.R
import com.example.smartbike.databinding.FragmentBluetoothBinding
import com.example.smartbike.databinding.FragmentDashboardBinding
import java.nio.charset.Charset

class BluetoothFragment : Fragment() {

    private var _binding: FragmentBluetoothBinding? = null
    private val SCAN_PERIOD: Long = 10000
    private val BLEID = "64:69:4E:8C:95:97"


    companion object {
        fun newInstance() = BluetoothFragment()
    }

    private lateinit var viewModel: BluetoothViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)

        val binding = _binding!!
        var statusText = binding.textViewBluetoothStatus
        var dataText = binding.textViewRealTimeData

        val root: View = binding.root

        val bluetoothManager: BluetoothManager = requireActivity().getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.getAdapter()

        if(bluetoothAdapter==null)
        {
            return root
        }

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

        var bluetoothGatt: BluetoothGatt? = null
        val deviceAddy = "64:69:4E:8C:95:97"
        Log.i("Connection", "Begin Connection attempt")

        val device = bluetoothAdapter.getRemoteDevice(deviceAddy)

        Log.i("Connection", "Got remote device")
        Log.i("Connection", device.toString())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.i("Bluetooth_Connect Permissions", "NEED PERMISSION")
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1234
            )
        }
        Log.i("Bluetooth_Connect Permissions", "PERMISSION GRANTED")

        Log.i("gattCallback", "Entering");
        val gattCallback = object: BluetoothGattCallback(){
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
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.

                            Log.i("STATE_CONNECTED", "NEED PERMISSION")

                            ActivityCompat.requestPermissions(
                                requireActivity(),
                                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                                6969
                            )

                        }

                        Log.i("STATE_CONNECTED", "PERMISSION GRANTED, DISCOVERING SERVICES")
                        gatt?.discoverServices();
                    }
                    BluetoothProfile.STATE_DISCONNECTED ->{
                        Log.i("STATE_DISCONNECTED", "Disconnected")
                    }
                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Characteristic read successfully.
                    val data = characteristic.value
                    // Handle the data here.
                }
            }


            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?){
                Log.i("onCharacteristicChanged", "Entered")
                //val data = characteristic?.value
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
                    return
                }
                if (gatt != null) {
                    //val data = gatt.readCharacteristic(characteristic)
                    val data = characteristic.toString();
                    Log.i("onCharacteristicChanged", data);
                    if(data!=null)
                    {
                        //val dataString = String(data, Charset.forName("UTF-8"))
                        //Log.i("onCharacteristicChanged", dataString)
                    }
                }


            }

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
                    val descriptorUUIDSTR = "00002902-0000-1000-8000-00805f9b34fb";

                    val service = gatt?.getService(serviceList?.get(2)?.uuid)

                    if(service.toString()!=serviceUUIDSTR)
                    {
                        Log.w("SERVICEUUID MISMATCH", service.toString());
                        Log.w("SERVICEUUID MISMATCH", serviceUUIDSTR);
                        return;
                    }

                    val character = service?.characteristics?.get(0);



                    if(character.toString()!=characterUUIDSTR)
                    {
                        Log.w("characterUUID MISMATCH", character.toString());
                        Log.w("characterUUID MISMATCH", characterUUIDSTR);
                        return;
                    }



                    val descriptor = character?.descriptors?.get(0);
                    if(descriptor.toString()!=descriptorUUIDSTR)
                    {
                        Log.w("descriptorUUID MISMATCH", descriptor.toString());
                        Log.w("descriptorUUID MISMATCH", descriptorUUIDSTR);
                        return;
                    }


                    //successful

                    Log.i("[BLE Info] serviceUUID", service.toString());
                    Log.i("[BLE Info] characterUUID", character.toString());
                    Log.i("[BLE Info] descriptorUUID", descriptor.toString());

                    //descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    //descriptor.

                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        Log.i("STATE_CONNECTED", "NEED PERMISSION");

                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                            6969
                        )

                        return
                    }
                    Log.i("PostPermission", "Permission Granted?");
                    if (gatt != null) {
                        if (descriptor != null) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        }
                    };


                    /*
                    if (serviceUUID != null) {
                        for(char in serviceUUID.characteristics) {
                            val charUuid = char.uuid.toString()
                            if(charUuid=="0000ffe1-0000-1000-8000-00805f9b34fb") {
                                //found
                                val character = gatt.getService()
                            }
                            Log.i("Service1 Characteristic", char.toString())
                            Log.i("Service1 Characteristic", charUuid)
                            for( desc in char.descriptors) {
                                val descUuid = desc.uuid.toString()
                                Log.i("Service1 Char Descriptor", desc.toString())
                                Log.i("Service1 Char Descriptor", descUuid)
                            }
                        }
                    }
                    */


                    /*
                    val service1 = gatt?.getService(serviceList?.get(0)?.uuid)
                    val service2 = gatt?.getService(serviceList?.get(1)?.uuid)
                    val service3 = gatt?.getService(serviceList?.get(2)?.uuid)

                    if (service1 != null) {
                        Log.i("Service1", service1.toString())
                        for(char in service1.characteristics) {
                            val charUuid = char.uuid.toString()
                            Log.i("Service1 Characteristic", char.toString())
                            Log.i("Service1 Characteristic", charUuid)
                            for( desc in char.descriptors)
                            {
                                val descUuid = desc.uuid.toString()
                                Log.i("Service1 Char Descriptor", desc.toString())
                                Log.i("Service1 Char Descriptor", descUuid)
                            }
                        }
                    }

                    if (service2 != null) {
                        Log.i("Service2", service2.toString())
                        for(char in service2.characteristics) {
                            val charUuid = char.uuid.toString()
                            Log.i("Service2 Characteristic", char.toString())
                            Log.i("Service2 Characteristic", charUuid)
                            for( desc in char.descriptors)
                            {
                                val descUuid = desc.uuid.toString()
                                Log.i("Service2 Char Descriptor", desc.toString())
                                Log.i("Service2 Char Descriptor", descUuid)
                            }
                        }
                    }

                    if (service3 != null) {
                        Log.i("Service3", service3.toString())
                        for(char in service3.characteristics) {
                            val charUuid = char.uuid.toString()
                            Log.i("Service3 Characteristic", char.toString())
                            Log.i("Service3 Characteristic", charUuid)
                            for( desc in char.descriptors)
                            {
                                val descUuid = desc.uuid.toString()
                                Log.i("Service3 Char Descriptor", desc.toString())
                                Log.i("Service3 Char Descriptor", descUuid)
                            }
                        }
                    }
                    */

                    /*
                    if(service1!=null)
                    {
                        val characteristic = service1?.getCharacteristic()
                    }
                    */
                }
                else
                {
                    Log.i("onServicesDiscovered", "NOT SUCCESS")
                    Log.i("onServicesDiscovered", BluetoothGatt.GATT_SUCCESS.toString())
                }
            }
        }

        bluetoothGatt = device.connectGatt(requireContext(), false, gattCallback)


        /*
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        var scanning = false
        var deviceInfo: ScanResult? = null
        var leScanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                Log.i("OnScanResult0", callbackType.toString())
                Log.i("OnScanResult0_1", result.toString())
                if (result != null) {
                    deviceInfo = result
                }
            }

            override fun onBatchScanResults(results: List<ScanResult>?) {
                Log.i("OnScanResult1", results.toString())
            }

            override fun onScanFailed(errorCode: Int) {
                Log.i("OnScanResult2", errorCode.toString())
            }
        }
        if(!scanning)
        {
            Log.i("!scanning", "entered")
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                scanning = false
                if (ActivityCompat.checkSelfPermission(
                        this.requireContext(),
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    //happy noises
                }
                if (bluetoothLeScanner != null) {
                    bluetoothLeScanner.stopScan(leScanCallback)
                }
            }, SCAN_PERIOD)
            Log.i("!scanning", "passed handler")
            scanning = true
            if (ActivityCompat.checkSelfPermission(
                    this.requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //happy noises
                Log.i("!scanning", "permissions are NOT GIVEN for scan")
                var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == RESULT_OK) {
                        //granted
                    }else{
                        //deny
                    }
                }
            }
            if (bluetoothLeScanner != null) {
                Log.i("!scanning", "scanning beginning")
                bluetoothLeScanner.startScan(leScanCallback)
                Log.i("!scanning", "scan called and done")
            }
        }
        else
        {
            Log.i("scanning", "entered")
            scanning = false
            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.stopScan(leScanCallback)
            }
        }
        if(deviceInfo != null)
        {
            var device = deviceInfo!!.device
            if(device!=null)
            {
                if(device.toString() == BLEID)
                {
                    var bluetoothGatt: BluetoothGatt? = null
                    var gattCallback: BluetoothGattCallback = null
                    bluetoothGatt = device.connectGatt(this.requireContext(), false, gattCallback)
                }
            }
        }
        */


        Log.i("Main", "Howdy")
        //Log.i("Main", leScanCallback.toString())

        return root
    }

}