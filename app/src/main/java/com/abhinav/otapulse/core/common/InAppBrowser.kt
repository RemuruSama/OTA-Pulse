package com.abhinav.otapulse.core.common

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.abhinav.otapulse.R
import com.abhinav.otapulse.feature.browser.InAppBrowserActivity

fun Fragment.openInAppBrowser(url: String, title: String? = null) {
    if (url.isNotBlank()) {
        startActivity(InAppBrowserActivity.createIntent(requireContext(), url, title))
    } else {
        Toast.makeText(requireContext(), R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
    }
}
