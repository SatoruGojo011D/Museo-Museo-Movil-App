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
import cl.duoc.museo.R
import cl.duoc.museo.databinding.FragmentRegisterBinding
import cl.duoc.museo.ui.viewmodels.RegisterViewModel
import cl.duoc.museo.ui.viewmodels.RegistrationEvent
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.avatar1.setOnClickListener { viewModel.selectAvatar("avatar_1") }
        binding.avatar2.setOnClickListener { viewModel.selectAvatar("avatar_2") }
        binding.avatar3.setOnClickListener { viewModel.selectAvatar("avatar_3") }
        binding.avatar4.setOnClickListener { viewModel.selectAvatar("avatar_4") }

        binding.imgAvatarPreview.setOnClickListener {
            Toast.makeText(requireContext(), "Selecciona una opción de avatar abajo", Toast.LENGTH_SHORT).show()
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text?.toString().orEmpty()
            val email = binding.etEmail.text?.toString().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()

            viewModel.register(name, email, password)
        }

        binding.tvToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.formState.collect { state ->
                        binding.tilName.error = state.nameError
                        binding.tilEmail.error = state.emailError
                        binding.tilPassword.error = state.passwordError
                        binding.btnRegister.isEnabled = !state.isLoading
                    }
                }

                launch {
                    viewModel.selectedAvatar.collect { avatarId ->
                        updateAvatarSelectionUi(avatarId)
                    }
                }

                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is RegistrationEvent.Success -> {
                                Toast.makeText(
                                    requireContext(),
                                    "¡Cuenta creada con éxito! Bienvenido/a.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                findNavController().navigate(R.id.action_registerFragment_to_dashboardFragment)
                            }
                            is RegistrationEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateAvatarSelectionUi(selectedId: String) {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.beige_gold)
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.divider_color)

        val avatarMap = mapOf(
            "avatar_1" to Pair(binding.avatar1, R.drawable.ic_person),
            "avatar_2" to Pair(binding.avatar2, R.drawable.ic_museum),
            "avatar_3" to Pair(binding.avatar3, R.drawable.ic_catalog),
            "avatar_4" to Pair(binding.avatar4, R.drawable.ic_settings),
        )

        avatarMap.forEach { (id, pair) ->
            val (view, drawableRes) = pair
            val isSelected = id == selectedId
            view.strokeColor = ColorStateList.valueOf(if (isSelected) selectedColor else defaultColor)
            view.strokeWidth = if (isSelected) 8f else 2f

            if (isSelected) {
                binding.imgAvatarPreview.setImageResource(drawableRes)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}