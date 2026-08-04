//package com.hairstyle.generator.aimakerover.ui.hairstyle
//
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.hairstyle.generator.aimakeover.R
//import com.hairstyle.generator.aimakeover.databinding.ItemHairstyleBinding
//import com.hairstyle.generator.aimakerover.domain.model.Hairstyle
//
//class HairstyleAdapter(
//    private var selected: Hairstyle?,
//    private val onSelected: (Hairstyle) -> Unit
//) : ListAdapter<Hairstyle, HairstyleAdapter.ViewHolder>(DiffCallback()) {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val binding = ItemHairstyleBinding.inflate(
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//        return ViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        holder.bind(getItem(position), getItem(position) == selected)
//    }
//
//    inner class ViewHolder(
//        private val binding: ItemHairstyleBinding
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(hairstyle: Hairstyle, isSelected: Boolean) {
//            binding.tvName.text = hairstyle.displayName
//            binding.root.strokeWidth = if (isSelected) 4 else 0
//            binding.root.strokeColor = ContextCompat.getColor(
//                binding.root.context,
//                R.color.primary
//            )
//            binding.root.setCardBackgroundColor(
//                ContextCompat.getColor(
//                    binding.root.context,
//                    if (isSelected) R.color.primary_container else R.color.surface_variant
//                )
//            )
//            binding.root.setOnClickListener {
//                val old = selected
//                selected = hairstyle
//                old?.let { notifyItemChanged(currentList.indexOf(it)) }
//                notifyItemChanged(this@ViewHolder.bindingAdapterPosition)
//                onSelected(hairstyle)
//            }
//        }
//    }
//
//    private class DiffCallback : DiffUtil.ItemCallback<Hairstyle>() {
//        override fun areItemsTheSame(oldItem: Hairstyle, newItem: Hairstyle) =
//            oldItem == newItem
//
//        override fun areContentsTheSame(oldItem: Hairstyle, newItem: Hairstyle) =
//            oldItem == newItem
//    }
//}
