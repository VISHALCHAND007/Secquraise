package com.example.secquralseassignment.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LocationHelper {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    interface LocationCallback {
        fun onLocationReceived(location: String)
        fun onError(errorMessage: String)
    }

    fun getCurrentLocation(mContext: Context, callback: LocationCallback) {
        var location = ""
        if (checkPermission(mContext)) {
            if (isLocationEnabled(mContext)) {
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext)
                fusedLocationProviderClient.lastLocation.addOnCompleteListener {
                    val result = it.result
                    try {
                        location = "${result.longitude},\n${result.latitude}"
                        callback.onLocationReceived(location)
                    }catch (e:Exception) {
                        showToast("Some error occurred.", mContext)
                    }
                }
            } else {
//                openSettings
                showToast("Enable Location", mContext)
                val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                mContext.startActivity(locationIntent)
            }
        } else {
            requestPermission(mContext)
        }
    }

    fun checkPermission(mContext: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(
                mContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                mContext,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                mContext,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    fun requestPermission(mContext: Context) {
        ActivityCompat.requestPermissions(
            mContext as Activity,
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.CAMERA),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun isLocationEnabled(mContext: Context): Boolean {
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    companion object {
        const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }
    fun showToast(str: String, mContext: Context) {
        Toast.makeText(mContext,str, Toast.LENGTH_SHORT).show()
    }
    fun removeLocation(mContext: Context) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext)
        fusedLocationProviderClient.removeLocationUpdates {  }
    }
}