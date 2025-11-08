package com.rifters.ebookreader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rifters.ebookreader.databinding.BottomSheetTocBinding
import com.rifters.ebookreader.model.TableOfContentsItem

class TocBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTocBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: TocAdapter
    private var tocItems: List<TableOfContentsItem> = emptyList()
    private var onTocItemSelectedListener: ((TableOfContentsItem) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tocItems = arguments?.getParcelableArrayList<TableOfContentsItem>(ARG_TOC_ITEMS) ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTocBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        displayToc()
    }

    private fun setupRecyclerView() {
        adapter = TocAdapter { tocItem ->
            onTocItemSelectedListener?.invoke(tocItem)
            dismiss()
        }
        
        binding.rvToc.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TocBottomSheet.adapter
        }
    }

    private fun displayToc() {
        if (tocItems.isEmpty()) {
            binding.rvToc.visibility = View.GONE
            binding.tvEmptyMessage.visibility = View.VISIBLE
        } else {
            binding.rvToc.visibility = View.VISIBLE
            binding.tvEmptyMessage.visibility = View.GONE
            adapter.submitList(tocItems)
        }
    }

    fun setOnTocItemSelectedListener(listener: (TableOfContentsItem) -> Unit) {
        onTocItemSelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TocBottomSheet"
        private const val ARG_TOC_ITEMS = "toc_items"

        fun newInstance(tocItems: List<TableOfContentsItem>): TocBottomSheet {
            return TocBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_TOC_ITEMS, ArrayList(tocItems))
                }
            }
        }
    }
}
