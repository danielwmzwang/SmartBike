package com.example.smartbike.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.smartbike.R
import com.example.smartbike.databinding.FragmentDashboardBinding
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    data class datapack(val dates: Vector<String>, val durations: Vector<Int>, val distance: Vector<Double>, val speed: Vector<Double>, val pedal: Vector<Double>, val tDuration: Double, val tDistance: Double, val tSpeed: Double)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        Log.i("Dashboard", "ONCREATEVIEW")

        //Read Data and Process for Summary Data
        var pack = readData();

        val txtDBDuration: TextView = binding.textViewDBDuration
        val txtDBDistance: TextView = binding.textViewDBDistance
        val txtDBSpeed: TextView = binding.textViewDBSpeed

        val tempdashDuration = (pack.tDuration/pack.durations.size).toInt()
        val tempH = tempdashDuration/3600
        val tempM = (tempdashDuration-tempH*3600)/60
        val tempS = (tempdashDuration-tempH*3600-tempM*60)
        val dashDuration = "" + (tempdashDuration/3600) + "H" + tempM + "M" + tempS + "S"
        val dashDistance = BigDecimal(pack.tDistance/pack.distance.size).setScale(1, RoundingMode.HALF_EVEN).toDouble()
        val dashSpeed = BigDecimal(pack.tSpeed/pack.speed.size).setScale(1, RoundingMode.HALF_EVEN).toDouble()
        txtDBDuration.text = (if(pack.tDuration== Double.NaN) 0 else dashDuration).toString()
        txtDBDistance.text = (if(pack.tDistance== Double.NaN) 0 else dashDistance).toString() + " Miles"
        txtDBSpeed.text = (if(pack.tSpeed== Double.NaN) 0 else dashSpeed).toString() + " MPH"

        //////GRAPH1//////
        val lineGraphView: GraphView = binding.idGraphView
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
            distArr[x-1] = (DataPoint(x.toDouble(), pack.distance[x-1]))
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
        lineGraphView.title = "Distance Graph"
        lineGraphView.gridLabelRenderer.horizontalAxisTitle = "Ride Number"
        lineGraphView.gridLabelRenderer.verticalAxisTitle = "Distance Travelled (Miles)"
        series.color = R.color.purple_200
        lineGraphView.addSeries(series)

        //////GRAPH2//////
        val lineGraphView2: GraphView = binding.idGraphView2
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
            distArr2[x-1] = (DataPoint(x.toDouble(), pack.speed[x-1]))
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

        /*
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        */
        return root
    }

    private fun readData(): datapack
    {
        /*
        val f = File(context?.filesDir, "data.csv")
        if(f.exists())
        {
            f.delete()
        }
        */


        Log.i("ReadData", "Entered")
        var dat = Vector<String>()
        var dur = Vector<Int>()
        var dis = Vector<Double>()
        var speed = Vector<Double>()
        var ped = Vector<Double>()

        var totalDuration = 0.0
        var totalDistance = 0.0
        var totalSpeed = 0.0

        try{
            Log.i("ReadData", "Try")
            var inp = context?.openFileInput("data.csv")

            if(inp!=null)
            {
                Log.i("ReadData", "Reading1")
                var inpReader = InputStreamReader(inp)
                Log.i("ReadData", "Reading2")
                var buffRead = BufferedReader(inpReader)
                Log.i("ReadData", "Reading3")
                var line = buffRead.readLine()
                Log.i("ReadData", "Reading4")
                Log.i("ReadData", line)
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

                    dur.add(timeinSeconds)
                    dis.add(BigDecimal(temp[3].toDouble()).setScale(1, RoundingMode.HALF_EVEN).toDouble())
                    speed.add(BigDecimal(temp[4].toDouble()).setScale(1, RoundingMode.HALF_EVEN).toDouble())
                    ped.add(BigDecimal(temp[5].toDouble()).setScale(1, RoundingMode.HALF_EVEN).toDouble())

                    totalDuration += timeinSeconds
                    totalDistance += temp[3].toDouble()
                    totalSpeed += temp[4].toDouble()

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
            return datapack(dat, dur, dis, speed, ped, totalDuration, totalDistance, totalSpeed)
        }
        catch(e: IOException)
        {
            Log.e("Dashboard", "Cannot read file: $e")
        }

        return datapack(dat, dur, dis, speed, ped, totalDuration, totalDistance, totalSpeed)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}