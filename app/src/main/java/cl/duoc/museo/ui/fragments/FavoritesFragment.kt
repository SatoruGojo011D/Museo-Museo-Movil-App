package cl.duoc.museo.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duoc.museo.R
import cl.duoc.museo.databinding.FragmentFavoritesBinding
import cl.duoc.museo.ui.adapters.FavoritesAdapter
import cl.duoc.museo.ui.viewmodels.FavoritesViewModel
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserFavorites()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            onItemClick = { favorite ->
                val isResource = favorite.classification.contains("Recurso", ignoreCase = true) ||
                    favorite.classification.contains("Libro", ignoreCase = true) ||
                    favorite.classification.contains("Curso", ignoreCase = true) ||
                    favorite.classification.contains("Material", ignoreCase = true)

                val bundle = Bundle().apply {
                    putString(if (isResource) "productId" else "museumId", favorite.museumId)
                    putString(if (isResource) "productName" else "museumTitle", favorite.museumName)
                }

                val actionId = if (isResource) {
                    R.id.action_favoritesFragment_to_productDetailFragment
                } else {
                    R.id.action_favoritesFragment_to_museumDetailFragment
                }

                findNavController().navigate(actionId, bundle)
            },
            onRemoveClick = { favorite ->
                viewModel.removeFavorite(favorite)
                Toast.makeText(
                    requireContext(),
                    "Eliminado de tus favoritos",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )

        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoritesAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnToggleMuseums.setOnClickListener {
            viewModel.setFilter("MUSEOS")
        }

        binding.btnToggleResources.setOnClickListener {
            viewModel.setFilter("RECURSOS")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favoritesList.collect { list ->
                        favoritesAdapter.submitList(list)
                    }
                }

                launch {
                    viewModel.isEmpty.collect { empty ->
                        binding.tvEmptyState.visibility = if (empty) View.VISIBLE else View.GONE
                        binding.rvFavorites.visibility = if (empty) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.activeFilter.collect { activeFilter ->
                        updateToggleUi(activeFilter)
                    }
                }

                launch {
                    viewModel.museumsCount.collect { count ->
                        binding.btnToggleMuseums.text = "Museos ($count)"
                    }
                }

                launch {
                    viewModel.resourcesCount.collect { count ->
                        binding.btnToggleResources.text = "Recursos ($count)"
                    }
                }
            }
        }
    }

    private fun updateToggleUi(activeFilter: String) {
        val darkBlueColor = ContextCompat.getColor(requireContext(), R.color.dark_blue)
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val textPrimaryColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val transparentColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)

        val isMuseums = activeFilter == "MUSEOS"

        if (isMuseums) {
            binding.btnToggleMuseums.backgroundTintList = ColorStateList.valueOf(darkBlueColor)
            binding.btnToggleMuseums.setTextColor(whiteColor)

            binding.btnToggleResources.backgroundTintList = ColorStateList.valueOf(transparentColor)
            binding.btnToggleResources.setTextColor(textPrimaryColor)
        } else {
            binding.btnToggleResources.backgroundTintList = ColorStateList.valueOf(darkBlueColor)
            binding.btnToggleResources.setTextColor(whiteColor)

            binding.btnToggleMuseums.backgroundTintList = ColorStateList.valueOf(transparentColor)
            binding.btnToggleMuseums.setTextColor(textPrimaryColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}