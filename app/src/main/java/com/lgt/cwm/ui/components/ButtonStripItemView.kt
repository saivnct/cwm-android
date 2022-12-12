package com.lgt.cwm.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import com.lgt.cwm.R
import com.lgt.cwm.databinding.ButtonStripItemViewBinding

class ButtonStripItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val iconView: ImageView
    private val labelView: TextView

    init {
        val binding: ButtonStripItemViewBinding =
            ButtonStripItemViewBinding.inflate(LayoutInflater.from(context), this, true);

        iconView = binding.icon
        labelView = binding.label

        val array = context.obtainStyledAttributes(attrs, R.styleable.ButtonStripItemView)

        val iconId = array.getResourceId(R.styleable.ButtonStripItemView_bsiv_icon, -1)
        val icon: Drawable? = if (iconId > 0) AppCompatResources.getDrawable(context, iconId) else null

        val contentDescription = array.getString(R.styleable.ButtonStripItemView_bsiv_icon_contentDescription)
        val label = array.getString(R.styleable.ButtonStripItemView_bsiv_label)

        iconView.setImageDrawable(icon)
        iconView.contentDescription = contentDescription
        labelView.text = label

        array.recycle()
    }

    fun setOnIconClickedListener(onIconClickedListener: (() -> Unit)?) {
        iconView.setOnClickListener { onIconClickedListener?.invoke() }
    }
}

