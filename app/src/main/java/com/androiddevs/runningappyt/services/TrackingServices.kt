package com.androiddevs.runningappyt.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavDeepLinkBuilder
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.other.Constants.ACTION_PAUSE_SERVICE
import com.androiddevs.runningappyt.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.androiddevs.runningappyt.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.androiddevs.runningappyt.other.Constants.ACTION_STOP_SERVICE
import com.androiddevs.runningappyt.other.Constants.FASTEST_LOCATION_INTERVAL
import com.androiddevs.runningappyt.other.Constants.LOCATION_UPDATE_INTERVAL
import com.androiddevs.runningappyt.other.Constants.NOTIFICATION_CHANNEL_ID
import com.androiddevs.runningappyt.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.androiddevs.runningappyt.other.Constants.NOTIFICATION_ID
import com.androiddevs.runningappyt.other.TrackingUtility
import com.androiddevs.runningappyt.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

class TrackingServices : LifecycleService() {

    var isFirstRun = true
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val timeRunInSeconds = MutableLiveData<Long>()

    companion object {

        //tiempo de ejecucion
        val timeRunMillins = MutableLiveData<Long>()

        //dato observable que indica si esta ejecutandose el servicio
        val isTracking = MutableLiveData<Boolean>()

        //lista de polilineas que representan nuestras rutas
        val pathPoints = MutableLiveData<Polylines>()

    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTrackig(isTracking: Boolean) {
        if (isTracking) {
            if (TrackingUtility.hasLocationPermissions(this)) {
                val request = LocationRequest().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if (isTracking.value!!) {
                result?.locations?.let { locations ->
                    for (location in locations) {
                        addPathPoint(location)
                        Timber.d(" NUEVA LOCALIZACION:${location.latitude}, ${location.longitude} ")
                    }
                }
            }
        }
    }

    //inicializa los valores de inicio de los mutable live data
    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
    }

    override fun onCreate() {
        super.onCreate()
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        isTracking.observe(this, Observer {
            updateLocationTrackig(it)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            // startForegroundService()
            when (it.action) {

                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        Timber.d(" Service Running....")
                        startForegroundService()
                    }
                }

                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused Service")
                    pauseTracking()
                }

                ACTION_STOP_SERVICE -> {
                    Timber.d("Stop Service")
                }

            }
        }
        return super.onStartCommand(intent, flags, startId)
    }


    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondtimestamp = 0L


    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                //dieferencia entre ahora y tiempo de inicio
                lapTime = System.currentTimeMillis() - timeStarted
                //colocamos la nueva diferencia
                timeRunMillins.postValue(timeRun + lapTime)
                if (timeRunMillins.value!! >= lastSecondtimestamp + 1000L) {
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1000L)
                    lastSecondtimestamp += 1000L
                }

            }
        }
    }

    private fun pauseTracking() {
        isTracking.postValue(false)
    }

    private fun addEmptyPolyline() = pathPoints.value?.apply {
        //agrego una nueva polylinea vacia
        add(mutableListOf())
        //actualizo el valor de la lista de polilineas
        pathPoints.postValue(this)
        //en caso de no tener ninguna polilinea ,agregamos una lista de polilineas vacias
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))


    private fun addPathPoint(location: Location?) {

        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            }
        }


    }

    private fun startForegroundService() {
        addEmptyPolyline()
        isTracking.postValue(true)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
            .setContentTitle("Corriendo App")
            .setContentText("00:00:00:00")
            .setContentIntent(getMainActivityPendingIntent())

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).also {
            it.action = ACTION_SHOW_TRACKING_FRAGMENT
        },
        FLAG_UPDATE_CURRENT
    )

    private fun getPendingIntent(): PendingIntent? {
        val resultIntent = android.content.Intent(
            this, MainActivity::class.java
        )
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)  //3
            .setDestination(R.id.action_global_trackingFragment)  //4
            .createTaskStackBuilder().addParentStack(MainActivity::class.java)
            .getPendingIntent(0, FLAG_UPDATE_CURRENT)
        return pendingIntent
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}