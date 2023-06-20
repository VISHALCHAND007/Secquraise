package com.example.secquralseassignment.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

class CheckConnection {
    fun checkConnectivity(mContext: Context): Boolean {
        val connectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkState: NetworkInfo? = connectivityManager.activeNetworkInfo

        return networkState?.isConnected != null
    }
}