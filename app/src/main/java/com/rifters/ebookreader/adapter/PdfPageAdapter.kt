package com.rifters.ebookreader.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.rifters.ebookreader.R

/**
 * Adapter for displaying PDF pages in a ViewPager2.
 * Each page is rendered as a Bitmap and displayed in an ImageView.
 */
class PdfPageAdapter : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    private val pages = mutableListOf<Bitmap?>()
    private var onPageClickListener: ((Int) -> Unit)? = null

    fun setPages(newPages: List<Bitmap?>) {
        pages.clear()
        pages.addAll(newPages)
        notifyDataSetChanged()
    }

    fun updatePage(position: Int, bitmap: Bitmap?) {
        if (position in pages.indices) {
            pages[position] = bitmap
            notifyItemChanged(position)
        }
    }

    fun setOnPageClickListener(listener: (Int) -> Unit) {
        onPageClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position], position)
    }

    override fun getItemCount(): Int = pages.size

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.pageImageView)

        fun bind(bitmap: Bitmap?, position: Int) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(R.drawable.ic_book)
            }

            itemView.setOnClickListener {
                onPageClickListener?.invoke(position)
            }
        }
    }
}
