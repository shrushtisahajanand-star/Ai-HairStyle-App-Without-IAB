//package com.hairstyle.generator.aimakerover.ui.loading
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.activity.OnBackPressedCallback
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.repeatOnLifecycle
//import androidx.navigation.fragment.findNavController
//import coil.load
//import com.hairstyle.generator.aimakeover.R
//import com.hairstyle.generator.aimakeover.databinding.FragmentLoadingBinding
//import com.hairstyle.generator.aimakerover.domain.model.ResultState
//import com.hairstyle.generator.aimakerover.ui.viewmodel.GenerationViewModel
//import com.hairstyle.generator.aimakerover.utils.FileUtils
//import com.hairstyle.generator.aimakerover.utils.showSnackbar
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.launch
//
//@AndroidEntryPoint
//class LoadingFragment : Fragment() {
//
//    private var _binding: FragmentLoadingBinding? = null
//    private val binding get() = _binding!!
//
//    private val viewModel: GenerationViewModel by activityViewModels()
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentLoadingBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        requireActivity().onBackPressedDispatcher.addCallback(
//            viewLifecycleOwner,
//            object : OnBackPressedCallback(true) {
//                override fun handleOnBackPressed() {
//                    // Block back during generation
//                }
//            }
//        )
//
//        binding.toolbar.setNavigationOnClickListener {
//            // Disabled during loading
//        }
//
//        binding.progressBar.isIndeterminate = false
//        binding.progressBar.max = 100
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.session.collect { session ->
//                    session.originalImageUri?.let { uri ->
//                        binding.ivUserPhoto.load(uri) {
//                            crossfade(true)
//                        }
//                    }
//                }
//            }
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.loadingState.collect { state ->
//                    binding.tvStatusLabel.text = state.currentStage.label
//                    binding.progressBar.isIndeterminate = false
//                    binding.progressBar.setProgressCompat(state.progress, true)
//                    binding.tvPercent.text = getString(R.string.percent_format, state.progress)
//                    binding.tvInfoTitle.text = state.currentStage.infoTitle
//                    binding.tvInfoSubtitle.text = state.currentStage.infoSubtitle
//                }
//            }
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.generationState.collect { state ->
//                    when (state) {
//                        is ResultState.Success -> {
////                            findNavController().navigate(R.id.action_loading_to_result)
//                        }
//                        is ResultState.Error -> {
//                            showSnackbar(state.message, getString(R.string.retry)) {
//                                val cacheDir = FileUtils.getCacheDir(requireContext(), "results")
//                                viewModel.retryGeneration(cacheDir)
//                            }
//                        }
//                        else -> Unit
//                    }
//                }
//            }
//        }
//
//        val currentState = viewModel.generationState.value
//        if (currentState is ResultState.Idle) {
//            val cacheDir = FileUtils.getCacheDir(requireContext(), "results")
//            viewModel.startGeneration(cacheDir)
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
