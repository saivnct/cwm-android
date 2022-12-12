package com.lgt.cwm.ui.setting.models


import android.view.View
import com.google.android.material.button.MaterialButton
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.fragments.models.PreferenceModel
import com.lgt.cwm.ui.adapter.mapping.LayoutFactory
import com.lgt.cwm.ui.adapter.mapping.MappingAdapter
import com.lgt.cwm.ui.adapter.mapping.MappingViewHolder
import com.lgt.cwm.ui.setting.ConversationSettingsIcon
import com.lgt.cwm.ui.setting.ConversationSettingsText

object Button {

    fun register(mappingAdapter: MappingAdapter) {
        mappingAdapter.registerFactory(Model.Primary::class.java, LayoutFactory({ ViewHolder(it) }, R.layout.conversation_settings_button_primary))
        mappingAdapter.registerFactory(Model.Tonal::class.java, LayoutFactory({ ViewHolder(it) }, R.layout.conversation_settings_button_tonal))
        mappingAdapter.registerFactory(Model.SecondaryNoOutline::class.java, LayoutFactory({ ViewHolder(it) }, R.layout.conversation_settings_button_secondary))
    }

    sealed class Model<T : Model<T>>(
        title: ConversationSettingsText?,
        icon: ConversationSettingsIcon?,
        isEnabled: Boolean,
        val onClick: () -> Unit
    ) : PreferenceModel<T>(
        title = title,
        icon = icon,
        isEnabled = isEnabled
    ) {
        class Primary(
            title: ConversationSettingsText?,
            icon: ConversationSettingsIcon?,
            isEnabled: Boolean,
            onClick: () -> Unit
        ) : Model<Primary>(title, icon, isEnabled, onClick)

        class Tonal(
            title: ConversationSettingsText?,
            icon: ConversationSettingsIcon?,
            isEnabled: Boolean,
            onClick: () -> Unit
        ) : Model<Tonal>(title, icon, isEnabled, onClick)

        class SecondaryNoOutline(
            title: ConversationSettingsText?,
            icon: ConversationSettingsIcon?,
            isEnabled: Boolean,
            onClick: () -> Unit
        ) : Model<SecondaryNoOutline>(title, icon, isEnabled, onClick)
    }

    class ViewHolder<T : Model<T>>(itemView: View) : MappingViewHolder<T>(itemView) {

        private val button: MaterialButton = itemView.findViewById(R.id.button)

        override fun bind(model: T) {
            button.text = model.title?.resolve(context)
            button.setOnClickListener {
                model.onClick()
            }
            button.icon = model.icon?.resolve(context)
            button.isEnabled = model.isEnabled
        }
    }
}
