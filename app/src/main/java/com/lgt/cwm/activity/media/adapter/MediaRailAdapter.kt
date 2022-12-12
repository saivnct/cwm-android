package com.lgt.cwm.activity.media.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.activity.media.models.MediaFileInfo
import com.lgt.cwm.ui.conversation.media.ThumbnailView
import com.lgt.cwm.ui.glide.GlideRequests
import com.lgt.cwm.util.adapter.StableIdGenerator

class MediaRailAdapter(glideRequests: GlideRequests, listener: RailItemListener,
                       editable: Boolean) : RecyclerView.Adapter<MediaRailAdapter.MediaRailViewHolder>() {
    private val media: MutableList<MediaFileInfo> = mutableListOf()
    private val stableIdGenerator: StableIdGenerator<MediaFileInfo> = StableIdGenerator()

    private var glideRequests: GlideRequests
    private var listener: RailItemListener

    private var addListener: RailItemAddListener? = null
    private var activePosition = 0
    private var interactive: Boolean
    private var editable = false

    companion object {
        private const val TYPE_MEDIA = 1
        private const val TYPE_BUTTON = 2
    }

    init {
        this.glideRequests = glideRequests
        this.listener = listener
        this.editable = editable
        interactive = true
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): MediaRailViewHolder {
        return when (type) {
            TYPE_MEDIA -> MediaViewHolder(
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.mediarail_media_item, viewGroup, false)
            )
            TYPE_BUTTON -> ButtonViewHolder(
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.mediarail_button_item, viewGroup, false)
            )
            else -> throw UnsupportedOperationException("Unsupported view type: $type")
        }
    }

    override fun onBindViewHolder(viewHolder: MediaRailViewHolder, i: Int) {
        when (getItemViewType(i)) {
            TYPE_MEDIA -> (viewHolder as MediaViewHolder).bind(
                media[i],
                i == activePosition,
                this.glideRequests,
                listener,
                i - activePosition,
                editable,
                interactive
            )
            TYPE_BUTTON -> (viewHolder as ButtonViewHolder).bind(addListener)
            else -> throw UnsupportedOperationException("Unsupported view type: " + getItemViewType(i))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (editable && position == itemCount - 1) {
            TYPE_BUTTON
        } else {
            TYPE_MEDIA
        }
    }

    override fun onViewRecycled(holder: MediaRailViewHolder) {
        holder.recycle()
    }

    override fun getItemCount(): Int {
        return if (editable) media.size + 1 else media.size
    }

    override fun getItemId(position: Int): Long {
        return when (getItemViewType(position)) {
            TYPE_MEDIA -> stableIdGenerator.getId(
                media[position]
            )
            TYPE_BUTTON -> Long.MAX_VALUE
            else -> throw UnsupportedOperationException(
                "Unsupported view type: " + getItemViewType(
                    position
                )
            )
        }
    }

    fun setMedia(media: List<MediaFileInfo>) {
        setMedia(media, activePosition)
    }

    fun setMedia(records: List<MediaFileInfo>, activePosition: Int) {
        this.activePosition = activePosition
        media.clear()
        media.addAll(records)
        notifyDataSetChanged()
    }

    fun setActivePosition(activePosition: Int) {
        this.activePosition = activePosition
        notifyDataSetChanged()
    }

    fun setAddButtonListener(addListener: RailItemAddListener?) {
        this.addListener = addListener
        notifyDataSetChanged()
    }

    fun setEditable(editable: Boolean) {
        this.editable = editable
        notifyDataSetChanged()
    }

    fun setInteractive(interactive: Boolean) {
        this.interactive = interactive
        notifyDataSetChanged()
    }

    abstract class MediaRailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun recycle()
    }

    internal class MediaViewHolder(itemView: View) : MediaRailViewHolder(itemView) {
        private val image: ThumbnailView
        private val outline: View
        private val deleteButton: View
        private val captionIndicator: View

        init {
            image = itemView.findViewById(R.id.rail_item_image)
            outline = itemView.findViewById(R.id.rail_item_outline)
            deleteButton = itemView.findViewById(R.id.rail_item_delete)
            captionIndicator = itemView.findViewById(R.id.rail_item_caption)
        }

        fun bind(media: MediaFileInfo, isActive: Boolean, glideRequests: GlideRequests, railItemListener: RailItemListener, distanceFromActive: Int, editable: Boolean, interactive: Boolean) {
            media.fileUri?.let {  image.setImageResource(glideRequests, it) }
            image.setOnClickListener {
                railItemListener.onRailItemClicked(distanceFromActive)
            }
            outline.visibility = if (isActive && interactive) View.VISIBLE else View.GONE
            captionIndicator.visibility = View.GONE
//            captionIndicator.visibility =
//                if (media.getCaption().isPresent()) View.VISIBLE else View.GONE
            if (editable && isActive && interactive) {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener { railItemListener.onRailItemDeleteClicked(distanceFromActive) }
            } else {
                deleteButton.visibility = View.GONE
            }
        }

        override fun recycle() {
            image.setOnClickListener(null)
            deleteButton.setOnClickListener(null)
        }
    }

    internal class ButtonViewHolder(itemView: View) : MediaRailViewHolder(itemView) {
        fun bind(addListener: RailItemAddListener?) {
            if (addListener != null) {
                itemView.setOnClickListener { addListener.onRailItemAddClicked() }
            }
        }

        override fun recycle() {
            itemView.setOnClickListener(null)
        }
    }

    interface RailItemListener {
        fun onRailItemClicked(distanceFromActive: Int)
        fun onRailItemDeleteClicked(distanceFromActive: Int)
    }

    interface RailItemAddListener {
        fun onRailItemAddClicked()
    }
}