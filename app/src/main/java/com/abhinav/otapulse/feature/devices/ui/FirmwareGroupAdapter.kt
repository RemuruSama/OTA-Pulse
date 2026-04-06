package com.abhinav.otapulse.feature.devices.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.databinding.ListItemFirmwareGroupEditorBinding
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.core.common.setHapticClickListener

class FirmwareGroupAdapter(
    private val onAddVariantClicked: (androidVersion: String) -> Unit,
    private val onRemoveVariantClicked: (androidVersion: String, variant: RegionVariant) -> Unit,
    private val onEditGroupClicked: (androidVersion: String) -> Unit,
    private val onDeleteGroupClicked: (androidVersion: String) -> Unit
) : ListAdapter<Pair<String, List<RegionVariant>>, FirmwareGroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ListItemFirmwareGroupEditorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(private val binding: ListItemFirmwareGroupEditorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: Pair<String, List<RegionVariant>>) {
            val (androidVersion, variants) = group
            binding.androidVersionTitle.text = androidVersion
            binding.variantCountText.text = binding.root.resources.getQuantityString(
                R.plurals.add_device_variant_count,
                variants.size,
                variants.size
            )

            val variantAdapter = RegionVariantEditorAdapter { variant ->
                onRemoveVariantClicked(androidVersion, variant)
            }
            binding.variantsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = variantAdapter
            }
            variantAdapter.submitList(variants)

            binding.variantsRecyclerView.isVisible = variants.isNotEmpty()
            binding.emptyVariantsView.isVisible = variants.isEmpty()

            binding.buttonAddVariantToGroup.setHapticClickListener {
                onAddVariantClicked(androidVersion)
            }
            binding.buttonEditGroup.setHapticClickListener {
                onEditGroupClicked(androidVersion)
            }
            binding.buttonDeleteGroup.setHapticClickListener {
                onDeleteGroupClicked(androidVersion)
            }
        }
    }

    class GroupDiffCallback : DiffUtil.ItemCallback<Pair<String, List<RegionVariant>>>() {
        override fun areItemsTheSame(oldItem: Pair<String, List<RegionVariant>>, newItem: Pair<String, List<RegionVariant>>): Boolean {
            return oldItem.first == newItem.first
        }

        override fun areContentsTheSame(oldItem: Pair<String, List<RegionVariant>>, newItem: Pair<String, List<RegionVariant>>): Boolean {
            return oldItem == newItem
        }
    }
}
