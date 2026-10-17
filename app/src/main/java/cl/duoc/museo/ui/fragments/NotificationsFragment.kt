package cl.duoc.museo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duoc.museo.R
import cl.duoc.museo.data.model.NotificationItem
import cl.duoc.museo.databinding.FragmentNotificationsBinding
import cl.duoc.museo.ui.adapters.NotificationsAdapter
import cl.duoc.museo.ui.viewmodels.NotificationsViewModel
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var notifAdapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
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
        viewModel.loadNotifications()
    }

    private fun setupRecyclerView() {
        notifAdapter = NotificationsAdapter(
            onNotificationClick = { notif ->
                viewModel.markAsRead(notif)
                handleNotificationDeepLink(notif)
            },
        )

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notifAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
    }

    private fun handleNotificationDeepLink(notif: NotificationItem) {
        when (notif.category.lowercase()) {
            "cursos" -> {
                findNavController().navigate(R.id.action_notificationsFragment_to_catalogFragment)
            }
            "museos" -> {
                val bundle = Bundle().apply {
                    putString("museumId", "mus_01")
                    putString("museumTitle", notif.message)
                }
                findNavController().navigate(R.id.action_notificationsFragment_to_museumDetailFragment, bundle)
            }
            "pedidos" -> {
                findNavController().navigate(R.id.action_notificationsFragment_to_historyFragment)
            }
            else -> {
                Toast.makeText(requireContext(), "Aviso: ${notif.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListenersAndChips() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSearch.setOnClickListener {
            Toast.makeText(requireContext(), "Buscando notificaciones", Toast.LENGTH_SHORT).show()
        }

        binding.chipGroupNotif.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val chipId = checkedIds.first()
            val category = when (chipId) {
                R.id.chipNotifCourses -> "Cursos"
                R.id.chipNotifMuseums -> "Museos"
                R.id.chipNotifOrders -> "Pedidos"
                else -> "Todas"
            }
            viewModel.setCategoryFilter(category)
        }

        binding.fabSendBroadcast.setOnClickListener {
            showAdminBroadcastDialog()
        }
    }

    private fun showAdminBroadcastDialog() {
        val inputView = EditText(requireContext()).apply {
            hint = "Título del aviso / broadcast"
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Simular Emisión FCM Push (Admin)")
            .setMessage("Crea una alerta de prueba en el sistema:")
            .setView(inputView)
            .setPositiveButton("Enviar Broadcast") { dialog, _ ->
                val titleText = inputView.text.toString().ifBlank { "Nuevo aviso broadcast FCM" }
                viewModel.sendAdminBroadcast(
                    title = titleText,
                    message = "Aviso emitido por la administración de Museo de los Museos.",
                    category = "Museos",
                )
                Toast.makeText(requireContext(), "¡Alerta emitida exitosamente!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.notificationsList.collect { list ->
                        notifAdapter.submitList(list)
                    }
                }

                launch {
                    viewModel.isFabVisible.collect { isVisible ->
                        binding.fabSendBroadcast.visibility = if (isVisible) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.isEmpty.collect { empty ->
                        binding.tvEmptyNotifs.visibility = if (empty) View.VISIBLE else View.GONE
                        binding.rvNotifications.visibility = if (empty) View.GONE else View.VISIBLE
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