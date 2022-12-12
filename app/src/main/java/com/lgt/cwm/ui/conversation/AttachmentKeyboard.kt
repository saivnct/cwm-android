package com.lgt.cwm.ui.conversation

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.databinding.AttachmentKeyboardBinding
import com.lgt.cwm.ui.conversation.adapter.AttachmentKeyboardButtonAdapter


class AttachmentKeyboard : FrameLayout {
    private lateinit var container: View
    private lateinit var buttonAdapter: AttachmentKeyboardButtonAdapter
    private var callback: Callback? = null
    private lateinit var mediaList: RecyclerView
    private lateinit var permissionText: View
    private lateinit var permissionButton: View

    companion object {
        private val DEFAULT_BUTTONS = listOf(
            AttachmentKeyboardButton.CAMERA,
            AttachmentKeyboardButton.GALLERY,
            AttachmentKeyboardButton.FILE,
            AttachmentKeyboardButton.CONTACT,
        )
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        val binding: AttachmentKeyboardBinding = AttachmentKeyboardBinding.inflate(LayoutInflater.from(context), this, true);
        container = binding.attachmentKeyboardContainer
        mediaList = binding.attachmentKeyboardMediaList
        permissionText = binding.attachmentKeyboardPermissionText
        permissionButton = binding.attachmentKeyboardPermissionButton
        val buttonList = binding.attachmentKeyboardButtonList

        mediaList.visibility = GONE

        buttonAdapter = AttachmentKeyboardButtonAdapter(object : AttachmentKeyboardButtonAdapter.Listener {
            override fun onClick(button: AttachmentKeyboardButton) {
                callback?.onAttachmentSelectorClicked(button)
            }
        })

        buttonList.adapter = buttonAdapter
        buttonAdapter.registerAdapterDataObserver(AttachmentButtonCenterHelper(buttonList))

        mediaList.layoutManager = GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false)

        buttonList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        buttonAdapter.setButtons(DEFAULT_BUTTONS)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

//    fun onMediaChanged(media: List<Media?>) {
//        if (StorageUtil.canReadFromMediaStore()) {
//            mediaAdapter.setMedia(media)
//            permissionButton.visibility = GONE
//            permissionText.visibility = GONE
//        } else {
//            permissionButton.visibility = VISIBLE
//            permissionText.visibility = VISIBLE
//            permissionButton.setOnClickListener { v: View? ->
//                if (callback != null) {
//                    callback.onAttachmentPermissionsRequested()
//                }
//            }
//        }
//    }

    fun show(height: Int) {
        val params = layoutParams
        params.height = height
        layoutParams = params
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    val isShowing: Boolean get() = visibility == VISIBLE

    interface Callback {
        fun onAttachmentSelectorClicked(button: AttachmentKeyboardButton)
        fun onAttachmentPermissionsRequested()
    }
}