package com.abhinav.otapulse.feature.settings.libraries

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.databinding.ItemLibraryBinding

data class Library(val name: String, val license: String, val url: String)

class LibrariesAdapter(
    private val libraries: List<Library>,
    private val onItemClick: (Library) -> Unit
) : RecyclerView.Adapter<LibrariesAdapter.LibraryViewHolder>() {

    inner class LibraryViewHolder(private val binding: ItemLibraryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(library: Library) {
            binding.libraryName.text = library.name
            binding.libraryLicense.text = library.license
            binding.root.setOnClickListener { onItemClick(library) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val binding = ItemLibraryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LibraryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int) {
        holder.bind(libraries[position])
    }

    override fun getItemCount(): Int = libraries.size
}
