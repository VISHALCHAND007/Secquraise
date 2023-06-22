package com.example.secquralseassignment.activity

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationHelper: LocationHelper
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    private lateinit var mFirebaseStorage: FirebaseStorage
    private lateinit var mFirebaseDatabaseReference: DatabaseReference
    private lateinit var mFirebaseStorageReference: StorageReference
    private lateinit var mChildEventListener: ChildEventListener
    private lateinit var photoUri: Uri
    private lateinit var mCalendar: Calendar
    private lateinit var file: File
    private lateinit var dataModel: DataModel
    private lateinit var dao: DataDAO

    //local variables
    private var mTimestamp: String = ""
    private var mFrequency: Int = 15
    private var mConnectivity: Boolean = false
    private var mBatteryCharging: Boolean = false
    private var mChargePercentage: Int = 0
    private var mLocationCoordinates: String = ""
    private var mCaptureCount: Int = 0


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
            locationHelper.showToast("Sync in progress...", this@MainActivity)
            for (i in mList.indices) {
                photoUri = Uri.parse(mList[i].mPhoto)
                Log.e("here==", photoUri.toString())
                mTimestamp = mList[i].mTimeStamp
                mCaptureCount = mList[i].mCaptureCount
                mFrequency = mList[i].mFrequency
                mConnectivity = mList[i].mConnectivity
                mBatteryCharging = mList[i].mBatteryCharging
                mChargePercentage = mList[i].mChargePercentage
                mLocationCoordinates = mList[i].mLocationCoordinates
                saveOnline(0)
            }
        } else {
            fetchAllData()
        }
    }

    private fun initElements() {
        locationHelper = LocationHelper()
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mFirebaseStorage = FirebaseStorage.getInstance()
        mFirebaseDatabaseReference = mFirebaseDatabase.reference.child("my_database")
        mFirebaseStorageReference = mFirebaseStorage.reference.child("photos")
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
        binding.scrollView.isHorizontalScrollBarEnabled = false

    }

    private fun initListeners() {
        binding.manualRefreshBtn.setOnClickListener {
            saveFetchedData(0)
        }
        mChildEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                locationHelper.showToast("Data added to database.", this@MainActivity)
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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

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
        takePermission()
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
                    photoUri.toString(),
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
        Handler(Looper.myLooper()!!).postDelayed({
            val fileName = SimpleDateFormat("yyyyMMdd_HHmmss").format(mCalendar.time) + ".jpg"
            //start loading
            binding.progressBar.visibility = View.VISIBLE
            binding.scrollView.visibility = View.GONE
            mFirebaseStorageReference.child(fileName).putFile(photoUri)
                .addOnSuccessListener {
                    val imageUrl = it.uploadSessionUri
                    dataModel =
                        DataModel(
                            imageUrl.toString(),
                            mTimestamp,
                            mCaptureCount,
                            mFrequency,
                            mConnectivity,
                            mBatteryCharging,
                            mChargePercentage,
                            mLocationCoordinates
                        )
                    try {
                        mFirebaseDatabaseReference.push().setValue(dataModel)
                    } catch (e: Exception) {
                        Log.e("saveFetchedData", "Error uploading data: ${e.message}")
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE
                    //delete data from room and remove the file from android storage
                    dao.delete(photoUri.toString())
                    fetchAllData()
                }
                .addOnFailureListener { e ->
                    Log.e("saveFetchedData", "Error uploading photo: ${e.message}")
                }
        }, time)
    }

    private fun getPhoto() {
//        mCaptureCount++
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val mFile = File(storageDir, "photo.${mCalendar.time.time}.png")
        file = mFile
        photoUri =
            FileProvider.getUriForFile(this, "com.example.secquralseassignment.fileprovider", mFile)
        launcher.launch(photoUri)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) {
            binding.imageView.setImageURI(null)
            binding.imageView.setImageURI(photoUri)
        }
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

    private fun takePermission() {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        } else {
            getPhoto()
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                getPhoto()
            } else {
                LocationHelper().showToast("Allow Camera Access", this@MainActivity)
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                startActivity(intent)
            }
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
        }
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