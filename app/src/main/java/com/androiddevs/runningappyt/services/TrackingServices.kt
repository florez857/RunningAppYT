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
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.other.Constants.ACTION_PAUSE_SERVICE
import com.androiddevs.runningappyt.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.androiddevs.runningappyt.other.Constants.ACTION_STOP_SERVICE
import com.androiddevs.runningappyt.other.Constants.FASTEST_LOCATION_INTERVAL
import com.androiddevs.runningappyt.other.Constants.LOCATION_UPDATE_INTERVAL
import com.androiddevs.runningappyt.other.Constants.NOTIFICATION_CHANNEL_ID
import com.androiddevs.runningappyt.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.androiddevs.runningappyt.other.Constants.NOTIFICATION_ID
import com.androiddevs.runningappyt.other.Constants.TIMER_UPDATE_INTERVAL
import com.androiddevs.runningappyt.other.TrackingUtility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingServices : LifecycleService() {

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var curNotificationCompat: NotificationCompat.Builder


    var isFirstRun = true
    var isServiceKiller=false
    private val timeRunInSeconds = MutableLiveData<Long>()

    //datos observados desde el exterior
    companion object {
        //tiempo de ejecucion
        val timeRunMillins = MutableLiveData<Long>()

        //dato observable que indica si esta ejecutandose el servicio
        //al actualizarce se actuaizan los botones del fragment
        //se activa o desactiva el boton de iniciar o pausar
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
        timeRunInSeconds.postValue(0L)
        timeRunMillins.postValue(0L)

    }

    override fun onCreate() {
        super.onCreate()
        curNotificationCompat = baseNotificationBuilder
        postInitialValues()

        isTracking.observe(this, Observer {
            updateLocationTrackig(it)
            updateNotificationTrackingState(it)
        })
    }

    private fun killerService(){
isServiceKiller=true
        isFirstRun=true
        pauseTracking()
        postInitialValues()
        stopForeground(true)
        stopSelf()
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
                        startTimer()
                    }
                }

                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused Service")
                    pauseTracking()
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stop Service")
                    killerService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    //varible que me dice si esta habilitado el temporizador
    private var isTimerEnabled = false

    //tiempo transcurrido desde que se inicia hasta que se pausa , la suma de todos los lapTime
    //indican el tiempo total transcurrido
    private var lapTime = 0L

    //tiempo total que transcurrio
    private var timeRun = 0L

    //tiempo en que inicia la carrera
    private var timeStarted = 0L

    //ultimo segundo
    private var lastSecondTimestamp = 0L


    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                //tiempo transcurrido desde el inicio
                lapTime = System.currentTimeMillis() - timeStarted
                //tiempo transcurrido en milisegundos se almacena en timeRunMillins
                timeRunMillins.postValue(timeRun + lapTime)
                //si el tiempo calculado es mayor al ultimo segundo almacenado
                //coloco el tiempo en segundos en timeRunSeconds y actualizo el ultimo segundo guardado o transcurrido
                if (timeRunMillins.value!! >= lastSecondTimestamp + 1000L) {
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1000L)
                    lastSecondTimestamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
            timeRun += lapTime
        }
    }

    private fun pauseTracking() {
        isTracking.postValue(false)
        isTimerEnabled = false
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
        startTimer()
        isTracking.postValue(true)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())



    timeRunInSeconds.observe(this, Observer {
        if(!isServiceKiller){
        val notification =
            curNotificationCompat.setContentText(TrackingUtility.getFormattedStopWatchTime(it ))
        notificationManager.notify(NOTIFICATION_ID, notification.build())}
    })
    }

//    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
//        this,
//        0,
//        Intent(this, MainActivity::class.java).also {
//            it.action = ACTION_SHOW_TRACKING_FRAGMENT
//        },
//        FLAG_UPDATE_CURRENT
//    )

//    private fun getPendingIntent(): PendingIntent? {
//        val resultIntent = android.content.Intent(
//            this, MainActivity::class.java
//        )
//        val pendingIntent = NavDeepLinkBuilder(this)
//            .setGraph(R.navigation.nav_graph)  //3
//            .setDestination(R.id.action_global_trackingFragment)  //4
//            .createTaskStackBuilder().addParentStack(MainActivity::class.java)
//            .getPendingIntent(0, FLAG_UPDATE_CURRENT)
//        return pendingIntent
//    }


    private fun updateNotificationTrackingState(isTracking: Boolean) {

        val notifictionActiionText = if (isTracking) "Pause" else "Resume"

        val pendingIntent = if (isTracking) {
            val pauseIntent = Intent(this, TrackingServices::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT)

        } else {
            val resumeIntent = Intent(this, TrackingServices::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 1, resumeIntent, FLAG_UPDATE_CURRENT)
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        curNotificationCompat.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationCompat, ArrayList<NotificationCompat.Action>())
        }
if(!isServiceKiller) {
    curNotificationCompat = baseNotificationBuilder.addAction(
        R.drawable.ic_pause_black_24dp,
        notifictionActiionText,
        pendingIntent
    )
    notificationManager.notify(NOTIFICATION_ID, curNotificationCompat.build())

}

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