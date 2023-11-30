package com.example.smartbike.ui.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.smartbike.R
import com.example.smartbike.databinding.FragmentDashboardBinding
import com.example.smartbike.services.BluetoothService
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.*
import java.lang.Math.abs
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.*

class DashboardFragment : Fragment()/*, BluetoothService.DataCallback*/ {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var isHistory = false

    private var currentdate = Date()

    private var selected = "ALL"


    private lateinit var spinner: Spinner


    data class datapack(val dates: Vector<String>, val durations: Vector<Int>, val distance: Vector<Double>, val speed: Vector<Double>, val pedal: Vector<Double>,
                        val cad: Vector<Double>, val pwr: Vector<Double>, val pitch: Vector<Double>, val calories: Vector<Double>,
                        val tDuration: Double, val tDistance: Double, val tSpeed: Double)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.i("DashboardFragment.kt", "Entered")

        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)


        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        Log.i("Dashboard", "ONCREATEVIEW")




        //Last Ride
        var lastRideBtn: Button = binding.switch0 //Last Ride Selector
        var lastRideRPM: GraphView = binding.lastRideRPM
        var lastRideSpeed: GraphView = binding.lastRideSpeed
        var lastRideDist: GraphView = binding.lastRideDist
        var lastRideCad: GraphView = binding.lastRideCad
        var lastRidePWR: GraphView = binding.lastRidePWR
        var lastRidePitch: GraphView = binding.lastRidePitch
        var lastRideCals: GraphView = binding.lastRideCals
        //History
        var historyBtn: Button = binding.switch1 //History Selector
        var histDuration: GraphView = binding.histDuration
        var histRPM: GraphView = binding.histRPM
        var histSpeed: GraphView = binding.histSpeed
        var histDistance: GraphView = binding.histDistance
        var histCadence: GraphView = binding.histCadence
        var histPWR: GraphView = binding.histPWR
        var histPitch: GraphView = binding.histPitch
        var histCals: GraphView = binding.histCals
        //filters
        var tf1: Button = binding.tf1
        var tf2: Button = binding.tf2
        var tf3: Button = binding.tf3
        var tf4: Button = binding.tf4
        //text
        var tv1: TextView = binding.tv1
        binding.switch0.setOnClickListener{
            isHistory = false;
            val color200 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_200))
            val color500 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500))
            lastRideBtn.backgroundTintList = color500
            historyBtn.backgroundTintList = color200

            lastRideRPM.visibility = View.VISIBLE
            lastRideSpeed.visibility  = View.VISIBLE
            lastRideDist.visibility = View.VISIBLE
            lastRideCad.visibility = View.VISIBLE
            lastRidePWR.visibility = View.VISIBLE
            lastRidePitch.visibility  = View.VISIBLE
            lastRideCals.visibility = View.VISIBLE

            histDuration.visibility = View.GONE
            histRPM.visibility = View.GONE
            histSpeed.visibility = View.GONE
            histDistance.visibility = View.GONE
            histCadence.visibility = View.GONE
            histPWR.visibility = View.GONE
            histPitch.visibility = View.GONE
            histCals.visibility = View.GONE
            tv1.visibility = View.GONE
            tf1.visibility = View.GONE
            tf2.visibility = View.GONE
            tf3.visibility = View.GONE
            tf4.visibility = View.GONE
        }
        binding.switch1.setOnClickListener{
            val f = File(context?.filesDir, "data.csv")
            if(!f.exists())
            {
                Toast.makeText(requireContext(), "No Data!!", Toast.LENGTH_SHORT).show()
            }
            else
            {
                isHistory = true;
                val color200 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_200))
                val color500 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500))
                lastRideBtn.backgroundTintList = color200
                historyBtn.backgroundTintList = color500

                lastRideRPM.visibility = View.GONE
                lastRideSpeed.visibility  = View.GONE
                lastRideDist.visibility = View.GONE
                lastRideCad.visibility = View.GONE
                lastRidePWR.visibility = View.GONE
                lastRidePitch.visibility  = View.GONE
                lastRideCals.visibility = View.GONE

                histDuration.visibility = View.VISIBLE
                histRPM.visibility = View.VISIBLE
                histSpeed.visibility = View.VISIBLE
                histDistance.visibility = View.VISIBLE
                histCadence.visibility = View.VISIBLE
                histPWR.visibility = View.VISIBLE
                histPitch.visibility = View.VISIBLE
                histCals.visibility = View.VISIBLE
                /*
                tv1.visibility = View.VISIBLE
                tf1.visibility = View.VISIBLE
                tf2.visibility = View.VISIBLE
                tf3.visibility = View.VISIBLE
                tf4.visibility = View.VISIBLE
                */
            }

        }

        binding.tf1.setOnClickListener{
            selected="1D"
            val color200 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_200))
            val color500 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500))
            tf1.backgroundTintList = color500
            tf2.backgroundTintList = color200
            tf3.backgroundTintList = color200
            tf4.backgroundTintList = color200

        }
        binding.tf2.setOnClickListener{
            selected="1W"
            val color200 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_200))
            val color500 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500))
            tf1.backgroundTintList = color200
            tf2.backgroundTintList = color500
            tf3.backgroundTintList = color200
            tf4.backgroundTintList = color200

            view?.requestLayout()
        }
        binding.tf3.setOnClickListener{
            selected="1M"
            val color200 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_200))
            val color500 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500))
            tf1.backgroundTintList = color200
            tf2.backgroundTintList = color200
            tf3.backgroundTintList = color500
            tf4.backgroundTintList = color200

            view?.requestLayout()
        }
        binding.tf4.setOnClickListener{
            selected="ALL"
            val color200 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_200))
            val color500 = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500))
            tf1.backgroundTintList = color200
            tf2.backgroundTintList = color200
            tf3.backgroundTintList = color200
            tf4.backgroundTintList = color500

            view?.requestLayout()
        }

        //Read Data and Process for Summary Data
        var pack = readData(binding);

        val txtDBDuration: TextView = binding.textViewDBDuration
        val txtDBDistance: TextView = binding.textViewDBDistance
        val txtDBSpeed: TextView = binding.textViewDBSpeed

        val tempdashDuration = (pack.tDuration/pack.durations.size).toInt()
        val tempH = tempdashDuration/3600
        val tempM = (tempdashDuration-tempH*3600)/60
        val tempS = (tempdashDuration-tempH*3600-tempM*60)
        val dashDuration = "" + (tempdashDuration/3600) + "H " + tempM + "M " + tempS + "S"
        Log.i("pack tDistance", "${pack.tDistance}")
        Log.i("pack distance size", "${pack.distance.size}")
        var dashDistance = 0.0
        var dashSpeed = 0.0
        if(pack.distance.size==0)
        {
            dashDistance = BigDecimal(pack.tDistance/1).setScale(1, RoundingMode.HALF_EVEN).toDouble()
        }
        else
        {
            dashDistance = BigDecimal(pack.tDistance/pack.distance.size).setScale(1, RoundingMode.HALF_EVEN).toDouble()
        }
        if(pack.speed.size==0)
        {
            dashSpeed = BigDecimal(pack.tSpeed/1).setScale(1, RoundingMode.HALF_EVEN).toDouble()
        }
        else
        {
            dashSpeed = BigDecimal(pack.tSpeed/pack.speed.size).setScale(1, RoundingMode.HALF_EVEN).toDouble()
        }

        txtDBDuration.text = (if(pack.tDuration== Double.NaN) 0 else dashDuration).toString()
        txtDBDistance.text = (if(pack.tDistance== Double.NaN) 0 else dashDistance).toString() + " Miles"
        txtDBSpeed.text = (if(pack.tSpeed== Double.NaN) 0 else dashSpeed).toString() + " MPH"

        //////GRAPH1//////

        val lineGraphView: GraphView = binding.histDistance
        var distArr = Array<DataPoint>(pack.distance.size){DataPoint(0.0,0.0)}
        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

        var maxValY = 0.0
        var maxValX = pack.distance.size.toDouble()
        for(x in 1..pack.distance.size)
        {
            Log.i("Graph",pack.distance[x-1].toString())
            if(pack.distance[x-1]>maxValY)
            {
                maxValY=pack.distance[x-1]
            }
            if(selected=="ALL")
            {
                distArr[x-1] = (DataPoint(x.toDouble(), pack.distance[x-1]))
            }
            else
            {
                val thisDateStr = pack.dates[x-1]
                val datesplit = thisDateStr.split("-")
                val year = (datesplit[0].replace("\\s".toRegex(), "")).toInt()
                val month = (datesplit[1].replace("\\s".toRegex(), "")).toInt()
                val day = (datesplit[2].replace("\\s".toRegex(), "")).toInt()
                val thisDate = Calendar.getInstance().apply{ set(year, month, day) }.time
                val newCal = Calendar.getInstance()
                newCal.time = currentdate
                if(selected=="1D")
                {
                    newCal.add(Calendar.DAY_OF_YEAR, -1)
                    val oneDay = newCal.time
                    if(thisDate.after(oneDay))
                    {
                        distArr[x-1] = (DataPoint(x.toDouble(), pack.distance[x-1]))
                    }

                }
                else if(selected=="1W")
                {
                    newCal.add(Calendar.WEEK_OF_YEAR, -1)
                    val oneWeek = newCal.time
                    if(thisDate.after(oneWeek))
                    {
                        distArr[x-1] = (DataPoint(x.toDouble(), pack.distance[x-1]))
                    }
                }
                else if(selected=="1M")
                {
                    newCal.add(Calendar.MONTH, -1)
                    val oneMonth = newCal.time
                    if(thisDate.after(oneMonth))
                    {
                        distArr[x-1] = (DataPoint(x.toDouble(), pack.distance[x-1]))
                    }
                }
            }

        }
        val series: LineGraphSeries<DataPoint> = LineGraphSeries(distArr)

        lineGraphView.animate()
        lineGraphView.animate()
        lineGraphView.viewport.isScrollable = true
        lineGraphView.viewport.isScalable = true
        lineGraphView.viewport.setScalableY(true)
        lineGraphView.viewport.setScrollableY(true)
        lineGraphView.viewport.setMaxX(maxValX)
        lineGraphView.viewport.setMaxY(maxValY)
        lineGraphView.viewport.setMinX(1.0)
        lineGraphView.viewport.setMinY(0.0)
        lineGraphView.viewport.setMaxY(100.0)
        lineGraphView.title = "Distance Graph"
        lineGraphView.gridLabelRenderer.horizontalAxisTitle = "Ride Number"
        lineGraphView.gridLabelRenderer.verticalAxisTitle = "Distance Travelled (Miles)"
        series.color = R.color.purple_200
        lineGraphView.addSeries(series)


        //////GRAPH2//////
        val lineGraphView2: GraphView = binding.histSpeed
        var distArr2 = Array<DataPoint>(pack.speed.size){DataPoint(0.0,0.0)}
        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

        var maxValY2 = 0.0
        var maxValX2 = pack.speed.size.toDouble()
        for(x in 1..pack.speed.size)
        {
            Log.i("Graph",pack.speed[x-1].toString())
            if(pack.speed[x-1]>maxValY2)
            {
                maxValY2=pack.speed[x-1]
            }
            if(selected=="ALL")
            {
                distArr2[x-1] = (DataPoint(x.toDouble(), pack.speed[x-1]))
            }
            else
            {
                val thisDateStr = pack.dates[x-1]
                val datesplit = thisDateStr.split("-")
                val year = (datesplit[0].replace("\\s".toRegex(), "")).toInt()
                val month = (datesplit[1].replace("\\s".toRegex(), "")).toInt()
                val day = (datesplit[2].replace("\\s".toRegex(), "")).toInt()
                val thisDate = Calendar.getInstance().apply{ set(year, month, day) }.time
                val newCal = Calendar.getInstance()
                newCal.time = currentdate
                if(selected=="1D")
                {
                    newCal.add(Calendar.DAY_OF_YEAR, -1)
                    val oneDay = newCal.time
                    if(thisDate.after(oneDay))
                    {
                        distArr2[x-1] = (DataPoint(x.toDouble(), pack.speed[x-1]))
                    }

                }
                else if(selected=="1W")
                {
                    newCal.add(Calendar.WEEK_OF_YEAR, -1)
                    val oneWeek = newCal.time
                    if(thisDate.after(oneWeek))
                    {
                        distArr2[x-1] = (DataPoint(x.toDouble(), pack.speed[x-1]))
                    }
                }
                else if(selected=="1M")
                {
                    newCal.add(Calendar.MONTH, -1)
                    val oneMonth = newCal.time
                    if(thisDate.after(oneMonth))
                    {
                        distArr2[x-1] = (DataPoint(x.toDouble(), pack.speed[x-1]))
                    }
                }
            }
        }
        val series2: LineGraphSeries<DataPoint> = LineGraphSeries(distArr2)

        lineGraphView2.animate()
        lineGraphView2.animate()
        lineGraphView2.viewport.isScrollable = true
        lineGraphView2.viewport.isScalable = true
        lineGraphView2.viewport.setScalableY(true)
        lineGraphView2.viewport.setScrollableY(true)
        lineGraphView2.viewport.setMaxX(maxValX2)
        lineGraphView2.viewport.setMaxY(maxValY2)
        lineGraphView2.viewport.setMinX(1.0)
        lineGraphView2.viewport.setMinY(0.0)
        lineGraphView2.title = "Speed Graph"
        lineGraphView2.gridLabelRenderer.horizontalAxisTitle = "Ride Number"
        lineGraphView2.gridLabelRenderer.verticalAxisTitle = "Average Speed (MPH)"
        series2.color = R.color.purple_200
        lineGraphView2.addSeries(series2)

        //////GRAPH3//////
        val lineGraphView3: GraphView = binding.histDuration
        var distArr3 = Array<DataPoint>(pack.durations.size){DataPoint(0.0,0.0)}
        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

        var maxValY3 = 0.0
        var maxValX3 = pack.durations.size.toDouble()
        for(x in 1..pack.durations.size)
        {
            Log.i("Graph",pack.durations[x-1].toString())
            if(pack.durations[x-1]>maxValY2)
            {
                maxValY2=(pack.durations[x-1].toDouble())
            }
            if(selected=="ALL")
            {
                distArr3[x-1] = (DataPoint(x.toDouble(), pack.durations[x-1].toDouble()))
            }
            else
            {
                val thisDateStr = pack.dates[x-1]
                val datesplit = thisDateStr.split("-")
                val year = (datesplit[0].replace("\\s".toRegex(), "")).toInt()
                val month = (datesplit[1].replace("\\s".toRegex(), "")).toInt()
                val day = (datesplit[2].replace("\\s".toRegex(), "")).toInt()
                val thisDate = Calendar.getInstance().apply{ set(year, month, day) }.time
                val newCal = Calendar.getInstance()
                newCal.time = currentdate
                if(selected=="1D")
                {
                    newCal.add(Calendar.DAY_OF_YEAR, -1)
                    val oneDay = newCal.time
                    if(thisDate.after(oneDay))
                    {
                        distArr3[x-1] = (DataPoint(x.toDouble(), pack.durations[x-1].toDouble()))
                    }

                }
                else if(selected=="1W")
                {
                    newCal.add(Calendar.WEEK_OF_YEAR, -1)
                    val oneWeek = newCal.time
                    if(thisDate.after(oneWeek))
                    {
                        distArr3[x-1] = (DataPoint(x.toDouble(), pack.durations[x-1].toDouble()))
                    }
                }
                else if(selected=="1M")
                {
                    newCal.add(Calendar.MONTH, -1)
                    val oneMonth = newCal.time
                    if(thisDate.after(oneMonth))
                    {
                        distArr3[x-1] = (DataPoint(x.toDouble(), pack.durations[x-1].toDouble()))
                    }
                }
            }
        }
        val series3: LineGraphSeries<DataPoint> = LineGraphSeries(distArr3)

        lineGraphView3.animate()
        lineGraphView3.animate()
        lineGraphView3.viewport.isScrollable = true
        lineGraphView3.viewport.isScalable = true
        lineGraphView3.viewport.setScalableY(true)
        lineGraphView3.viewport.setScrollableY(true)
        lineGraphView3.viewport.setMaxX(maxValX3)
        lineGraphView3.viewport.setMaxY(maxValY3)
        lineGraphView3.viewport.setMinX(1.0)
        lineGraphView3.viewport.setMinY(0.0)
        lineGraphView3.title = "Duration Graph"
        lineGraphView3.gridLabelRenderer.horizontalAxisTitle = "Ride Number"
        lineGraphView3.gridLabelRenderer.verticalAxisTitle = "Average Duration (Min)"
        series3.color = R.color.purple_200
        lineGraphView3.addSeries(series3)

        //////GRAPH4//////
        val lineGraphView4: GraphView = binding.histRPM
        var distArr4 = Array<DataPoint>(pack.pedal.size){DataPoint(0.0,0.0)}
        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

        var maxValY4 = 0.0
        var maxValX4 = pack.pedal.size.toDouble()
        for(x in 1..pack.pedal.size)
        {
            Log.i("Graph",pack.pedal[x-1].toString())
            if(pack.pedal[x-1]>maxValY4)
            {
                maxValY4=pack.pedal[x-1]
            }
            if(selected=="ALL")
            {
                distArr4[x-1] = (DataPoint(x.toDouble(), pack.pedal[x-1]))
            }
            else
            {
                val thisDateStr = pack.dates[x-1]
                val datesplit = thisDateStr.split("-")
                val year = (datesplit[0].replace("\\s".toRegex(), "")).toInt()
                val month = (datesplit[1].replace("\\s".toRegex(), "")).toInt()
                val day = (datesplit[2].replace("\\s".toRegex(), "")).toInt()
                val thisDate = Calendar.getInstance().apply{ set(year, month, day) }.time
                val newCal = Calendar.getInstance()
                newCal.time = currentdate
                if(selected=="1D")
                {
                    newCal.add(Calendar.DAY_OF_YEAR, -1)
                    val oneDay = newCal.time
                    if(thisDate.after(oneDay))
                    {
                        distArr4[x-1] = (DataPoint(x.toDouble(), pack.pedal[x-1]))
                    }

                }
                else if(selected=="1W")
                {
                    newCal.add(Calendar.WEEK_OF_YEAR, -1)
                    val oneWeek = newCal.time
                    if(thisDate.after(oneWeek))
                    {
                        distArr4[x-1] = (DataPoint(x.toDouble(), pack.pedal[x-1]))
                    }
                }
                else if(selected=="1M")
                {
                    newCal.add(Calendar.MONTH, -1)
                    val oneMonth = newCal.time
                    if(thisDate.after(oneMonth))
                    {
                        distArr4[x-1] = (DataPoint(x.toDouble(), pack.pedal[x-1]))
                    }
                }
            }
        }
        val series4: LineGraphSeries<DataPoint> = LineGraphSeries(distArr4)

        lineGraphView4.animate()
        lineGraphView4.animate()
        lineGraphView4.viewport.isScrollable = true
        lineGraphView4.viewport.isScalable = true
        lineGraphView4.viewport.setScalableY(true)
        lineGraphView4.viewport.setScrollableY(true)
        lineGraphView4.viewport.setMaxX(maxValX4)
        lineGraphView4.viewport.setMaxY(maxValY4)
        lineGraphView4.viewport.setMinX(1.0)
        lineGraphView4.viewport.setMinY(0.0)
        lineGraphView4.title = "RPM Graph"
        lineGraphView4.gridLabelRenderer.horizontalAxisTitle = "Ride Number"
        lineGraphView4.gridLabelRenderer.verticalAxisTitle = "Average RPM"
        series4.color = R.color.purple_200
        lineGraphView4.addSeries(series4)

        //////GRAPH5//////
        val lineGraphView5: GraphView = binding.histCadence
        var distArr5 = Array<DataPoint>(pack.cad.size){DataPoint(0.0,0.0)}
        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

        var maxValY5 = 0.0
        var maxValX5 = pack.cad.size.toDouble()
        for(x in 1..pack.cad.size)
        {
            Log.i("Graph",pack.cad[x-1].toString())
            if(pack.cad[x-1]>maxValY5)
            {
                maxValY5=pack.cad[x-1]
            }
            if(selected=="ALL")
            {
                distArr5[x-1] = (DataPoint(x.toDouble(), pack.cad[x-1]))
            }
            else
            {
                val thisDateStr = pack.dates[x-1]
                val datesplit = thisDateStr.split("-")
                val year = (datesplit[0].replace("\\s".toRegex(), "")).toInt()
                val month = (datesplit[1].replace("\\s".toRegex(), "")).toInt()
                val day = (datesplit[2].replace("\\s".toRegex(), "")).toInt()
                val thisDate = Calendar.getInstance().apply{ set(year, month, day) }.time
                val newCal = Calendar.getInstance()
                newCal.time = currentdate
                if(selected=="1D")
                {
                    newCal.add(Calendar.DAY_OF_YEAR, -1)
                    val oneDay = newCal.time
                    if(thisDate.after(oneDay))
                    {
                        distArr5[x-1] = (DataPoint(x.toDouble(), pack.cad[x-1]))
                    }

                }
                else if(selected=="1W")
                {
                    newCal.add(Calendar.WEEK_OF_YEAR, -1)
                    val oneWeek = newCal.time
                    if(thisDate.after(oneWeek))
                    {
                        distArr5[x-1] = (DataPoint(x.toDouble(), pack.cad[x-1]))
                    }
                }
                else if(selected=="1M")
                {
                    newCal.add(Calendar.MONTH, -1)
                    val oneMonth = newCal.time
                    if(thisDate.after(oneMonth))
                    {
                        distArr5[x-1] = (DataPoint(x.toDouble(), pack.cad[x-1]))
                    }
                }
            }
        }
        val series5: LineGraphSeries<DataPoint> = LineGraphSeries(distArr5)

        lineGraphView5.animate()
        lineGraphView5.animate()
        lineGraphView5.viewport.isScrollable = true
        lineGraphView5.viewport.isScalable = true
        lineGraphView5.viewport.setScalableY(true)
        lineGraphView5.viewport.setScrollableY(true)
        lineGraphView5.viewport.setMaxX(maxValX5)
        lineGraphView5.viewport.setMaxY(maxValY5)
        lineGraphView5.viewport.setMinX(1.0)
        lineGraphView5.viewport.setMinY(0.0)
        lineGraphView5.title = "Cadence Graph"
        lineGraphView5.gridLabelRenderer.horizontalAxisTitle = "Ride Number"
        lineGraphView5.gridLabelRenderer.verticalAxisTitle = "Average Cadence (RPM)"
        series5.color = R.color.purple_200
        lineGraphView5.addSeries(series5)

        //////GRAPH2//////
        val lineGraphView6: GraphView = binding.histPWR
        var distArr6 = Array<DataPoint>(pack.pwr.size){DataPoint(0.0,0.0)}
        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

        var maxValY6 = 0.0
        var maxValX6 = pack.pwr.size.toDouble()
        for(x in 1..pack.pwr.size)
        {
            Log.i("Graph",pack.pwr[x-1].toString())
            if(pack.pwr[x-1]>maxValY6)
            {
                maxValY6=pack.pwr[x-1]
            }
            if(selected=="ALL")
            {
                distArr6[x-1] = (DataPoint(x.toDouble(), pack.pwr[x-1]))
            }
            else
            {
                val thisDateStr = pack.dates[x-1]
                val datesplit = thisDateStr.split("-")
                val year = (datesplit[0].replace("\\s".toRegex(), "")).toInt()
                val month = (datesplit[1].replace("\\s".toRegex(), "")).toInt()
                val day = (datesplit[2].replace("\\s".toRegex(), "")).toInt()
                val thisDate = Calendar.getInstance().apply{ set(year, month, day) }.time
                val newCal = Calendar.getInstance()
                newCal.time = currentdate
                if(selected=="1D")
                {
                    newCal.add(Calendar.DAY_OF_YEAR, -1)
                    val oneDay = newCal.time
                    if(thisDate.after(oneDay))
                    {
                        distArr6[x-1] = (DataPoint(x.toDouble(), pack.pwr[x-1]))
                    }

                }
                else if(selected=="1W")
                {
                    newCal.add(Calendar.WEEK_OF_YEAR, -1)
                    val oneWeek = newCal.time
                    if(thisDate.after(oneWeek))
                    {
                        distArr6[x-1] = (DataPoint(x.toDouble(), pack.pwr[x-1]))
                    }
                }
                else if(selected=="1M")
                {
                    newCal.add(Calendar.MONTH, -1)
                    val oneMonth = newCal.time
                    if(thisDate.after(oneMonth))
                    {
                        distArr6[x-1] = (DataPoint(x.toDouble(), pack.pwr[x-1]))
                    }
                }
            }
        }
        val series6: LineGraphSeries<DataPoint> = LineGraphSeries(distArr6)

        lineGraphView6.animate()
        lineGraphView6.animate()
        lineGraphView6.viewport.isScrollable = true
        lineGraphView6.viewport.isScalable = true
        lineGraphView6.viewport.setScalableY(true)
        lineGraphView6.viewport.setScrollableY(true)
        lineGraphView6.viewport.setMaxX(maxValX6)
        lineGraphView6.viewport.setMaxY(maxValY6)
        lineGraphView6.viewport.setMinX(1.0)
        lineGraphView6.viewport.setMinY(0.0)
        lineGraphView6.title = "Power Graph"
        lineGraphView6.gridLabelRenderer.horizontalAxisTitle = "Ride Number"
        lineGraphView6.gridLabelRenderer.verticalAxisTitle = "Average Power (Watts)"
        series6.color = R.color.purple_200
        lineGraphView6.addSeries(series6)

        //////GRAPH2//////
        val lineGraphView7: GraphView = binding.histPitch
        var distArr7 = Array<DataPoint>(pack.pitch.size){DataPoint(0.0,0.0)}
        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

        var maxValY7 = 0.0
        var maxValX7 = pack.pitch.size.toDouble()
        for(x in 1..pack.pitch.size)
        {
            Log.i("Graph",pack.pitch[x-1].toString())
            if(pack.pitch[x-1]>maxValY7)
            {
                maxValY7=pack.pitch[x-1]
            }
            if(selected=="ALL")
            {
                distArr7[x-1] = (DataPoint(x.toDouble(), pack.pitch[x-1]))
            }
            else
            {
                val thisDateStr = pack.dates[x-1]
                val datesplit = thisDateStr.split("-")
                val year = (datesplit[0].replace("\\s".toRegex(), "")).toInt()
                val month = (datesplit[1].replace("\\s".toRegex(), "")).toInt()
                val day = (datesplit[2].replace("\\s".toRegex(), "")).toInt()
                val thisDate = Calendar.getInstance().apply{ set(year, month, day) }.time
                val newCal = Calendar.getInstance()
                newCal.time = currentdate
                if(selected=="1D")
                {
                    newCal.add(Calendar.DAY_OF_YEAR, -1)
                    val oneDay = newCal.time
                    if(thisDate.after(oneDay))
                    {
                        distArr7[x-1] = (DataPoint(x.toDouble(), pack.pitch[x-1]))
                    }

                }
                else if(selected=="1W")
                {
                    newCal.add(Calendar.WEEK_OF_YEAR, -1)
                    val oneWeek = newCal.time
                    if(thisDate.after(oneWeek))
                    {
                        distArr7[x-1] = (DataPoint(x.toDouble(), pack.pitch[x-1]))
                    }
                }
                else if(selected=="1M")
                {
                    newCal.add(Calendar.MONTH, -1)
                    val oneMonth = newCal.time
                    if(thisDate.after(oneMonth))
                    {
                        distArr7[x-1] = (DataPoint(x.toDouble(), pack.pitch[x-1]))
                    }
                }
            }
        }
        val series7: LineGraphSeries<DataPoint> = LineGraphSeries(distArr7)

        lineGraphView7.animate()
        lineGraphView7.animate()
        lineGraphView7.viewport.isScrollable = true
        lineGraphView7.viewport.isScalable = true
        lineGraphView7.viewport.setScalableY(true)
        lineGraphView7.viewport.setScrollableY(true)
        lineGraphView7.viewport.setMaxX(maxValX7)
        lineGraphView7.viewport.setMaxY(maxValY7)
        lineGraphView7.viewport.setMinX(1.0)
        lineGraphView7.viewport.setMinY(0.0)
        lineGraphView7.title = "Pitch Graph"
        lineGraphView7.gridLabelRenderer.horizontalAxisTitle = "Ride Number"
        lineGraphView7.gridLabelRenderer.verticalAxisTitle = "Average Pitch (Degrees)"
        series7.color = R.color.purple_200
        lineGraphView7.addSeries(series7)

        //////GRAPH2//////
        val lineGraphView8: GraphView = binding.histCals
        var distArr8 = Array<DataPoint>(pack.calories.size){DataPoint(0.0,0.0)}
        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

        var maxValY8 = 0.0
        var maxValX8 = pack.calories.size.toDouble()
        for(x in 1..pack.calories.size)
        {
            Log.i("Graph",pack.calories[x-1].toString())
            if(pack.calories[x-1]>maxValY2)
            {
                maxValY8=pack.calories[x-1]
            }
            if(selected=="ALL")
            {
                distArr8[x-1] = (DataPoint(x.toDouble(), pack.calories[x-1]))
            }
            else
            {
                val thisDateStr = pack.dates[x-1]
                val datesplit = thisDateStr.split("-")
                val year = (datesplit[0].replace("\\s".toRegex(), "")).toInt()
                val month = (datesplit[1].replace("\\s".toRegex(), "")).toInt()
                val day = (datesplit[2].replace("\\s".toRegex(), "")).toInt()
                val thisDate = Calendar.getInstance().apply{ set(year, month, day) }.time
                val newCal = Calendar.getInstance()
                newCal.time = currentdate
                if(selected=="1D")
                {
                    newCal.add(Calendar.DAY_OF_YEAR, -1)
                    val oneDay = newCal.time
                    if(thisDate.after(oneDay))
                    {
                        distArr8[x-1] = (DataPoint(x.toDouble(), pack.calories[x-1]))
                    }

                }
                else if(selected=="1W")
                {
                    newCal.add(Calendar.WEEK_OF_YEAR, -1)
                    val oneWeek = newCal.time
                    if(thisDate.after(oneWeek))
                    {
                        distArr8[x-1] = (DataPoint(x.toDouble(), pack.calories[x-1]))
                    }
                }
                else if(selected=="1M")
                {
                    newCal.add(Calendar.MONTH, -1)
                    val oneMonth = newCal.time
                    if(thisDate.after(oneMonth))
                    {
                        distArr8[x-1] = (DataPoint(x.toDouble(), pack.calories[x-1]))
                    }
                }
            }
        }
        val series8: LineGraphSeries<DataPoint> = LineGraphSeries(distArr8)

        lineGraphView8.animate()
        lineGraphView8.animate()
        lineGraphView8.viewport.isScrollable = true
        lineGraphView8.viewport.isScalable = true
        lineGraphView8.viewport.setScalableY(true)
        lineGraphView8.viewport.setScrollableY(true)
        lineGraphView8.viewport.setMaxX(maxValX8)
        lineGraphView8.viewport.setMaxY(maxValY8)
        lineGraphView8.viewport.setMinX(1.0)
        lineGraphView8.viewport.setMinY(0.0)
        lineGraphView8.title = "Calories Graph"
        lineGraphView8.gridLabelRenderer.horizontalAxisTitle = "Ride Number"
        lineGraphView8.gridLabelRenderer.verticalAxisTitle = "Average Calories (kCals)"
        series8.color = R.color.purple_200
        lineGraphView8.addSeries(series8)


        /*
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        */
        return root
    }

    @SuppressLint("NewApi")
    private fun readData(binding: FragmentDashboardBinding): datapack
    {
        /*
        val f = File(context?.filesDir, "data.csv")
        if(f.exists())
        {
            f.delete()
        }
        */

        val f = File(context?.filesDir, "data.csv")
        var numLines = 0
        if(f.exists())
        {
            f.forEachLine{numLines++}
        }


        Log.i("ReadData", "Entered")
        var dat = Vector<String>()
        var dur = Vector<Int>()
        var dis = Vector<Double>()
        var speed = Vector<Double>()
        var ped = Vector<Double>()
        var cad = Vector<Double>()
        var pwr = Vector<Double>()
        var pitch = Vector<Double>()
        var calories = Vector<Double>()

        var lastRideRPM = Vector<Double>()
        var lastRideSpeed = Vector<Double>()
        var lastRideDist = Vector<Double>()
        var lastRideCad = Vector<Double>()
        var lastRidePWR = Vector<Double>()
        var lastRidePitch = Vector<Double>()
        var lastRideCals = Vector<Double>()

        var totalDuration = 0.0
        var totalDistance = 0.0
        var totalSpeed = 0.0

        var rideDuration = 0.0
        var rideDistance = 0.0
        var rideSpeed = 0.0

        try{
            Log.i("ReadData", "Try")
            //check exists
            val f = File(context?.filesDir, "data.csv")
            if(!f.exists())
            {
                Log.i("ReadData", "REJECTED - File does NOT exist")
                dat.add("")
                dur.add(0)
                dis.add(0.0)
                speed.add(0.0)
                ped.add(0.0)
                totalDuration = 0.0
                totalDistance = 0.0
                totalSpeed = 0.0
                return datapack(dat, dur, dis, speed, ped, cad, pwr, pitch, calories, totalDuration, totalDistance, totalSpeed)
            }
            else
            {
                Log.i("ReadData", "ACCEPTED - File Exists")
            }
            //check num files
            val n = Files.lines(f.toPath()).count()
            Log.i("DashboardRead","numlines: $n")
            if(n.toInt()==0)
            {
                Log.i("ReadData", "REJECTED - File is EMPTY")
                dat.add("")
                dur.add(0)
                dis.add(0.0)
                speed.add(0.0)
                ped.add(0.0)
                totalDuration = 0.0
                totalDistance = 0.0
                totalSpeed = 0.0
                f.delete()
                return datapack(dat, dur, dis, speed, ped, cad, pwr, pitch, calories, totalDuration, totalDistance, totalSpeed)
            }
            else
            {
                Log.i("ReadData", "ACCEPTED - File has $n lines")
            }

            var inp = context?.openFileInput("data.csv")

            var count = 0
            if(inp!=null)
            {
                Log.i("ReadData", "Reading1")
                var inpReader = InputStreamReader(inp)
                Log.i("ReadData", "Reading2")
                var buffRead = BufferedReader(inpReader)
                Log.i("ReadData", "Reading3")
                var line = buffRead.readLine()
                Log.i("ReadData", "Reading4")
                Log.i("ReadData", line.toString())
                while(line!=null)
                {

                    line = buffRead.readLine()
                    if(line==null)
                    {
                        Log.w("ReadData", "BROKEN")
                        break
                    }
                    Log.i("ReadData", "Reading!!!")
                    val temp = line.split(",").toTypedArray()
                    dat.add(temp[1])

                    var tempDuration = temp[2]
                    val t1 = tempDuration.split("h", "m")
                    val h = t1[0].trim().toInt()
                    val m = t1[1].split("s")[0].toInt()
                    val s = t1[2].dropLast(1).toInt()

                    val timeinSeconds = h*3600+m*60+s

                    dur.add(timeinSeconds/60)
                    dis.add(BigDecimal(temp[3].toDouble()).setScale(1, RoundingMode.HALF_EVEN).toDouble())
                    speed.add(BigDecimal(temp[4].toDouble()).setScale(1, RoundingMode.HALF_EVEN).toDouble())
                    ped.add(BigDecimal(temp[5].toDouble()).setScale(1, RoundingMode.HALF_EVEN).toDouble())
                    cad.add(BigDecimal(temp[6].toDouble()).setScale(1, RoundingMode.HALF_EVEN).toDouble())
                    pwr.add(BigDecimal(temp[7].toDouble()).setScale(1, RoundingMode.HALF_EVEN).toDouble())
                    pitch.add(BigDecimal(temp[8].toDouble()).setScale(1, RoundingMode.HALF_EVEN).toDouble())
                    calories.add(BigDecimal(temp[9].toDouble()/1000).setScale(1, RoundingMode.HALF_EVEN).toDouble())

                    totalDuration += timeinSeconds
                    totalDistance += temp[3].toDouble()
                    totalSpeed += temp[4].toDouble()
                    count++
                    //last ride info
                    if(count==numLines-1)
                    {
                        rideDuration+=timeinSeconds
                        rideDistance+=temp[3].toDouble()
                        rideSpeed+=temp[4].toDouble()

                        //for the sake of filter testing
                        Log.i("datestuff","${temp[1]}")
                        val datesplit = temp[1].split("-")
                        Log.i("datestuff","${datesplit[0]}")
                        val year = (datesplit[0].replace("\\s".toRegex(), "")).toInt()
                        val month = (datesplit[1].replace("\\s".toRegex(), "")).toInt()
                        val day = (datesplit[2].replace("\\s".toRegex(), "")).toInt()
                        currentdate = Calendar.getInstance().apply{ set(year, month, day) }.time

                        val lrDurText: TextView = binding.lrDurText
                        val lrSpeedText: TextView = binding.lrSpeedText
                        val lrDistanceText: TextView = binding.lrDistanceText

                        val tempH = (rideDuration/3600).toInt()
                        val tempM = ((rideDuration-tempH*3600)/60).toInt()
                        val tempS = (rideDuration-tempH*3600-tempM*60).toInt()
                        val dashDuration = "" + abs(tempH) + "H " + abs(tempM) + "M " + abs(tempS) + "S"
                        val dashDistance = BigDecimal(rideDistance).setScale(1, RoundingMode.HALF_EVEN).toDouble()
                        val dashSpeed = BigDecimal(rideSpeed).setScale(1, RoundingMode.HALF_EVEN).toDouble()

                        lrDurText.text = (dashDuration).toString()
                        lrSpeedText.text = (dashDistance).toString() + " Miles"
                        lrDistanceText.text = (dashSpeed).toString() + " MPH"


                        //lrDurText
                        //lrSpeedText
                        //lrDistanceText

                        for(x in 10..temp.size-1)
                        {
                            val lastridetemp = temp[x].split("&").toTypedArray()
                            Log.i("Loopy", "${lastridetemp[2]}")
                            Log.i("Loopy", "${temp.size}")
                            lastRideRPM.add(lastridetemp[2].toDouble())
                            lastRideSpeed.add(lastridetemp[1].toDouble())
                            lastRideDist.add(lastridetemp[0].toDouble())
                            lastRideCad.add(lastridetemp[3].toDouble())
                            lastRidePWR.add(lastridetemp[4].toDouble())
                            lastRidePitch.add(lastridetemp[5].toDouble())
                            lastRideCals.add(lastridetemp[6].toDouble()/1000)
                        }
                        //${intDist[x]}&${intSpeed[x]}&${intRPM[x]}&${intCad[x]}&${intPWR[x]}&${intCal[x]}"

                        //////GRAPH1//////

                        val lineGraphView: GraphView = binding.lastRideRPM
                        var distArr = Array<DataPoint>(lastRideRPM.size){DataPoint(0.0,0.0)}
                        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

                        var maxValY = 0.0
                        var maxValX = lastRideRPM.size.toDouble()
                        for(x in 1..lastRideRPM.size)
                        {
                            Log.i("Graph",lastRideRPM[x-1].toString())
                            if(lastRideRPM[x-1]>maxValY)
                            {
                                maxValY=lastRideRPM[x-1]
                            }
                            distArr = distArr.plus(DataPoint(x.toDouble(), lastRideRPM[x-1]))
                        }
                        val series: LineGraphSeries<DataPoint> = LineGraphSeries(distArr)

                        lineGraphView.animate()
                        lineGraphView.animate()
                        lineGraphView.viewport.isScrollable = true
                        lineGraphView.viewport.isScalable = true
                        lineGraphView.viewport.setScalableY(true)
                        lineGraphView.viewport.setScrollableY(true)
                        lineGraphView.viewport.setMaxX(maxValX)
                        lineGraphView.viewport.setMaxY(maxValY)
                        lineGraphView.viewport.setMinX(0.0)
                        lineGraphView.viewport.setMinY(0.0)
                        lineGraphView.title = "RPM Graph"
                        lineGraphView.gridLabelRenderer.horizontalAxisTitle = "Interval (15 Minutes)"
                        lineGraphView.gridLabelRenderer.verticalAxisTitle = "Revolutions per Minute"
                        series.color = R.color.purple_200
                        lineGraphView.addSeries(series)

                        //graph 2
                        val lineGraphView2: GraphView = binding.lastRideSpeed
                        var distArr2 = Array<DataPoint>(lastRideSpeed.size){DataPoint(0.0,0.0)}
                        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

                        var maxValY2 = 0.0
                        var maxValX2 = lastRideSpeed.size.toDouble()
                        for(x in 1..lastRideSpeed.size)
                        {
                            Log.i("Graph",lastRideSpeed[x-1].toString())
                            if(lastRideSpeed[x-1]>maxValY2)
                            {
                                maxValY2=lastRideSpeed[x-1]
                            }
                            distArr2 = distArr2.plus(DataPoint(x.toDouble(), lastRideSpeed[x-1]))
                        }
                        val series2: LineGraphSeries<DataPoint> = LineGraphSeries(distArr2)

                        lineGraphView2.animate()
                        lineGraphView2.animate()
                        lineGraphView2.viewport.isScrollable = true
                        lineGraphView2.viewport.isScalable = true
                        lineGraphView2.viewport.setScalableY(true)
                        lineGraphView2.viewport.setScrollableY(true)
                        lineGraphView2.viewport.setMaxX(maxValX2)
                        lineGraphView2.viewport.setMaxY(maxValY2)
                        lineGraphView2.viewport.setMinX(0.0)
                        lineGraphView2.viewport.setMinY(0.0)
                        lineGraphView2.title = "Speed Graph"
                        lineGraphView2.gridLabelRenderer.horizontalAxisTitle = "Interval (15 Minutes)"
                        lineGraphView2.gridLabelRenderer.verticalAxisTitle = "Speed (MPH)"
                        series2.color = R.color.purple_200
                        lineGraphView2.addSeries(series2)

                        //graph 3
                        val lineGraphView3: GraphView = binding.lastRideDist
                        var distArr3 = Array<DataPoint>(lastRideDist.size){DataPoint(0.0,0.0)}
                        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

                        var maxValY3 = 0.0
                        var maxValX3 = lastRideDist.size.toDouble()
                        for(x in 1..lastRideDist.size)
                        {
                            Log.i("Graph",lastRideDist[x-1].toString())
                            if(lastRideDist[x-1]>maxValY3)
                            {
                                maxValY3=lastRideDist[x-1]
                            }
                            distArr3 = distArr3.plus(DataPoint(x.toDouble(), lastRideDist[x-1]))
                        }
                        val series3: LineGraphSeries<DataPoint> = LineGraphSeries(distArr3)

                        lineGraphView3.animate()
                        lineGraphView3.animate()
                        lineGraphView3.viewport.isScrollable = true
                        lineGraphView3.viewport.isScalable = true
                        lineGraphView3.viewport.setScalableY(true)
                        lineGraphView3.viewport.setScrollableY(true)
                        lineGraphView3.viewport.setMaxX(maxValX3)
                        lineGraphView3.viewport.setMaxY(maxValY3)
                        lineGraphView3.viewport.setMinX(0.0)
                        lineGraphView3.viewport.setMinY(0.0)
                        lineGraphView3.title = "Distance Graph"
                        lineGraphView3.gridLabelRenderer.horizontalAxisTitle = "Interval (15 Minutes)"
                        lineGraphView3.gridLabelRenderer.verticalAxisTitle = "Distance per Interval (Miles)"
                        series3.color = R.color.purple_200
                        lineGraphView3.addSeries(series3)

                        //graph 4
                        val lineGraphView4: GraphView = binding.lastRideCad
                        var distArr4 = Array<DataPoint>(lastRideCad.size){DataPoint(0.0,0.0)}
                        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

                        var maxValY4 = 0.0
                        var maxValX4 = lastRideCad.size.toDouble()
                        for(x in 1..lastRideCad.size)
                        {
                            Log.i("Graph",lastRideCad[x-1].toString())
                            if(lastRideCad[x-1]>maxValY)
                            {
                                maxValY=lastRideCad[x-1]
                            }
                            distArr4 = distArr4.plus(DataPoint(x.toDouble(), lastRideCad[x-1]))
                        }
                        val series4: LineGraphSeries<DataPoint> = LineGraphSeries(distArr4)

                        lineGraphView4.animate()
                        lineGraphView4.animate()
                        lineGraphView4.viewport.isScrollable = true
                        lineGraphView4.viewport.isScalable = true
                        lineGraphView4.viewport.setScalableY(true)
                        lineGraphView4.viewport.setScrollableY(true)
                        lineGraphView4.viewport.setMaxX(maxValX4)
                        lineGraphView4.viewport.setMaxY(maxValY4)
                        lineGraphView4.viewport.setMinX(0.0)
                        lineGraphView4.viewport.setMinY(0.0)
                        lineGraphView4.title = "Cadence Graph"
                        lineGraphView4.gridLabelRenderer.horizontalAxisTitle = "Interval (15 Minutes)"
                        lineGraphView4.gridLabelRenderer.verticalAxisTitle = "Cadence per Interval (RPM)"
                        series4.color = R.color.purple_200
                        lineGraphView4.addSeries(series4)

                        //graph 5
                        val lineGraphView5: GraphView = binding.lastRidePWR
                        var distArr5 = Array<DataPoint>(lastRidePWR.size){DataPoint(0.0,0.0)}
                        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

                        var maxValY5 = 0.0
                        var maxValX5 = lastRidePWR.size.toDouble()
                        for(x in 1..lastRidePWR.size)
                        {
                            Log.i("Graph",lastRidePWR[x-1].toString())
                            if(lastRidePWR[x-1]>maxValY5)
                            {
                                maxValY5=lastRidePWR[x-1]
                            }
                            distArr5 = distArr5.plus(DataPoint(x.toDouble(), lastRidePWR[x-1]))
                        }
                        val series5: LineGraphSeries<DataPoint> = LineGraphSeries(distArr5)

                        lineGraphView5.animate()
                        lineGraphView5.animate()
                        lineGraphView5.viewport.isScrollable = true
                        lineGraphView5.viewport.isScalable = true
                        lineGraphView5.viewport.setScalableY(true)
                        lineGraphView5.viewport.setScrollableY(true)
                        lineGraphView5.viewport.setMaxX(maxValX5)
                        lineGraphView5.viewport.setMaxY(maxValY5)
                        lineGraphView5.viewport.setMinX(0.0)
                        lineGraphView5.viewport.setMinY(0.0)
                        lineGraphView5.title = "Power Graph"
                        lineGraphView5.gridLabelRenderer.horizontalAxisTitle = "Interval (15 Minutes)"
                        lineGraphView5.gridLabelRenderer.verticalAxisTitle = "Power per Interval (Watts)"
                        series5.color = R.color.purple_200
                        lineGraphView5.addSeries(series5)

                        //graph 2
                        val lineGraphView6: GraphView = binding.lastRidePitch
                        var distArr6 = Array<DataPoint>(lastRidePitch.size){DataPoint(0.0,0.0)}
                        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

                        var maxValY6 = 0.0
                        var maxValX6 = lastRidePitch.size.toDouble()
                        for(x in 1..lastRidePitch.size)
                        {
                            Log.i("Graph",lastRidePitch[x-1].toString())
                            if(lastRidePitch[x-1]>maxValY6)
                            {
                                maxValY6=lastRidePitch[x-1]
                            }
                            distArr6 = distArr6.plus(DataPoint(x.toDouble(), lastRidePitch[x-1]))
                        }
                        val series6: LineGraphSeries<DataPoint> = LineGraphSeries(distArr6)

                        lineGraphView6.animate()
                        lineGraphView6.animate()
                        lineGraphView6.viewport.isScrollable = true
                        lineGraphView6.viewport.isScalable = true
                        lineGraphView6.viewport.setScalableY(true)
                        lineGraphView6.viewport.setScrollableY(true)
                        lineGraphView6.viewport.setMaxX(maxValX6)
                        lineGraphView6.viewport.setMaxY(maxValY6)
                        lineGraphView6.viewport.setMinX(0.0)
                        lineGraphView6.viewport.setMinY(0.0)
                        lineGraphView6.title = "Pitch Graph"
                        lineGraphView6.gridLabelRenderer.horizontalAxisTitle = "Interval (15 Minutes)"
                        lineGraphView6.gridLabelRenderer.verticalAxisTitle = "Pitch per Interval (Degrees)"
                        series6.color = R.color.purple_200
                        lineGraphView6.addSeries(series6)

                        //graph 7
                        val lineGraphView7: GraphView = binding.lastRideCals
                        var distArr7 = Array<DataPoint>(lastRideCals.size){DataPoint(0.0,0.0)}
                        //var listy = List<DataPoint>(pack.distance.size, DataPoint(0.0,0.0))

                        var maxValY7 = 0.0
                        var maxValX7 = lastRideCals.size.toDouble()
                        for(x in 1..lastRideCals.size)
                        {
                            Log.i("Graph",lastRideCals[x-1].toString())
                            if(lastRideCals[x-1]>maxValY7)
                            {
                                maxValY7=lastRideCals[x-1]
                            }
                            distArr7 = distArr7.plus(DataPoint(x.toDouble(), lastRideCals[x-1]))
                        }
                        val series7: LineGraphSeries<DataPoint> = LineGraphSeries(distArr7)

                        lineGraphView7.animate()
                        lineGraphView7.animate()
                        lineGraphView7.viewport.isScrollable = true
                        lineGraphView7.viewport.isScalable = true
                        lineGraphView7.viewport.setScalableY(true)
                        lineGraphView7.viewport.setScrollableY(true)
                        lineGraphView7.viewport.setMaxX(maxValX7)
                        lineGraphView7.viewport.setMaxY(maxValY7)
                        lineGraphView7.viewport.setMinX(0.0)
                        lineGraphView7.viewport.setMinY(0.0)
                        lineGraphView7.title = "Calories Graph"
                        lineGraphView7.gridLabelRenderer.horizontalAxisTitle = "Interval (15 Minutes)"
                        lineGraphView7.gridLabelRenderer.verticalAxisTitle = "Calories Burned per Interval (KCAL)"
                        series7.color = R.color.purple_200
                        lineGraphView7.addSeries(series7)

                        /*
                        val lastridetemp = temp[x].split("&").toTypedArray()
                            lastRideRPM.add(lastridetemp[2].toDouble())
                            lastRideSpeed.add(lastridetemp[1].toDouble())
                            lastRideDist.add(lastridetemp[0].toDouble())
                            lastRideCad.add(lastridetemp[3].toDouble())
                            lastRidePWR.add(lastridetemp[4].toDouble())
                            lastRidePitch.add(lastridetemp[5].toDouble())
                            lastRideCals.add(lastridetemp[6].toDouble())
                         */
                    }

                    /*
                    var outString = "$x, $date, $duration, $distance, $speed, $rpm, $cad, $pwr, $pitch, $calories"

                    for(x in 0..numIntervals)
                    {
                        outString +=", ${intDist[x]}&${intSpeed[x]}&${intRPM[x]}&${intCad[x]}&${intPWR[x]}&${intCal[x]}"
                    }
                     */

                    //line=buffRead.readLine()
                }
                buffRead.close()
                inpReader.close()
                inp.close()
            }
        }
        catch(e: FileNotFoundException)
        {
            //Log.e("Dashboard", "File not found$e")
            dat.add("")
            dur.add(0)
            dis.add(0.0)
            speed.add(0.0)
            ped.add(0.0)
            totalDuration = 0.0
            totalDistance = 0.0
            totalSpeed = 0.0
            return datapack(dat, dur, dis, speed, ped, cad, pwr, pitch, calories, totalDuration, totalDistance, totalSpeed)
        }
        catch(e: IOException)
        {
            Log.e("Dashboard", "Cannot read file: $e")
        }

        return datapack(dat, dur, dis, speed, ped, cad, pwr, pitch, calories, totalDuration, totalDistance, totalSpeed)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    /*
    override fun onDataReceived(data: String)
    {
        Log.i("[Dashboard] onDataReceived", data)
    }
    */
}