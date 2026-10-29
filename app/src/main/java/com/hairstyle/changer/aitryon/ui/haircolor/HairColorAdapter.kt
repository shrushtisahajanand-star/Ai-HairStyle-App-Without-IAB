//package com.hairstyle.changer.aitryon.ui.haircolor
//
//import android.graphics.drawable.GradientDrawable
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.hairstyle.changer.aitryon.R
//import com.hairstyle.changer.aitryon.databinding.ItemHairColorBinding
//import com.hairstyle.changer.aitryon.domain.model.HairColor
//
//class HairColorAdapter(
//    private var selected: HairColor?,
//    private val onSelected: (HairColor) -> Unit
//) : ListAdapter<HairColor, HairColorAdapter.ViewHolder>(DiffCallback()) {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val binding = ItemHairColorBinding.inflate(
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
//        private val binding: ItemHairColorBinding
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(color: HairColor, isSelected: Boolean) {
//            binding.tvName.text = color.displayName
//
//            val drawable = GradientDrawable().apply {
//                shape = GradientDrawable.OVAL
//                setColor(color.colorHex.toInt())
//                setStroke(
//                    if (isSelected) 6 else 2,
//                    ContextCompat.getColor(
//                        binding.root.context,
//                        if (isSelected) R.color.primary else R.color.divider_light
//                    )
//                )
//            }
//            binding.colorSwatch.background = drawable
//
//            binding.root.strokeWidth = if (isSelected) 3 else 0
//            binding.root.strokeColor = ContextCompat.getColor(binding.root.context, R.color.primary)
//
//            binding.root.setOnClickListener {
//                val old = selected
//                selected = color
//                old?.let { notifyItemChanged(currentList.indexOf(it)) }
//                notifyItemChanged(this@ViewHolder.bindingAdapterPosition)
//                onSelected(color)
//            }
//        }
//    }
//
//    private class DiffCallback : DiffUtil.ItemCallback<HairColor>() {
//        override fun areItemsTheSame(oldItem: HairColor, newItem: HairColor) =
//            oldItem == newItem
//
//        override fun areContentsTheSame(oldItem: HairColor, newItem: HairColor) =
//            oldItem == newItem
//    }
//}
