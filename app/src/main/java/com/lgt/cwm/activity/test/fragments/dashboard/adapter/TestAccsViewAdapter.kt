package com.lgt.cwm.activity.test.fragments.dashboard.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.databinding.RowAccItemBinding
import com.lgt.cwm.db.entity.Account
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

/**
 * Created by giangtpu on 04/07/2022.
 */
@FragmentScoped
class TestAccsViewAdapter @Inject constructor():  RecyclerView.Adapter<TestAccsViewAdapter.AccsViewHolder>(){
    private var items: List<Account> = arrayListOf()
    private var listener: OnItemClickListener? = null

    fun setItems(items: List<Account>){
        this.items = items;
        notifyDataSetChanged();
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    class AccsViewHolder(val binding: RowAccItemBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(acc: Account) {
            binding.account = acc
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): AccsViewHolder {
        val context = viewGroup.context
        val binding: RowAccItemBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.row_acc_item, viewGroup, false)
        return AccsViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AccsViewHolder, position: Int) {
        val account = items[position]
        val binding = holder.binding
        holder.bind(account);

        binding.rlitem.setOnClickListener{
            this.listener?.onItemClick(account, position)
        }

        binding.btnActive.setOnClickListener {
            this.listener?.onItemActiveClick(account, position)
        }

        binding.btnTest.setOnClickListener {
            this.listener?.onItemTestClick(account, position)
        }


    }

    interface OnItemClickListener {
        fun onItemTestClick(item: Account, position: Int)
        fun onItemActiveClick(item: Account, position: Int)
        fun onItemClick(item: Account, position: Int)
    }


}