package com.example.secquralseassignment.activity

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.example.secquralseassignment.R
import com.example.secquralseassignment.database.DataDAO
import com.example.secquralseassignment.database.DataEntity
import com.example.secquralseassignment.database.Database
import com.example.secquralseassignment.databinding.ActivityMainBinding
import com.example.secquralseassignment.model.DataModel
import com.example.secquralseassignment.utils.CheckBattery
import com.example.secquralseassignment.utils.CheckConnection
import com.example.secquralseassignment.utils.LocationHelper
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationHelper: LocationHelper
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    private lateinit var mFirebaseDatabaseReference: DatabaseReference
    private lateinit var mChildEventListener: ChildEventListener
    private lateinit var mCalendar: Calendar
    private lateinit var dataModel: DataModel
    private lateinit var dao: DataDAO
    private var toastCounter = 1

    //local variables
    private var mTimestamp: String = ""
    private var mFrequency: Int = 15
    private var mConnectivity: Boolean = false
    private var mBatteryCharging: Boolean = false
    private var mChargePercentage: Int = 0
    private var mLocationCoordinates: String = ""
    private var mCaptureCount: Int = 0
    private val CHANNEL_ID = "12"
    private val NOTIFICATION_ID = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        initElements()
        initTasks()
        checkPermission()
        initListeners()
        saveOfflineData()
    }

    private fun saveOfflineData() {
        val mList = dao.getAllData()
        if (CheckConnection().checkConnectivity(this@MainActivity) && mList.isNotEmpty()) {
            binding.progressBar.visibility = View.VISIBLE
            locationHelper.showToast("Sync in progress...", this@MainActivity)
            lifecycleScope.launch {
                syncData(mList)
            }
        } else {
            fetchAllData()
        }
    }

    private suspend fun syncData(mList: List<DataEntity>) {
        mList.forEach { data ->
            val job = lifecycleScope.launch(Dispatchers.IO) {
                mTimestamp = data.mTimeStamp
                mCaptureCount = data.mCaptureCount
                mFrequency = data.mFrequency
                mConnectivity = data.mConnectivity
                mBatteryCharging = data.mBatteryCharging
                mChargePercentage = data.mChargePercentage
                mLocationCoordinates = data.mLocationCoordinates
            }
            job.join()
            saveOnline(0)
            delay(10)
        }
        fetchAllData()
    }

    private fun initElements() {
        locationHelper = LocationHelper()
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mFirebaseDatabaseReference = mFirebaseDatabase.reference.child("my_database")
        mCalendar = Calendar.getInstance()
        dao = Database.getDatabaseInstance(this@MainActivity).dataDao()
    }

    private fun checkPermission() {
        if (!locationHelper.checkPermission(this@MainActivity)) {
            locationHelper.requestPermission(this@MainActivity)
        }
    }

    private fun initTasks() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun initListeners() {
        binding.manualRefreshBtn.setOnClickListener {
            //to save the data at that instant
            saveFetchedData(0)
        }
        mChildEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (CheckConnection().checkConnectivity(this@MainActivity) && toastCounter == 1) {
                    toastCounter++
                    locationHelper.showToast("Data added to database.", this@MainActivity)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }
        mFirebaseDatabaseReference.addChildEventListener(mChildEventListener)

        binding.frequencyEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty())
                    saveFetchedData(s.toString().toInt())
            }
        })
    }

    private fun fetchAllData() {
        saveTimeStamp()
        getChargingStatus()
        getConnectivity()
        getBatteryChargePercentage()
        getUserLocation()
        updateData()
        saveFetchedData(mFrequency)
    }

    @SuppressLint("SetTextI18n")
    private fun updateData() {
        mCaptureCount++
        //setting value
        binding.frequencyEt.setText("15")
        binding.captureTv.text = mCaptureCount.toString()
    }

    private fun saveFetchedData(frequency: Int) {
        val time: Long = (frequency * 60 * 1000).toLong()

        //checking connectivity on saving with respect to that
        if (CheckConnection().checkConnectivity(this@MainActivity)) {
            saveOnline(time)
        } else {
            //saving to room db
            /*
            1. Creating entity
            2. Saving it in local db
            */
            Handler(Looper.myLooper()!!).postDelayed({
                val entity = DataEntity(
                    mTimestamp,
                    mCaptureCount,
                    mFrequency,
                    mConnectivity,
                    mBatteryCharging,
                    mChargePercentage,
                    mLocationCoordinates
                )
                dao.insert(entity)
                fetchAllData()
            }, time)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun saveOnline(time: Long) {
        dataModel =
            DataModel(
                mTimestamp,
                mCaptureCount,
                mFrequency,
                mConnectivity,
                mBatteryCharging,
                mChargePercentage,
                mLocationCoordinates
            )

        Handler(Looper.myLooper()!!).postDelayed({
            //start loading
            binding.progressBar.visibility = View.VISIBLE
            try {
                mFirebaseDatabaseReference.push().setValue(dataModel)
                //delete data from room and remove the file from android storage
                dao.delete(dataModel.mTimeStamp)
            } catch (e: Exception) {
                Log.e("saveFetchedData", "Error uploading data: ${e.message}")
            }
            binding.progressBar.visibility = View.GONE
            fetchAllData()
        }, time)
    }

    private fun saveTimeStamp() {
        mTimestamp = formatDate(Calendar.getInstance().time)
        binding.dateTimeTv.text = mTimestamp
    }

    @SuppressLint("SimpleDateFormat")
    private fun formatDate(time: Date): String {
        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        return simpleDateFormat.format(time)
    }


    private fun getConnectivity() {
        if (CheckConnection().checkConnectivity(this@MainActivity)) {
            mConnectivity = true
            binding.connectivityTv.text = getString(R.string.on)
        } else {
            binding.connectivityTv.text = getString(R.string.off)
            mConnectivity = false
        }
    }

    private fun getChargingStatus() {
        // 2 = Charging
        // 3 = Discharging
        if (CheckBattery().checkCharging(this@MainActivity) == 2) {
            mBatteryCharging = true
            binding.batteryChargingTv.text = getString(R.string.on)
        } else {
            binding.batteryChargingTv.text = getString(R.string.off)
            mBatteryCharging = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getBatteryChargePercentage() {
        val batteryPercentage = CheckBattery().getChargingPercentage(this@MainActivity)
        if (batteryPercentage != null) {
            mChargePercentage = batteryPercentage
            binding.chargePercentageTv.text = "$batteryPercentage%"
            if (batteryPercentage < 20)
                sendLowBatteryNotification()
        }
    }

    private fun sendLowBatteryNotification() {
        //create notification manager
        val notification = NotificationCompat.Builder(this@MainActivity)
        notification.setSmallIcon(android.R.drawable.sym_def_app_icon)
        notification.setContentTitle(getString(R.string.app_name))
        notification.setContentText(getString(R.string.batter_less_than_20))
        notification.setAutoCancel(true)

        //set pending intent
        val touchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this@MainActivity,
            0,
            touchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        notification.setContentIntent(pendingIntent)

        //notification manager
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //creating channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            "My notification",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        notification.setChannelId(CHANNEL_ID)

        //connect with notification manager
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    private fun getUserLocation() {
        locationHelper.getCurrentLocation(
            this@MainActivity,
            object : LocationHelper.LocationCallback {
                override fun onLocationReceived(location: String) {
                    mLocationCoordinates = location
                    binding.locationTv.text = location
                }

                override fun onError(errorMessage: String) {
                    getUserLocation()
                }
            })
    }

    @SuppressLint("SetTextI18n")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LocationHelper.PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationHelper.showToast("Permission Granted", this@MainActivity)
                getUserLocation()
            } else if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                locationHelper.showToast("Permission Denied", this@MainActivity)
                locationHelper.requestPermission(this@MainActivity)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val alertDialog = AlertDialog.Builder(this@MainActivity)
        alertDialog.setTitle("Do you want to exit?")
        alertDialog.setPositiveButton(
            "Yes"
        ) { _: DialogInterface?, _: Int ->
            // When the user click yes button then app will close
            finishAffinity()
        }
        alertDialog.setNegativeButton(
            "No"
        ) { _: DialogInterface?, _: Int ->
            // When the user click yes button then app will close

        }
        alertDialog.create()
        alertDialog.show()
    }
}