package com.abhinav.otapulse.feature.otatools.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentOtaToolsBinding
import com.abhinav.otapulse.core.common.setHapticClickListener

class OtaToolsFragment : Fragment() {

    private var _binding: FragmentOtaToolsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtaToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCards()
        runEnterAnimation()
    }

    private fun setupCards() {
        binding.cardManualQuery.setHapticClickListener {
            openTool(ManualQueryFragment())
        }
        binding.cardPartitionExtraction.setHapticClickListener {
            openTool(PartitionExtractionFragment())
        }
        binding.cardLinkResolver.setHapticClickListener {
            openTool(LinkResolverFragment())
        }
        binding.cardArbChecker.setHapticClickListener {
            openTool(ArbCheckerFragment())
        }
    }

    private fun openTool(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                R.anim.nav_enter,
                R.anim.nav_exit,
                R.anim.nav_pop_enter,
                R.anim.nav_pop_exit
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun runEnterAnimation() {
        val viewsToAnimate = listOf(
            binding.tvBracketLabel,
            binding.tvHeaderTitle,
            binding.tvHeaderSubtitle,
            binding.cardHeroIcon,
            binding.cardManualQuery,
            binding.cardPartitionExtraction,
            binding.cardLinkResolver,
            binding.cardArbChecker
        )
        com.abhinav.otapulse.core.common.AnimationUtils.animateEntrance(viewsToAnimate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
