package com.example.smartbike.services

/*
Created By: Daniel Wang
Page Purpose:
This page is a COPY of Bluetooth Service. This page allows for emulation testing to be done without use of the phone or bike
This service handles ALL bluetooth SERVER functions

In other words, this module is for development purposes ONLY. This is not used in the final product but is kept for technical merit

Essentially, this page "pretends" to be the BLE Device on the bike and transmits random information out
Furthermore, it copies the appropriate UUID's from the bikes BLE Device to truly and completely copy it
As most functions are nearly identical and it is not a final product item, there will be significantly less comments here
 */

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.bluetooth.*
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class BLEServerService : Service() {

    private val TAG = "BLEServerService"

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothGattServer: BluetoothGattServer

    //bike data
    private val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    private val characteristic = BluetoothGattCharacteristic(
        CHARACTERISTIC_UUID,
        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
    )

    companion object{
        const val ACTION_SEND_DATA = "com.example.SmartBike.ACTION_SEND_DATA"
    }

    private var connectedDevice: BluetoothDevice? = null


    //all components here are identical to BluetoothService but are flipped to being the SERVER version
    //i.e., instead of reading, this is mostly sending data
    private val serverCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Device connected: ${device?.address}")
                connectedDevice = device
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Device disconnected: ${device?.address}")
                connectedDevice = null
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Log.i("onCharacteristicReadRequest","Pre-perm")
            if (characteristic?.uuid == CHARACTERISTIC_UUID) {
                if (ContextCompat.checkSelfPermission(
                        this@BLEServerService,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@BLEServerService as Activity,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN),
                        6969
                    )
                //return
                }
                Log.i("onCharacteristicReadRequest","Post-perm")
                bluetoothGattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    characteristic.value
                )
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            Log.i("onCharacteristicWriteRequest","Pre-perm")
            if (characteristic?.uuid == CHARACTERISTIC_UUID) {
                characteristic.value = value
                if (ContextCompat.checkSelfPermission(
                        this@BLEServerService,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@BLEServerService as Activity,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN),
                        6969
                    )
                    //return
                }
                Log.i("onCharacteristicWriteRequest","Post-perm")
                bluetoothGattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "Advertising failed with error code $errorCode")
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BLEServerService = this@BLEServerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        initBluetooth()
    }

    @SuppressLint("MissingPermission")
    private fun initBluetooth() {
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled.")
            return
        }
        else
        {
            Log.i("initBluetooth", "Bluetooth is enabled.")
        }
        Log.i("initBluetooth","Pre-perm")

        Log.i("initBluetooth","Post-perm")
        bluetoothGattServer = bluetoothManager.openGattServer(this, serverCallback)
        bluetoothGattServer.addService(createGattService())
        Log.i("initBluetooth","Post Service")

        startAdvertising()
        Log.i("initBluetooth","Post-Ad")
    }

    private fun createGattService(): BluetoothGattService {
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        characteristic.value = byteArrayOf(0) // Initial value

        val config = BluetoothGattCharacteristic(
            CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(config)

        return service
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        Log.i("startAdvertising","Entered")
        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .build()


        bluetoothAdapter.bluetoothLeAdvertiser.startAdvertising(
            advertiseSettings,
            advertiseData,
            advertiseCallback
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("onStartCommand", "entered")
        when(intent?.action)
        {
            ACTION_SEND_DATA ->{
                transmitData("Howdy!");
                while(true)
                {
                    transmitData("MEOW")
                }
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun transmitData(msg: String)
    {
        connectedDevice?.let{
            characteristic.value = msg.toByteArray()
            bluetoothGattServer.notifyCharacteristicChanged(
                it, characteristic, false
            )
        }

    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        stopAdvertising()
        bluetoothGattServer.close()
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        bluetoothAdapter.bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
    }

}