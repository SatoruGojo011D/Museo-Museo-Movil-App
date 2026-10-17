package cl.duoc.museo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import cl.duoc.museo.R
import cl.duoc.museo.data.model.MuseumMapItem
import cl.duoc.museo.databinding.FragmentVirtualMuseumsMapBinding
import cl.duoc.museo.ui.viewmodels.MapViewModel
import cl.duoc.museo.ui.views.IberoamericaMapView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

open class VirtualMuseumsMapFragment : Fragment() {

    private var _binding: FragmentVirtualMuseumsMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()
    private var mapView: IberoamericaMapView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVirtualMuseumsMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMapFragment()
        setupFilterChips()
        setupCardClickListener()
        observeViewModel()
    }

    private fun setupMapFragment() {
        mapView = IberoamericaMapView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            onMuseumSelected = { museumId ->
                viewModel.museums.value.firstOrNull { it.id == museumId }?.let(viewModel::selectMuseum)
            }
        }
        binding.mapContainer.addView(mapView)
        renderMarkers(viewModel.museums.value)
    }

    private fun setupFilterChips() {
        binding.chipTodos.setOnClickListener {
            viewModel.applyFilter("Todos")
        }
        binding.chipCountry.setOnClickListener {
            val countries = viewModel.museums.value
                .map { it.country }
                .distinct()
                .sorted()
            if (countries.isEmpty()) {
                Toast.makeText(requireContext(), "No hay países para filtrar.", Toast.LENGTH_SHORT).show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Filtrar por país")
                    .setItems(countries.toTypedArray()) { _, which ->
                        viewModel.applyFilter("country:${countries[which]}")
                    }
                    .show()
            }
        }
        binding.chipProject.setOnClickListener {
            val projects = viewModel.museums.value.map { it.category }.distinct().sorted()
            if (projects.isEmpty()) {
                Toast.makeText(requireContext(), "No hay proyectos para filtrar.", Toast.LENGTH_SHORT).show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Filtrar por proyecto")
                    .setItems(projects.toTypedArray()) { _, which ->
                        viewModel.applyFilter("project:${projects[which]}")
                    }
                    .show()
            }
        }
    }

    private fun setupCardClickListener() {
        val navigateToDetail = {
            val selected = viewModel.selectedMuseum.value
            if (selected != null) {
                val bundle = Bundle().apply {
                    putString("museumId", selected.id)
                    putString("museumTitle", selected.title)
                }
                findNavController().navigate(R.id.action_mapFragment_to_museumDetailFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "Selecciona un museo en el mapa", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardMuseumInfo.setOnClickListener { navigateToDetail() }
        binding.btnMuseumDetail.setOnClickListener { navigateToDetail() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.museums.collect { museumList ->
                        renderMarkers(museumList)
                    }
                }

                launch {
                    viewModel.selectedMuseum.collect { museum ->
                        if (museum != null) {
                            updateFloatingCardUi(museum)
                            binding.cardMuseumInfo.visibility = View.VISIBLE
                        } else {
                            binding.cardMuseumInfo.visibility = View.GONE
                        }
                        renderMarkers(viewModel.museums.value)
                    }
                }
            }
        }
    }

    private fun renderMarkers(museumList: List<MuseumMapItem>) {
        mapView?.setMuseums(museumList, viewModel.selectedMuseum.value?.id)
    }

    private fun updateFloatingCardUi(museum: MuseumMapItem) {
        binding.tvMuseumTitle.text = museum.title
        binding.tvMuseumLocation.text = museum.location
        binding.tvTag1.text = museum.tag1
        binding.tvTag2.text = museum.tag2
        binding.tvRatingScore.text = museum.rating.toString()
        binding.tvRatingCount.text = getString(R.string.map_reviews_format, museum.reviewsCount)
    }

    override fun onDestroyView() {
        mapView?.onMuseumSelected = null
        mapView = null
        super.onDestroyView()
        _binding = null
    }
}