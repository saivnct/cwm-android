package com.lgt.cwm.activity.conversation.fragments.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R

import com.lgt.cwm.databinding.ShareContactSelectionItemBinding
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

@FragmentScoped
class ShareSelectionAdapter @Inject constructor():  ListAdapter<String, ShareSelectionAdapter.ShareSelectionViewHolder>(ShareSelectionCallback()){
    private lateinit var context: Context

    @Inject
    lateinit var debugConfig: DebugConfig

    private class ShareSelectionCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }


    class ShareSelectionViewHolder(val binding: ShareContactSelectionItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind() {
            binding.executePendingBindings()
        }

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ShareSelectionViewHolder {
        context = viewGroup.context
        val binding: ShareContactSelectionItemBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.share_contact_selection_item, viewGroup, false)
        return ShareSelectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShareSelectionViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        if (position == 0) {
            binding.threadName.text = item
        } else {
            binding.threadName.text = ", $item"
        }

        holder.bind();
    }
}