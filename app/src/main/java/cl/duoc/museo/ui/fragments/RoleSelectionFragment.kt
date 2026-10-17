package cl.duoc.museo.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cl.duoc.museo.R
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.databinding.FragmentRoleSelectionBinding

class RoleSelectionFragment : Fragment() {

    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!

    private var selectedRole: Role = Role.CLIENT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRoleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        updateRoleCardsUi(Role.CLIENT)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cardRoleClient.setOnClickListener {
            selectedRole = Role.CLIENT
            updateRoleCardsUi(Role.CLIENT)
        }

        binding.cardRoleResearcher.setOnClickListener {
            selectedRole = Role.RESEARCHER
            updateRoleCardsUi(Role.RESEARCHER)
        }

        binding.cardRoleAdmin.setOnClickListener {
            selectedRole = Role.ADMINISTRATOR
            updateRoleCardsUi(Role.ADMINISTRATOR)
        }

        binding.btnRegisterRole.setOnClickListener {
            if (selectedRole != Role.CLIENT) {
                Toast.makeText(
                    requireContext(),
                    "El registro está disponible para perfiles de Cliente.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@setOnClickListener
            }
            val bundle = Bundle().apply {
                putString("selectedRole", selectedRole.name)
            }
            findNavController().navigate(R.id.action_roleSelectionFragment_to_registerFragment, bundle)
        }

        binding.btnLoginRole.setOnClickListener {
            val bundle = Bundle().apply {
                putString("selectedRole", selectedRole.name)
            }
            findNavController().navigate(R.id.action_roleSelectionFragment_to_loginFragment, bundle)
        }
    }

    private fun updateRoleCardsUi(role: Role) {
        val goldColor = ContextCompat.getColor(requireContext(), R.color.beige_gold)
        val dividerColor = ContextCompat.getColor(requireContext(), R.color.divider_color)
        val selectedBg = ContextCompat.getColor(requireContext(), R.color.card_selected_bg)
        val surfaceBg = ContextCompat.getColor(requireContext(), R.color.surface_card)
        val textSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Card Client
        val isClientSelected = role == Role.CLIENT
        binding.cardRoleClient.setStrokeColor(ColorStateList.valueOf(if (isClientSelected) goldColor else dividerColor))
        binding.cardRoleClient.strokeWidth = if (isClientSelected) 6 else 2
        binding.cardRoleClient.setCardBackgroundColor(ColorStateList.valueOf(if (isClientSelected) selectedBg else surfaceBg))
        binding.icArrowClient.imageTintList = ColorStateList.valueOf(if (isClientSelected) goldColor else textSecondary)

        // Card Researcher
        val isResearcherSelected = role == Role.RESEARCHER
        binding.cardRoleResearcher.setStrokeColor(ColorStateList.valueOf(if (isResearcherSelected) goldColor else dividerColor))
        binding.cardRoleResearcher.strokeWidth = if (isResearcherSelected) 6 else 2
        binding.cardRoleResearcher.setCardBackgroundColor(ColorStateList.valueOf(if (isResearcherSelected) selectedBg else surfaceBg))
        binding.icArrowResearcher.imageTintList = ColorStateList.valueOf(if (isResearcherSelected) goldColor else textSecondary)

        // Card Admin
        val isAdminSelected = role == Role.ADMINISTRATOR
        binding.cardRoleAdmin.setStrokeColor(ColorStateList.valueOf(if (isAdminSelected) goldColor else dividerColor))
        binding.cardRoleAdmin.strokeWidth = if (isAdminSelected) 6 else 2
        binding.cardRoleAdmin.setCardBackgroundColor(ColorStateList.valueOf(if (isAdminSelected) selectedBg else surfaceBg))
        binding.icArrowAdmin.imageTintList = ColorStateList.valueOf(if (isAdminSelected) goldColor else textSecondary)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}