package com.example.smartbike

/*
PLEASE START READING AND GRADING FROM HERE

Created By: Daniel Wang
Page Purpose:
The purpose of this page is to initiate the BluetoothService as well as all fragments.
Specifically: HomeFragment (Workout Page), DashboardFragment (Dashboard Page), and SettingsFragment (Settings Page)
This page serves as the primary initial driver for the entire application

Important Commenting Notes:
- ALL PERMISSIONS ACROSS THE APP ARE HANDLED IN THIS FILE
- ALL Log.i(), Log.w(), Log.e() commands are "Write to Console" commands that mean
    Information, Warning, and Error respectively. These are PURELY for debugging - the user does not see it
- the comment 'safety' usually implies that the if statement is there to handle very rare edge cases
  > these are so highly insignificant that most of them (if not all) have never been triggered a single time
  > regardless, the safety's are in place to keep bad things from happening
- some older code(s) that are occasionally used or files used for testing are kept in the project
    for technical merit
 */


import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.smartbike.databinding.ActivityMainBinding
import com.example.smartbike.services.BluetoothService
import com.example.smartbike.ui.dashboard.DashboardFragment
import com.example.smartbike.ui.home.HomeFragment
import com.example.smartbike.ui.settings.SettingsFragment


class MainActivity : AppCompatActivity() {

    //global variables
    private lateinit var binding: ActivityMainBinding
    private val homeFragment = HomeFragment()
    private val settingsFragment = SettingsFragment()
    private val dashboardFragment = DashboardFragment()

    //define the serviceConnection for later use when initiating and connecting the service
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?){
            val binder = service as BluetoothService.LocalBinder
            val bluetoothService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            //do nothing
        }
    }

    //check ALL permissions needed across the ENTIRE APPLICATION
    //The usage of this function renders all permission checking across the application unnecessary
    private fun checkPermissions(activity: Activity)
    {
        /*
        List of permissions I ask for:
        - BLUETOOTH_CONNECT: Permission to connect/communicate with nearby devices
        - BLUETOOTH: Allows overall bluetooth permissions (broad)
        - BLUETOOTH_ADMIN: Allows access and usage of more advanced bluetooth features
        - ACCESS_FINE_LOCATION: Allows precise device location
        - ACCESS_COARSE_LOCATION: Allows approximate device location
        Note: Both FINE and COARSE are asked for in the event the user declines one.
         */
        Log.i("checkPermissions", "Entered")

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("Bluetooth_Connect Permissions", "NEED PERMISSION")
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1
            )
        }

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("Bluetooth Permissions", "NEED PERMISSION")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH),
                2
            )
        }

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("Bluetooth_Admin Permissions", "NEED PERMISSION")
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
                3
            )
        }

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("Location Fine/Coarse Permissions", "NEED PERMISSION")
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                3
            )
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                3
            )
        }

        Log.i("PERMISSIONS", "All permissions GRANTED")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Initiate and set up the entire front-end
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard, R.id.navigation_workout, R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //check permissions
        checkPermissions(this)

        //initiate and connect the BluetoothService
        val intent = Intent(this, BluetoothService::class.java)
        startService(intent)

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

    }
}