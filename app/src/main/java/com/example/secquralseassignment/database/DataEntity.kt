package com.example.secquralseassignment.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

@Entity (tableName = "data_table")
class DataEntity(
    @ColumnInfo (name = "photo")
    val mPhoto: String,
    @ColumnInfo (name = "timestamp")
    val mTimeStamp: String,
    @ColumnInfo (name = "capture_count")
    val mCaptureCount: Int,
    @ColumnInfo (name = "frequency")
    val mFrequency: Int,
    @ColumnInfo (name = "connectivity")
    val mConnectivity: Boolean,
    @ColumnInfo (name = "battery_charging_status")
    val mBatteryCharging: Boolean,
    @ColumnInfo (name = "charge_percentage")
    val mChargePercentage: Int,
    @ColumnInfo (name = "location_coordinates")
    val mLocationCoordinates: String
) {
    @PrimaryKey (autoGenerate = true)
    var id: Int = 0
}