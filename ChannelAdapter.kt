package com.sitotv.iptv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sitotv.iptv.R
import com.sitotv.iptv.models.Channel

class ChannelAdapter(
    private val onClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelAdapter.VH>(DIFF) {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logo: ImageView = itemView.findViewById(R.id.ivLogo)
        val name: TextView  = itemView.findViewById(R.id.tvName)
        val group: TextView = itemView.findViewById(R.id.tvGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = getItem(position)
        holder.name.text  = ch.name
        holder.group.text = ch.group.ifEmpty { "—" }

        Glide.with(holder.itemView.context)
            .load(ch.logo)
            .placeholder(R.drawable.ic_tv_placeholder)
            .error(R.drawable.ic_tv_placeholder)
            .centerCrop()
            .into(holder.logo)

        holder.itemView.setOnClickListener { onClick(ch) }
        holder.itemView.setOnFocusChangeListener { v, focused ->
            v.scaleX = if (focused) 1.1f else 1f
            v.scaleY = if (focused) 1.1f else 1f
            v.elevation = if (focused) 8f else 0f
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Channel>() {
            override fun areItemsTheSame(a: Channel, b: Channel) = a.url == b.url
            override fun areContentsTheSame(a: Channel, b: Channel) = a == b
        }
    }
}
