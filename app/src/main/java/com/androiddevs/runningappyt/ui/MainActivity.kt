package com.androiddevs.runningappyt.ui

import android.content.Intent
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.hide
import com.androiddevs.runningappyt.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.androiddevs.runningappyt.show
import com.androiddevs.runningappyt.ui.fragments.SetupFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

       navigateToTrackingFragmentIfNeeded(intent)

        setSupportActionBar(toolbar)

        bottomNavigationView.setupWithNavController(nav_host_fragment.findNavController())

        bottomNavigationView.setOnNavigationItemReselectedListener {
            /*no-op*/
        }


        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                TransitionManager.beginDelayedTransition(rootView, Slide(Gravity.BOTTOM).excludeTarget(R.id.nav_host_fragment, true))
                when (f) {
                    is SetupFragment -> {
                        rootView.bottomNavigationView.visibility = View.GONE
                    }
                    else -> {
                        rootView.bottomNavigationView.visibility = View.VISIBLE
                    }
                }
            }
        }, true)

//        lifecycleScope.launchWhenResumed {
//
//            nav_host_fragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
//
//                when (destination.id) {
//                    R.id.runFragment, R.id.settingsFragment, R.id.statisticsFragment ->
//                        bottomNavigationView.show()
//                    else -> bottomNavigationView.hide()
//                }
//            }
//        }

//        nav_host_fragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
//
//            when (destination.id) {
//                R.id.runFragment, R.id.settingsFragment, R.id.statisticsFragment ->
//                    bottomNavigationView.visibility = View.VISIBLE
//                else -> bottomNavigationView.visibility = View.GONE
//            }
//        }
    }




    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?){
        if(intent?.action==ACTION_SHOW_TRACKING_FRAGMENT){
            nav_host_fragment.findNavController().navigate(R.id.action_global_trackingFragment)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    navigateToTrackingFragmentIfNeeded(intent)
    }
}
