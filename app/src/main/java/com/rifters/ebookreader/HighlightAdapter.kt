package com.rifters.ebookreader

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rifters.ebookreader.databinding.ItemHighlightBinding
import com.rifters.ebookreader.model.Highlight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HighlightAdapter(
    private val onHighlightClick: (Highlight) -> Unit,
    private val onDeleteClick: (Highlight) -> Unit
) : ListAdapter<Highlight, HighlightAdapter.HighlightViewHolder>(HighlightDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HighlightViewHolder {
        val binding = ItemHighlightBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HighlightViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HighlightViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HighlightViewHolder(
        private val binding: ItemHighlightBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(highlight: Highlight) {
            binding.apply {
                tvHighlightText.text = highlight.selectedText
                
                // Set highlight color indicator
                viewColorIndicator.setBackgroundColor(highlight.color)
                
                // Format timestamp
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                val date = Date(highlight.timestamp)
                tvTimestamp.text = dateFormat.format(date)
                
                // Show note if available
                if (highlight.note != null && highlight.note.isNotEmpty()) {
                    tvNote.text = highlight.note
                    tvNote.visibility = android.view.View.VISIBLE
                } else {
                    tvNote.visibility = android.view.View.GONE
                }
                
                // Page info
                tvPageInfo.text = root.context.getString(R.string.page_format, highlight.page + 1)
                
                root.setOnClickListener {
                    onHighlightClick(highlight)
                }
                
                btnDelete.setOnClickListener {
                    onDeleteClick(highlight)
                }
            }
        }
    }

    class HighlightDiffCallback : DiffUtil.ItemCallback<Highlight>() {
        override fun areItemsTheSame(oldItem: Highlight, newItem: Highlight): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Highlight, newItem: Highlight): Boolean {
            return oldItem == newItem
        }
    }
}
