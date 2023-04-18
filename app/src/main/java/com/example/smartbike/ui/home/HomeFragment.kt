package com.example.smartbike.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.smartbike.R
import com.example.smartbike.databinding.FragmentHomeBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.maps.DirectionsApiRequest
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.PendingResult
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
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



        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }
/*
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
    private fun genData()
    {
        val f = File(context?.filesDir, "data.csv")
        var outStream = OutputStreamWriter(context?.openFileOutput("data.csv", Context.MODE_APPEND))

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
        var dh = (startTime / (1000*60*60))%24
        var dm = (startTime / (1000*60))%60
        var ds = (startTime / 1000) %60
        var tempDuration = dh + dm.toDouble()/60 + ds.toDouble()/3600
        duration = "" + dh + "h" + dm + "m" + ds + "s"

        Log.i("genData", "StartTime: $startTime, dh: $dh, dm: $dm, ds: $ds");

        //Distance Data
        distance = (14.2+(-10..10).random()) * tempDuration

        //Speed Data
        speed = distance/tempDuration.toDouble()

        //Pedal Rate Data
        pedRate = (5..15).random()

        //Write to file
        val date = "$year:$month:$day$time"
        val outString = "6969, $date, $duration, $distance, $speed, $pedRate\n"
        outStream.write(outString)

        outStream.close();
        Log.i("genData", "Fully Complete");
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun searchInit(query: String)
    {
        val geocodeAPI = GeoApiContext.Builder()
            .apiKey("AIzaSyCz07lN-GCqnb01-F8NwFO9obIlaB3G64c")
            .build()
        val geoRes = GeocodingApi.geocode(geocodeAPI, query).await()[0]
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

    override fun onMapReady(map: GoogleMap) {
        //Initial Set Up
        gMap = map

        val uiSettings = gMap.uiSettings
        uiSettings.isZoomControlsEnabled = true
        uiSettings.isMyLocationButtonEnabled = true
        uiSettings.isCompassEnabled = true


        val ZACH = LatLng(30.6213255, -96.3405284)

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ZACH, 18f))

        val searchView = view?.findViewById<SearchView>(R.id.searchBarText)
        searchView?.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean{
                searchInit(query)

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