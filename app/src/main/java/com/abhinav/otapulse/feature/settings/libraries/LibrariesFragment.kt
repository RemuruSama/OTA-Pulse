package com.abhinav.otapulse.feature.settings.libraries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import com.abhinav.otapulse.core.common.openInAppBrowser
import com.abhinav.otapulse.databinding.FragmentLibrariesBinding

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

        val libraries = buildLibraries()

        val adapter = LibrariesAdapter(libraries) { library ->
            try {
                openInAppBrowser(library.url, library.name)
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

    private fun buildLibraries(): List<Library> = listOf(
        Library("Activity", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/activity"),
        Library("Activity KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
        Library("Apache Commons Compress", "Apache License 2.0", "https://commons.apache.org/proper/commons-compress/"),
        Library("AppCompat", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/appcompat"),
        Library("ConstraintLayout", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/constraintlayout"),
        Library("FlexboxLayout", "Apache License 2.0", "https://github.com/google/flexbox-layout"),
        Library("Fragment", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/fragment"),
        Library("Fragment KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
        Library("Glide", "BSD, MIT, and Apache License 2.0", "https://github.com/bumptech/glide"),
        Library("Gson", "Apache License 2.0", "https://github.com/google/gson"),
        Library("Hilt Android", "Apache License 2.0", "https://dagger.dev/hilt/"),
        Library("Hilt Work", "Apache License 2.0", "https://developer.android.com/training/dependency-injection/hilt-jetpack"),
        Library("JSON in Java", "The JSON License", "https://github.com/stleary/JSON-java"),
        Library("Core KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
        Library("Lifecycle LiveData KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
        Library("Lifecycle ViewModel KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
        Library("Markwon", "Apache License 2.0", "https://github.com/noties/Markwon"),
        Library("Material Components for Android", "Apache License 2.0", "https://github.com/material-components/material-components-android"),
        Library("Navigation Fragment KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
        Library("Navigation UI KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
        Library("OkHttp", "Apache License 2.0", "https://square.github.io/okhttp/"),
        Library("Protocol Buffers Java Lite", "BSD 3-Clause License", "https://github.com/protocolbuffers/protobuf"),
        Library("Protocol Buffers Kotlin Lite", "BSD 3-Clause License", "https://github.com/protocolbuffers/protobuf"),
        Library("RecyclerView", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/recyclerview"),
        Library("realme-ota", "Open Source", "https://github.com/R0rt1z2/realme-ota"),
        Library("WorkManager KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
        Library("XZ for Java", "Public Domain", "https://tukaani.org/xz/java.html"),
        Library("kotlinx-coroutines-android", "Apache License 2.0", "https://github.com/Kotlin/kotlinx.coroutines")
    ).sortedBy { it.name }
}
