package com.binunpacker.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExtractedAdapter(private var items: List<ExtractedEntry>) :
    RecyclerView.Adapter<ExtractedAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvDetails: TextView = view.findViewById(R.id.tvDetails)
    }

    fun updateItems(newItems: List<ExtractedEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_extracted, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = items[position]
        holder.tvType.text = "#${entry.index + 1}  ${entry.typeName}"
        holder.tvDetails.text =
            "Offset: ${entry.offset}   Size: ${formatSize(entry.length)}   (.${entry.extension})"
    }

    override fun getItemCount(): Int = items.size

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.2f MB", mb)
    }
}
