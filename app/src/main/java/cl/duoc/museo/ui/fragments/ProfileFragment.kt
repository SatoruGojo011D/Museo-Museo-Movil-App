package cl.duoc.museo.ui.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cl.duoc.museo.R
import cl.duoc.museo.databinding.FragmentProfileBinding
import cl.duoc.museo.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserProfile()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.itemPersonalData.setOnClickListener {
            showPersonalDataDialog()
        }

        binding.itemShippingAddresses.setOnClickListener {
            showShippingAddressDialog()
        }

        binding.itemInterests.setOnClickListener {
            showInterestsDialog()
        }

        binding.itemNotificationSettings.setOnClickListener {
            val state = viewModel.uiState.value
            val selected = intArrayOf(if (state.notificationsEnabled) 0 else 1)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Notificaciones")
                .setSingleChoiceItems(arrayOf("Activadas", "Desactivadas"), selected[0]) { _, which ->
                    selected[0] = which
                }
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar") { _, _ ->
                    viewModel.setNotificationsEnabled(selected[0] == 0)
                }
                .show()
        }

        binding.itemAccessibility.setOnClickListener {
            val selected = intArrayOf(if (viewModel.uiState.value.largeTextEnabled) 1 else 0)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Accesibilidad")
                .setSingleChoiceItems(arrayOf("Tamaño estándar", "Texto grande"), selected[0]) { _, which ->
                    selected[0] = which
                }
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar") { _, _ ->
                    viewModel.setLargeTextEnabled(selected[0] == 1)
                }
                .show()
        }

        binding.itemLanguage.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Idioma")
                .setMessage("El idioma disponible en esta versión es Español.")
                .setPositiveButton("Usar Español") { _, _ ->
                    viewModel.setLanguage("Español")
                }
                .show()
        }

        binding.itemHelpSupport.setOnClickListener {
            Toast.makeText(requireContext(), "Ayuda y soporte - Museo de los Museos", Toast.LENGTH_SHORT).show()
        }

        binding.containerLogout.setOnClickListener {
            viewModel.logout()
        }

        binding.imgAvatar.setOnClickListener {
            viewModel.cycleAvatar()
        }
    }

    private fun showPersonalDataDialog() {
        val state = viewModel.uiState.value
        val nameInput = EditText(requireContext()).apply {
            hint = "Nombre completo"
            setSingleLine()
            setText(state.name)
        }
        val emailInput = EditText(requireContext()).apply {
            hint = "Correo electrónico"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine()
            setText(state.email)
        }
        val form = createDialogForm(nameInput, emailInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Datos personales")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                viewModel.updatePersonalData(nameInput.text.toString(), emailInput.text.toString())
            }
            .show()
    }

    private fun showShippingAddressDialog() {
        val addressInput = EditText(requireContext()).apply {
            hint = "Dirección de envío"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(viewModel.uiState.value.shippingAddress)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Dirección de envío")
            .setView(createDialogForm(addressInput))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                viewModel.updateShippingAddress(addressInput.text.toString())
            }
            .show()
    }

    private fun showInterestsDialog() {
        val options = arrayOf("Historia", "Arte", "Conservación", "Educación")
        val selected = options.map { it in viewModel.uiState.value.interests }.toBooleanArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mis intereses")
            .setMultiChoiceItems(options, selected) { _, which, isChecked ->
                selected[which] = isChecked
            }
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                viewModel.updateInterests(options.filterIndexed { index, _ -> selected[index] }.toSet())
            }
            .show()
    }

    private fun createDialogForm(vararg fields: EditText): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            fields.forEachIndexed { index, field ->
                if (index > 0) field.setPadding(field.paddingLeft, padding / 2, field.paddingRight, field.paddingBottom)
                addView(field)
            }
        }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.tvUserName.text = state.name
                        binding.tvUserEmail.text = state.email
                        binding.tvRoleBadge.text = state.role.displayName
                        binding.tvLanguageVal.text = state.language
                        binding.tvUserName.textSize = if (state.largeTextEnabled) 22f else 18f
                        binding.tvUserEmail.textSize = if (state.largeTextEnabled) 16f else 14f
                        binding.tvRoleBadge.textSize = if (state.largeTextEnabled) 14f else 12f

                        val clientVisibility = if (state.isClientOptionsVisible) View.VISIBLE else View.GONE
                        binding.itemShippingAddresses.visibility = clientVisibility
                        binding.dividerAddresses.visibility = clientVisibility
                        binding.itemInterests.visibility = clientVisibility
                        binding.dividerInterests.visibility = clientVisibility

                        val avatar = when (state.avatarRes) {
                            "avatar_2" -> R.drawable.ic_museum
                            "avatar_3" -> R.drawable.ic_catalog
                            "avatar_4" -> R.drawable.ic_settings
                            else -> R.drawable.ic_person
                        }
                        binding.imgAvatar.setImageResource(avatar)
                    }
                }

                launch {
                    viewModel.messageEvent.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }

                launch {
                    viewModel.logoutEvent.collect {
                        Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_profileFragment_to_welcomeFragment)
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