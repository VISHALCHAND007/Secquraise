package com.example.secquralseassignment.activity

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.secquralseassignment.R
import com.example.secquralseassignment.databinding.ActivityMainBinding
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


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationHelper: LocationHelper
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    private lateinit var mFirebaseStorage: FirebaseStorage
    private lateinit var mFirebaseDatabaseReference: DatabaseReference
    private lateinit var mFirebaseStorageReference: StorageReference
    private lateinit var mChildEventListener: ChildEventListener

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
        fetchAllData()
    }

    private fun initElements() {
        locationHelper = LocationHelper()
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mFirebaseStorage = FirebaseStorage.getInstance()
        mFirebaseDatabaseReference = mFirebaseDatabase.reference.child("my_database")
        mFirebaseStorageReference = mFirebaseStorage.reference.child("photos")
    }

    private fun checkPermission() {
        if (!locationHelper.checkPermission(this@MainActivity)) {
            locationHelper.requestPermission(this@MainActivity)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initTasks() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding.scrollView.isHorizontalScrollBarEnabled = false

        //setting the default init values
        binding.frequencyEt.setText("15")
        binding.captureTv.text = "0"
    }

    private fun initListeners() {
        binding.manualRefreshBtn.setOnClickListener {

            //upload and get the url of the image from storage
            val url: Uri =
                Uri.parse("https://imgv3.fotor.com/images/blog-richtext-image/take-a-picture-with-camera.png"); //suppose this is image url
            mFirebaseStorageReference.child(url.lastPathSegment!!).putFile(url)
                .addOnSuccessListener {
                    val imageUrl = it.uploadSessionUri
//                val dataModel = DataModel(1, 15, true, false, 54, "flkajdflkje;lj kdjfldkjflk")
//                mFirebaseDatabaseReference.push().setValue(dataModel)
                }

            binding.progressBar.visibility = View.VISIBLE
            binding.scrollView.visibility = View.GONE
            Handler(Looper.myLooper()!!).postDelayed({
                fetchAllData()
                binding.scrollView.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }, 200)
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
    }

    private fun fetchAllData() {
        getPhoto()
        getConnectivity()
        getChargingStatus()
        getBatteryChargePercentage()
        getUserLocation()
    }

    private fun getPhoto() {
        takePermission()
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, "photo.jpg")
        val photoUri = FileProvider.getUriForFile(this, "com.example.secquralseassignment.fileprovider", file)

        val launcher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                Glide.with(this@MainActivity).load(photoUri).into(binding.imageView)
            }
        }

        launcher.launch(photoUri)
    }

    private fun takePermission() {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PHOTO
            )
    }


    private fun getConnectivity() {
        if (CheckConnection().checkConnectivity(this@MainActivity))
            binding.connectivityTv.text = getString(R.string.on)
        else
            binding.connectivityTv.text = getString(R.string.off)
    }

    private fun getChargingStatus() {
        // 2 = Charging
        // 3 = Discharging
        if (CheckBattery().checkCharging(this@MainActivity) == 2)
            binding.batteryChargingTv.text = getString(R.string.on)
        else
            binding.batteryChargingTv.text = getString(R.string.off)
    }

    @SuppressLint("SetTextI18n")
    private fun getBatteryChargePercentage() {
        val batteryPercentage = CheckBattery().getChargingPercentage(this@MainActivity)
        binding.chargePercentageTv.text = "$batteryPercentage%"
    }

    private fun getUserLocation() {
        locationHelper.getCurrentLocation(
            this@MainActivity,
            object : LocationHelper.LocationCallback {
                override fun onLocationReceived(location: String) {
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
    companion object{
        const val REQUEST_PHOTO = 10
    }

//    override fun onDestroy() {
//        super.onDestroy()
////        locationHelper.removeLocation()
//    }
}