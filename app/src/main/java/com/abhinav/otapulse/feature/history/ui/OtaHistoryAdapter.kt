package com.abhinav.otapulse.feature.history.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.model.OtaHistoryEntry
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OtaHistoryAdapter(
    private val onItemClick: ((OtaHistoryEntry) -> Unit)? = null
) : ListAdapter<OtaHistoryEntry, OtaHistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    private var lastAnimatedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ota_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
        
        // Entrance animation — only animate items appearing for the first time
        if (position > lastAnimatedPosition) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_slide_up)
            animation.startOffset = (position * 50).toLong().coerceAtMost(300)
            holder.itemView.startAnimation(animation)
            lastAnimatedPosition = position
        }
    }

    override fun onViewDetachedFromWindow(holder: HistoryViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    fun resetAnimations() {
        lastAnimatedPosition = -1
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val tvVersion: TextView = itemView.findViewById(R.id.tvVersion)
        private val chipRegion: Chip = itemView.findViewById(R.id.chipRegion)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvSize: TextView = itemView.findViewById(R.id.tvSize)
        private val layoutExtraInfo: LinearLayout = itemView.findViewById(R.id.layoutExtraInfo)
        private val tvAndroidVersion: TextView = itemView.findViewById(R.id.tvAndroidVersion)
        private val tvSecurityPatch: TextView = itemView.findViewById(R.id.tvSecurityPatch)
        private val icSecurity: ImageView = itemView.findViewById(R.id.icSecurity)
        private val dotSeparator2: TextView = itemView.findViewById(R.id.dotSeparator2)
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun bind(entry: OtaHistoryEntry) {
            // Device name
            val isHomeUpdateRecord = entry.deviceName == "Custom Device" || entry.deviceName.startsWith("Custom|") || entry.deviceName.equals("This Device", ignoreCase = true)
            val displayDeviceName = if (entry.deviceName.startsWith("Custom|")) {
                entry.deviceName.removePrefix("Custom|").ifBlank {
                    (entry.otaUpdate.versionName ?: entry.otaUpdate.componentVersion).substringBefore("_")
                }
            } else if (entry.deviceName == "Custom Device") {
                (entry.otaUpdate.versionName ?: entry.otaUpdate.componentVersion).substringBefore("_").ifBlank { "Custom Device" }
            } else {
                entry.deviceName
            }
            tvDeviceName.text = displayDeviceName

            // Version
            tvVersion.text = entry.otaUpdate.versionName ?: entry.otaUpdate.componentVersion

            // Region chip
            if (entry.region.isNotBlank() && !isHomeUpdateRecord) {
                chipRegion.visibility = View.VISIBLE
                chipRegion.text = entry.region
            } else {
                chipRegion.visibility = View.GONE
            }

            // Date
            tvDate.text = dateFormat.format(Date(entry.timestamp))

            // Size
            tvSize.text = entry.otaUpdate.size.ifBlank { "—" }

            // Extra info: Android version + Security patch
            val hasAndroidVersion = !entry.otaUpdate.realAndroidVersion.isNullOrBlank()
            val hasSecurityPatch = !entry.otaUpdate.securityPatch.isNullOrBlank()

            if (hasAndroidVersion || hasSecurityPatch) {
                layoutExtraInfo.visibility = View.VISIBLE

                if (hasAndroidVersion) {
                    tvAndroidVersion.visibility = View.VISIBLE
                    tvAndroidVersion.text = entry.otaUpdate.realAndroidVersion?.removePrefix("Android ")?.trim()
                } else {
                    tvAndroidVersion.visibility = View.GONE
                    itemView.findViewById<ImageView>(R.id.icAndroid).visibility = View.GONE
                }

                if (hasSecurityPatch) {
                    tvSecurityPatch.visibility = View.VISIBLE
                    icSecurity.visibility = View.VISIBLE
                    tvSecurityPatch.text = entry.otaUpdate.securityPatch
                } else {
                    tvSecurityPatch.visibility = View.GONE
                    icSecurity.visibility = View.GONE
                }

                // Show dot separator only if both are visible
                dotSeparator2.visibility = if (hasAndroidVersion && hasSecurityPatch) View.VISIBLE else View.GONE
            } else {
                layoutExtraInfo.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClick?.invoke(entry)
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<OtaHistoryEntry>() {
        override fun areItemsTheSame(oldItem: OtaHistoryEntry, newItem: OtaHistoryEntry): Boolean {
            return oldItem.timestamp == newItem.timestamp && 
                   oldItem.region == newItem.region &&
                   (oldItem.otaUpdate.versionName ?: oldItem.otaUpdate.componentVersion) == (newItem.otaUpdate.versionName ?: newItem.otaUpdate.componentVersion)
        }

        override fun areContentsTheSame(oldItem: OtaHistoryEntry, newItem: OtaHistoryEntry): Boolean {
            return oldItem == newItem
        }
    }
}
