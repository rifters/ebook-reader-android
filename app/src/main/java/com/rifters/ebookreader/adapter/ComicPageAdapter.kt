package com.rifters.ebookreader.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.rifters.ebookreader.R

/**
 * Adapter for displaying comic book pages in a ViewPager2.
 * Each page is displayed as an image.
 */
class ComicPageAdapter : RecyclerView.Adapter<ComicPageAdapter.ComicPageViewHolder>() {

    private val pages = mutableListOf<Bitmap>()
    private var onPageClickListener: ((Int) -> Unit)? = null

    fun setPages(newPages: List<Bitmap>) {
        pages.clear()
        pages.addAll(newPages)
        notifyDataSetChanged()
    }

    fun setOnPageClickListener(listener: (Int) -> Unit) {
        onPageClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComicPageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comic_page, parent, false)
        return ComicPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComicPageViewHolder, position: Int) {
        holder.bind(pages[position], position)
    }

    override fun getItemCount(): Int = pages.size

    inner class ComicPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.comicPageImageView)

        fun bind(bitmap: Bitmap, position: Int) {
            imageView.setImageBitmap(bitmap)
            itemView.setOnClickListener {
                onPageClickListener?.invoke(position)
            }
        }
    }
}
