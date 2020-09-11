package com.androiddevs.runningappyt.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.other.Constants.ACTION_PAUSE_SERVICE
import com.androiddevs.runningappyt.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.androiddevs.runningappyt.other.Constants.MAP_ZOOM
import com.androiddevs.runningappyt.other.Constants.POLYLINE_COLOR
import com.androiddevs.runningappyt.other.Constants.POLYLINE_WIDTH
import com.androiddevs.runningappyt.services.Polyline
import com.androiddevs.runningappyt.services.TrackingServices
import com.androiddevs.runningappyt.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private val viewModel: MainViewModel by viewModels()

    private var mapa: GoogleMap? = null

    //bandera de seguimiento del estado si se esta siguiendo o no
    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnToggleRun.setOnClickListener {
          toggleRun()
        }

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            mapa = it
            addAllPolylines()
        }
        suscriberToObservers()
    }

    private fun  suscriberToObservers(){

        TrackingServices.isTracking.observe(viewLifecycleOwner, Observer {
             updateTrackink(it)
        })

        TrackingServices.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints=it
            addLatestPolyline()
            moveCameraTouser()
        })

    }

    private fun toggleRun(){
        if(isTracking){
            sendCommandToService(ACTION_PAUSE_SERVICE)
        }else{
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTrackink(isTracking:Boolean){
        this.isTracking=isTracking
        if (!isTracking){
            btnToggleRun.text="Start"
            btnFinishRun.visibility=View.VISIBLE
        }else{
            btnToggleRun.text="Stop"
            btnFinishRun.visibility=View.GONE
        }

    }

    private fun addAllPolylines(){
        for(polyline in pathPoints){
            val polylineOptions=PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            mapa?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline() {

        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
         mapa?.addPolyline(polylineOptions)
        }

    }
    private fun moveCameraTouser(){

        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()){
            mapa?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(), MAP_ZOOM
                )
            )
        }

    }


    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingServices::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}