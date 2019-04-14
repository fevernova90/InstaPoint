/*
 * Copyright 2019 Gotelz Enterprise. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gotelz.instapoint

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Text


class MainActivity : AppCompatActivity() {


    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private val interval: Long = 2000
    private val fastestInterval: Long = 1000
    lateinit var mLastLocation: Location
    private lateinit var mLocationRequest: LocationRequest
    private val requestPermissionId = 10

    private lateinit var getBtn: Button
    private lateinit var longText: TextView
    private lateinit var latiText: TextView
    private lateinit var stopBtn: ImageButton
    private lateinit var accuracyVal: TextView
    private lateinit var altitudeVal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mLocationRequest = LocationRequest()

        getBtn = findViewById(R.id.getBtn)
        stopBtn = findViewById(R.id.cancelBtn)
        longText = findViewById(R.id.longitudeTextView)
        latiText = findViewById(R.id.latitudeTextView)
        accuracyVal = findViewById(R.id.accuracyValue)
        altitudeVal = findViewById(R.id.altitudeValue)

        stopBtn.visibility = View.INVISIBLE

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }

        getBtn.setOnClickListener {
            if (checkPermissionForLocation(this)) {
                startLocationUpdates()
                getBtn.isEnabled = false
                stopBtn.visibility = View.VISIBLE
                getBtn.text = "Retrieving Signal ..."
            }
        }

        stopBtn.setOnClickListener {
            stopLocationUpdates()
        }

    }

    private fun checkPermissionForLocation(context: Context) : Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), requestPermissionId)
                false
            }
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == requestPermissionId) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
                getBtn.isEnabled = false
                stopBtn.isEnabled = true
            } else {
                Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS is currently disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ -> startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 11)  }
            .setNegativeButton("No") { dialog, _ -> dialog.cancel()
                                            finish()
                                        }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // some work
            onLocationChanged(locationResult.lastLocation)
        }
    }

    fun onLocationChanged(location: Location) {
        mLastLocation = location
        val accuracy = mLastLocation.accuracy
        latiText.text = mLastLocation.latitude.toString()
        longText.text = mLastLocation.longitude.toString()
        if (mLastLocation.hasAltitude()) {
            val altitude = mLastLocation.altitude
            altitudeVal.text = String.format("%.2f", altitude) + " m"
        }
        accuracyVal.text = String.format("%.2f", accuracy) + " m"


        if (accuracy <= 3) {
            stopLocationUpdates()
        } else if (accuracy > 3 && accuracy <= 5) {
            accuracyVal.setTextColor(getColorFromAttr(R.attr.colorPrimaryVariant))
            altitudeVal.setTextColor(getColorFromAttr(R.attr.colorPrimaryVariant))
            latitudeTextView.setTextColor(getColorFromAttr(R.attr.colorPrimaryVariant))
            longitudeTextView.setTextColor(getColorFromAttr(R.attr.colorPrimaryVariant))
        } else {
            accuracyVal.setTextColor(getColorFromAttr(R.attr.colorSecondaryVariant))
            altitudeVal.setTextColor(getColorFromAttr(R.attr.colorSecondaryVariant))
            latitudeTextView.setTextColor(getColorFromAttr(R.attr.colorSecondaryVariant))
            longitudeTextView.setTextColor(getColorFromAttr(R.attr.colorSecondaryVariant))
        }

    }

    fun Context.getColorFromAttr(
        attrColor: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }

    protected fun startLocationUpdates() {

        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.setInterval(interval)
        mLocationRequest.setFastestInterval(fastestInterval)

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)

        val locationSettingRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingRequest)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    private fun stopLocationUpdates() {
        mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
        getBtn.isEnabled = true
        stopBtn.visibility = View.INVISIBLE
        getBtn.text = "Get Point"
    }



//    private fun checkPermission(vararg perm:String) : Boolean {
//        val havePermission = perm.toList().all {
//            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
//        }
//        if (!havePermission) {
//            if (perm.toList().any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }) {
//                val dialog = AlertDialog.Builder(this)
//                    .setTitle("Permission")
//                    .setMessage("Permission needed!")
//                    .setPositiveButton("OK") {id, v -> ActivityCompat.requestPermissions(this, perm, permissionId) }
//                    .setNegativeButton("No", {id, v -> })
//                    .create()
//                    dialog.show()
//
//            } else {
//                ActivityCompat.requestPermissions(this, perm, permissionId)
//            }
//            return false
//        }
//        return true
//    }

}
