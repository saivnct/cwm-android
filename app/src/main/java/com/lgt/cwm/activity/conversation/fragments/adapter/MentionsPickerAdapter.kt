package com.lgt.cwm.activity.conversation.fragments.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.databinding.MentionsPickerListItemBinding
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.models.ThreadParticipantInfo
import com.lgt.cwm.ui.avatar.AvatarConstants
import com.lgt.cwm.ui.avatar.AvatarGenerator
import com.lgt.cwm.ui.avatar.RandomColors
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.ViewUtil
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject


@FragmentScoped
class MentionsPickerAdapter @Inject constructor(): ListAdapter<ThreadParticipantInfo, MentionsPickerAdapter.MentionsPickerViewHolder>(MentionsPickerCallback()){
    private lateinit var context: Context
    @Inject
    lateinit var debugConfig: DebugConfig

    private var listener: OnItemClickListener? = null

    private class MentionsPickerCallback : DiffUtil.ItemCallback<ThreadParticipantInfo>() {

        override fun areItemsTheSame(oldItem: ThreadParticipantInfo, newItem: ThreadParticipantInfo): Boolean {
            return oldItem.phoneFull == newItem.phoneFull
        }

        override fun areContentsTheSame(oldItem: ThreadParticipantInfo, newItem: ThreadParticipantInfo): Boolean {
            return oldItem.equals(newItem)
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    inner class MentionsPickerViewHolder(val binding: MentionsPickerListItemBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(item: ThreadParticipantInfo) {
            val size = ViewUtil.dpToPx(36)
            val avatar = AvatarGenerator.AvatarBuilder(context)
                .setLabel(item.getName(context))
                .setAvatarSize(size)
                .setTextSize(24)
                .setBackgroundColor(RandomColors(AvatarConstants.COLOR700).getColor(item.getName(context)))
                .toCircle()
                .build()

            binding.avatar = avatar

            binding.executePendingBindings()
        }
    }


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MentionsPickerViewHolder {
        context = viewGroup.context
        val binding: MentionsPickerListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.mentions_picker_list_item, viewGroup, false)
        return MentionsPickerViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return super.getItemCount()
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MentionsPickerViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        binding.container.setOnClickListener {
            listener?.onItemActiveClick(item, position)
        }

        binding.recipientViewName.text = item.getName(context)

        holder.bind(item);
    }


    interface OnItemClickListener {
        fun onItemActiveClick(item: ThreadParticipantInfo, position: Int)
    }
}