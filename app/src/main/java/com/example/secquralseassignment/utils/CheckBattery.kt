package com.example.secquralseassignment.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class CheckBattery {
    fun checkCharging(mContext: Context): Int? {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            mContext.registerReceiver(null, ifilter)
        }
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    }
    fun getChargingPercentage(mContext: Context): Int? {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            mContext.registerReceiver(null, ifilter)
        }
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    }
}