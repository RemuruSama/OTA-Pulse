package com.abhinav.otapulse.ui.settings.libraries

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.abhinav.otapulse.databinding.FragmentLibrariesBinding
import com.abhinav.otapulse.util.setHapticClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LibrariesFragment : Fragment() {

    private var _binding: FragmentLibrariesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibrariesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        val libraries = listOf(
            Library("Android Jetpack", "Apache License 2.0", "https://developer.android.com/jetpack"),
            Library("realme-ota", "Open Source", "https://github.com/R0rt1z2/realme-ota"),
            Library("Material Components for Android", "Apache License 2.0", "https://github.com/material-components/material-components-android"),
            Library("Hilt", "Apache License 2.0", "https://dagger.dev/hilt/"),
            Library("Retrofit", "Apache License 2.0", "https://square.github.io/retrofit/"),
            Library("OkHttp", "Apache License 2.0", "https://square.github.io/okhttp/"),
            Library("Glide", "BSD, Part MIT and Apache 2.0", "https://github.com/bumptech/glide"),
            Library("Fetch", "Apache License 2.0", "https://github.com/tonyofrancis/Fetch"),
            Library("Gson", "Apache License 2.0", "https://github.com/google/gson"),
            Library("FlexboxLayout", "Apache License 2.0", "https://github.com/google/flexbox-layout"),
            Library("Coroutines", "Apache License 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
            Library("Activity KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("Fragment KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("Lifecycle KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("Navigation KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("WorkManager KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx")
        ).sortedBy { it.name }

        val adapter = LibrariesAdapter(libraries) { library ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(library.url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not open link", Toast.LENGTH_SHORT).show()
            }
        }

        binding.librariesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.librariesRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
