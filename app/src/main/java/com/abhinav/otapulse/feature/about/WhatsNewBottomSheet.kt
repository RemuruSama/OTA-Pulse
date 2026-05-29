package com.abhinav.otapulse.feature.about

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.abhinav.otapulse.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.noties.markwon.Markwon

/**
 * A bottom sheet that displays "What's New" changelog after an app update.
 *
 * Usage:
 * ```
 * WhatsNewBottomSheet.newInstance("3.0.7", "## Changes\n- Added AMOLED theme\n...")
 *     .show(supportFragmentManager, "whats_new")
 * ```
 */
class WhatsNewBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_VERSION = "arg_version"
        private const val ARG_CHANGELOG = "arg_changelog"

        private const val PREFS_NAME = "whats_new_prefs"
        private const val KEY_LAST_SHOWN_VERSION = "last_shown_version"

        fun newInstance(version: String, changelog: String): WhatsNewBottomSheet {
            return WhatsNewBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_VERSION, version)
                    putString(ARG_CHANGELOG, changelog)
                }
            }
        }

        /**
         * Returns true if the "What's New" sheet should be shown for this version.
         * Call this on launch to decide whether to fetch and display.
         */
        fun shouldShow(context: Context, currentVersion: String): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastShown = prefs.getString(KEY_LAST_SHOWN_VERSION, null)
            return lastShown != currentVersion
        }

        /**
         * Mark the current version as shown so it won't appear again until the next update.
         */
        fun markShown(context: Context, currentVersion: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LAST_SHOWN_VERSION, currentVersion).apply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_whats_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val version = arguments?.getString(ARG_VERSION).orEmpty()
        val changelog = arguments?.getString(ARG_CHANGELOG).orEmpty()

        view.findViewById<TextView>(R.id.whatsNewTitle).text =
            getString(R.string.whats_new_title, version)

        val changelogView = view.findViewById<TextView>(R.id.whatsNewChangelog)
        val markwon = Markwon.create(requireContext())
        markwon.setMarkdown(changelogView, changelog)

        view.findViewById<View>(R.id.whatsNewDismissButton).setOnClickListener {
            dismiss()
        }

        // Mark as shown
        markShown(requireContext(), version)
    }
}
