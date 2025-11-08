package com.rifters.ebookreader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rifters.ebookreader.databinding.ItemCollectionBinding
import com.rifters.ebookreader.model.CollectionWithBooks

class CollectionAdapter(
    private val onItemClick: (CollectionWithBooks) -> Unit,
    private val onMenuClick: (CollectionWithBooks) -> Unit
) : ListAdapter<CollectionWithBooks, CollectionAdapter.CollectionViewHolder>(CollectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val binding = ItemCollectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CollectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CollectionViewHolder(
        private val binding: ItemCollectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(collectionWithBooks: CollectionWithBooks) {
            binding.collectionName.text = collectionWithBooks.collection.name
            
            val bookCount = collectionWithBooks.books.size
            binding.bookCount.text = if (bookCount == 1) {
                binding.root.context.getString(R.string.one_book_in_collection)
            } else {
                binding.root.context.getString(R.string.books_in_collection, bookCount)
            }

            binding.root.setOnClickListener {
                onItemClick(collectionWithBooks)
            }

            binding.menuButton.setOnClickListener {
                onMenuClick(collectionWithBooks)
            }
        }
    }

    class CollectionDiffCallback : DiffUtil.ItemCallback<CollectionWithBooks>() {
        override fun areItemsTheSame(
            oldItem: CollectionWithBooks,
            newItem: CollectionWithBooks
        ): Boolean {
            return oldItem.collection.id == newItem.collection.id
        }

        override fun areContentsTheSame(
            oldItem: CollectionWithBooks,
            newItem: CollectionWithBooks
        ): Boolean {
            return oldItem.collection == newItem.collection && 
                   oldItem.books.size == newItem.books.size
        }
    }
}
