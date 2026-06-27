package com.sitotv.iptv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sitotv.iptv.R
import com.sitotv.iptv.models.Category
import com.sitotv.iptv.models.PlaylistEntity

// ─── Category sidebar ─────────────────────────────────────────────────────────
class CategoryAdapter(
    private val onClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.VH>(DIFF) {

    private var selectedPos = 0

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView  = itemView.findViewById(R.id.tvCategoryName)
        val count: TextView = itemView.findViewById(R.id.tvCategoryCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = getItem(position)
        holder.name.text  = cat.name
        holder.count.text = if (cat.count > 0) "(${cat.count})" else ""

        val isSelected = position == selectedPos
        holder.itemView.isSelected = isSelected
        holder.itemView.alpha = if (isSelected) 1.0f else 0.7f

        holder.itemView.setOnClickListener {
            val prev = selectedPos
            selectedPos = holder.adapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPos)
            onClick(cat)
        }

        holder.itemView.setOnFocusChangeListener { v, focused ->
            v.alpha = if (focused || isSelected) 1.0f else 0.7f
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(a: Category, b: Category) = a.name == b.name
            override fun areContentsTheSame(a: Category, b: Category) = a == b
        }
    }
}

// ─── Playlist selector ────────────────────────────────────────────────────────
class PlaylistAdapter(
    private val onSelect: (PlaylistEntity) -> Unit,
    private val onDelete: (PlaylistEntity) -> Unit
) : ListAdapter<PlaylistEntity, PlaylistAdapter.VH>(DIFF2) {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvPlaylistName)
        val type: TextView = itemView.findViewById(R.id.tvPlaylistType)
        val del: View      = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pl = getItem(position)
        holder.name.text = pl.name
        holder.type.text = when (pl.type) {
            "m3u_url"  -> "M3U URL"
            "m3u_file" -> "M3U Fichier"
            "xtream"   -> "Xtream Codes"
            else       -> pl.type
        }
        holder.itemView.setOnClickListener { onSelect(pl) }
        holder.del.setOnClickListener { onDelete(pl) }

        holder.itemView.setOnFocusChangeListener { v, focused ->
            v.scaleX = if (focused) 1.05f else 1f
            v.scaleY = if (focused) 1.05f else 1f
        }
    }

    companion object {
        val DIFF2 = object : DiffUtil.ItemCallback<PlaylistEntity>() {
            override fun areItemsTheSame(a: PlaylistEntity, b: PlaylistEntity) = a.id == b.id
            override fun areContentsTheSame(a: PlaylistEntity, b: PlaylistEntity) = a == b
        }
    }
}
