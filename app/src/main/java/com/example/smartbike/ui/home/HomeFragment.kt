package com.example.smartbike.ui.home

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.smartbike.R
import com.example.smartbike.databinding.FragmentHomeBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.android.PolyUtil
import com.google.maps.model.ComponentFilter.route
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.TimeUnit


class HomeFragment : Fragment(), OnMapReadyCallback{

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    private lateinit var gMap: GoogleMap
    private lateinit var mapView: MapView
    private lateinit var mapFrag: SupportMapFragment

    private lateinit var searchBtn: Button

    private var startTime: Long = 0
    private var endTime: Long = 0


    private var lastMark: Marker? = null

    private var resetCam = true

    private lateinit var speedText: TextView;
    private lateinit var incText: TextView;
    private lateinit var distText: TextView;
    private lateinit var rpmText: TextView;
    private lateinit var cadText: TextView;
    private lateinit var pwrText: TextView;

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
    private var totcalories = 0.0;

    private var interval = 1.0;
    private var lastSpeed = 0.0;
    private var lastRPM = 0.0;
    private var lastCad = 0.0;
    private var lastInc = 0.0;
    private var lastPWR = 0.0;

    private lateinit var userLocation: Location


    /*
    # Intervals (N)
    Speed
    RPM
    Cadence
    Incline
    Power
    Total Distance
    7 rows
    N columns
     */
    var intervalData: MutableList<MutableList<Double>> = mutableListOf()

    val apiKey="AIzaSyCz07lN-GCqnb01-F8NwFO9obIlaB3G64c"


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        Log.i("[HOMEFRAG] OnCreateView1","binding: $_binding")
        //val binding = _binding!!
        val root: View = binding.root
        var started = false
        var button: Button = binding.startendbtn
        Log.i("[HOMEFRAG] OnCreateView2","binding: $_binding")
        binding.startendbtn.setOnClickListener{
            started = !started
            val intent = Intent("StartTransmission")


            if(started) //begin
            {
                button.text = "End"
                startTime = System.currentTimeMillis()
                intent.putExtra("data", "STARTING")
            }
            else //end
            {
                //genData();
                button.text = "Start"
                intent.putExtra("data", "ENDING")
                endTime = System.currentTimeMillis()
                recordInterval()
                writeToFile()
            }
            requireActivity().sendBroadcast(intent)
        }
        Log.i("[HOMEFRAG] OnCreateView3","binding: $_binding")

        val searchBtn = binding.searchButton
        searchBtn.setOnClickListener{
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(requireContext())
            startAutocomplete.launch(intent)
        }
        Log.i("[HOMEFRAG] OnCreateView4","binding: $_binding")



        speedText = binding.speedText
        incText = binding.inclineText
        distText = binding.distanceText
        rpmText = binding.RPMText
        cadText = binding.CadText
        pwrText = binding.PowerText

        Log.i("[HOMEFRAG] OnCreateView5","binding: $_binding")

        Log.i("[HOMEFRAG] OnCreateView6","speedText: $speedText")



        Log.i("onCreateView HOMEFRAG", "FINISHED")


        return root
    }




    private val startAutocomplete =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val place = Autocomplete.getPlaceFromIntent(intent)
                    Log.i(
                        "startAuto1", "Place: ${place.name}, ${place.id}, ${place.latLng}"
                    )
                    plotRoute(place)
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                // The user canceled the operation.
                Log.i("startAuto2", "User canceled autocomplete")
            }
        }

    private fun plotRoute(place: Place)
    {
        val origin = LatLng(userLocation.latitude, userLocation.longitude)
        val destin = place.id

        val urlString=
            "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=${origin.latitude},${origin.longitude}" +
                    "&destination=place_id:$destin" +
                    "&mode=bicycling"+
                    "&key=$apiKey"

        Log.i("URLSTRING", "$urlString")

        val url = URL(urlString)
        val connect = url.openConnection() as HttpURLConnection
        try{
            val inputStream = connect.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val result = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                result.append(line)
            }

            Log.i("RESULTPLOT", "$result")

        } finally {
            connect.disconnect()
        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("HOMEFRAG", "onViewCreated")

        mapView = view.findViewById<MapView>(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        Places.initialize(requireContext(), apiKey)

        //val placesClient = Places.createClient(requireContext())

        val filter = IntentFilter("DataTransmission")
        getActivity()?.registerReceiver(receiver, filter)

    }

    private fun recordInterval()
    {
        //TODO
        val mets = (6..10).random()
        var userage = 21
        val userweight = 150
        val userfeet = 5
        val userinch = 8
        var bmr = 88.362 + (13.397*userweight/2.205) + (4.799*(userfeet*12+userinch)*2.54) - (5.677*userage)
        val cals = bmr*mets/(24*0.25)

        totcalories += cals

        val temp = mutableListOf(interval, lastSpeed, lastRPM, lastCad, lastInc, lastPWR, totDistance, cals)
        intervalData.add(temp)
        interval++;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun writeToFile()
    {
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




        val date = "$year-$month-$day"

        var outString = "$numLines, $date, $duration, $distance, $speed, $rpm, $cad, $pwr, $pitch, $totcalories"

        Log.i("PRELOOP WRITING", "$intervalData")
        for(x in 0..interval.toInt()-2)
        {
            Log.i("WRITING", "$x")
            val row = intervalData[x]
            outString+=", ${row[6]}&${row[1]}&${row[2]}&${row[3]}&${row[5]}&${row[4]}&${row[7]}"
            /*
            # Intervals (N) 0
            Speed 1
            RPM 2
            Cadence 3
            Incline 4
            Power 5
            Total Distance 6
            Calories 7
             */
        }
        outString+="\n"
        outStream.write(outString)
        outStream.close()
        Log.i("HOME WriteToFile", "Complete")
    }

    private val receiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?){
            val intake = intent?.getStringExtra("data")
            if(intake!=null)
            {
                val toArr = intake.split(':')
                val dataType = toArr[0]
                val data = toArr[1]
                /*
                Log.i("HOMEFRAG RECEIVER", "data: $data")
                Log.i("HOMEFRAG RECEIVER", "speedText: $speedText")
                */
                if(dataType=="s")
                {
                    speedText.text = data + " MPH"
                    speedDP++
                    avgSpeed += data.toDouble()
                    lastSpeed = data.toDouble()
                }
                else if(dataType=="i")
                {
                    incText.text = "∠" + data + "°"
                    incDP++
                    avgInc += data.toDouble()
                    lastInc = data.toDouble()
                }
                else if(dataType=="d")
                {
                    distText.text = data + " Miles"
                    totDistance = data.toDouble()
                }
                else if(dataType=="r")
                {
                    rpmText.text = data + " RPM (Tire)"
                    rpmDP++
                    avgRPM += data.toDouble()
                    lastRPM = data.toDouble()
                }
                else if(dataType=="c")
                {
                    cadText.text = data + " RPM (Cad)"
                    cadDP++
                    avgCad += data.toDouble()
                    lastCad = data.toDouble()
                }
                else if(dataType=="p")
                {
                    pwrText.text = data + " Watts"
                    pwrDP++
                    avgPWR += data.toDouble()
                    lastPWR = data.toDouble()
                }

                if(((startTime-System.currentTimeMillis())/(60000*15))>=interval)
                {
                    /*
                    elapsed (ms) = startTime-current time in milli
                    elapsed (min) = elapsed(ms)/60000
                    interval # = elapsed(min)/15
                     */
                    recordInterval()
                }

            }
            else
            {
                Log.w("[HOMEFRAG]", "broadcast receiver got NULL DATA")
            }

        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroyView() {
        super.onDestroyView()
        Log.i("HOMEFRAG", "onDestroyView")

        if(speedDP>0 || cadDP>0)
        {
            recordInterval()
            writeToFile()
        }

        _binding = null
    }


    private fun searchInit(query: String, map: GoogleMap)
    {
        Log.i("HOMEFRAG", "searchInit")


        val geocodeAPI = GeoApiContext.Builder()
            .apiKey("$apiKey")
            .build()
        val geoRes1 = GeocodingApi.geocode(geocodeAPI, query).await()
        if(geoRes1.isNotEmpty())
        {
            val geoRes = geoRes1[0]
            if(geoRes.geometry!=null)
            {
                Log.i("SearchInit","Entered first stage");
                val dirReq = DirectionsApiRequest(geocodeAPI)
                val fuse = LocationServices.getFusedLocationProviderClient(requireActivity())
                val locInterval = 1000.0
                val locFastInterval = 500.0
                val locMaxWait = 1000.0
                val fusedHelper = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,
                    locInterval.toLong()
                )
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(locFastInterval.toLong())
                    .setMaxUpdateDelayMillis(locMaxWait.toLong())
                    .build()
                //Default at Zach
                var mylat = 30.6213255
                var mylong = -96.3405284
                Log.i("SearchInit","Pre-Pull");
                //Try to pull current location
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }

                fuse.requestLocationUpdates(fusedHelper, object: LocationCallback(){
                    override fun onLocationResult(locRes: LocationResult) {
                        val lastLoc = locRes.lastLocation
                        val user = MarkerOptions()
                            .position(LatLng(lastLoc?.latitude!!, lastLoc?.longitude!!))
                            .title("User Location")
                            //.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.drawable.ic_android)))
                        Log.i("User Loc","${lastLoc.latitude}, ${lastLoc.longitude}")
                        lastMark?.remove()
                        lastMark = map.addMarker(user)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastMark?.position!!, 15f))
                        mylat = lastLoc.latitude
                        mylong = lastLoc.longitude

                        Log.i("SearchInit","Post-Pull");
                        val myloc = "$mylat,$mylong"
                        val dest = geoRes.geometry.location.lat.toString() + "," + geoRes.geometry.location.lng.toString()
                        dirReq.origin(myloc)
                        dirReq.destination(dest)
                        dirReq.mode(TravelMode.BICYCLING)

                        Log.i("HomeFrag: SearchInit", "My Location: $myloc ; Destination: $dest")

                        val client = OkHttpClient().newBuilder().build()
                        val mediaType = "text/plain".toMediaTypeOrNull()
                        //val body = "".toRequestBody(mediaType)

                        Log.i("HomeFrag: SearchInit", "preAPI")


                        GlobalScope.launch{
                            val request = okhttp3.Request.Builder()
                                .url("https://maps.googleapis.com/maps/api/directions/json?origin=$myloc&destination=$dest&mode=bicycling&key=$apiKey")
                                .get()
                                .build()
                            val response = client.newCall(request).execute()

                            Log.i("HomeFrag: SearchInit", "received response")

                            //mapView = view?.findViewById<MapView>(R.id.mapView)!! //ohboy
                            val json = response.body?.string()
                            val jsonResp = JSONObject(json)
                            Log.i("HomeFrag: SearchInit", "parsing json")
                            val routes = jsonResp.getJSONArray("routes")
                            val legs = routes.getJSONObject(0).getJSONArray("legs")
                            val legTemp = legs.getJSONObject(0)
                            Log.i("HomeFrag: SearchInit", "halfway")
                            val startLoc = legTemp.getJSONObject("start_location")
                            val endLoc = legTemp.getJSONObject("end_location")
                            val dist = legTemp.getJSONObject("distance").getString("text")
                            val dur = legTemp.getJSONObject("duration").getString("text")
                            val pts = routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points")
                            Log.i("HomeFrag: SearchInit", "parsing complete")
                            val decodedPts = PolyUtil.decode(pts)
                            Log.i("HomeFrag: SearchInit", "decoding complete")
                            val polylineOptions = PolylineOptions().addAll(decodedPts).color(Color.RED)
                            Log.i("HomeFrag: SearchInit", "polyline created")

                            val startMark = MarkerOptions().position(LatLng(startLoc.getDouble("lat"), startLoc.getDouble("lng"))).title("Start")
                            val endMark = MarkerOptions().position(LatLng(endLoc.getDouble("lat"), endLoc.getDouble("lng"))).title("End")
                            Log.i("HomeFrag: SearchInit", "markers created")
                            //mapView = (activity as FragmentActivity).supportFragmentManager.findFragmentById(R.id.mapView) as MapView
                            Log.i("HomeFrag: SearchInit", "mapFrag created")
                            withContext(Dispatchers.Main)
                            {
                                mapView.getMapAsync{gMap ->
                                    //gMap.addMarker(startMark)
                                    gMap.addMarker(endMark)
                                    gMap.addPolyline(polylineOptions)}
                                Log.i("HomeFrag: SearchInit", "Complete")
                            }

                        }
                    }
                }, null)

            }
            else
            {
                Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
            }
        }
        else
        {
            Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
        }


    }

    override fun onMapReady(map: GoogleMap) {
        Log.i("HOMEFRAG", "onMapReady")

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
        }
        Log.i("HOMEFRAG", "PASSED")

        //Initial Set Up
        gMap = map

        gMap.isMyLocationEnabled = true

        val uiSettings = gMap.uiSettings
        uiSettings.isZoomControlsEnabled = true
        uiSettings.isMyLocationButtonEnabled = true
        uiSettings.isCompassEnabled = true

        Log.i("onMapReady", "1")
        val ZACH = LatLng(30.6213255, -96.3405284)

        val fuse = LocationServices.getFusedLocationProviderClient(requireActivity())
        Log.i("onMapReady", "2")
        val locationReq = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(1000)

        Log.i("onMapReady", "3")
        val locationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                //Log.i("onMapReady", "INSIDE")
                if(location!=null)
                {
                    userLocation = location
                    val userLat = location.latitude
                    val userLong = location.longitude

                    if(resetCam)
                    {
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(userLat, userLong), 15f))
                        resetCam = false
                    }

                }
            }
        }


        fuse.requestLocationUpdates(locationReq, locationCallback, null)

        /*
        val searchView = view?.findViewById<SearchView>(R.id.searchButton)
        searchView?.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean{
                searchInit(query, map)

                searchView.clearFocus()
                searchView.setQuery("", false)
                searchView.isIconified = true
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })
        */
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()

    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }


}
