package com.example.smartbike.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.smartbike.R
import com.example.smartbike.databinding.FragmentHomeBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.maps.DirectionsApiRequest
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.PendingResult
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request.Builder
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.*
import java.nio.charset.Charset
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var gMap: GoogleMap
    private lateinit var mapView: MapView
    private lateinit var mapFrag: SupportMapFragment

    private lateinit var searchBtn: Button

    private var startTime: Long = 0

    private var lastMark: Marker? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        var started = false
        var button: Button = binding.startendbtn
        binding.startendbtn.setOnClickListener{
            started = !started
            if(started)
            {
                button.text = "End"
                startTime = System.currentTimeMillis()
            }
            else
            {
                genData();
                button.text = "Start"
            }
        }
        /*
        val bluetoothManager: BluetoothManager? = context?.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.getAdapter()
        if(bluetoothAdapter==null)
        {
            Log.e("Bluetooth", "Does not support bluetooth")
        }
        else
        {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val paired: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                paired?.forEach{
                    device->
                    val deviceName = device.name
                    val deviceMAC = device.address
                    if(deviceName == "DESKTOP-NLD1U1E")
                    {
                        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                        val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                        socket.connect()
                        Toast.makeText(requireContext(), "Connection Successful!", Toast.LENGTH_SHORT).show()
                        /*
                        var inpStream: InputStream?=null
                        try {
                            inpStream = socket.inputStream
                            val buffer = ByteArray(1024)
                            val bytesRead = inpStream.read(buffer)
                            if(bytesRead>0)
                            {
                                val msg = String(buffer, 0, bytesRead, Charset.forName("UTF-8"))
                                Toast.makeText(requireContext(), "$msg", Toast.LENGTH_SHORT).show()
                            }
                        }
                        catch(e: IOException)
                        {
                            Log.e("bluetooth", "$e")
                        }
                        finally {
                            inpStream?.close()
                        }
                        */
                    }
                }
            }

        }*/

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById<MapView>(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    private fun genData()
    {
        val f = File(context?.filesDir, "data.csv")

        var outStream = OutputStreamWriter(context?.openFileOutput("data.csv", Context.MODE_APPEND))
        var numLines = 0
        if(f.exists())
        {
            f.forEachLine{numLines++}
            Log.i("File Exists", "$numLines")
            if(numLines==0)
            {
                outStream.write("id, Date, Duration, Distance, Average Speed, Average Pedal Rate\n")
            }
        }
        else
        {
            Log.i("File doesn't exist", "Writing stuff")
            outStream.write("id, Date, Duration, Distance, Average Speed, Average Pedal Rate\n")
        }
        //Generate New Data
        var cal = GregorianCalendar()
        var year = 2000
        var month = 1
        var day = 1
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

            val f = File(context?.filesDir, "user.csv")
            var userweight = 150
            var userage = 25
            var userfeet = 5
            var userinch = 8
            var isMale = true
            if(f.exists()) {
                var inp = context?.openFileInput("user.csv")
                if (inp != null) {
                    var inpReader = InputStreamReader(inp)
                    var buffRead = BufferedReader(inpReader)
                    var line = buffRead.readLine()
                    val temp = line.split(",").toTypedArray()
                    var male = temp[4].toBoolean()
                    //"$userage, $userfeet, $userinch, $userweight, $isMale"

                    userage = (temp[0].replace("\\s".toRegex(), "")).toInt()
                    userfeet = (temp[1].replace("\\s".toRegex(), "")).toInt()
                    userinch = (temp[2].replace("\\s".toRegex(), "")).toInt()
                    userweight = (temp[3].replace("\\s".toRegex(), "")).toInt()
                    if (male) {
                        isMale = true
                    } else {
                        isMale = false
                    }
                }
            }

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
        var outString = "$numLines, $date, $duration, $distance, $speed, $rpm, $cad, $pwr, $pitch, $calories"

        for(x in 0..numIntervals)
        {
            outString +=", ${intDist[x]}&${intSpeed[x]}&${intRPM[x]}&${intCad[x]}&${intPWR[x]}&${intPitch[x]}&${intCal[x]}"
        }
        outString+="\n"

        Log.i("outString", "$outString")

        /*
        var intDist = Vector<Double>()
        var intSpeed = Vector<Double>()
        var intRPM = Vector<Double>()
        var intCad = Vector<Double>()
        var intPWR = Vector<Double>()
        var intCal = Vector<Double>()
         */

        outStream.write(outString)

        outStream.close();
        Log.i("genData", "Fully Complete");
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun searchInit(query: String, map: GoogleMap)
    {
        val apiKey="AIzaSyCz07lN-GCqnb01-F8NwFO9obIlaB3G64c"
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
        //Initial Set Up
        gMap = map

        val uiSettings = gMap.uiSettings
        uiSettings.isZoomControlsEnabled = true
        uiSettings.isMyLocationButtonEnabled = true
        uiSettings.isCompassEnabled = true


        val ZACH = LatLng(30.6213255, -96.3405284)

        val fuse = LocationServices.getFusedLocationProviderClient(requireActivity())
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
        fuse.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, object : CancellationToken() {
            override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                CancellationTokenSource().token

            override fun isCancellationRequested() = false
        })
            .addOnSuccessListener { location: Location? ->
                if (location == null) {
                    Log.i("SearchInit", "Failed Pull")
                    Toast.makeText(requireContext(), "Cannot get location.", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.i(
                        "SearchInit",
                        "Successful Pull, ${location.latitude}, ${location.longitude}"
                    )
                    val mylat = location.latitude
                    val mylong = location.longitude

                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(mylat, mylong), 18f))
                }
            }

        val searchView = view?.findViewById<SearchView>(R.id.searchBarText)
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
        //searchBarText
        //dirBtn
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



/*
//newer one
private fun searchInit(query: String)
    {
        val geocodeAPI = GeoApiContext.Builder()
            .apiKey("AIzaSyCz07lN-GCqnb01-F8NwFO9obIlaB3G64c")
            .build()
        val geoRes1 = GeocodingApi.geocode(geocodeAPI, query).await()
        if(geoRes1.isNotEmpty())
        {
            val geoRes = geoRes1[0]
            if(geoRes.geometry!=null)
            {
                //val dest = LatLng(geoRes.geometry.location.lat, geoRes.geometry.location.lng)
                val dirReq = DirectionsApiRequest(geocodeAPI)
                val fuse = LocationServices.getFusedLocationProviderClient(requireContext())
                var mylat = 30.6213255
                var mylong = -96.3405284
                if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    fuse.lastLocation.addOnSuccessListener { location->
                        mylat = location.latitude
                        mylong = location.longitude
                    }
                }
                else
                {
                    ActivityCompat.requestPermissions(requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
                }
                val myloc = "$mylat,$mylong"
                val dest = geoRes.geometry.location.lat.toString() + "," + geoRes.geometry.location.lng.toString()
                dirReq.origin(myloc)
                dirReq.destination(dest)
                dirReq.mode(TravelMode.BICYCLING)

                dirReq.setCallback(object: PendingResult.Callback<DirectionsResult>{
                    override fun onResult(res: DirectionsResult)
                    {
                        if(res.routes.isNotEmpty())
                        {
                            val route = res.routes[0]
                            val options = PolylineOptions()
                            for(leg in route.legs)
                            {
                                for(step in leg.steps)
                                {
                                    val path = PolyUtil.decode(step.polyline.encodedPath)
                                    val poly = PolylineOptions().addAll(path).color(Color.BLUE)
                                    gMap.addPolyline(poly)
                                }
                            }
                        }
                    }
                    override fun onFailure(e: Throwable)
                    {
                        Toast.makeText(requireContext(), "Direction request failed", Toast.LENGTH_SHORT).show()
                    }
                })
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


    private fun searchInit(){
        val searchBox = view?.findViewById<TextInputEditText>(R.id.searchBarText)
        val searchBtn = view?.findViewById<Button>(R.id.dirBtn)

        searchBtn?.setOnClickListener{
            val loc = searchBox?.text.toString()
            if(loc.isNotEmpty())
            {
                val geo = Geocoder(requireContext())
                val apiKey="AIzaSyCz07lN-GCqnb01-F8NwFO9obIlaB3G64c"
                val queryURL = "https://maps.googleapis.com/maps.api/geocode/json?address=$loc&key=$apiKey"
                val vol = Volley.newRequestQueue(requireContext())

                val stringQuery = StringRequest(
                    Request.Method.GET, queryURL,
                    {
                        response->
                        val json = JSONObject(response)
                        val status = json.getString("status")
                        if(status == "OK")
                        {
                            val out = json.getJSONArray("results")
                            if(out.length()>0)
                            {
                                val result = out.getJSONObject(0)
                                val geometry = result.getJSONObject("geometry")
                                val location = geometry.getJSONObject("location")
                                val latitude = location.getDouble("lat")
                                val longitude = location.getDouble("lng")
                                val addy = result.getString("formatted_address")
                                val latLong = LatLng(latitude, longitude)

                                val markerOptions = MarkerOptions().position(latLong).title(addy)
                                gMap.addMarker(markerOptions)
                                //gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, 14f))

                                val userLocation = LocationServices.getFusedLocationProviderClient(requireActivity())
                                if (ActivityCompat.checkSelfPermission(
                                        requireContext(),
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                        requireContext(),
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                }
                                userLocation.lastLocation.addOnSuccessListener{ loc: Location?->
                                    if(loc!=null)
                                    {
                                        val geoContext = GeoApiContext.Builder().apiKey(apiKey).build()
                                        val dirQuery = DirectionsApiRequest(geoContext)
                                            .origin("${loc.latitude}, ${loc.longitude}")
                                            .destination("$latLong")
                                            .mode(TravelMode.BICYCLING)

                                        //dirQuery.key(getString(R.string.google_maps_key))

                                        dirQuery.setCallback(object: PendingResult.Callback<DirectionsResult>
                                        {
                                            override fun onResult(result: DirectionsResult){
                                                val routeList = result.routes
                                                for(route in routeList)
                                                {
                                                    val node = route.overviewPolyline.decodePath()
                                                    val polOptions = PolylineOptions().addAll(node.map{LatLng(it.lat, it.lng)}).color(
                                                        Color.BLUE)
                                                    gMap.addPolyline(polOptions)
                                                }
                                            }
                                            override fun onFailure(e: Throwable)
                                            {
                                                Toast.makeText(requireContext(), "Failed to get directions", Toast.LENGTH_SHORT).show()
                                            }

                                        })
                                    }
                                    else{
                                        Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, 14f))
                            }
                            else
                            {
                                Toast.makeText(requireContext(), "No results found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else
                        {
                            Toast.makeText(requireContext(), "Error: $status", Toast.LENGTH_SHORT).show()
                        }
                    },
                    {
                        error->
                        Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
                vol.add(stringQuery)
            }
            else
            {
                Toast.makeText(requireContext(), "Please enter a location", Toast.LENGTH_SHORT).show()
            }
        }
    }
*/