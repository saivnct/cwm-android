package com.lgt.cwm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import com.lgt.cwm.R

class MaxHeightScrollView : ScrollView {
    private var maxHeight = -1

    constructor(context: Context) : super(context) {
        initialize(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(attrs)
    }

    private fun initialize(attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.MaxHeightScrollView, 0, 0)
            maxHeight = typedArray.getDimensionPixelOffset(R.styleable.MaxHeightScrollView_scrollView_maxHeight, -1)
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasure = heightMeasureSpec
        if (maxHeight >= 0) {
            heightMeasure = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        }
        super.onMeasure(widthMeasureSpec, heightMeasure)
    }
}