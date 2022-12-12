package com.lgt.cwm.activity.testchat.fragments.chat.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.databinding.RowWschatItemBinding
import com.lgt.cwm.ws.WSChatMsg
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

/**
 * Created by giangtpu on 7/6/22.
 */
@FragmentScoped
class TestChatViewAdapter @Inject constructor():  RecyclerView.Adapter<TestChatViewAdapter.TestChatViewHolder>() {
    private var items: MutableList<WSChatMsg> = mutableListOf()

    fun setItems(items: MutableList<WSChatMsg>){
        this.items = items
        notifyDataSetChanged()
    }

    fun addItem(item: WSChatMsg){
        this.items.add(item)
        notifyDataSetChanged()
    }

    class TestChatViewHolder(val binding: RowWschatItemBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(wsChatMsg: WSChatMsg) {
            binding.wsChatMsg = wsChatMsg
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TestChatViewHolder {
        val context = viewGroup.context
        val binding: RowWschatItemBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.row_wschat_item, viewGroup, false)
        return TestChatViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TestChatViewHolder, position: Int) {
        val wsChatMsg = items[position]
        val binding = holder.binding
        holder.bind(wsChatMsg);
    }

}