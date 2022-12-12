package com.lgt.cwm.activity.home.fragments.call.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.activity.home.fragments.call.models.CallLog
import com.lgt.cwm.databinding.CallLogItemBinding
import com.lgt.cwm.ui.avatar.AvatarConstants
import com.lgt.cwm.ui.avatar.AvatarGenerator
import com.lgt.cwm.ui.avatar.RandomColors
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.ViewUtil
import dagger.hilt.android.scopes.FragmentScoped
import kotlinx.android.synthetic.main.conversation_item_header.view.*
import javax.inject.Inject


@FragmentScoped
class CallLogAdapter @Inject constructor():  RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder>(){

    @Inject
    lateinit var debugConfig: DebugConfig

    private lateinit var context: Context

    private var items: List<CallLog> = arrayListOf()
    private var listener: OnItemClickListener? = null

    fun setItems(items: List<CallLog>){
        this.items = items;
        notifyDataSetChanged();
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    class CallLogViewHolder(val binding: CallLogItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(item: CallLog) {
            binding.callLog = item
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): CallLogViewHolder {
        context = viewGroup.context
        val binding: CallLogItemBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.call_log_item, viewGroup, false)
        return CallLogViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding
        var name = "-"
        item.name?.let {
            name = it
        }

        val size = ViewUtil.dpToPx(48)
        //test size 24sp
        val avatar = AvatarGenerator.AvatarBuilder(context)
            .setLabel(name)
            .setAvatarSize(size)
            .setTextSize(24)
            .setBackgroundColor(RandomColors(AvatarConstants.COLOR700).getColor(name))
            .toCircle()
            .build()


        binding.imageViewAvatar.setImageDrawable(avatar)
        binding.callLogItemName.text = "Call log name"
        binding.callLogItemType.text = "Incoming call" + "(32s)"
        binding.callLogItemDate.text = "17/05"

        binding.viewCallLogItem.setOnClickListener {
            listener?.onItemActiveClick(item, position)
        }

        holder.bind(item);
    }

    interface OnItemClickListener {
        fun onItemActiveClick(item: CallLog, position: Int)
    }
}