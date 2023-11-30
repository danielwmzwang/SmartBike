package com.example.smartbike

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

    private lateinit var binding: ActivityMainBinding
    private val homeFragment = HomeFragment()
    private val settingsFragment = SettingsFragment()
    private val dashboardFragment = DashboardFragment()


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?){
            val binder = service as BluetoothService.LocalBinder
            val bluetoothService = binder.getService()


            //bluetoothService.setBluetoothCallback("home", homeFragment)
            //bluetoothService.setBluetoothCallback("settings", settingsFragment)
            //bluetoothService.setBluetoothCallback("dashboard", dashboardFragment)



        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    private fun checkPermissions(activity: Activity)
    {
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
        Log.i("MainActivity.kt", "Entered")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        Log.i("MainActivity.kt", "Check 1")
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard, R.id.navigation_workout, R.id.navigation_settings
            )
        )
        Log.i("MainActivity.kt", "Check 2")
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        checkPermissions(this)

        val intent = Intent(this, BluetoothService::class.java)
        startService(intent)

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

    }



    /*
    override fun setCallback(bluetoothService: BluetoothService) {
        TODO("Not yet implemented")
    }
    */
}