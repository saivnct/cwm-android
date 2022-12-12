package com.lgt.cwm.activity.conversation.fragments.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.databinding.ContactShareSelectionItemBinding
import com.lgt.cwm.models.SignalThreadExt
import com.lgt.cwm.ui.avatar.AvatarConstants
import com.lgt.cwm.ui.avatar.AvatarGenerator
import com.lgt.cwm.ui.avatar.RandomColors
import com.lgt.cwm.util.Constants
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.ViewUtil
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject


@FragmentScoped
class ContactShareSelectionAdapter @Inject constructor(): ListAdapter<SignalThreadExt, ContactShareSelectionAdapter.ContactShareSelectionViewHolder>(ContactShareSelectionCallback()){
    private lateinit var context: Context
    @Inject
    lateinit var debugConfig: DebugConfig

    private var listener: OnItemClickListener? = null

    private var selectedItems: ArrayList<String> = arrayListOf()

    private class ContactShareSelectionCallback : DiffUtil.ItemCallback<SignalThreadExt>() {

        override fun areItemsTheSame(oldItem: SignalThreadExt, newItem: SignalThreadExt): Boolean {
            return oldItem.threadId.equals(newItem.threadId)
        }

        //todo: check selected status here
        override fun areContentsTheSame(oldItem: SignalThreadExt, newItem: SignalThreadExt): Boolean {
            return oldItem.isContentTheSameWith(newItem)
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    inner class ContactShareSelectionViewHolder(val binding: ContactShareSelectionItemBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(item: SignalThreadExt) {
            binding.signalThreadExt = item
            val size = ViewUtil.dpToPx(48)
            val avatar = AvatarGenerator.AvatarBuilder(context)
                .setLabel(item.threadName)
                .setAvatarSize(size)
                .setTextSize(24)
                .setBackgroundColor(RandomColors(AvatarConstants.COLOR700).getColor(item.threadName))
                .toCircle()
                .build()

            binding.avatar = avatar

            binding.executePendingBindings()
        }
    }


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ContactShareSelectionViewHolder {
        context = viewGroup.context
        val binding: ContactShareSelectionItemBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.contact_share_selection_item, viewGroup, false)
        return ContactShareSelectionViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return super.getItemCount()
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ContactShareSelectionViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        binding.checkBox.visibility = View.VISIBLE

        val isChecked = selectedItems.contains(item.threadId)
        binding.checkBox.isChecked = isChecked


        binding.viewContactListItem.setOnClickListener {
            val isSelected = selectedItems.contains(item.threadId)
            var success = false
            if (isSelected){
                success = removeSelectedItem(item.threadId)
            }else{
                success = addSelectedItem(item.threadId)
            }

            if (success){
                listener?.onItemActiveClick(item, position, !isSelected)
            }
        }

        holder.bind(item);
    }

    fun removeSelectedItem(threadId: String): Boolean{
        selectedItems.remove(threadId)
        notifyDataSetChanged()
        return true
    }

    fun addSelectedItem(contactId: String): Boolean{
        if (selectedItems.size <= Constants.MAXIMUM_FORWARD) {
            selectedItems.add(contactId)
            notifyDataSetChanged()
            return true
        }
        return false
    }



    interface OnItemClickListener {
        fun onItemActiveClick(item: SignalThreadExt, position: Int, selected: Boolean)
    }
}