package cl.duoc.museo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duoc.museo.R
import cl.duoc.museo.data.model.Order
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.databinding.FragmentOrderHistoryBinding
import cl.duoc.museo.ui.adapters.OrderHistoryAdapter
import cl.duoc.museo.ui.viewmodels.OrderHistoryViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class OrderHistoryFragment : Fragment() {

    private var _binding: FragmentOrderHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderHistoryViewModel by viewModels()
    private lateinit var orderAdapter: OrderHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListenersAndChips()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderHistoryAdapter(
            onOrderClick = { order ->
                val role = viewModel.userRole.value
                if (role == Role.ADMINISTRATOR) {
                    showAdminStatusUpdateDialog(order)
                } else {
                    val formattedId = "#2024-${order.id.toString().padStart(5, '0')}"
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Solicitud $formattedId")
                        .setMessage(
                            getString(
                                R.string.order_history_detail_format,
                                order.date,
                                order.status,
                                order.productCount,
                                order.country,
                                order.shippingType,
                                String.format(java.util.Locale.ROOT, "%.2f", order.totalAmount),
                            ),
                        )
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            },
        )

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
    }

    private fun setupClickListenersAndChips() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSearch.setOnClickListener {
            Toast.makeText(requireContext(), "Búsqueda de solicitudes", Toast.LENGTH_SHORT).show()
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val chipId = checkedIds.first()
            val filter = when (chipId) {
                R.id.chipPending -> "Pendientes"
                R.id.chipSent -> "Enviadas"
                R.id.chipDelivered -> "Entregadas"
                else -> "Todas"
            }
            viewModel.setStatusFilter(filter)
        }
    }

    private fun showAdminStatusUpdateDialog(order: Order) {
        val statuses = arrayOf("Pendiente", "Confirmado", "Enviado", "Entregado")
        val formattedId = "#2024-${order.id.toString().padStart(5, '0')}"

        AlertDialog.Builder(requireContext())
            .setTitle("Actualizar Estado ($formattedId)")
            .setItems(statuses) { dialog, which ->
                val newStatus = statuses[which]
                viewModel.updateOrderStatus(order, newStatus)
                Toast.makeText(
                    requireContext(),
                    "Estado actualizado a '$newStatus' exitosamente.",
                    Toast.LENGTH_SHORT,
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.ordersList.collect { list ->
                        orderAdapter.submitList(list)
                    }
                }

                launch {
                    viewModel.isEmpty.collect { empty ->
                        binding.tvEmptyOrders.visibility = if (empty) View.VISIBLE else View.GONE
                        binding.rvOrders.visibility = if (empty) View.GONE else View.VISIBLE
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