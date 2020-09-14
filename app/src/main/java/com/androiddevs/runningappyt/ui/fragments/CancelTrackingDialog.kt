package com.androiddevs.runningappyt.ui.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.androiddevs.runningappyt.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CancelTrackingDialog : DialogFragment() {

    private var yesListener: (() -> Unit)? = null

    fun setYesListener(listener: () -> Unit) {
        yesListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)

            .setTitle("Cancelar La Carrera?")
            .setMessage("estas seguro que quieres cancelar la carrera actual")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("SI") { _, _ ->
                yesListener?.let { yes ->
                    yes()
                }
            }
            .setNegativeButton("NO") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()


    }
}