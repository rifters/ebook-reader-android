package com.rifters.ebookreader

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rifters.ebookreader.databinding.DialogAddNoteBinding

class AddNoteDialogFragment : DialogFragment() {

    private var _binding: DialogAddNoteBinding? = null
    private val binding get() = _binding!!
    
    private var initialNote: String? = null
    private var onNoteSavedListener: ((String?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialNote = arguments?.getString(ARG_INITIAL_NOTE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set initial note if editing
        initialNote?.let {
            binding.etNote.setText(it)
            binding.etNote.setSelection(it.length)
            binding.tvDialogTitle.text = getString(R.string.edit_note)
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            val noteText = binding.etNote.text?.toString()?.trim()
            val finalNote = if (noteText.isNullOrEmpty()) null else noteText
            onNoteSavedListener?.invoke(finalNote)
            dismiss()
        }
    }

    fun setOnNoteSavedListener(listener: (String?) -> Unit) {
        onNoteSavedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddNoteDialogFragment"
        private const val ARG_INITIAL_NOTE = "initial_note"

        fun newInstance(initialNote: String? = null): AddNoteDialogFragment {
            return AddNoteDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_NOTE, initialNote)
                }
            }
        }
    }
}
