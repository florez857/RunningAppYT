package com.androiddevs.runningappyt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.androiddevs.runningappyt.other.Constants
import com.androiddevs.runningappyt.services.TrackingServices

class AutoArranque:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
//        Intent(context, TrackingServices::class.java).also {
//            it.action = Constants.ACTION_START_OR_RESUME_SERVICE
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context?.startForegroundService(it);
//            } else {
//                context?.startService(it);
//            }
//            Log.i("Autostart", "started");
//
//        }
    }
}