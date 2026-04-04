package com.abhinav.otapulse.feature.devices.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentDeviceBinding
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceFragment : Fragment(R.layout.fragment_device) {

    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DeviceViewModel by viewModels()

    private var defaultCardBackgroundColor: Int = 0

    private val uiUpdateHandler = Handler(Looper.getMainLooper())
    private var supportStatusUpdateRunnable: Runnable? = null

    companion object {
        private const val SUPPORT_STATUS_VISIBILITY_DELAY_MS = 2000L
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDeviceBinding.bind(view)

        // Fix for OplusLongshot crash on Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            binding.root.scrollCaptureHint = View.SCROLL_CAPTURE_HINT_EXCLUDE
        }

        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)
        defaultCardBackgroundColor = typedValue.data

        populateInitialUi(viewModel.deviceInfo)
        observeRealtimeData()

        supportStatusUpdateRunnable = Runnable {
            if (isAdded && _binding != null) { 
                updateDeviceSupportStatus(viewModel.deviceInfo)
            }
        }
        supportStatusUpdateRunnable?.let {
            uiUpdateHandler.postDelayed(it, SUPPORT_STATUS_VISIBILITY_DELAY_MS)
        }
        runEnterAnimation()
    }

    private fun runEnterAnimation() {
        val viewsToAnimate = listOfNotNull(
            binding.heroBackground,
            binding.deviceImageView,
            binding.deviceNameTextView,
            binding.deviceModelTextView,
            binding.deviceSupportedTextView,
            binding.statsHeader,
            binding.statsGrid,
            binding.softwareHeader,
            binding.softwareInfoCard 
        )
        com.abhinav.otapulse.core.common.AnimationUtils.animateEntrance(viewsToAnimate)
    }

    private fun populateInitialUi(deviceInfo: DeviceInfo) {
        with(binding) {
            // Hero Section
            deviceImageView.setImageResource(R.drawable.ic_deviceimage)
            deviceNameTextView.text = deviceInfo.deviceName
            deviceModelTextView.text = deviceInfo.deviceModel
            
            // Software Info (List)
            setupInfoItem(root.findViewById(R.id.androidVersionLayout), R.drawable.ic_android, getString(R.string.android_version), deviceInfo.androidVersion)
            setupInfoItem(root.findViewById(R.id.osVersionLayout), R.drawable.ic_os_version, getString(R.string.os_version), deviceInfo.osVersion)
            setupInfoItem(root.findViewById(R.id.otaVersionLayout), R.drawable.ic_ota_version, getString(R.string.ota_version), deviceInfo.otaVersion)
            setupInfoItem(root.findViewById(R.id.incrementalOsVersionLayout), R.drawable.ic_incremental_version, getString(R.string.incremental_os_version), deviceInfo.incrementalOsVersion)
            setupInfoItem(root.findViewById(R.id.kernelVersionLayout), R.drawable.ic_kernel_version, getString(R.string.kernel_version), deviceInfo.kernelVersion)
            setupInfoItem(root.findViewById(R.id.securityPatchLayout), R.drawable.ic_security, getString(R.string.security_patch_date), deviceInfo.securityPatch)

            // Hardware Info (Grid)
            setupStatItem(root.findViewById(R.id.ramLayout), R.drawable.ic_ram, getString(R.string.amount_of_ram), deviceInfo.totalRam, deviceInfo.ramUsagePercent)
            setupStatItem(root.findViewById(R.id.socLayout), R.drawable.ic_soc, getString(R.string.system_on_a_chip), deviceInfo.soc)
            setupStatItem(root.findViewById(R.id.cpuLayout), R.drawable.ic_cpu, getString(R.string.processor_frequency), deviceInfo.cpuInfo)
            setupStatItem(root.findViewById(R.id.storageLayout), R.drawable.ic_storage, getString(R.string.storage), deviceInfo.storage, deviceInfo.storageUsagePercent)
            setupStatItem(root.findViewById(R.id.batteryLayout), R.drawable.ic_battery, getString(R.string.battery), deviceInfo.battery)
            setupStatItem(root.findViewById(R.id.displayLayout), R.drawable.ic_display, getString(R.string.display), deviceInfo.display)
        }
    }

    private fun updateDeviceSupportStatus(deviceInfo: DeviceInfo) {
        with(binding) {
            val supportedText: String
            val supportedIconColorRes: Int
            val supportedBgRes: Int

            if (deviceInfo.isSupported) {
                supportedText = getString(R.string.device_supported)
                supportedIconColorRes = R.color.material_green_500
            } else {
                supportedText = getString(R.string.device_not_supported)
                supportedIconColorRes = R.color.material_red_500
            }

            deviceSupportedTextView.text = supportedText
            deviceSupportedTextView.setTextColor(ContextCompat.getColor(requireContext(), supportedIconColorRes))
        }
    }


    private fun observeRealtimeData() {
        viewModel.ramInfoLiveData.observe(viewLifecycleOwner) { ramInfo ->
            // Update logic for new layout (item_stat_card)
            binding.root.findViewById<View>(R.id.ramLayout)?.let { itemView ->
                itemView.findViewById<TextView>(R.id.statValue).text = ramInfo.usageString
                itemView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.statProgress)?.apply {
                    progress = ramInfo.percent
                    isVisible = true
                }
            }
        }

        viewModel.batteryInfoLiveData.observe(viewLifecycleOwner) { batteryString ->
            binding.root.findViewById<View>(R.id.batteryLayout)?.let { itemView ->
                itemView.findViewById<TextView>(R.id.statValue).text = batteryString
            }
        }
    }

    // Helper for List Items (device_info_item.xml)
    private fun setupInfoItem(
        itemView: View,
        iconResId: Int,
        title: String,
        value: String,
        progress: Int? = null,
        iconTintColorRes: Int? = null,
        cardBackgroundColorRes: Int? = null
    ) {
        val itemIconCard = itemView.findViewById<MaterialCardView>(R.id.itemIconCard)
        val itemIcon = itemView.findViewById<ImageView>(R.id.itemIcon)
        val itemTitle = itemView.findViewById<TextView>(R.id.itemTitle)
        val itemValue = itemView.findViewById<TextView>(R.id.itemValue)
        val itemProgress = itemView.findViewById<ProgressBar>(R.id.itemProgress)

        itemIcon.setImageResource(iconResId)

        if (iconTintColorRes != null) {
            val tintColor = ContextCompat.getColorStateList(itemView.context, iconTintColorRes)
            DrawableCompat.setTintList(itemIcon.drawable.mutate(), tintColor)
        } else {
            itemIcon.clearColorFilter()
            itemIcon.setImageDrawable(ContextCompat.getDrawable(itemView.context, iconResId))
        }

        if (cardBackgroundColorRes != null) {
            itemIconCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, cardBackgroundColorRes))
        } else {
            itemIconCard.setCardBackgroundColor(defaultCardBackgroundColor)
        }

        itemTitle.text = title
        itemValue.text = value
        itemValue.isVisible = value.isNotEmpty()

        itemProgress?.isVisible = progress != null
        progress?.let { itemProgress?.progress = it }
    }
    
    // Helper for Grid Items (item_stat_card.xml)
    private fun setupStatItem(
        itemView: View,
        iconResId: Int,
        title: String,
        value: String,
        progress: Int? = null
    ) {
        val statIcon = itemView.findViewById<ImageView>(R.id.statIcon)
        val statTitle = itemView.findViewById<TextView>(R.id.statTitle)
        val statValue = itemView.findViewById<TextView>(R.id.statValue)
        val statProgress = itemView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.statProgress)

        statIcon.setImageResource(iconResId)
        statTitle.text = title
        statValue.text = value
        
        statProgress.isVisible = progress != null
        progress?.let { statProgress.progress = it }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startRealtimeUpdates() // For RAM/Battery
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopRealtimeUpdates() // For RAM/Battery
    }

    override fun onDestroyView() {
        super.onDestroyView()
        supportStatusUpdateRunnable?.let { uiUpdateHandler.removeCallbacks(it) }
        supportStatusUpdateRunnable = null
        _binding = null
    }
}
