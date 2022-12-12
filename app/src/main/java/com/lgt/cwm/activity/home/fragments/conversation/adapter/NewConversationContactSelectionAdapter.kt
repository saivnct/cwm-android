package com.lgt.cwm.activity.home.fragments.conversation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.databinding.ContactSelectionListItemBinding
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.ui.avatar.AvatarConstants
import com.lgt.cwm.ui.avatar.AvatarGenerator
import com.lgt.cwm.ui.avatar.RandomColors
import com.lgt.cwm.ui.contact.LetterHeaderDecoration
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.ViewUtil
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject


@FragmentScoped
class NewConversationContactSelectionAdapter @Inject constructor():  RecyclerView.Adapter<NewConversationContactSelectionAdapter.ContactSelectionListViewHolder>(){
    private lateinit var context: Context
    @Inject
    lateinit var debugConfig: DebugConfig

    private var items: List<Contact> = arrayListOf()
    private var listener: OnItemClickListener? = null

    fun setItems(items: List<Contact>){
        this.items = items.sortedBy { it.name };
        notifyDataSetChanged();
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    inner class ContactSelectionListViewHolder(val binding: ContactSelectionListItemBinding) : RecyclerView.ViewHolder(binding.root),
        LetterHeaderDecoration.LetterHeaderItem {
        private var letterHeader: String? = null

        fun bind(item: Contact) {

            binding.contact = item
            val size = ViewUtil.dpToPx(48)
            val avatar = AvatarGenerator.AvatarBuilder(context)
                .setLabel(item.name)
                .setAvatarSize(size)
                .setTextSize(24)
                .setBackgroundColor(RandomColors(AvatarConstants.COLOR700).getColor(item.name))
                .toCircle()
                .build()

            binding.avatar = avatar

            binding.executePendingBindings()
        }

        override fun getHeaderLetter(): String? {
            return letterHeader
        }

        fun setLetterHeaderCharacter(letterHeaderCharacter: String?) {
            letterHeader = letterHeaderCharacter
        }
    }


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ContactSelectionListViewHolder {
        context = viewGroup.context
        val binding: ContactSelectionListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.contact_selection_list_item, viewGroup, false)
        return ContactSelectionListViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun isPositionHeader(position: Int): Boolean {
        if (position == 0) return true
        val current = items[position].name.first()
        val previous = items[position - 1].name.first()
        return !current.toString().equals(previous.toString(), ignoreCase = true)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ContactSelectionListViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding

        val isHeader = isPositionHeader(position)

        if (isHeader) {
            //set section letter
            holder.setLetterHeaderCharacter(item.getNameHeader())
        } else {
            holder.setLetterHeaderCharacter(null)
        }

        //not use checkbox
        binding.checkBox.visibility = View.GONE

        binding.viewContactListItem.setOnClickListener {
            listener?.onItemActiveClick(item, position)
        }

        holder.bind(item);
    }

    interface OnItemClickListener {
        fun onItemActiveClick(item: Contact, position: Int)
    }
}