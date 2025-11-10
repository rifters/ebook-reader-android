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
        val replacementsEnabled = preferencesManager.isTtsReplacementsEnabled()
        
        binding.ttsRateSlider.value = ttsRate
        binding.ttsRateValue.text = String.format("%.1fx", ttsRate)
        
        binding.ttsPitchSlider.value = ttsPitch
        binding.ttsPitchValue.text = String.format("%.1fx", ttsPitch)
        
        binding.switchReplacements.isChecked = replacementsEnabled
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
        
        // Replacements toggle
        binding.switchReplacements.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setTtsReplacementsEnabled(isChecked)
        }
        
        // Manage replacements button
        binding.btnManageReplacements.setOnClickListener {
            showReplacementsDialog()
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
    
    private fun showReplacementsDialog() {
        val replacements = com.rifters.ebookreader.util.TtsReplacementProcessor.getReplacementsList(
            preferencesManager.getTtsReplacements()
        )
        
        val items = replacements.map { "${it.first} → ${it.second}" }.toTypedArray()
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.tts_replacements)
            .setItems(items) { _, which ->
                // Show edit dialog for selected replacement
                val selected = replacements[which]
                showEditReplacementDialog(selected.first, selected.second)
            }
            .setNeutralButton(R.string.add_replacement) { _, _ ->
                showEditReplacementDialog("", "")
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }
    
    private fun showEditReplacementDialog(key: String, value: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_replacement, null)
        val editKey = dialogView.findViewById<android.widget.EditText>(R.id.editFindText)
        val editValue = dialogView.findViewById<android.widget.EditText>(R.id.editReplaceWith)
        
        editKey.setText(key)
        editValue.setText(value)
        
        val builder = android.app.AlertDialog.Builder(requireContext())
            .setTitle(if (key.isEmpty()) R.string.add_replacement else R.string.edit_replacement)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newKey = editKey.text.toString()
                val newValue = editValue.text.toString()
                
                if (newKey.isNotEmpty()) {
                    var replacementsJson = preferencesManager.getTtsReplacements()
                    
                    // Remove old key if it changed
                    if (key.isNotEmpty() && key != newKey) {
                        replacementsJson = com.rifters.ebookreader.util.TtsReplacementProcessor.removeReplacement(replacementsJson, key)
                    }
                    
                    // Add/update new key
                    replacementsJson = com.rifters.ebookreader.util.TtsReplacementProcessor.addReplacement(
                        replacementsJson, newKey, newValue
                    )
                    
                    preferencesManager.setTtsReplacements(replacementsJson)
                    android.widget.Toast.makeText(requireContext(), "Replacement saved", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
        
        // Add delete button if editing existing
        if (key.isNotEmpty()) {
            builder.setNeutralButton("Delete") { _, _ ->
                val replacementsJson = com.rifters.ebookreader.util.TtsReplacementProcessor.removeReplacement(
                    preferencesManager.getTtsReplacements(), key
                )
                preferencesManager.setTtsReplacements(replacementsJson)
                android.widget.Toast.makeText(requireContext(), "Replacement deleted", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        builder.show()
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
