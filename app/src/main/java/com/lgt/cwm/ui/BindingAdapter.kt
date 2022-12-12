package com.lgt.cwm.ui

import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.lgt.cwm.R
import com.lgt.cwm.util.DateUtil


/**
 * Created by giangtpu on 04/07/2022.
 */
@BindingAdapter("app:error")
fun edtError(editText: EditText, error: String) {
    if (error.isNotBlank()){
        editText.error = error
    }else{
        editText.error = null
    }
}

@BindingAdapter("app:time")
fun txtViewTimeString(textView: TextView, time: Long?) {
    if (time != null){
        textView.text = DateUtil.convertLongToTime(time)
    }else{
        textView.text = ""
    }
}

@BindingAdapter("app:accActive")
fun btnViewBgAccActive(btn: Button, isActive: Boolean) {
    val context = btn.context

    if (isActive){
        btn.background = context.getDrawable(R.drawable.bg_color_button)
    }else{
        btn.background = context.getDrawable(R.drawable.bg_color_button_disable)
    }
}

@BindingAdapter("app:visible")
fun viewVisible(view: View, isShow: Boolean) {
    val context = view.context

    if (isShow){
        view.visibility = View.VISIBLE
    }else{
        view.visibility = View.GONE
    }
}

@BindingAdapter("bind:imageDrawableBitmap")
fun loadImage(iv: ImageView, bitmap: BitmapDrawable?) {
    iv.setImageDrawable(bitmap)
}


