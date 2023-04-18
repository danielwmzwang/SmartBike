package com.example.smartbike.ui.settings

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.smartbike.databinding.FragmentSettingsBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.lang.Math.round
import java.util.Calendar
import java.util.GregorianCalendar

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
                genData(numEntries);
            }
        }

        binding.destruct.setOnClickListener{
            val f = File(context?.filesDir, "data.csv")
            if(f.exists())
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
        var time = ""
        var temp = 0
        var duration = ""
        var distance = 0.0
        var speed = 0.0
        var pedRate = 0
        for(x in 1..inp)
        {
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
            var tempDuration = dh + dm.toDouble()/60 + ds.toDouble()/3600
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
        }
        outStream.close();
        Log.i("genData", "Fully Complete");
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}