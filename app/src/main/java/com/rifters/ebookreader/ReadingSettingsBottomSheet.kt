package com.rifters.ebookreader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rifters.ebookreader.databinding.BottomSheetReadingSettingsBinding
import com.rifters.ebookreader.model.ReadingPreferences
import com.rifters.ebookreader.model.ReadingTheme
import com.rifters.ebookreader.util.PreferencesManager

class ReadingSettingsBottomSheet : BottomSheetDialogFragment() {
    
    private var _binding: BottomSheetReadingSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private var currentPreferences: ReadingPreferences? = null
    private var onSettingsApplied: ((ReadingPreferences) -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReadingSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        currentPreferences = preferencesManager.getReadingPreferences()
        
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        currentPreferences?.let { prefs ->
            // Set font family
            when (prefs.fontFamily) {
                "sans-serif" -> binding.fontSansSerif.isChecked = true
                "serif" -> binding.fontSerif.isChecked = true
                "monospace" -> binding.fontMonospace.isChecked = true
            }
            
            // Set theme
            when (prefs.theme) {
                ReadingTheme.LIGHT -> binding.themeLight.isChecked = true
                ReadingTheme.DARK -> binding.themeDark.isChecked = true
                ReadingTheme.SEPIA -> binding.themeSepia.isChecked = true
            }
            
            // Set line spacing
            binding.lineSpacingSlider.value = prefs.lineSpacing
            binding.lineSpacingValue.text = String.format("%.1fx", prefs.lineSpacing)
            
            // Set margins
            binding.marginSlider.value = prefs.marginHorizontal.toFloat()
            binding.marginValue.text = "${prefs.marginHorizontal}dp"
        }
    }
    
    private fun setupListeners() {
        // Line spacing slider
        binding.lineSpacingSlider.addOnChangeListener { _, value, _ ->
            binding.lineSpacingValue.text = String.format("%.1fx", value)
        }
        
        // Margin slider
        binding.marginSlider.addOnChangeListener { _, value, _ ->
            binding.marginValue.text = "${value.toInt()}dp"
        }
        
        // Apply button
        binding.btnApply.setOnClickListener {
            applySettings()
        }
    }
    
    private fun applySettings() {
        val fontFamily = when (binding.fontChipGroup.checkedChipId) {
            R.id.fontSerif -> "serif"
            R.id.fontMonospace -> "monospace"
            else -> "sans-serif"
        }
        
        val theme = when (binding.themeChipGroup.checkedChipId) {
            R.id.themeDark -> ReadingTheme.DARK
            R.id.themeSepia -> ReadingTheme.SEPIA
            else -> ReadingTheme.LIGHT
        }
        
        val lineSpacing = binding.lineSpacingSlider.value
        val margin = binding.marginSlider.value.toInt()
        
        val preferences = ReadingPreferences(
            fontFamily = fontFamily,
            theme = theme,
            lineSpacing = lineSpacing,
            marginHorizontal = margin,
            marginVertical = margin
        )
        
        preferencesManager.saveReadingPreferences(preferences)
        onSettingsApplied?.invoke(preferences)
        dismiss()
    }
    
    fun setOnSettingsAppliedListener(listener: (ReadingPreferences) -> Unit) {
        onSettingsApplied = listener
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        const val TAG = "ReadingSettingsBottomSheet"
        
        fun newInstance(): ReadingSettingsBottomSheet {
            return ReadingSettingsBottomSheet()
        }
    }
}
