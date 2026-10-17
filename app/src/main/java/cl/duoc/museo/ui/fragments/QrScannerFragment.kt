package cl.duoc.museo.ui.fragments

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cl.duoc.museo.R
import cl.duoc.museo.databinding.FragmentQrScannerBinding
import cl.duoc.museo.ui.viewmodels.QrScannerViewModel
import cl.duoc.museo.ui.viewmodels.ScanResult
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import java.io.IOException
import java.util.EnumMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QrScannerFragment : Fragment() {

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QrScannerViewModel by viewModels()
    private var isCameraPermissionGranted = false
    private var isFragmentResumed = false
    private var isScannerConfigured = false
    private val supportedFormats = listOf(
        BarcodeFormat.QR_CODE,
        BarcodeFormat.EAN_13,
        BarcodeFormat.EAN_8,
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.CODE_128,
        BarcodeFormat.CODE_39,
        BarcodeFormat.CODE_93,
        BarcodeFormat.ITF,
        BarcodeFormat.CODABAR,
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        isCameraPermissionGranted = isGranted
        if (isGranted && isFragmentResumed && _binding != null) {
            startCameraScanner()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.qr_permission_denied),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    // Gallery Image Picker Launcher
    private val pickGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { scanQrFromGalleryUri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentQrScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
        configureBarcodeScanner()
    }

    private fun startCameraScanner() {
        if (!isCameraPermissionGranted || !isFragmentResumed) return
        binding.barcodeScanner.resume()
    }

    private fun configureBarcodeScanner() {
        if (isScannerConfigured) return
        binding.barcodeScanner.decoderFactory = DefaultDecoderFactory(supportedFormats)
        binding.barcodeScanner.decodeContinuous { result ->
            val text = result.text.orEmpty()
            if (text.isNotBlank()) {
                viewModel.processScannedQrCode(text)
            }
        }
        isScannerConfigured = true
    }

    private fun setupClickListeners() {
        // Navigation Back Button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Flashlight Toggles
        binding.btnFlashToggle.setOnClickListener {
            toggleTorchIfAvailable()
        }

        binding.btnFlashControl.setOnClickListener {
            toggleTorchIfAvailable()
        }
        setupGalleryPicker()
    }

    private fun toggleTorchIfAvailable() {
        if (!isCameraPermissionGranted) {
            Toast.makeText(requireContext(), getString(R.string.qr_permission_denied), Toast.LENGTH_SHORT).show()
        } else if (!requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Toast.makeText(requireContext(), "Este dispositivo no tiene linterna.", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.toggleTorch()
        }
    }

    private fun setupGalleryPicker() {
        binding.btnGalleryControl.setOnClickListener {
            pickGalleryLauncher.launch("image/*")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isTorchOn.collect { isOn ->
                        if (isCameraPermissionGranted && isFragmentResumed) {
                            binding.barcodeScanner.setTorch(isOn)
                        }
                        if (isOn) {
                            binding.btnFlashToggle.setColorFilter(
                                ContextCompat.getColor(requireContext(), R.color.beige_gold),
                            )
                        } else {
                            binding.btnFlashToggle.setColorFilter(
                                ContextCompat.getColor(requireContext(), R.color.white),
                            )
                        }
                    }
                }

                launch {
                    viewModel.scanResult.collect { result ->
                        when (result) {
                            is ScanResult.Success -> {
                                val bundle = Bundle().apply {
                                    putString("scannedCode", result.code)
                                    putString("contentId", result.contentId)
                                    putBoolean("isMuseum", result.isMuseum)
                                    putString("scannedType", result.type)
                                    putString("scannedTitle", result.title)
                                }
                                binding.barcodeScanner.pause()
                                findNavController().navigate(
                                    R.id.action_qrScannerFragment_to_qrResultFragment,
                                    bundle,
                                )
                            }
                            is ScanResult.Error -> {
                                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scanQrFromGalleryUri(uri: Uri) {
        try {
            val contentResolver = requireContext().contentResolver
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val scannedText = withContext(Dispatchers.IO) {
                        decodeCodeFromImage(uri, contentResolver)
                    }
                    viewModel.processScannedQrCode(scannedText)
                } catch (_: NotFoundException) {
                    Toast.makeText(
                        requireContext(),
                        "No se encontró un código QR o de barras en la imagen.",
                        Toast.LENGTH_SHORT,
                    ).show()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.e(
                        "QrScannerFragment",
                        "No se pudo leer la imagen (esquema=${uri.scheme}, proveedor=${uri.authority}).",
                        exception,
                    )
                    Toast.makeText(
                        requireContext(),
                        "No se pudo leer la imagen. Intenta con otro archivo.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        } catch (exception: IllegalStateException) {
            Log.e("QrScannerFragment", "El visor de imagen ya no está disponible.", exception)
        }
    }

    private fun decodeCodeFromImage(uri: Uri, resolver: ContentResolver): String {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val sampleSize = calculateSampleSize(info.size.width, info.size.height, 1600)
                decoder.setTargetSize(
                    (info.size.width / sampleSize).coerceAtLeast(1),
                    (info.size.height / sampleSize).coerceAtLeast(1),
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            decodeBitmapWithBitmapFactory(uri, resolver)
        }

        return try {
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.POSSIBLE_FORMATS, supportedFormats)
                put(DecodeHintType.TRY_HARDER, true)
            }
            MultiFormatReader().apply { setHints(hints) }.decode(binaryBitmap).text
        } finally {
            bitmap.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun decodeBitmapWithBitmapFactory(uri: Uri, resolver: ContentResolver): android.graphics.Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: throw IOException("No se pudo abrir la imagen seleccionada.")

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("La imagen seleccionada no contiene dimensiones válidas.")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, 1600)
        }
        return resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IOException("No se pudo decodificar la imagen seleccionada.")
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    override fun onResume() {
        super.onResume()
        isFragmentResumed = true
        viewModel.resetLastScannedCode()
        isCameraPermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (isCameraPermissionGranted) {
            startCameraScanner()
        } else if (_binding != null) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onPause() {
        isFragmentResumed = false
        super.onPause()
        if (isCameraPermissionGranted) binding.barcodeScanner.pause()
    }

    override fun onDestroyView() {
        isScannerConfigured = false
        super.onDestroyView()
        _binding = null
    }
}