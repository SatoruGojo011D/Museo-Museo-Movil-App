package cl.duoc.museo.ui.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import cl.duoc.museo.R
import cl.duoc.museo.databinding.FragmentProductDetailBinding
import cl.duoc.museo.ui.viewmodels.CartEvent
import cl.duoc.museo.ui.viewmodels.ProductDetailViewModel
import cl.duoc.museo.utils.MoneyFormatter
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productId = arguments?.getString("productId")
        val productName = arguments?.getString("productName")
        viewModel.initProduct(productId, productName)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Back Navigation
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Favorite Toggle Header Button
        binding.btnFavoriteHeader.setOnClickListener {
            viewModel.toggleFavorite()
        }

        // Stepper Quantity Control
        binding.btnMinus.setOnClickListener {
            viewModel.decrementQuantity()
        }

        binding.btnPlus.setOnClickListener {
            viewModel.incrementQuantity()
        }

        // Primary Action: Add to Cart / Request Info
        binding.btnAddToCart.setOnClickListener {
            viewModel.addToCart()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.progressProduct.visibility =
                            if (state.isLoading) View.VISIBLE else View.GONE
                        binding.tvProductError.visibility =
                            if (state.errorMessage != null) View.VISIBLE else View.GONE
                        binding.tvProductError.text = state.errorMessage
                        binding.groupProductDetails.visibility =
                            if (state.product != null) View.VISIBLE else View.GONE
                        binding.btnBack.visibility = View.VISIBLE

                        val product = state.product ?: return@collect

                        binding.tvProductTitle.text = product.name
                        binding.tvProductCategory.text = product.category
                        binding.tvPrice.text = MoneyFormatter.formatUsd(product.price)
                        binding.tvDescription.text = product.description
                        binding.imgProductHero.contentDescription = product.name
                        Glide.with(binding.imgProductHero)
                            .load(product.imageUrl.takeIf { it.isNotBlank() })
                            .placeholder(R.drawable.bg_museum_interior)
                            .error(R.drawable.bg_museum_interior)
                            .centerCrop()
                            .into(binding.imgProductHero)
                        binding.tvQuantity.text = state.quantity.toString()
                        binding.btnAddToCart.text = state.buttonText
                        binding.btnAddToCart.isEnabled = state.isStockAvailable
                        binding.btnFavoriteHeader.isEnabled = true

                        // Course vs. Physical Product Adaptation
                        if (state.isCourse) {
                            binding.layoutQuantityStepper.visibility = View.GONE
                            binding.tvStockStatus.text = state.stockMessage
                            binding.vStatusDot.backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(requireContext(), R.color.dark_blue)
                            )
                            binding.tvStockStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.dark_blue)
                            )
                        } else {
                            binding.layoutQuantityStepper.visibility = View.VISIBLE
                            binding.tvStockStatus.text = state.stockMessage
                            val statusColor = if (state.isStockAvailable) {
                                ContextCompat.getColor(requireContext(), R.color.green_success)
                            } else {
                                ContextCompat.getColor(requireContext(), R.color.primary)
                            }
                            binding.vStatusDot.backgroundTintList = ColorStateList.valueOf(statusColor)
                            binding.tvStockStatus.setTextColor(statusColor)
                        }

                        // Header Favorite Icon State
                        if (state.isFavorite) {
                            binding.btnFavoriteHeader.setImageResource(R.drawable.ic_favorite)
                            binding.btnFavoriteHeader.imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(requireContext(), R.color.primary)
                            )
                        } else {
                            binding.btnFavoriteHeader.setImageResource(R.drawable.ic_favorite_border)
                            binding.btnFavoriteHeader.imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(requireContext(), R.color.white)
                            )
                        }
                    }
                }

                launch {
                    viewModel.cartEvent.collect { event ->
                        when (event) {
                            is CartEvent.Success -> {
                                animateAddToCartSuccess()
                                val msg = if (viewModel.uiState.value.isCourse) {
                                    "¡Solicitud de información enviada!"
                                } else {
                                    "¡Agregado al carrito exitosamente!"
                                }
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            }
                            is CartEvent.OutOfStock -> {
                                animateOutOfStockShake()
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                            is CartEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun animateAddToCartSuccess() {
        val scaleX = ObjectAnimator.ofFloat(binding.btnAddToCart, View.SCALE_X, 1.0f, 1.12f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(binding.btnAddToCart, View.SCALE_Y, 1.0f, 1.12f, 1.0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 350
            interpolator = OvershootInterpolator()
            start()
        }
    }

    private fun animateOutOfStockShake() {
        val shake = ObjectAnimator.ofFloat(
            binding.btnAddToCart,
            View.TRANSLATION_X,
            0f, 25f, -25f, 15f, -15f, 6f, -6f, 0f
        )
        shake.duration = 400
        shake.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}