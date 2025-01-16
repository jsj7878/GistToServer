package com.myme.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 5000 // 5초 간격
        fastestInterval = 5000
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    private val handler = Handler(Looper.getMainLooper())
    //좌표 보낼 서버 주소
    private val serverUrl = "https://your-server-endpoint.com/location"
    //출발, 도착 신호 보낼 서버 주소
    private val serverUrl2 = "https://your-server-endpoint.com/location"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 위치 클라이언트 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        findViewById<TextView>(R.id.start_button).setOnClickListener {
            Toast.makeText(
                this@MainActivity,
                "출발",
                Toast.LENGTH_SHORT
            ).show()
        }
        findViewById<TextView>(R.id.arrive_button).setOnClickListener {
            Toast.makeText(
                this@MainActivity,
                "도착",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 권한 요청
        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                startLocationUpdates()
            } else {
                Log.e("Permission", "Location permission denied")
            }
        }

        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("Location", "Missing location permission")
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d("Location", "Lat: $latitude, Lon: $longitude")
                runOnUiThread {
                    findViewById<TextView>(R.id.text).text = "Lat: $latitude, Lon: $longitude"
                }
                // 서버로 좌표 전송
                sendLocationToServer(latitude, longitude)
            }
        }
    }

    private fun sendLocationToServer(latitude: Double, longitude: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()

                // JSON 데이터 생성
                val json = JSONObject().apply {
                    put("latitude", latitude)
                    put("longitude", longitude)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

                // HTTP 요청 생성
                val request = Request.Builder()
                    .url(serverUrl)
                    .post(requestBody)
                    .build()

                // 요청 실행
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("Server", "Message sent successfully: ${response.body?.string()}")
                } else {
                    Log.e("Server", "Failed to send Message: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("Server", "Error sending Message: ${e.message}")
            }
        }
    }
    private fun sendMessageToServer(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()

                // JSON 데이터 생성
                val json = JSONObject().apply {
                    put("message", message)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

                // HTTP 요청 생성
                val request = Request.Builder()
                    .url(serverUrl2)
                    .post(requestBody)
                    .build()

                // 요청 실행
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("Server", "Location sent successfully: ${response.body?.string()}")
                } else {
                    Log.e("Server", "Failed to send location: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("Server", "Error sending location: ${e.message}")
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacksAndMessages(null)
    }
}
