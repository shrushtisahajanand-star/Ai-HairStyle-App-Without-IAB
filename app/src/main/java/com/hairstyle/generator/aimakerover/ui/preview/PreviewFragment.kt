//package com.hairstyle.generator.aimakerover.ui.preview
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.repeatOnLifecycle
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.GridLayoutManager
//import coil.load
//import com.hairstyle.generator.aimakeover.R
//import com.hairstyle.generator.aimakeover.databinding.FragmentPreviewBinding
//import com.hairstyle.generator.aimakerover.domain.model.HairColor
//import com.hairstyle.generator.aimakerover.domain.model.Hairstyle
//import com.hairstyle.generator.aimakerover.ui.haircolor.HairColorAdapter
//import com.hairstyle.generator.aimakerover.ui.hairstyle.HairstyleAdapter
//import com.hairstyle.generator.aimakerover.ui.viewmodel.GenerationViewModel
//import com.hairstyle.generator.aimakerover.utils.showSnackbar
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.launch
//
//@AndroidEntryPoint
//class PreviewFragment : Fragment() {
//
//    private var _binding: FragmentPreviewBinding? = null
//    private val binding get() = _binding!!
//
//    private val viewModel: GenerationViewModel by activityViewModels()
//    private lateinit var hairstyleAdapter: HairstyleAdapter
//    private lateinit var hairColorAdapter: HairColorAdapter
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        binding.toolbar.setNavigationOnClickListener {
//            findNavController().navigateUp()
//        }
//
//        setupDefaultSelections()
//        setupHairstyleList()
//        setupColorList()
//        observePreviewImage()
//
//        binding.btnRetake.setOnClickListener {
//            findNavController().navigateUp()
//        }
//
//        binding.btnGenerate.setOnClickListener {
//            val session = viewModel.session.value
//            if (session.selectedHairstyle == null || session.selectedColor == null) {
//                showSnackbar(getString(R.string.error_select_style_color))
//                return@setOnClickListener
//            }
//            viewModel.resetGenerationState()
//            findNavController().navigate(R.id.action_preview_to_loading)
//        }
//    }
//
//    private fun setupDefaultSelections() {
//        val session = viewModel.session.value
//        if (session.selectedHairstyle == null) {
//            viewModel.setHairstyle(Hairstyle.POMPADOUR)
//        }
//        if (session.selectedColor == null) {
//            viewModel.setHairColor(HairColor.BLONDE)
//        }
//    }
//
//    private fun setupHairstyleList() {
//        val selected = viewModel.session.value.selectedHairstyle
//        hairstyleAdapter = HairstyleAdapter(selected) { hairstyle ->
//            viewModel.setHairstyle(hairstyle)
//        }
//        binding.rvHairstyles.apply {
//            layoutManager = GridLayoutManager(requireContext(), 2)
//            adapter = hairstyleAdapter
//        }
//        hairstyleAdapter.submitList(Hairstyle.entries)
//    }
//
//    private fun setupColorList() {
//        val selected = viewModel.session.value.selectedColor
//        hairColorAdapter = HairColorAdapter(selected) { color ->
//            viewModel.setHairColor(color)
//        }
//        binding.rvColors.apply {
//            layoutManager = GridLayoutManager(requireContext(), 3)
//            adapter = hairColorAdapter
//        }
//        hairColorAdapter.submitList(HairColor.entries)
//    }
//
//    private fun observePreviewImage() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.session.collect { session ->
//                    session.originalImageUri?.let { uri ->
//                        binding.ivPreview.load(uri) {
//                            crossfade(true)
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
