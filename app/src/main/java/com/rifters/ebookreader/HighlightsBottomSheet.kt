package com.rifters.ebookreader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rifters.ebookreader.database.BookDatabase
import com.rifters.ebookreader.databinding.BottomSheetHighlightsBinding
import com.rifters.ebookreader.model.Highlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HighlightsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHighlightsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: HighlightAdapter
    private var bookId: Long = -1
    private var onHighlightSelectedListener: ((Highlight) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookId = arguments?.getLong(ARG_BOOK_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHighlightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadHighlights()
    }

    private fun setupRecyclerView() {
        adapter = HighlightAdapter(
            onHighlightClick = { highlight ->
                onHighlightSelectedListener?.invoke(highlight)
                dismiss()
            },
            onDeleteClick = { highlight ->
                deleteHighlight(highlight)
            }
        )
        
        binding.rvHighlights.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HighlightsBottomSheet.adapter
        }
    }

    private fun loadHighlights() {
        lifecycleScope.launch {
            val database = BookDatabase.getDatabase(requireContext())
            val highlights = withContext(Dispatchers.IO) {
                database.highlightDao().getHighlightsForBookSync(bookId)
            }
            
            withContext(Dispatchers.Main) {
                if (highlights.isEmpty()) {
                    binding.rvHighlights.visibility = View.GONE
                    binding.tvEmptyMessage.visibility = View.VISIBLE
                } else {
                    binding.rvHighlights.visibility = View.VISIBLE
                    binding.tvEmptyMessage.visibility = View.GONE
                    adapter.submitList(highlights)
                }
            }
        }
    }

    private fun deleteHighlight(highlight: Highlight) {
        lifecycleScope.launch {
            val database = BookDatabase.getDatabase(requireContext())
            withContext(Dispatchers.IO) {
                database.highlightDao().deleteHighlight(highlight)
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    R.string.highlight_removed,
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            // Reload highlights after deletion
            loadHighlights()
        }
    }

    fun setOnHighlightSelectedListener(listener: (Highlight) -> Unit) {
        onHighlightSelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "HighlightsBottomSheet"
        private const val ARG_BOOK_ID = "book_id"

        fun newInstance(bookId: Long): HighlightsBottomSheet {
            return HighlightsBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_BOOK_ID, bookId)
                }
            }
        }
    }
}
