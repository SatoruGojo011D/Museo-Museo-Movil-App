package cl.duoc.museo.ui.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import java.io.File
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import cl.duoc.museo.R
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.databinding.FragmentDashboardBinding
import cl.duoc.museo.ui.viewmodels.DashboardViewModel
import cl.duoc.museo.ui.viewmodels.ProfileViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
        setupEntranceAnimations()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserData()
    }

    private fun setupClickListeners() {
        // Quick Access Row Navigation
        binding.btnQuickMap.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_mapFragment)
        }

        binding.btnQuickCatalog.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_catalogFragment)
        }

        binding.btnQuickQr.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_qrScannerFragment)
        }

        binding.btnQuickFavorites.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_favoritesFragment)
        }

        binding.btnQuickHistory.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_historyFragment)
        }

        // Header Actions
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_notificationsFragment)
        }

        binding.imgAvatar.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_profileFragment)
        }

        // Explora Cards Navigation
        binding.cardCursos.setOnClickListener {
            when (viewModel.uiState.value.role) {
                Role.ADMINISTRATOR -> findNavController().navigate(R.id.action_dashboardFragment_to_historyFragment)
                Role.RESEARCHER -> findNavController().navigate(R.id.action_dashboardFragment_to_mapFragment)
                Role.CLIENT -> findNavController().navigate(R.id.action_dashboardFragment_to_catalogFragment)
            }
        }

        binding.cardProductos.setOnClickListener {
            if (viewModel.uiState.value.role == Role.RESEARCHER) {
                findNavController().navigate(R.id.action_dashboardFragment_to_mapFragment)
            } else {
                findNavController().navigate(R.id.action_dashboardFragment_to_catalogFragment)
            }
        }

        binding.cardMuseosVirtuales.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_mapFragment)
        }

        binding.cardNovedades.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_notificationsFragment)
        }

        // Destacados Section Navigation
        binding.btnVerTodos.setOnClickListener {
            navigateToRoleContent()
        }

        binding.cardFeatured.setOnClickListener {
            navigateToRoleContent()
        }
    }

    private fun navigateToRoleContent() {
        val destination = if (viewModel.uiState.value.role == Role.RESEARCHER) {
            R.id.action_dashboardFragment_to_mapFragment
        } else {
            R.id.action_dashboardFragment_to_catalogFragment
        }
        findNavController().navigate(destination)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvGreeting.text = state.greetingText
                    binding.tvSubtitle.text = state.subtitleText
                    binding.vNotificationBadge.visibility =
                        if (state.hasUnreadNotifications) View.VISIBLE else View.GONE
                    applyRoleLayout(state.role)

                    if (state.avatarResId.startsWith(ProfileViewModel.PROFILE_PHOTO_PREFIX)) {
                        val photoPath = state.avatarResId.removePrefix(ProfileViewModel.PROFILE_PHOTO_PREFIX)
                        Glide.with(binding.imgAvatar)
                            .load(File(photoPath))
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .centerCrop()
                            .into(binding.imgAvatar)
                    } else {
                        val avatarDrawable = when (state.avatarResId) {
                            "avatar_2" -> R.drawable.ic_museum
                            "avatar_3" -> R.drawable.ic_catalog
                            "avatar_4" -> R.drawable.ic_settings
                            else -> R.drawable.ic_person
                        }
                        binding.imgAvatar.setImageResource(avatarDrawable)
                    }
                }
            }
        }
    }

    private fun applyRoleLayout(role: Role) {
        val isClient = role == Role.CLIENT
        val isResearcher = role == Role.RESEARCHER
        val isAdministrator = role == Role.ADMINISTRATOR

        binding.btnQuickQr.visibility = if (isAdministrator) View.GONE else View.VISIBLE
        binding.btnQuickFavorites.visibility = if (isClient) View.VISIBLE else View.GONE
        binding.btnQuickHistory.visibility = if (isResearcher) View.GONE else View.VISIBLE
        binding.btnQuickCatalog.visibility = if (isResearcher) View.GONE else View.VISIBLE

        binding.tvExploraTitle.text = when (role) {
            Role.CLIENT -> getString(R.string.dashboard_explora_title)
            Role.RESEARCHER -> "Investigación"
            Role.ADMINISTRATOR -> "Gestión"
        }
        binding.tvCardCoursesTitle.text = when (role) {
            Role.CLIENT -> getString(R.string.dashboard_card_courses_title)
            Role.RESEARCHER -> "Proyectos"
            Role.ADMINISTRATOR -> "Solicitudes"
        }
        binding.tvCardCoursesSubtitle.text = when (role) {
            Role.CLIENT -> getString(R.string.dashboard_card_courses_sub)
            Role.RESEARCHER -> "Revisa fichas museológicas"
            Role.ADMINISTRATOR -> "Revisa pedidos recibidos"
        }
        binding.tvCardProductsTitle.text = when (role) {
            Role.CLIENT -> getString(R.string.dashboard_card_products_title)
            Role.RESEARCHER -> "Registros"
            Role.ADMINISTRATOR -> "Catálogo"
        }
        binding.tvCardProductsSubtitle.text = when (role) {
            Role.CLIENT -> getString(R.string.dashboard_card_products_sub)
            Role.RESEARCHER -> "Museos virtuales"
            Role.ADMINISTRATOR -> "Productos y cursos"
        }
        binding.tvCardVirtualTitle.text = getString(R.string.dashboard_card_virtual_title)
        binding.tvCardVirtualSubtitle.text = when (role) {
            Role.RESEARCHER -> "Investiga Iberoamérica"
            else -> getString(R.string.dashboard_card_virtual_sub)
        }
        binding.tvCardNewsTitle.text = when (role) {
            Role.RESEARCHER -> "Alertas"
            else -> getString(R.string.dashboard_card_news_title)
        }
        binding.tvCardNewsSubtitle.text = when (role) {
            Role.RESEARCHER -> "Novedades museológicas"
            Role.ADMINISTRATOR -> "Publicaciones y avisos"
            Role.CLIENT -> getString(R.string.dashboard_card_news_sub)
        }
    }

    private fun setupEntranceAnimations() {
        val animatableViews = listOf(
            binding.cardQuickAccess,
            binding.cardCursos,
            binding.cardProductos,
            binding.cardMuseosVirtuales,
            binding.cardNovedades,
            binding.cardFeatured,
        )

        val animators = animatableViews.mapIndexed { index, targetView ->
            targetView.alpha = 0f
            targetView.translationY = 40f

            val alphaAnimator = ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f)
            val translationAnimator = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_Y, 40f, 0f)

            AnimatorSet().apply {
                playTogether(alphaAnimator, translationAnimator)
                duration = 600
                startDelay = (index * 90).toLong()
                interpolator = DecelerateInterpolator()
            }
        }

        AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}