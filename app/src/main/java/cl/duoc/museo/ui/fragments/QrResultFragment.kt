package cl.duoc.museo.ui.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import cl.duoc.museo.R
import cl.duoc.museo.databinding.FragmentQrResultBinding
import cl.duoc.museo.ui.viewmodels.QrResultViewModel
import kotlinx.coroutines.launch

class QrResultFragment : Fragment() {

    private var _binding: FragmentQrResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QrResultViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentQrResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scannedCode = arguments?.getString("scannedCode")
        viewModel.resolveScannedCode(scannedCode)

        setupClickListeners()
        observeViewModel()
        setupEntranceAnimations()
    }

    private fun setupClickListeners() {
        // Navigation Back & Rescan Buttons
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnRescan.setOnClickListener {
            findNavController().popBackStack()
        }

        // Primary Detail Navigation
        binding.btnViewDetail.setOnClickListener {
            val currentState = viewModel.uiState.value
            if (!currentState.isValid) return@setOnClickListener
            currentState.externalUrl?.let { url ->
                openExternalUrl(url)
                return@setOnClickListener
            }
            val targetId = currentState.targetId

            if (currentState.isMuseum) {
                val bundle = Bundle().apply {
                    putString("museumId", targetId)
                    putString("museumTitle", currentState.title)
                }
                findNavController().navigate(
                    R.id.action_qrResultFragment_to_museumDetailFragment,
                    bundle,
                )
            } else {
                val bundle = Bundle().apply {
                    putString("productId", targetId)
                    putString("productName", currentState.title)
                }
                findNavController().navigate(
                    R.id.action_qrResultFragment_to_productDetailFragment,
                    bundle,
                )
            }
        }

        binding.tvVideoUrl.setOnClickListener {
            viewModel.uiState.value.externalUrl?.let(::openExternalUrl)
        }
    }

    private fun openExternalUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                getString(R.string.qr_result_no_browser),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isValid) {
                        binding.layoutSuccessState.visibility = View.VISIBLE
                        binding.layoutErrorGroup.visibility = View.GONE
                        binding.layoutActionButtons.visibility = View.VISIBLE

                        binding.tvItemTitle.text = state.title
                        binding.tvCategoryBadge.text = state.type
                        binding.tvAuthorLabel.text = state.author
                        binding.tvItemDescription.text = state.description
                        binding.tvVideoUrl.visibility = if (state.externalUrl.isNullOrBlank()) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                        binding.tvVideoUrl.text = state.externalUrl.orEmpty()
                        binding.btnViewDetail.setText(
                            if (state.externalUrl.isNullOrBlank()) {
                                R.string.btn_view_detail
                            } else {
                                R.string.qr_result_open_video
                            },
                        )
                    } else {
                        binding.layoutSuccessState.visibility = View.GONE
                        binding.layoutErrorGroup.visibility = View.VISIBLE

                        if (state.errorMessage.isNotBlank()) {
                            binding.tvErrorMessage.text = state.errorMessage
                        }
                    }
                }
            }
        }
    }

    private fun setupEntranceAnimations() {
        // Entrance animation: Fade-in and scale-up for checkmark icon and result preview card
        val viewsToAnimate = listOf(
            binding.vSuccessIconContainer,
            binding.cardResult,
        )

        val animators = viewsToAnimate.mapIndexed { index, targetView ->
            targetView.alpha = 0f
            targetView.scaleX = 0.85f
            targetView.scaleY = 0.85f

            val alphaAnim = ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f)
            val scaleXAnim = ObjectAnimator.ofFloat(targetView, View.SCALE_X, 0.85f, 1f)
            val scaleYAnim = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 0.85f, 1f)

            AnimatorSet().apply {
                playTogether(alphaAnim, scaleXAnim, scaleYAnim)
                duration = 600
                startDelay = (index * 120).toLong()
                interpolator = DecelerateInterpolator()
            }
        }

        AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}