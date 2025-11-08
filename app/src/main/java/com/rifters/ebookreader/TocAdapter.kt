package com.rifters.ebookreader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rifters.ebookreader.databinding.ItemTocBinding
import com.rifters.ebookreader.model.TableOfContentsItem

class TocAdapter(
    private val onItemClick: (TableOfContentsItem) -> Unit
) : ListAdapter<TableOfContentsItem, TocAdapter.TocViewHolder>(TocDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TocViewHolder {
        val binding = ItemTocBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TocViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TocViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TocViewHolder(
        private val binding: ItemTocBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: TableOfContentsItem) {
            binding.tvTocTitle.text = item.title
            
            // Apply indentation based on level
            val paddingStart = (item.level * 24).coerceIn(0, 72)
            binding.tvTocTitle.setPadding(
                paddingStart,
                binding.tvTocTitle.paddingTop,
                binding.tvTocTitle.paddingEnd,
                binding.tvTocTitle.paddingBottom
            )
        }
    }

    private class TocDiffCallback : DiffUtil.ItemCallback<TableOfContentsItem>() {
        override fun areItemsTheSame(
            oldItem: TableOfContentsItem,
            newItem: TableOfContentsItem
        ): Boolean {
            return oldItem.href == newItem.href
        }

        override fun areContentsTheSame(
            oldItem: TableOfContentsItem,
            newItem: TableOfContentsItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
