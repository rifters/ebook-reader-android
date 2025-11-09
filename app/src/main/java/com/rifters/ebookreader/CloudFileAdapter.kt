package com.rifters.ebookreader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rifters.ebookreader.cloud.CloudFile
import com.rifters.ebookreader.databinding.ItemCloudFileBinding

/**
 * Adapter for displaying cloud files in a RecyclerView
 */
class CloudFileAdapter(
    private val onFileClick: (CloudFile) -> Unit,
    private val onFileLongClick: (CloudFile) -> Unit
) : ListAdapter<CloudFile, CloudFileAdapter.CloudFileViewHolder>(CloudFileDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CloudFileViewHolder {
        val binding = ItemCloudFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CloudFileViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CloudFileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class CloudFileViewHolder(
        private val binding: ItemCloudFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(file: CloudFile) {
            binding.fileName.text = file.name
            
            // Set icon based on file type
            binding.fileIcon.setImageResource(
                if (file.isDirectory) {
                    R.drawable.ic_folder
                } else {
                    R.drawable.ic_book
                }
            )
            
            // Show file size for files
            binding.fileSize.text = if (file.isDirectory) {
                "Folder"
            } else {
                formatFileSize(file.size)
            }
            
            // Set provider badge
            binding.providerBadge.text = file.provider
            
            // Click listeners
            binding.root.setOnClickListener {
                onFileClick(file)
            }
            
            binding.root.setOnLongClickListener {
                onFileLongClick(file)
                true
            }
        }
        
        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }
    
    private class CloudFileDiffCallback : DiffUtil.ItemCallback<CloudFile>() {
        override fun areItemsTheSame(oldItem: CloudFile, newItem: CloudFile): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: CloudFile, newItem: CloudFile): Boolean {
            return oldItem == newItem
        }
    }
}
