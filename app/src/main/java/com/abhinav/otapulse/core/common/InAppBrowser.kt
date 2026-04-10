package com.abhinav.otapulse.core.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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

fun Context.openExternalBrowser(url: String) {
    if (url.isBlank()) {
        Toast.makeText(this, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
        return
    }

    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
    }
}

fun Fragment.openExternalBrowser(url: String) {
    requireContext().openExternalBrowser(url)
}
