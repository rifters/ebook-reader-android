package com.rifters.ebookreader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rifters.ebookreader.databinding.BottomSheetTtsControlsBinding
import com.rifters.ebookreader.util.PreferencesManager

class TtsControlsBottomSheet : BottomSheetDialogFragment() {
    
    private var _binding: BottomSheetTtsControlsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private var onSettingsChanged: ((Float, Float) -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTtsControlsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        // Load current TTS settings
        val ttsRate = preferencesManager.getTtsRate()
        val ttsPitch = preferencesManager.getTtsPitch()
        
        binding.ttsRateSlider.value = ttsRate
        binding.ttsRateValue.text = String.format("%.1fx", ttsRate)
        
        binding.ttsPitchSlider.value = ttsPitch
        binding.ttsPitchValue.text = String.format("%.1fx", ttsPitch)
    }
    
    private fun setupListeners() {
        // TTS rate slider - apply in real time
        binding.ttsRateSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.ttsRateValue.text = String.format("%.1fx", value)
                preferencesManager.setTtsRate(value)
                onSettingsChanged?.invoke(value, preferencesManager.getTtsPitch())
            }
        }
        
        // TTS pitch slider - apply in real time
        binding.ttsPitchSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.ttsPitchValue.text = String.format("%.1fx", value)
                preferencesManager.setTtsPitch(value)
                onSettingsChanged?.invoke(preferencesManager.getTtsRate(), value)
            }
        }
        
        // Reset button
        binding.btnReset.setOnClickListener {
            binding.ttsRateSlider.value = 1.0f
            binding.ttsPitchSlider.value = 1.0f
        }
        
        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    fun setOnSettingsChangedListener(listener: (Float, Float) -> Unit) {
        onSettingsChanged = listener
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        const val TAG = "TtsControlsBottomSheet"
        
        fun newInstance(): TtsControlsBottomSheet {
            return TtsControlsBottomSheet()
        }
    }
}
