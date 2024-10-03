package com.example.opsc7312_ice_task_4

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread
import android.widget.TextView // Add a TextView to show distance and time

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Enable My Location layer if permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    // Move the camera to the current location
                    val userLocation = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))

                    // Fetch and display nearby landmarks
                    fetchNearbyLandmarks(userLocation)
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    // Fetch actual nearby landmarks from Google Places API
    private fun fetchNearbyLandmarks(currentLocation: LatLng) {
        val placesUrl = getPlacesUrl(currentLocation)

        thread {
            try {
                val result = URL(placesUrl).readText()
                val landmarks = parseNearbyPlaces(result)

                runOnUiThread {
                    for (landmark in landmarks) {
                        mMap.addMarker(MarkerOptions().position(landmark).title("Landmark"))
                    }

                    // Add click listener to markers to show directions
                    mMap.setOnMarkerClickListener { marker ->
                        marker?.let {
                            val destination = marker.position
                            calculateRoute(currentLocation, destination)
                        }
                        true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Construct the URL to fetch nearby places using Places API
    private fun getPlacesUrl(location: LatLng): String {
        val apiKey = "YOUR_PLACES_API_KEY"
        val latitude = location.latitude
        val longitude = location.longitude
        val radius = 1000 // Search radius in meters

        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$latitude,$longitude&radius=$radius&type=tourist_attraction&key=$apiKey"
    }

    // Parse the JSON response from the Places API to get nearby landmarks
    private fun parseNearbyPlaces(jsonResult: String): List<LatLng> {
        val landmarks = mutableListOf<LatLng>()
        val jsonObject = JSONObject(jsonResult)
        val results = jsonObject.getJSONArray("results")

        for (i in 0 until results.length()) {
            val place = results.getJSONObject(i)
            val location = place.getJSONObject("geometry").getJSONObject("location")
            val lat = location.getDouble("lat")
            val lng = location.getDouble("lng")
            landmarks.add(LatLng(lat, lng))
        }

        return landmarks
    }

    // Method to calculate route, time, and distance
    private fun calculateRoute(origin: LatLng, destination: LatLng) {
        val url = getDirectionsUrl(origin, destination)

        thread {
            val result = URL(url).readText()
            runOnUiThread {
                val (route, info) = parseDirections(result) ?: return@runOnUiThread

                // Draw the route on the map
                val polylineOptions = PolylineOptions().addAll(route).width(10f).color(0xFF0000FF.toInt())
                mMap.addPolyline(polylineOptions)

                // Display the distance and time on the UI
                findViewById<TextView>(R.id.distanceTimeTextView).text = info
            }
        }
    }

    // Construct the URL for Directions API
    private fun getDirectionsUrl(origin: LatLng, destination: LatLng): String {
        val strOrigin = "origin=${origin.latitude},${origin.longitude}"
        val strDest = "destination=${destination.latitude},${destination.longitude}"
        val key = "YOUR_DIRECTIONS_API_KEY"
        val mode = "mode=driving"

        return "https://maps.googleapis.com/maps/api/directions/json?$strOrigin&$strDest&$mode&$key"
    }

    // Parse the JSON response from Directions API to get route, distance, and time
    private fun parseDirections(jsonResult: String): Pair<List<LatLng>, String>? {
        val jsonObject = JSONObject(jsonResult)
        val routes = jsonObject.getJSONArray("routes")

        if (routes.length() == 0) return null

        val route = routes.getJSONObject(0)
        val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")

        // Get distance and duration
        val legs = route.getJSONArray("legs")
        val leg = legs.getJSONObject(0)
        val distance = leg.getJSONObject("distance").getString("text")
        val duration = leg.getJSONObject("duration").getString("text")

        // Return the route and the distance and time info
        return Pair(PolyUtil.decode(overviewPolyline), "Distance: $distance, Time: $duration")
    }
}
