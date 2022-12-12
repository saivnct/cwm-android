package com.lgt.cwm.util

import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.lgt.cwm.databinding.NullRecyclerviewHeaderBinding
import kotlin.math.max

/**
 * A sticky header decoration for android's RecyclerView.
 * Currently only supports LinearLayoutManager in VERTICAL orientation.
 */
open class StickyHeaderDecoration(
    adapter: StickyHeaderAdapter<*>,
    renderInline: Boolean,
    sticky: Boolean,
    type: Int
) : ItemDecoration() {
    private val headerCache: MutableMap<Long, RecyclerView.ViewHolder>
    private val adapter: StickyHeaderAdapter<*>
    private val renderInline: Boolean
    private val sticky: Boolean
    private val type: Int

    /**
     * @param adapter the sticky header adapter to use
     */
    init {
        this.adapter = adapter
        this.headerCache = HashMap()
        this.renderInline = renderInline
        this.sticky = sticky
        this.type = type
    }

    companion object {
        private fun translatedChildPosition(parent: RecyclerView, position: Int): Int {
            return if (isReverseLayout(parent)) parent.childCount - 1 - position else position
        }

        private fun getChildY(child: View): Int {
            return child.y.toInt()
        }

        private fun isReverseLayout(parent: RecyclerView): Boolean {
            return parent.layoutManager is LinearLayoutManager &&
                    (parent.layoutManager as LinearLayoutManager?)!!.reverseLayout
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        var headerHeight = 0
        if (position != RecyclerView.NO_POSITION && hasHeader(parent, adapter, position)) {
            val header = getHeader(parent, adapter, position).itemView
            headerHeight = getHeaderHeightForLayout(header)
        }
        outRect.set(0, headerHeight, 0, 0)
    }

    protected fun hasHeader(
        parent: RecyclerView,
        adapter: StickyHeaderAdapter<*>,
        adapterPos: Int): Boolean {
        val headerId: Long = adapter.getHeaderId(adapterPos)
        if (headerId == StickyHeaderAdapter.NO_HEADER_ID) {
            return false
        }
        val isReverse = isReverseLayout(parent)
        val itemCount = adapter.getItemCount()
        if ((isReverse && adapterPos == itemCount - 1 && adapter.getHeaderId(adapterPos) != -1L) ||
            (!isReverse && adapterPos == 0)) {
            return true
        }
        val previous = adapterPos + if (isReverse) 1 else -1
        val previousHeaderId = adapter.getHeaderId(previous)
        return (previousHeaderId == StickyHeaderAdapter.NO_HEADER_ID) || (headerId != previousHeaderId)
    }

    protected fun getHeader(
        parent: RecyclerView,
        adapter: StickyHeaderAdapter<*>,
        position: Int): RecyclerView.ViewHolder {

        val key = adapter.getHeaderId(position)
        var headerHolder = headerCache[key]
        if (headerHolder == null) {
            if (key != StickyHeaderAdapter.NO_HEADER_ID) {
                headerHolder = adapter.onCreateHeaderViewHolder(parent, position, type)
                adapter.onBindHeaderViewHolder(headerHolder, position, type)
            }
            if (headerHolder == null) {
                val binding: NullRecyclerviewHeaderBinding = NullRecyclerviewHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                headerHolder = object : RecyclerView.ViewHolder(binding.root) {}
            }
            headerCache[key] = headerHolder
        }
        val header = headerHolder.itemView
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)
        val childWidth = ViewGroup.getChildMeasureSpec(widthSpec,
            parent.paddingLeft + parent.paddingRight, header.layoutParams.width)
        val childHeight = ViewGroup.getChildMeasureSpec(heightSpec,
            parent.paddingTop + parent.paddingBottom, header.layoutParams.height)
        header.measure(childWidth, childHeight)
        header.layout(0, 0, header.measuredWidth, header.measuredHeight)
        return headerHolder
    }

    /**
     * {@inheritDoc}
     */
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val count = parent.childCount
        var start = 0
        for (layoutPos in 0 until count) {
            val child = parent.getChildAt(translatedChildPosition(parent, layoutPos))
            val adapterPos = parent.getChildAdapterPosition(child)
            val key = adapter.getHeaderId(adapterPos)
            if (key == StickyHeaderAdapter.NO_HEADER_ID) {
                start = layoutPos + 1
            }
            if (adapterPos != RecyclerView.NO_POSITION && ((layoutPos == start && sticky) || hasHeader(parent, adapter, adapterPos))) {
                val header = getHeader(parent, adapter, adapterPos).itemView
                c.save()
                val left = parent.left
                val top = getHeaderTop(parent, child, header, adapterPos, layoutPos)
                c.translate(left.toFloat(), top.toFloat())
                header.draw(c)
                c.restore()
            }
        }
    }

    protected fun getHeaderTop(
        parent: RecyclerView, child: View, header: View, adapterPos: Int, layoutPos: Int): Int {
        val headerHeight = getHeaderHeightForLayout(header)
        var top = getChildY(child) - headerHeight
        if (sticky && layoutPos == 0) {
            val count = parent.childCount
            val currentId = adapter.getHeaderId(adapterPos)
            // find next view with header and compute the offscreen push if needed
            for (i in 1 until count) {
                val adapterPosHere = parent.getChildAdapterPosition(parent.getChildAt(translatedChildPosition(parent, i)))
                if (adapterPosHere != RecyclerView.NO_POSITION) {
                    val nextId = adapter.getHeaderId(adapterPosHere)
                    if (nextId != currentId) {
                        val next = parent.getChildAt(translatedChildPosition(parent, i))
                        val offset = getChildY(next) - (headerHeight + getHeader(parent, adapter, adapterPosHere).itemView.height)
                        return if (offset < 0) {
                            offset
                        } else {
                            break
                        }
                    }
                }
            }
            if (sticky) top = max(0, top)
        }
        return top
    }

    private fun getHeaderHeightForLayout(header: View): Int {
        return if (renderInline) 0 else header.height
    }

    /**
     * The adapter to assist the [StickyHeaderDecoration] in creating and binding the header views.
     */
    interface StickyHeaderAdapter<T : RecyclerView.ViewHolder> {
        companion object {
            const val NO_HEADER_ID = -1L
        }

        fun getItemCount(): Int

        /**
         * Returns the header id for the item at the given position.
         *
         *
         * Return [.NO_HEADER_ID] if it does not have one.
         *
         * @param position the item position
         * @return the header id
         */
        fun getHeaderId(position: Int): Long

        /**
         * Creates a new header ViewHolder.
         *
         *
         * Only called if getHeaderId returns [.NO_HEADER_ID].
         *
         * @param parent   the header's view parent
         * @param position position in the adapter
         * @return a view holder for the created view
         */
        fun onCreateHeaderViewHolder(parent: ViewGroup, position: Int, type: Int): T

        /**
         * Updates the header view to reflect the header data for the given position.
         *
         * @param viewHolder the header view holder
         * @param position   the header's item position
         */
        fun onBindHeaderViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int, type: Int)
    }

}