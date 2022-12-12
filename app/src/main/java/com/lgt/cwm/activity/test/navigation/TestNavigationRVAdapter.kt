package com.lgt.cwm.activity.test.navigation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import dagger.hilt.android.scopes.ActivityScoped

import kotlinx.android.synthetic.main.row_nav_drawer.view.*
import javax.inject.Inject

/**
 * Created by giangtpu on 6/29/22.
 */
@ActivityScoped
class TestNavigationRVAdapter @Inject constructor() :
    RecyclerView.Adapter<TestNavigationRVAdapter.NavigationItemViewHolder>() {

    private lateinit var context: Context

    private var currentPos: Int? = null
    private var items: ArrayList<TestNavigationItemModel> = arrayListOf()

    fun setNavItem(items: ArrayList<TestNavigationItemModel>){
        this.items = items;
        notifyDataSetChanged();
    }

    class NavigationItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NavigationItemViewHolder {
        context = parent.context
        val navItem = LayoutInflater.from(parent.context).inflate(R.layout.row_nav_drawer, parent, false)
        return NavigationItemViewHolder(navItem)
    }

    fun setSelectItem(value: Int?) {
        currentPos = value
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: NavigationItemViewHolder, position: Int) {
        // To highlight the selected Item, show different background color
        if (currentPos!=null && position == currentPos) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.selectedNavItemBackground))
            holder.itemView.tvNavTitle.setTextColor(ContextCompat.getColor(context, R.color.primaryColor))
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            holder.itemView.tvNavTitle.setTextColor(ContextCompat.getColor(context, R.color.primaryTextColor))
        }

        holder.itemView.tvNavTitle.text = context.getString(items[position].title)
        holder.itemView.imgNavIcon.setImageResource(items[position].icon)
    }
}