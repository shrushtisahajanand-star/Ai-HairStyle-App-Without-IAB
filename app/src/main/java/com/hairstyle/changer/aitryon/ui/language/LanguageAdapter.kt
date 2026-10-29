package com.hairstyle.changer.aitryon.ui.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val onLanguageClick: (LanguageItem) -> Unit
) : ListAdapter<LanguageItem, LanguageAdapter.LanguageViewHolder>(DiffCallback) {

    private var selectedCode: String = ""

    fun setSelectedCode(code: String) {
        if (selectedCode == code) return
        selectedCode = code
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(getItem(position), getItem(position).code == selectedCode)
    }

    inner class LanguageViewHolder(
        private val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LanguageItem, isSelected: Boolean) {
            binding.ivFlag.setImageResource(item.flagRes)
            binding.tvNativeName.text = item.nativeName
            binding.tvEnglishName.text = item.englishName
            binding.languageItemRoot.setBackgroundResource(
                if (isSelected) R.drawable.bg_language_item_selected
                else R.drawable.bg_create2
            )
            binding.ivRadio.setImageResource(
                if (isSelected) R.drawable.ic_language_radio_selected
                else R.drawable.ic_language_radio_unselected
            )
            binding.languageItemRoot.setOnClickListener {
                onLanguageClick(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<LanguageItem>() {
        override fun areItemsTheSame(oldItem: LanguageItem, newItem: LanguageItem): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: LanguageItem, newItem: LanguageItem): Boolean {
            return oldItem == newItem
        }
    }
}
