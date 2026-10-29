package com.hairstyle.changer.aitryon.ui.creation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hairstyle.changer.aitryon.databinding.ItemUserCreationBinding
import com.hairstyle.changer.aitryon.domain.model.UserCreation
import java.io.File

class UserCreationAdapter(
    private val onItemClick: (UserCreation) -> Unit
) : ListAdapter<UserCreation, UserCreationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserCreationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemUserCreationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserCreation) {
            binding.ivCreation.load(File(item.localPath)) {
                crossfade(true)
            }
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<UserCreation>() {
        override fun areItemsTheSame(oldItem: UserCreation, newItem: UserCreation) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: UserCreation, newItem: UserCreation) =
            oldItem == newItem
    }
}
