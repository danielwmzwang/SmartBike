package com.example.smartbike.ui.settings

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.smartbike.R
import com.example.smartbike.databinding.FragmentSettingsBinding
import java.io.*
import java.lang.Math.round
import java.util.*

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var userage = 25
    private var userfeet = 5
    private var userinch = 8
    private var userweight = 150
    private var isMale = true
    private var isMaleHolder = true //hold value until update is hit

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val settingsViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.fakeDataGen.setOnClickListener{
            val numEntries = binding.genDataNumEntries.text.toString().toIntOrNull()
            if(numEntries != null && numEntries > 0)
            {
                genData(numEntries)
            }
        }

        var maleBtn: Button = binding.male
        var femaleBtn: Button = binding.female
        val f = File(context?.filesDir, "user.csv")
        if(f.exists())
        {
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

        binding.updateInfo.setOnClickListener {
            userage = (binding.age.text.toString().replace("\\s".toRegex(), "")).toInt()
            userfeet = (binding.heightFeet.text.toString().replace("\\s".toRegex(), "")).toInt()
            userinch = (binding.heightInch.text.toString().replace("\\s".toRegex(), "")).toInt()
            userweight = (binding.weight.text.toString().replace("\\s".toRegex(), "")).toInt()
            isMale = isMaleHolder

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

        binding.destruct.setOnClickListener{
            val f = File(context?.filesDir, "data.csv")
            if(f.exists())
            {
                f.delete()
            }
            val f2 = File(context?.filesDir, "user.csv")
            if(f2.exists())
            {
                f.delete()
            }
        }
/*
        val textView: TextView = binding.textSettings
        settingsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }*/
        return root
    }

    private fun genData(inp: Int)
    {
        val f = File(context?.filesDir, "data.csv")
        if(f.exists())
        {
            f.delete()
        }
        var outStream = OutputStreamWriter(context?.openFileOutput("data.csv", Context.MODE_PRIVATE))

        outStream.write("id, Date, Duration, Distance, Average Speed, Average Pedal Rate\n")
        var cal = GregorianCalendar()
        var year = 2000
        var month = 1
        var day = 1

        for(x in 1..inp)
        {
            var time = ""
            var temp = 0
            var duration = ""
            var distance = 0.0
            var speed = 0.0
            var rpm = 0.0
            var pwr = 0.0
            var calories = 0.0
            var cad = 0.0
            var pitch = 0.0
            //Generate New Data

            //Time Data
            day += (1..4).random()
            temp = (0..23).random()
            time = "T" + temp + ":" + (0..59).random() + ":" + (0..59).random()
            if(day>=26)
            {
                day = 1
                month++
            }
            if(month==12)
            {
                month = 1
                year++
            }
            //Duration Data
            var dh = (0..4).random()
            var ds = (0..59).random()
            var dm = (0..59).random()
            var tempDuration = dh + dm.toDouble()/60 + ds.toDouble()/3600 //time in seconds
            var durInSeconds = dh*60*60+dm*60+ds
            duration = "" + dh + "h" + dm + "m" + ds + "s"

            //intervals defined as '15 min'
            val numIntervals = (durInSeconds/900).toInt() //in seconds
            val numLeft = (durInSeconds%900).toInt() //in seconds

            //interval data
            var intDist = Vector<Double>()
            var intSpeed = Vector<Double>()
            var intRPM = Vector<Double>()
            var intCad = Vector<Double>()
            var intPWR = Vector<Double>()
            var intPitch = Vector<Double>()
            var intCal = Vector<Double>()
            val tireCircumference = 86.3938/63360 //inches to miles
            val tireCircumferenceInch = 86.3938
            for(x in 0..numIntervals)
            {
                Log.i("numIntervals","$numIntervals")
                //15mph is average speed of a biker according to google
                val tempSpeed = (15+(-5..5).random()) //added some randomness
                intSpeed.add(tempSpeed.toDouble())
                speed+=tempSpeed.toDouble()
                val tempDist = tempSpeed.toDouble()/4 //15 minute interval
                intDist.add(tempDist)
                distance+=tempDist
                val temprpm = tempDist/(15*tireCircumference)
                intRPM.add(temprpm)
                rpm += temprpm
                val tempcad = (temprpm*2.5)/tireCircumferenceInch
                intCad.add(tempcad)
                cad+=tempcad
                val tempPWR = (80..120).random()
                intPWR.add(tempPWR.toDouble())
                pwr+=tempPWR.toDouble()
                val tempPitch = (-10..10).random()
                intPitch.add(tempPitch.toDouble())
                pitch+=tempPitch.toDouble()


                val mets = (6..10).random()
                var bmr = 0.0
                if(isMale)
                {
                    bmr = 88.362 + (13.397*userweight/2.205) + (4.799*(userfeet*12+userinch)*2.54) - (5.677*userage)
                }
                else
                {
                    bmr = 447.593 + (9.247*userweight/2.205) + (3.098*(userfeet*12+userinch)*2.54) - (4.33*userage)
                }
                val tempcal = bmr*mets/(24*0.25)
                intCal.add(tempcal)
                calories+=tempcal
            }

            speed/=(numIntervals+1)
            rpm/=(numIntervals+1)
            cad/=(numIntervals+1)
            pwr/=(numIntervals+1)

            //Pedal Rate Data
            //pedRate = (5..15).random()

            //Write to file
            //val date = "$year:$month:$day$time"
            val date = "$year-$month-$day"
            var outString = "$x, $date, $duration, $distance, $speed, $rpm, $cad, $pwr, $pitch, $calories"

            for(x in 0..numIntervals)
            {
                outString +=", ${intDist[x]}&${intSpeed[x]}&${intRPM[x]}&${intCad[x]}&${intPWR[x]}&${intPitch[x]}&${intCal[x]}"
            }
            outString+="\n"

            /*
            var intDist = Vector<Double>()
            var intSpeed = Vector<Double>()
            var intRPM = Vector<Double>()
            var intCad = Vector<Double>()
            var intPWR = Vector<Double>()
            var intCal = Vector<Double>()
             */

            outStream.write(outString)

            //Debug Purposes
            if(x==inp/2)
            {
                Log.i("genData", "50% Complete");
            }
            /*
            //Generate New Data

            //Time Data
            day += (1..4).random()
            temp = (0..23).random()
            time = "T" + temp + ":" + (0..59).random() + ":" + (0..59).random()
            if(day>=26)
            {
                day = 1
                month++
            }
            if(month==12)
            {
                month = 1
                year++
            }
            //Duration Data
            var dh = (0..4).random()
            var ds = (0..59).random()
            var dm = (0..59).random()
            var tempDuration = dh + dm.toDouble()/60 + ds.toDouble()/3600 //time in seconds
            duration = "" + dh + "h" + dm + "m" + ds + "s"

            //Distance Data
            distance = (14.2+(-10..10).random()) * tempDuration

            //Speed Data
            speed = distance/tempDuration.toDouble()

            //Pedal Rate Data
            pedRate = (5..15).random()

            //Write to file
            val date = "$year:$month:$day$time"
            val outString = "$x, $date, $duration, $distance, $speed, $pedRate\n"
            outStream.write(outString)

            //Debug Purposes
            if(x==inp/2)
            {
                Log.i("genData", "50% Complete");
            }
            */
        }
        outStream.close();
        Log.i("genData", "Fully Complete");
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}