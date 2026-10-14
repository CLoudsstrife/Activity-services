package com.example.backgroundservices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStartLatihan: Button

    companion object {
        const val ACTION_SERVICE_STOPPED_JUMP = "nit2x.paba.backgroundservice.ACTION_SERVICE_STOPPED_JUMP"
        const val ACTION_LATIHAN_FINISHED = "nit2x.paba.backgroundservice.ACTION_LATIHAN_FINISHED"
        const val EXTRA_JUMP_COUNT = "EXTRA_JUMP_COUNT"
        const val EXTRA_MODE_LATIHAN = "EXTRA_MODE_LATIHAN"
        const val REQUEST_CODE_PERMISSIONS = 101
    }

    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        btnStart = findViewById(R.id.btnStart)
        btnStartLatihan = findViewById(R.id.btnStartLatihan)

        handleIntent(intent)

        btnStart.setOnClickListener {
            if (hasAllPermissions()) {
                startTrackerService(isLatihanMode = false)
            } else {
                ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_CODE_PERMISSIONS)
            }
        }

        btnStartLatihan.setOnClickListener {
            if (hasAllPermissions()) {
                startTrackerService(isLatihanMode = true)
            } else {
                ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_CODE_PERMISSIONS)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_SERVICE_STOPPED_JUMP -> {
                statusTextView.text = "Background services sudah berhenti karena Anda melompat"
            }
            ACTION_LATIHAN_FINISHED -> {
                val count = intent.getIntExtra(EXTRA_JUMP_COUNT, 0)
                statusTextView.text = "Latihan Selesai!\nJumlah Lompatan: $count"
                addCalendarEvent(count)
            }
        }
    }

    private fun addCalendarEvent(jumpCount: Int) {
        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Latihan Lompat Selesai")
            putExtra(CalendarContract.Events.DESCRIPTION, "Berhasil menyelesaikan latihan dengan $jumpCount kali lompatan.")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, System.currentTimeMillis() + 60 * 60 * 1000)
        }
        if (calendarIntent.resolveActivity(packageManager) != null) {
            startActivity(calendarIntent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun hasAllPermissions(): Boolean {
        return permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startTrackerService(isLatihanMode: Boolean) {
        val serviceIntent = Intent(this, TrackerService::class.java).apply {
            putExtra(EXTRA_MODE_LATIHAN, isLatihanMode)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        statusTextView.text = if (isLatihanMode) "Latihan Dimulai... Silakan melompat!" else "Background service sedang berjalan..."
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startTrackerService(isLatihanMode = false)
            } else {
                statusTextView.text = "Izin ditolak, service tidak dapat dijalankan."
            }
        }
    }
}