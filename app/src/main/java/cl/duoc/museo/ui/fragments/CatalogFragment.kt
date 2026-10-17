package cl.duoc.museo.ui.fragments

import android.os.Bundle
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cl.duoc.museo.R
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.databinding.FragmentCatalogBinding
import cl.duoc.museo.ui.adapters.CatalogAdapter
import cl.duoc.museo.ui.viewmodels.CatalogViewModel
import cl.duoc.museo.ui.viewmodels.CatalogEvent
import kotlinx.coroutines.launch

class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CatalogViewModel by viewModels()
    private lateinit var catalogAdapter: CatalogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupCategoryChips()
        setupFabAndFilterButtons()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCatalogData()
    }

    private fun setupRecyclerView() {
        catalogAdapter = CatalogAdapter(
            onItemClick = { item ->
                val role = viewModel.userRole.value
                if (role == Role.ADMINISTRATOR) {
                    showAdminItemOptions(item)
                } else {
                    val bundle = Bundle().apply {
                        putString("productId", item.id)
                        putString("productName", item.title)
                    }
                    findNavController().navigate(R.id.action_catalogFragment_to_productDetailFragment, bundle)
                }
            },
            onAddToCartClick = { item ->
                viewModel.addToCart(item) {
                    Toast.makeText(
                        requireContext(),
                        "¡'${item.title}' agregado al carrito!",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            isAdministrator = { viewModel.userRole.value == Role.ADMINISTRATOR },
        )

        binding.rvCatalog.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = catalogAdapter
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCategoryChips() {
        binding.chipCatAll.setOnClickListener {
            viewModel.selectCategory("Todos")
        }
        binding.chipCatCourses.setOnClickListener {
            viewModel.selectCategory("Cursos")
        }
        binding.chipCatBooks.setOnClickListener {
            viewModel.selectCategory("Libros")
        }
        binding.chipCatMaterials.setOnClickListener {
            viewModel.selectCategory("Materiales")
        }
    }

    private fun setupFabAndFilterButtons() {
        binding.btnFilterOptions.setOnClickListener {
            val sortOptions = arrayOf(
                "Recomendados",
                "Precio: menor a mayor",
                "Precio: mayor a menor",
                "Nombre: A-Z",
            )
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ordenar catálogo")
                .setItems(sortOptions) { _, which ->
                    viewModel.setSortOption(sortOptions[which])
                }
                .show()
        }

        binding.fabAddProduct.setOnClickListener {
            showCatalogItemEditor(null)
        }
    }

    private fun showAdminItemOptions(item: cl.duoc.museo.data.model.CatalogItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.title)
            .setItems(arrayOf("Modificar", "Desactivar")) { _, which ->
                if (which == 0) {
                    showCatalogItemEditor(item)
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Desactivar elemento")
                        .setMessage("¿Quieres quitar “${item.title}” del catálogo?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Desactivar") { _, _ ->
                            viewModel.deactivateCatalogItem(item)
                        }
                        .show()
                }
            }
            .show()
    }

    private fun showCatalogItemEditor(item: cl.duoc.museo.data.model.CatalogItem?) {
        val titleInput = EditText(requireContext()).apply {
            hint = "Nombre"
            setSingleLine()
            setText(item?.title.orEmpty())
        }
        val categories = arrayOf("Cursos", "Libros", "Materiales")
        val categoryInput = AutoCompleteTextView(requireContext()).apply {
            hint = "Categoría"
            setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))
            setText(item?.category ?: categories.first(), false)
        }
        val priceInput = EditText(requireContext()).apply {
            hint = "Precio en US$"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(item?.price?.toString().orEmpty())
        }
        val descriptionInput = EditText(requireContext()).apply {
            hint = "Descripción"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            setText(item?.description.orEmpty())
        }
        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(titleInput)
            addView(categoryInput)
            addView(priceInput)
            addView(descriptionInput)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (item == null) "Agregar al catálogo" else "Modificar elemento")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                viewModel.saveCatalogItem(
                    existingItem = item,
                    titleInput = titleInput.text.toString(),
                    category = categoryInput.text.toString(),
                    priceInput = priceInput.text.toString(),
                    descriptionInput = descriptionInput.text.toString(),
                )
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.catalogItems.collect { itemList ->
                        catalogAdapter.submitList(itemList)
                    }
                }

                launch {
                    viewModel.isFabVisible.collect { isVisible ->
                        binding.fabAddProduct.visibility = if (isVisible) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is CatalogEvent.Message ->
                                Toast.makeText(requireContext(), event.text, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}