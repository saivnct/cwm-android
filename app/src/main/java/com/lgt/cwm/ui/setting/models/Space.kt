package com.lgt.cwm.ui.setting.models

import android.view.View
import androidx.annotation.Px
import androidx.core.view.updateLayoutParams
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.fragments.models.PreferenceModel
import com.lgt.cwm.ui.adapter.mapping.LayoutFactory
import com.lgt.cwm.ui.adapter.mapping.MappingAdapter
import com.lgt.cwm.ui.adapter.mapping.MappingViewHolder

/**
 * Adds extra space between elements in a DSL fragment
 */
data class Space(
    @Px val pixels: Int
) {

    companion object {
        fun register(mappingAdapter: MappingAdapter) {
            mappingAdapter.registerFactory(Model::class.java, LayoutFactory({ ViewHolder(it) }, R.layout.conversation_settings_space_preference))
        }
    }

    class Model(val space: Space) : PreferenceModel<Model>() {
        override fun areItemsTheSame(newItem: Model): Boolean {
            return true
        }

        override fun areContentsTheSame(newItem: Model): Boolean {
            return super.areContentsTheSame(newItem) && newItem.space == space
        }
    }

    class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {
        override fun bind(model: Model) {
            itemView.updateLayoutParams {
                height = model.space.pixels
            }
        }
    }
}
