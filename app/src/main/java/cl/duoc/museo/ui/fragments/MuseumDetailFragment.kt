package cl.duoc.museo.ui.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import cl.duoc.museo.R
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.databinding.FragmentMuseumDetailBinding
import cl.duoc.museo.ui.viewmodels.MuseumDetailViewModel
import kotlinx.coroutines.launch

class MuseumDetailFragment : Fragment() {

    private var _binding: FragmentMuseumDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MuseumDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMuseumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val museumId = arguments?.getString("museumId")
        viewModel.init(museumId)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Navigation Back Button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Primary Action: External Browser Intent for 3D Virtual Tour
        binding.btnVirtualTour.setOnClickListener {
            val url = viewModel.uiState.value.museum?.projectUrl
                ?: "https://museocolonial.gov.co/tour-virtual"

            try {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No se pudo abrir el enlace virtual: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        // Favorite Toggles
        binding.btnFavoriteHeader.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.btnFavoriteToggle.setOnClickListener {
            viewModel.toggleFavorite()
        }

        // Researcher Role Extension Action
        binding.btnEditRecord.setOnClickListener {
            Toast.makeText(requireContext(), "Modo Investigador: Abriendo editor de ficha técnica...", Toast.LENGTH_LONG).show()
        }

        // Admin Role Extension Actions
        binding.btnApproveRecord.setOnClickListener {
            viewModel.approvePublication()
            Toast.makeText(requireContext(), "Ficha de museo aprobada y verificada.", Toast.LENGTH_SHORT).show()
        }

        binding.btnRejectRecord.setOnClickListener {
            viewModel.rejectPublication()
            Toast.makeText(requireContext(), "Ficha marcada para revisión.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val museum = state.museum ?: return@collect

                    // Populate UI
                    binding.tvMuseumTitle.text = museum.name
                    binding.tvLocation.text = getString(R.string.museum_location_format, museum.country)
                    binding.tvDescription.text = museum.description
                    binding.tvPublicationStatus.text = state.publicationStatus

                    // Role-based Control Visibilities
                    when (state.userRole) {
                        Role.CLIENT -> {
                            binding.btnEditRecord.visibility = View.GONE
                            binding.cardPublicationState.visibility = View.GONE
                        }
                        Role.RESEARCHER -> {
                            binding.btnEditRecord.visibility = View.VISIBLE
                            binding.cardPublicationState.visibility = View.GONE
                        }
                        Role.ADMINISTRATOR -> {
                            binding.btnEditRecord.visibility = View.GONE
                            binding.cardPublicationState.visibility = View.VISIBLE
                        }
                    }

                    // Update Favorite States on Header Icon & Bottom Button
                    updateFavoriteStateUi(state.isFavorite)
                }
            }
        }
    }

    private fun updateFavoriteStateUi(isFavorite: Boolean) {
        val redColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val textPrimaryColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        if (isFavorite) {
            binding.btnFavoriteHeader.setImageResource(R.drawable.ic_favorite)
            binding.btnFavoriteHeader.imageTintList = ColorStateList.valueOf(redColor)

            binding.btnFavoriteToggle.setText(R.string.btn_remove_favorites)
            binding.btnFavoriteToggle.setIconResource(R.drawable.ic_favorite)
            binding.btnFavoriteToggle.iconTint = ColorStateList.valueOf(redColor)
        } else {
            binding.btnFavoriteHeader.setImageResource(R.drawable.ic_favorite_border)
            binding.btnFavoriteHeader.imageTintList = ColorStateList.valueOf(textPrimaryColor)

            binding.btnFavoriteToggle.setText(R.string.btn_save_favorites)
            binding.btnFavoriteToggle.setIconResource(R.drawable.ic_favorite_border)
            binding.btnFavoriteToggle.iconTint = ColorStateList.valueOf(textPrimaryColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}