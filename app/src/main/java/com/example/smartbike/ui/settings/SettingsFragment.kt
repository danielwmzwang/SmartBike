package com.example.smartbike.ui.settings

/*
Created By: Daniel Wang
Page Purpose:
functions:
 - edit user information
 - delete all files (full wipe)
 - read bluetooth status updates
 */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.smartbike.R
import com.example.smartbike.databinding.FragmentSettingsBinding
import com.example.smartbike.services.BLEServerService
import com.example.smartbike.services.BluetoothService
import java.io.*
import java.lang.Math.round
import java.util.*

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    //define the defaults
    private var userage = 25
    private var userfeet = 5
    private var userinch = 8
    private var userweight = 150
    private var isMale = true
    private var isMaleHolder = true //hold value until update is hit

    ///define all the texts
    private lateinit var connectionStatusText: TextView
    private lateinit var dataReadyStatusText: TextView

    private lateinit var age: EditText
    private lateinit var heightFeet: EditText
    private lateinit var heightInch: EditText
    private lateinit var weight: EditText

    //for server purposes only
    private var serverOn = false;

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val settingsViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root


        //initialize everything
        var maleBtn: Button = binding.male
        var femaleBtn: Button = binding.female
        //check the user.csv file for data
        val f = File(context?.filesDir, "user.csv")
        if(f.exists())
        {
            //if it exists, extract the data we need into the input boxes
            var inp = context?.openFileInput("user.csv")
            if(inp!=null)
            {
                var inpReader = InputStreamReader(inp)
                var buffRead = BufferedReader(inpReader)
                var line = buffRead.readLine()
                val temp = line.split(",").toTypedArray()
                var uweight: EditText = binding.weight
                var uage: EditText = binding.age
                var uhfeet: EditText = binding.heightFeet
                var uhinch: EditText = binding.heightInch
                var male = (temp[4].replace("\\s".toRegex(), ""))
                //"$userage, $userfeet, $userinch, $userweight, $isMale"
                uage.setText(temp[0])
                uhfeet.setText(temp[1])
                uhinch.setText(temp[2])
                uweight.setText(temp[3])
                Log.i("isMale", "$male")
                if(male=="true")
                {
                    Log.i("Entered Male", "$male")
                    isMaleHolder = true
                    val color200 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_200))
                    val color500 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500))
                    maleBtn.backgroundTintList = color500
                    femaleBtn.backgroundTintList = color200
                }
                else
                {
                    Log.i("Entered Female", "$male")
                    isMaleHolder = false
                    val color200 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_200))
                    val color500 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500))
                    maleBtn.backgroundTintList = color200
                    femaleBtn.backgroundTintList = color500
                }
            }
        }

        //detect if the gender is swapped
        binding.male.setOnClickListener{
            isMaleHolder = true
            val color200 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_200))
            val color500 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500))
            maleBtn.backgroundTintList = color500
            femaleBtn.backgroundTintList = color200
        }

        binding.female.setOnClickListener{
            isMaleHolder = false
            val color200 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_200))
            val color500 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500))
            maleBtn.backgroundTintList = color200
            femaleBtn.backgroundTintList = color500
        }

        //detect if the user wants to update with new information
        binding.updateInfo.setOnClickListener {
            userage = (binding.age.text.toString().replace("\\s".toRegex(), "")).toInt()
            userfeet = (binding.heightFeet.text.toString().replace("\\s".toRegex(), "")).toInt()
            userinch = (binding.heightInch.text.toString().replace("\\s".toRegex(), "")).toInt()
            userweight = (binding.weight.text.toString().replace("\\s".toRegex(), "")).toInt()
            isMale = isMaleHolder

            //validation
            var valid = true
            if(userage<=12 || userage>=90)
            {
                //write a "toast" in case user input is bad
                toaster("Invalid Input: 12<userage<90")
                valid = false
            }
            else if(userfeet<=4 || userfeet>=7)
            {
                //write a "toast" in case user input is bad
                toaster("Invalid Input: 4<userfeet<7")
                valid = false
            }
            else if(userinch<0 || userinch>=12)
            {
                //write a "toast" in case user input is bad
                toaster("Invalid Input: 0<=userinches<12")
                valid = false
            }
            else if(userweight<=60 || userweight>=350)
            {
                //write a "toast" in case user input is bad
                toaster("Invalid Input: 60<userweight<350")
                valid = false
            }

            //OK

            if(valid)
            {
                //write valid items to csv file
                val f = File(context?.filesDir, "user.csv")
                if(f.exists())
                {
                    f.delete()
                }

                Log.i("isMale Writing", "$isMale, $isMaleHolder")
                var outStream = OutputStreamWriter(context?.openFileOutput("user.csv", Context.MODE_PRIVATE))
                var outString = "$userage, $userfeet, $userinch, $userweight, $isMale"

                outStream.write(outString)
                outStream.close()
            }
            else
            {
                Log.w("Settings Update", "FAILED IN VALIDATION")
            }
        }

        //if user requests wipe of data
        binding.destruct.setOnClickListener{
            val f = File(context?.filesDir, "data.csv")
            //delete data.csv if it exists
            if(f.exists())
            {
                f.delete()
            }
            val f2 = File(context?.filesDir, "user.csv")
            //delete user.csv if it exists
            if(f2.exists())
            {
                f.delete()
            }
        }
        return root
    }

    //write 'toasts' (pop-up messages) to the screen
    private fun toaster(msg: String)
    {
        //LENGTH_SHORT keeps it up for a short period of time
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        //this is only for when the server is used for testing purposes
        //kept for technical merit
        /*
        val serverBtn = view.findViewById<Button>(R.id.ServerBtn)
        serverBtn.setOnClickListener{
            if(!serverOn)
            {
                //turn it on
                serverBtn.text = "SERVER OFF"
                startBLEServerService()
            }
            else
            {
                //turn it off
                serverBtn.text = "SERVER ON"
                stopBLEServerService()
            }
        }
         */
        //detail which filter to look for
        val filter = IntentFilter("StatusTransmission")
        //register the broadcast
        getActivity()?.registerReceiver(receiver, filter)
    }

    private val receiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?){
            //detail which header to listen for
            val intake = intent?.getStringExtra("data")
            //safety
            if(intake!=null)
            {
                //split by ':'
                val toArr = intake.split(':')
                val status = toArr[0]
                val text = toArr[1]
                //write to screen
                if(status=="bluetooth")
                {
                    connectionStatusText.text = "Bluetooth: " + text
                }
                if(status=="data")
                {
                    dataReadyStatusText.text = "Data Collection: " + text
                }

            }
            else
            {
                Log.w("[HOMEFRAG]", "broadcast receiver got NULL DATA")
            }
        }
    }

    //only for server related tasks
    private fun startBLEServerService(){
        val serviceIntent = Intent(requireContext(), BLEServerService::class.java)
        serviceIntent.action = BLEServerService.ACTION_SEND_DATA
        requireContext().startService(serviceIntent)
    }

    //only for server related tasks
    private fun stopBLEServerService(){
        val serviceIntent = Intent(requireContext(), BLEServerService::class.java)
        requireContext().stopService(serviceIntent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}