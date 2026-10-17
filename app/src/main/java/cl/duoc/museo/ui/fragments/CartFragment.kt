package cl.duoc.museo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duoc.museo.R
import cl.duoc.museo.databinding.FragmentCartBinding
import cl.duoc.museo.ui.adapters.CartAdapter
import cl.duoc.museo.ui.viewmodels.CartViewModel
import cl.duoc.museo.ui.viewmodels.CheckoutEvent
import kotlinx.coroutines.launch

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CartViewModel by viewModels()
    private lateinit var cartAdapter: CartAdapter

    private val countries = arrayOf("Chile", "Colombia", "México", "Perú", "España", "Argentina")
    private val shippingTypesNational = arrayOf("Estándar (7-14 días) US$ 12", "Expreso (3-5 días) US$ 18")
    private val shippingTypesInternational = arrayOf("Estándar (10-20 días) US$ 25", "Expreso (5-8 días) US$ 38")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupDropdowns()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserCart()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onIncrementClick = { item -> viewModel.incrementQuantity(item) },
            onDecrementClick = { item -> viewModel.decrementQuantity(item) },
            onDeleteClick = { item -> viewModel.deleteItem(item) },
        )

        binding.rvCart.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun setupDropdowns() {
        // Country Dropdown Setup
        val countryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            countries,
        )
        binding.actvCountry.setAdapter(countryAdapter)
        binding.actvCountry.setText(viewModel.selectedCountry.value, false)

        binding.actvCountry.setOnItemClickListener { _, _, position, _ ->
            val selected = countries[position]
            viewModel.selectCountry(selected)
            updateShippingDropdown(selected)
        }

        updateShippingDropdown(viewModel.selectedCountry.value)
    }

    private fun updateShippingDropdown(country: String) {
        val options = if (country.equals("Chile", ignoreCase = true)) {
            shippingTypesNational
        } else {
            shippingTypesInternational
        }

        val shippingAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            options,
        )
        binding.actvShippingType.setAdapter(shippingAdapter)
        val defaultSelected = options[0]
        binding.actvShippingType.setText(defaultSelected, false)
        viewModel.selectShippingType(defaultSelected)

        binding.actvShippingType.setOnItemClickListener { _, _, position, _ ->
            val selected = options[position]
            viewModel.selectShippingType(selected)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCheckout.setOnClickListener {
            viewModel.checkout()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cartItems.collect { items ->
                        cartAdapter.submitList(items)
                        val isEmpty = items.isEmpty()
                        binding.tvEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
                        binding.rvCart.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        binding.btnCheckout.isEnabled = !isEmpty
                    }
                }

                launch {
                    viewModel.subtotal.collect { sub ->
                        binding.tvSubtotalValue.text = "US$ " + sub.toInt().toString()
                    }
                }

                launch {
                    viewModel.shippingCost.collect { ship ->
                        binding.tvShippingValue.text = "US$ " + ship.toInt().toString()
                    }
                }

                launch {
                    viewModel.totalAmount.collect { total ->
                        binding.tvTotalValue.text = "US$ " + total.toInt().toString()
                    }
                }

                launch {
                    viewModel.checkoutEvent.collect { event ->
                        when (event) {
                            is CheckoutEvent.Success -> {
                                Toast.makeText(
                                    requireContext(),
                                    "¡Pago procesado con éxito! Total: US$ ${event.orderTotal.toInt()}",
                                    Toast.LENGTH_LONG,
                                ).show()
                                findNavController().navigate(R.id.action_cartFragment_to_historyFragment)
                            }
                            is CheckoutEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
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