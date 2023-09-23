package com.example.secquralseassignment.model

import android.net.Uri

data class DataModel(
    val mTimeStamp: String,
    val mCaptureCount: Int,
    val mFrequency: Int,
    val mConnectivity: Boolean,
    val mBatteryCharging: Boolean,
    val mChargePercentage: Int,
    val mLocationCoordinates: String
)