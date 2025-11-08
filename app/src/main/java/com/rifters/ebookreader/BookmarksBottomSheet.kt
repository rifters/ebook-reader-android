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
import com.rifters.ebookreader.databinding.BottomSheetBookmarksBinding
import com.rifters.ebookreader.model.Bookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBookmarksBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: BookmarkAdapter
    private var bookId: Long = -1
    private var onBookmarkSelectedListener: ((Bookmark) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookId = arguments?.getLong(ARG_BOOK_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadBookmarks()
    }

    private fun setupRecyclerView() {
        adapter = BookmarkAdapter(
            onBookmarkClick = { bookmark ->
                onBookmarkSelectedListener?.invoke(bookmark)
                dismiss()
            },
            onDeleteClick = { bookmark ->
                deleteBookmark(bookmark)
            },
            onEditClick = { bookmark ->
                showEditNoteDialog(bookmark)
            }
        )
        
        binding.rvBookmarks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BookmarksBottomSheet.adapter
        }
    }

    private fun loadBookmarks() {
        lifecycleScope.launch {
            val database = BookDatabase.getDatabase(requireContext())
            val bookmarks = withContext(Dispatchers.IO) {
                database.bookmarkDao().getBookmarksForBookSync(bookId)
            }
            
            withContext(Dispatchers.Main) {
                if (bookmarks.isEmpty()) {
                    binding.rvBookmarks.visibility = View.GONE
                    binding.tvEmptyMessage.visibility = View.VISIBLE
                } else {
                    binding.rvBookmarks.visibility = View.VISIBLE
                    binding.tvEmptyMessage.visibility = View.GONE
                    adapter.submitList(bookmarks)
                }
            }
        }
    }

    private fun deleteBookmark(bookmark: Bookmark) {
        lifecycleScope.launch {
            val database = BookDatabase.getDatabase(requireContext())
            withContext(Dispatchers.IO) {
                database.bookmarkDao().deleteBookmark(bookmark)
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    R.string.bookmark_removed,
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            // Reload bookmarks after deletion
            loadBookmarks()
        }
    }

    private fun showEditNoteDialog(bookmark: Bookmark) {
        val dialog = AddNoteDialogFragment.newInstance(bookmark.note)
        dialog.setOnNoteSavedListener { note ->
            updateBookmarkNote(bookmark, note)
        }
        dialog.show(childFragmentManager, AddNoteDialogFragment.TAG)
    }

    private fun updateBookmarkNote(bookmark: Bookmark, note: String?) {
        lifecycleScope.launch {
            val database = BookDatabase.getDatabase(requireContext())
            val updatedBookmark = bookmark.copy(note = note)
            
            withContext(Dispatchers.IO) {
                database.bookmarkDao().updateBookmark(updatedBookmark)
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    R.string.note_updated,
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            // Reload bookmarks after update
            loadBookmarks()
        }
    }

    fun setOnBookmarkSelectedListener(listener: (Bookmark) -> Unit) {
        onBookmarkSelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BookmarksBottomSheet"
        private const val ARG_BOOK_ID = "book_id"

        fun newInstance(bookId: Long): BookmarksBottomSheet {
            return BookmarksBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_BOOK_ID, bookId)
                }
            }
        }
    }
}
