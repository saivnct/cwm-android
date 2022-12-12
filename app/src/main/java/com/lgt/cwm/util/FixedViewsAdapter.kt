package com.lgt.cwm.util

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class FixedViewsAdapter(vararg viewList: View) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val viewList: List<View>
    private var hidden = false


    init {
        this.viewList = listOf(*viewList)
    }

    override fun getItemCount(): Int {
        return if (hidden) 0 else viewList.size
    }

    /**
     * @return View type is the index.
     */
    override fun getItemViewType(position: Int): Int {
        return position
    }

    /**
     * @param viewType The index in the list of views.
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(viewList[viewType]) {}
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {}

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun hide() {
        setHidden(true)
    }

    fun show() {
        setHidden(false)
    }

    private fun setHidden(hidden: Boolean) {
        if (this.hidden != hidden) {
            this.hidden = hidden
            if (hidden) {
                notifyItemRangeRemoved(0, viewList.size)
            } else {
                notifyItemRangeInserted(0, viewList.size)
            }
        }
    }

}