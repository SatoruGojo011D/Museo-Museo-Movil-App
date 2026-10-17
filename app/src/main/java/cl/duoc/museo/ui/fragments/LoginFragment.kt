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
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.databinding.FragmentLoginBinding
import cl.duoc.museo.ui.viewmodels.LoginEvent
import cl.duoc.museo.ui.viewmodels.LoginViewModel
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if role argument was passed from RoleSelectionFragment
        arguments?.getString("selectedRole")?.let { roleStr ->
            try {
                val role = Role.valueOf(roleStr)
                viewModel.selectRole(role)
            } catch (e: Exception) {
                // Ignore fallback to default
            }
        }

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cardRoleAdmin.setOnClickListener {
            viewModel.selectRole(Role.ADMINISTRATOR)
        }

        binding.cardRoleResearcher.setOnClickListener {
            viewModel.selectRole(Role.RESEARCHER)
        }

        binding.cardRoleClient.setOnClickListener {
            viewModel.selectRole(Role.CLIENT)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            viewModel.login(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty()
            viewModel.requestPasswordReset(email)
        }

        binding.tvToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedRole.collect { role ->
                        updateRoleCardsUi(role)
                        populateTestCredentials(role)
                    }
                }

                launch {
                    viewModel.formState.collect { state ->
                        binding.tilEmail.error = state.emailError
                        binding.tilPassword.error = state.passwordError
                        binding.btnLogin.isEnabled = !state.isLoading
                    }
                }

                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is LoginEvent.Success -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Bienvenido/a (${viewModel.selectedRole.value.displayName})",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                            }
                            is LoginEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                            is LoginEvent.Info -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateRoleCardsUi(selectedRole: Role) {
        val goldColor = ContextCompat.getColor(requireContext(), R.color.beige_gold)
        val dividerColor = ContextCompat.getColor(requireContext(), R.color.divider_color)
        val selectedBg = ContextCompat.getColor(requireContext(), R.color.card_selected_bg)
        val surfaceBg = ContextCompat.getColor(requireContext(), R.color.surface_card)
        val textSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Card Admin
        val isAdminSelected = selectedRole == Role.ADMINISTRATOR
        binding.cardRoleAdmin.setStrokeColor(ColorStateList.valueOf(if (isAdminSelected) goldColor else dividerColor))
        binding.cardRoleAdmin.strokeWidth = if (isAdminSelected) 6 else 2
        binding.cardRoleAdmin.setCardBackgroundColor(ColorStateList.valueOf(if (isAdminSelected) selectedBg else surfaceBg))
        binding.icArrowAdmin.imageTintList = ColorStateList.valueOf(if (isAdminSelected) goldColor else textSecondary)

        // Card Researcher
        val isResearcherSelected = selectedRole == Role.RESEARCHER
        binding.cardRoleResearcher.setStrokeColor(ColorStateList.valueOf(if (isResearcherSelected) goldColor else dividerColor))
        binding.cardRoleResearcher.strokeWidth = if (isResearcherSelected) 6 else 2
        binding.cardRoleResearcher.setCardBackgroundColor(ColorStateList.valueOf(if (isResearcherSelected) selectedBg else surfaceBg))
        binding.icArrowResearcher.imageTintList = ColorStateList.valueOf(if (isResearcherSelected) goldColor else textSecondary)

        // Card Client
        val isClientSelected = selectedRole == Role.CLIENT
        binding.cardRoleClient.setStrokeColor(ColorStateList.valueOf(if (isClientSelected) goldColor else dividerColor))
        binding.cardRoleClient.strokeWidth = if (isClientSelected) 6 else 2
        binding.cardRoleClient.setCardBackgroundColor(ColorStateList.valueOf(if (isClientSelected) selectedBg else surfaceBg))
        binding.icArrowClient.imageTintList = ColorStateList.valueOf(if (isClientSelected) goldColor else textSecondary)
    }

    private fun populateTestCredentials(role: Role) {
        val (testEmail, testPass) = viewModel.getTestCredentialsForRole(role)
        val currentEmail = binding.etEmail.text?.toString().orEmpty()

        // Auto populate if empty or matches any test email format
        if (currentEmail.isEmpty() || currentEmail.endsWith("@museo.org")) {
            binding.etEmail.setText(testEmail)
            binding.etPassword.setText(testPass)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}