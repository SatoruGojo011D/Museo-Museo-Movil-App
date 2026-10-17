package cl.duoc.museo.ui.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cl.duoc.museo.R
import cl.duoc.museo.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEntranceAnimations()
        setupClickListeners()
    }

    private fun setupEntranceAnimations() {
        val animatableViews = listOf(
            binding.imgLogo,
            binding.tvTitle,
            binding.tvSubtitle,
            binding.btnStart,
            binding.btnLogin,
        )

        val animators = animatableViews.mapIndexed { index, targetView ->
            targetView.alpha = 0f
            targetView.translationY = 50f

            val alphaAnimator = ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f)
            val translationAnimator = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_Y, 50f, 0f)

            AnimatorSet().apply {
                playTogether(alphaAnimator, translationAnimator)
                duration = 800
                startDelay = (index * 120).toLong()
                interpolator = DecelerateInterpolator()
            }
        }

        AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    private fun setupClickListeners() {
        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_roleSelectionFragment)
        }

        binding.btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}