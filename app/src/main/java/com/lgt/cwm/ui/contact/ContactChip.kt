package com.lgt.cwm.ui.contact

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.chip.Chip
import com.lgt.cwm.activity.home.fragments.contact.models.SelectedContact
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.ui.avatar.AvatarConstants
import com.lgt.cwm.ui.avatar.AvatarGenerator
import com.lgt.cwm.ui.avatar.RandomColors
import com.lgt.cwm.util.ViewUtil

class ContactChip : Chip {
    var contact: Contact? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setAvatar(contact: Contact?) {
        if (contact != null) {
            val size = ViewUtil.dpToPx(24)
            //test size 24sp
            val avatar = AvatarGenerator.AvatarBuilder(context)
                .setLabel(contact.name)
                .setAvatarSize(size)
                .setTextSize(24)
                .setBackgroundColor(RandomColors(AvatarConstants.COLOR700).getColor(contact.name))
                .toCircle()
                .build()

            chipIcon = avatar
        }
    }
}