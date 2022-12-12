package com.lgt.cwm.ui.conversation.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.ui.conversation.AttachmentKeyboardButton


internal class AttachmentKeyboardButtonAdapter(listener: Listener) : RecyclerView.Adapter<AttachmentKeyboardButtonAdapter.ButtonViewHolder>() {
    private val buttons: MutableList<AttachmentKeyboardButton>
    private val listener: Listener

    init {
        buttons = ArrayList()
        this.listener = listener
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return buttons[position].titleRes.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        return ButtonViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.attachment_keyboard_button_item, parent, false))
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bind(buttons[position], listener)
    }

    override fun onViewRecycled(holder: ButtonViewHolder) {
        holder.recycle()
    }

    override fun getItemCount(): Int {
        return buttons.size
    }

    fun setButtons(buttons: List<AttachmentKeyboardButton>) {
        this.buttons.clear()
        this.buttons.addAll(buttons)
        notifyDataSetChanged()
    }

    internal interface Listener {
        fun onClick(button: AttachmentKeyboardButton)
    }

    internal class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView
        private val title: TextView

        init {
            image = itemView.findViewById(R.id.icon)
            title = itemView.findViewById(R.id.label)
        }

        fun bind(button: AttachmentKeyboardButton, listener: Listener) {
            image.setImageResource(button.iconRes)

            val unwrappedDrawable = AppCompatResources.getDrawable(itemView.context, R.drawable.icon_button_squircle)
            val wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable!!)
            when (button) {
                AttachmentKeyboardButton.CAMERA -> { DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(itemView.context, R.color.attachment_keyboard_button_color_1)) }
                AttachmentKeyboardButton.GALLERY -> { DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(itemView.context, R.color.attachment_keyboard_button_color_2)) }
                AttachmentKeyboardButton.FILE -> { DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(itemView.context, R.color.attachment_keyboard_button_color_3)) }
                AttachmentKeyboardButton.CONTACT -> { DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(itemView.context, R.color.attachment_keyboard_button_color_4)) }
            }
            image.background = wrappedDrawable

            title.setText(button.titleRes)
            title.visibility = GONE
            itemView.setOnClickListener { listener.onClick(button) }
        }

        fun recycle() {
            itemView.setOnClickListener(null)
        }
    }

}