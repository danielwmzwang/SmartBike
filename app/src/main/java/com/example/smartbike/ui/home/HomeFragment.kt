package com.example.smartbike.ui.home

/*
Created By: Daniel Wang
Page Purpose:
This page is the workout page
the functions include:
 - User can hit 'start/stop' button to initiate/stop their workout respectively
 - User can search locations/addresses/coordinates/places and get directions to it
    from their current location
 - Users data will save upon finishing
 */

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    //Google Maps Platform related variables
    private lateinit var gMap: GoogleMap
    private lateinit var mapView: MapView
    private lateinit var mapFrag: SupportMapFragment

    private lateinit var searchBtn: Button


    private var startTime: Long = 0
    private var endTime: Long = 0

    //last position
    private var lastMark: Marker? = null
    //when true, reset the camera back onto the user
    private var resetCam = true

    //the TextViews to update with real-time data
    private lateinit var speedText: TextView;
    private lateinit var incText: TextView;
    private lateinit var distText: TextView;
    private lateinit var rpmText: TextView;
    private lateinit var cadText: TextView;
    private lateinit var pwrText: TextView;

    //store local variables for later
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

    //store interval data
    private var interval = 1.0;
    private var lastSpeed = 0.0;
    private var lastRPM = 0.0;
    private var lastCad = 0.0;
    private var lastInc = 0.0;
    private var lastPWR = 0.0;

    //current user location
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
    //actual local interval data storage
    var intervalData: MutableList<MutableList<Double>> = mutableListOf()

    //google api key
    val apiKey="AIzaSyCz07lN-GCqnb01-F8NwFO9obIlaB3G64c"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //setup
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        Log.i("[HOMEFRAG] OnCreateView1","binding: $_binding")
        val root: View = binding.root
        var started = false

        //the start/end button click listener
        var button: Button = binding.startendbtn
        Log.i("[HOMEFRAG] OnCreateView2","binding: $_binding")
        binding.startendbtn.setOnClickListener{
            started = !started
            val intent = Intent("StartTransmission")

            if(started) //begin
            {
                //save the start time for later use
                button.text = "End"
                startTime = System.currentTimeMillis()
                //tell BluetoothService the user has started the workout
                intent.putExtra("data", "STARTING")
            }
            else //end
            {
                //genData();
                button.text = "Start"
                //tell BluetoothService the user has ended the workout
                intent.putExtra("data", "ENDING")
                //collect the end time
                endTime = System.currentTimeMillis()
                //record the last interval
                recordInterval()
                //write all the data to data.csv
                writeToFile()
            }
            //send the broadcast
            requireActivity().sendBroadcast(intent)
        }

        //initiate the search button for users to search for places
        val searchBtn = binding.searchButton
        searchBtn.setOnClickListener{
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(requireContext())
            startAutocomplete.launch(intent)
        }

        //initialize values
        speedText = binding.speedText
        incText = binding.inclineText
        distText = binding.distanceText
        rpmText = binding.RPMText
        cadText = binding.CadText
        pwrText = binding.PowerText

        return root
    }

    //Google Places Search API
    private val startAutocomplete =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            //safety
            if (result.resultCode == Activity.RESULT_OK) {
                //collect the data
                val intent = result.data
                //safety
                if (intent != null) {
                    //get the place information
                    val place = Autocomplete.getPlaceFromIntent(intent)
                    Log.i(
                        "startAuto1", "Place: ${place.name}, ${place.id}, ${place.latLng}"
                    )
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) { //user shut it off
                // The user canceled the operation.
                Log.i("startAuto2", "User canceled autocomplete")
            }
        }

    //route plotting
    private fun plotRoute(place: Place)
    {
        //use users latlong and place id's information
        val origin = LatLng(userLocation.latitude, userLocation.longitude)
        val destin = place.id

        //query string
        val urlString=
            "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=${origin.latitude},${origin.longitude}" +
                    "&destination=place_id:$destin" +
                    "&mode=bicycling"+
                    "&key=$apiKey"

        Log.i("URLSTRING", "$urlString")

        //make the url a url object
        val url = URL(urlString)
        //connect through internet (wifi)
        val connect = url.openConnection() as HttpURLConnection
        try{
            //get the information about the route nad plot
            val inputStream = connect.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val result = StringBuilder()
            var line: String?
            //store it locally
            while (reader.readLine().also { line = it } != null) {
                result.append(line)
            }

            Log.i("RESULTPLOT", "$result")

        } finally {
            //disconnect from internet after completion
            connect.disconnect()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("HOMEFRAG", "onViewCreated")
        //initialize the map after the layout is complete
        mapView = view.findViewById<MapView>(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        //initialize the places api
        Places.initialize(requireContext(), apiKey)
        //enable receiving end of broadcasts
        val filter = IntentFilter("DataTransmission")
        getActivity()?.registerReceiver(receiver, filter)

    }

    //record the interval data
    private fun recordInterval()
    {

        //set up all data associated with calculating calories
        val mets = 8.0
        var userage = 21
        var userweight = 150
        var userfeet = 5
        var userinch = 8
        var male = true

        //retrieve actual data
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
                male = (temp[4].replace("\\s".toRegex(), "")).toBoolean()
                //"$userage, $userfeet, $userinch, $userweight, $isMale"
                userage = temp[0].toInt()
                userfeet = temp[1].toInt()
                userinch = temp[2].toInt()
                userweight = temp[3].toInt()
            }
        }
        //calculate the bmr
        var bmr = 0.0
        if(male)
        {
            bmr = 88.362 + (13.397*userweight/2.205) + (4.799*(userfeet*12+userinch)*2.54) - (5.677*userage)
        }
        else
        {
            bmr = 447.593 + (9.247*userweight/2.205) + (3.098*(userfeet*12+userinch)*2.54) - (4.33*userage)
        }

        //calculate the calories
        val cals = bmr*mets/(24*0.25)

        //add it
        totcalories += cals

        //make a list of interval data
        val temp = mutableListOf(interval, lastSpeed, lastRPM, lastCad, lastInc, lastPWR, totDistance, cals)
        //include it
        intervalData.add(temp)
        //add one to interval count
        interval++;
    }

    //this function just writes to the file
    @RequiresApi(Build.VERSION_CODES.O)
    private fun writeToFile()
    {
        //set up variables
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
        //conversions
        val dh = TimeUnit.MILLISECONDS.toHours(times)
        val dm = TimeUnit.MILLISECONDS.toMinutes(times) % 60
        val ds = TimeUnit.MILLISECONDS.toSeconds(times) % 60
        val durInSeconds = dh*60*60+dm*60+ds
        //write duration in the proper format
        var duration = "" + dh + "h" + dm + "m" + ds + "s"
        //safety
        if(cad.isNaN() || pitch.isNaN() || pwr.isNaN() || rpm.isNaN() || speed.isNaN() || distance.isNaN())
        {
            Log.i("WriteToFile", "ISNAN - REJECTED")
            Log.i("WriteToFile", "$cad, $pitch, $pwr, $rpm, $speed, $distance")
            Log.i("WriteToFile", "${cad.isNaN()}, ${pitch.isNaN()}, ${pwr.isNaN()}, ${rpm.isNaN()}, ${speed.isNaN()}, ${distance.isNaN()}")
            return
        }

        var numLines = 0;
        //locate the file
        val f = File(context?.filesDir, "data.csv")
        //boolean if the file exists or not
        var exists = f.exists()
        //open the file (this will create the file if the file does not exist)
        var outStream = OutputStreamWriter(context?.openFileOutput("data.csv", Context.MODE_PRIVATE))
        if(!exists)
        {
            //write a new header if it didnt exist earlier
            Log.i("writeToFile", "File does not exist")
            outStream.write("id, Date, Duration, Distance, Average Speed, Average Pedal Rate\n")
        }
        else
        {
            //otherwise, start counting the number of rows already in the file
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
        //add one to account for header
        numLines++;

        //set up date format
        val date = "$year-$month-$day"

        //combine into one string for writing
        var outString = "$numLines, $date, $duration, $distance, $speed, $rpm, $cad, $pwr, $pitch, $totcalories"

        //loop through and begin writing all of the interval data in
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
        //end line
        outString+="\n"
        //write the string to the file
        outStream.write(outString)
        //close the file
        outStream.close()
        Log.i("HOME WriteToFile", "Complete")
    }

    //a test function to simulate broadcasting without BluetoothService,
    //this is replica of onReceive
    fun writeReception(data: String, dataType: String)
    {
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
    }

    //this receiver is in charge of processing received data
    private val receiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?){
            //specify which data header we want to collect
            val intake = intent?.getStringExtra("data")
            //safety
            if(intake!=null)
            {
                //split the data string by ':'
                val toArr = intake.split(':')
                //0 is the dataType and 1 is the actual data
                val dataType = toArr[0]
                val data = toArr[1]
                //find which one it is and update the appropriate text
                if(dataType=="s")
                {
                    //update text
                    speedText.text = data + " MPH"
                    //update number of data points (DP's)
                    speedDP++
                    //update the last and average values
                    avgSpeed += data.toDouble()
                    lastSpeed = data.toDouble()
                }
                else if(dataType=="i")
                {
                    //update text
                    incText.text = "∠" + data + "°"
                    //update number of data points (DP's)
                    incDP++
                    //update the last and average values
                    avgInc += data.toDouble()
                    lastInc = data.toDouble()
                }
                else if(dataType=="d")
                {
                    //update text
                    distText.text = data + " Miles"
                    //update total distance
                    totDistance = data.toDouble()
                }
                else if(dataType=="r")
                {
                    //update text
                    rpmText.text = data + " RPM (Tire)"
                    //update number of data points (DP's)
                    rpmDP++
                    //update the last and average values
                    avgRPM += data.toDouble()
                    lastRPM = data.toDouble()
                }
                else if(dataType=="c")
                {
                    //update text
                    cadText.text = data + " RPM (Cad)"
                    //update number of data points (DP's)
                    cadDP++
                    //update the last and average values
                    avgCad += data.toDouble()
                    lastCad = data.toDouble()
                }
                else if(dataType=="p")
                {
                    //update text
                    pwrText.text = data + " Watts"
                    //update number of data points (DP's)
                    pwrDP++
                    //update the last and average values
                    avgPWR += data.toDouble()
                    lastPWR = data.toDouble()
                }
                if(((startTime-System.currentTimeMillis())/(60000*15))>=interval)
                {
                    recordInterval()
                }
            }
            else
            {
                Log.w("[HOMEFRAG]", "broadcast receiver got NULL DATA")
            }

        }
    }


    //when user leaves page, save the data
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroyView() {
        super.onDestroyView()
        Log.i("HOMEFRAG", "onDestroyView")

        //as long as a single data point for Speed or Cadence is recorded, record data
        //rationale: one of the two must be triggered when BLE sends data, therefore
        //guaranteeing we received any amount of recordable or relevant data
        if(speedDP>0 || cadDP>0)
        {
            recordInterval()
            writeToFile()
        }

        _binding = null
    }

    //function to search for a specific place
    private fun searchInit(query: String, map: GoogleMap)
    {
        //initiate geocodeapi and provide api key
        val geocodeAPI = GeoApiContext.Builder()
            .apiKey("$apiKey")
            .build()
        val geoRes1 = GeocodingApi.geocode(geocodeAPI, query).await()
        if(geoRes1.isNotEmpty())
        {
            val geoRes = geoRes1[0]
            if(geoRes.geometry!=null)
            {
                //stage 1, definitions
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
                //establish defaults
                var mylat = 30.6213255
                var mylong = -96.3405284
                Log.i("SearchInit","Pre-Pull");
                //pull current locations
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
                        //add the marker to mark where the user is
                        lastMark?.remove()
                        lastMark = map.addMarker(user)
                        //move the camera
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastMark?.position!!, 15f))
                        mylat = lastLoc.latitude
                        mylong = lastLoc.longitude

                        Log.i("SearchInit","Post-Pull");
                        //provide location information and mode
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

                        //initiate attempts to get pathing
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

    //this runs when the GoogleMap visuals are loaded and ready
    override fun onMapReady(map: GoogleMap) {
        Log.i("HOMEFRAG", "onMapReady")
        //due to the highly sensitive nature of locations, permissions is asked one more time
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
        //location is given
        gMap.isMyLocationEnabled = true
        //set up customizations
        val uiSettings = gMap.uiSettings
        uiSettings.isZoomControlsEnabled = true
        uiSettings.isMyLocationButtonEnabled = true
        uiSettings.isCompassEnabled = true

        Log.i("onMapReady", "1")
        //input zach location as default
        val ZACH = LatLng(30.6213255, -96.3405284)

        //attempt to grab actual current location
        val fuse = LocationServices.getFusedLocationProviderClient(requireActivity())
        Log.i("onMapReady", "2")
        val locationReq = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(1000)

        //grab actual current location and move camera
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

        //uses the callback function above
        fuse.requestLocationUpdates(locationReq, locationCallback, null)
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
