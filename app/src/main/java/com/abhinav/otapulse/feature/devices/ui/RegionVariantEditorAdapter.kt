package com.abhinav.otapulse.feature.devices.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.databinding.ListItemRegionVariantEditorBinding
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.core.common.setHapticClickListener

class RegionVariantEditorAdapter(
    private val onRemoveClicked: (RegionVariant) -> Unit
) : ListAdapter<RegionVariant, RegionVariantEditorAdapter.VariantViewHolder>(VariantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariantViewHolder {
        val binding = ListItemRegionVariantEditorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VariantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VariantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VariantViewHolder(private val binding: ListItemRegionVariantEditorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(variant: RegionVariant) {
            binding.variantRegionBadge.text = variant.region
            binding.variantDisplayName.text = variant.displayName
            binding.variantProductModel.text = variant.productName
            binding.variantFirmwareVersion.text = variant.firmwareVersion
            binding.buttonRemoveVariant.setHapticClickListener {
                onRemoveClicked(variant)
            }
        }
    }

    class VariantDiffCallback : DiffUtil.ItemCallback<RegionVariant>() {
        override fun areItemsTheSame(oldItem: RegionVariant, newItem: RegionVariant): Boolean {
            return oldItem.displayName == newItem.displayName && oldItem.productName == newItem.productName
        }

        override fun areContentsTheSame(oldItem: RegionVariant, newItem: RegionVariant): Boolean {
            return oldItem == newItem
        }
    }
}
