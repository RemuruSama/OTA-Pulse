package com.abhinav.otapulse.ui.devices

import android.graphics.drawable.AnimatedVectorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.ListPopupWindow
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.ListItemPredefinedDeviceBinding
import com.abhinav.otapulse.domain.model.Device
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.domain.model.RegionVariant
import com.abhinav.otapulse.util.setHapticClickListener
import com.google.android.material.button.MaterialButton

class DeviceAdapter(
    private val onFetchDetails: (Device, RegionVariant) -> Unit,
    private val onDownload: (OtaUpdate, Device, RegionVariant) -> Unit,
    private val onToggleFavorite: (String) -> Unit,
    private val onCopyLink: (String) -> Unit,
    private val onViewChangelog: (String) -> Unit,
    private val onDeleteCustomDevice: (Device) -> Unit,
    private val onEditCustomDevice: (Device) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    private var otaDetailsMap: Map<String, Result<OtaUpdate>> = emptyMap()
    private var expandedPosition: Int = RecyclerView.NO_POSITION

    private val selectedAndroidVersions = mutableMapOf<String, String>()
    private val selectedVariants = mutableMapOf<String, RegionVariant>()

    fun updateOtaDetails(newDetails: Map<String, Result<OtaUpdate>>) {
        otaDetailsMap = newDetails
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ListItemPredefinedDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(private val binding: ListItemPredefinedDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: Device) {
            val isExpanded = bindingAdapterPosition == expandedPosition
            val currentSelectedVariant = selectedVariants[device.name]

            binding.deviceNameTextView.text = device.name
            binding.deviceModelTextView.text = currentSelectedVariant?.productModel ?: "Select a variant"
            binding.favoriteButton.setImageResource(if (device.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline)
            binding.expandArrow.rotation = if (isExpanded) 180f else 0f
            binding.expandableContentLayout.isVisible = isExpanded

            binding.moreOptionsButton.isVisible = device.isCustom
            binding.moreOptionsButton.setHapticClickListener {
                showPopupMenu(it, device)
            }

            binding.favoriteButton.setHapticClickListener { onToggleFavorite(device.name) }
            binding.headerLayout.setHapticClickListener {
                val previousExpandedPosition = expandedPosition
                expandedPosition = if (isExpanded) RecyclerView.NO_POSITION else bindingAdapterPosition
                notifyItemChanged(previousExpandedPosition)
                notifyItemChanged(expandedPosition)
            }

            if (isExpanded) {
                val currentAndroidVersion = selectedAndroidVersions[device.name] ?: device.firmwareGroups.keys.maxOrNull()
                populateAndroidVersions(device, currentAndroidVersion)

                val variantsForVersion = currentAndroidVersion?.let { device.firmwareGroups[it] } ?: emptyList()
                populateRegionalVariants(device, variantsForVersion, currentSelectedVariant)

                handleOtaDetails(device, currentSelectedVariant)
            }
        }

        private fun populateAndroidVersions(device: Device, selectedVersion: String?) {
            binding.androidVersionLayout.removeAllViews()
            device.firmwareGroups.keys.forEach { version ->
                val button = createCapsuleButton(version, version == selectedVersion)
                button.setHapticClickListener {
                    selectedAndroidVersions[device.name] = version
                    selectedVariants.remove(device.name)
                    otaDetailsMap = otaDetailsMap.filterKeys { !it.startsWith(device.name) }
                    notifyItemChanged(bindingAdapterPosition)
                }
                binding.androidVersionLayout.addView(button)
            }
        }

        private fun populateRegionalVariants(device: Device, variants: List<RegionVariant>, currentSelectedVariant: RegionVariant?) {
            binding.regionalVariantFlexboxLayout.removeAllViews()
            variants.forEach { variant ->
                val button = createCapsuleButton(variant.displayName, variant == currentSelectedVariant)
                button.setHapticClickListener {
                    selectedVariants[device.name] = variant
                    onFetchDetails(device, variant)
                    notifyItemChanged(bindingAdapterPosition)
                }
                binding.regionalVariantFlexboxLayout.addView(button)
            }
        }

        private fun handleOtaDetails(device: Device, selectedVariant: RegionVariant?) {
            val deviceKey = selectedVariant?.let { "${device.name}_${it.displayName}" }
            val otaResult = deviceKey?.let { otaDetailsMap[it] }

            binding.loadingIndicator.isVisible = device.isLoadingDetails
            if (device.isLoadingDetails) {
                val drawable = ContextCompat.getDrawable(binding.root.context, R.drawable.loading_dots_animation)
                if (drawable is AnimatedVectorDrawable) {
                    binding.loadingIndicator.setImageDrawable(drawable)
                    drawable.start()
                }
            } else {
                (binding.loadingIndicator.drawable as? AnimatedVectorDrawable)?.stop()
                binding.loadingIndicator.setImageDrawable(null)
            }

            binding.otaDetailsContainer.isVisible = !device.isLoadingDetails && otaResult?.isSuccess == true

            binding.errorCard.isVisible = !device.isLoadingDetails && otaResult?.isFailure == true

            otaResult?.onSuccess { otaUpdate ->
                bindOtaSuccess(otaUpdate, device, selectedVariant!!)
            }?.onFailure { error ->
                val errorMessage = error.message ?: "Failed to retrieve update information. Please try again later."
                binding.errorTextView.text = when {
                    errorMessage.contains("No update", ignoreCase = true) -> "No new update is available for this version."
                    errorMessage.contains("Server Error: 2004 (artifactV1Result is empty)", ignoreCase = true) -> "No update found for the selected device and variant."
                    errorMessage.contains("network", ignoreCase = true) -> "Network error. Please check your connection."
                    errorMessage.contains("HTTP", ignoreCase = true) -> "Server error. Please try again later."
                    else -> errorMessage
                }
                binding.retryButton.setHapticClickListener { onFetchDetails(device, selectedVariant!!) }
            }
        }

        private fun bindOtaSuccess(ota: OtaUpdate, device: Device, variant: RegionVariant) {
            val otaDetailsTitle = binding.root.context.getString(R.string.ota_details_title)
            val fullRegionName = getFullRegionName(variant.displayName)
            binding.tvComponentName.text = "$otaDetailsTitle: $fullRegionName"
            binding.tvVersionName.text = ota.versionName
            binding.tvAndroidVersion.text = ota.realAndroidVersion
            binding.tvSecurityPatch.text = ota.securityPatch
            binding.tvRealOsVersion.text = ota.realOsVersion
            binding.tvSize.text = ota.size
            binding.tvMd5.text = ota.md5
            binding.btnDownloadOta.setHapticClickListener { onDownload(ota, device, variant) }
            binding.btnCopyLink.setHapticClickListener { onCopyLink(ota.url) }
            binding.btnChangelog.setHapticClickListener { onViewChangelog(ota.panelUrl ?: "") }
        }

        private fun showPopupMenu(view: android.view.View, device: Device) {
            val listPopupWindow = ListPopupWindow(view.context)
            listPopupWindow.anchorView = view
            
            val menuItems = listOf(
                MenuItemData("Edit", R.drawable.ic_edit),
                MenuItemData("Delete", R.drawable.ic_delete)
            )
            
            val adapter = MenuAdapter(view.context, menuItems)
            listPopupWindow.setAdapter(adapter)
            
            listPopupWindow.setOnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> onEditCustomDevice(device)
                    1 -> onDeleteCustomDevice(device)
                }
                listPopupWindow.dismiss()
            }
            
            listPopupWindow.width = view.context.resources.getDimensionPixelSize(R.dimen.menu_width_compact)
            listPopupWindow.isModal = true
            listPopupWindow.show()
        }


        private fun createCapsuleButton(text: String, isSelected: Boolean): MaterialButton {
            val inflater = LayoutInflater.from(binding.root.context)
            return (inflater.inflate(R.layout.capsule_button, binding.regionalVariantFlexboxLayout, false) as MaterialButton).apply {
                this.text = text
                this.isSelected = isSelected
            }
        }

        private fun getFullRegionName(regionCode: String): String {
            return when (regionCode) {
                "GLO" -> "Global"
                "CN" -> "China"
                "VN" -> "Vietnam"
                "IN" -> "India"
                "EU" -> "Europe"
                "TR" -> "Turkey"
                "RU" -> "Russia"
                "MEA" -> "Middle East & Africa"
                "SA" -> "Saudi Arabia"
                "SG" -> "Singapore"
                "TH" -> "Thailand"
                "LATAM" -> "Latin America"
                "BR" -> "Brazil"
                "TW" -> "Taiwan"
                "MY" -> "Malaysia"
                "ID" -> "Indonesia"
                else -> regionCode
            }
        }
    }

    private data class MenuItemData(val title: String, val iconRes: Int)

    private class MenuAdapter(
        context: android.content.Context,
        private val items: List<MenuItemData>
    ) : android.widget.BaseAdapter() {
        private val inflater = android.view.LayoutInflater.from(context)

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): MenuItemData = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
            val view = convertView ?: inflater.inflate(R.layout.list_item_menu_compact, parent, false)
            val item = getItem(position)
            
            view.findViewById<android.widget.ImageView>(R.id.menuIcon).setImageResource(item.iconRes)
            view.findViewById<android.widget.TextView>(R.id.menuTitle).text = item.title
            
            return view
        }
    }



    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean = oldItem == newItem
    }
}
