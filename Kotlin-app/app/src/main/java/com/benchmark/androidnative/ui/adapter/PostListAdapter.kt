package com.benchmark.androidnative.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.benchmark.androidnative.databinding.ItemListTileBinding
import com.benchmark.androidnative.model.PostItem

class PostListAdapter : RecyclerView.Adapter<PostListAdapter.ViewHolder>() {

    private val items = mutableListOf<PostItem>()

    fun submitList(newItems: List<PostItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListTileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemListTileBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostItem, number: Int) {
            binding.tvAvatarNumber.text = number.toString()
            binding.tvTitle.text = post.title
            binding.tvSubtitle.text = post.body
        }
    }
}
