package com.lgt.cwm.ui.popup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lgt.cwm.databinding.AttachmentKeyboardBottomSheetBinding
import com.lgt.cwm.ui.conversation.AttachmentKeyboard
import com.lgt.cwm.ui.conversation.AttachmentKeyboardButton

class ModalBottomSheetDialogFragment (val callback: AttachmentKeyboard.Callback) : BottomSheetDialogFragment(), AttachmentKeyboard.Callback {

    companion object {
        fun newInstance(callback: AttachmentKeyboard.Callback) = ModalBottomSheetDialogFragment(callback)
    }

    lateinit var binding: AttachmentKeyboardBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AttachmentKeyboardBottomSheetBinding.inflate(inflater, container, false)
//        setStyle(STYLE_NORMAL, R.style.TutorialBottomSheetDialog)
        binding.attachmentKeyboard.setCallback(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDialog()
        binding.marginDismiss.setOnClickListener {
            dismissNow()
        }

        requireDialog().setOnShowListener {
            val dialog = it as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.setBackgroundResource(android.R.color.transparent)
//                dialog.behavior.peekHeight = ViewUtil.dpToPx(200)
                dialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                sheet.parent.parent.requestLayout()
            }

        }
    }

    private fun initDialog() {
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

//        requireDialog().window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//        requireDialog().window?.statusBarColor = requireContext().getColor(android.R.color.transparent)
    }

    override fun onAttachmentSelectorClicked(button: AttachmentKeyboardButton) {
        dismiss()
        callback.onAttachmentSelectorClicked(button)
    }

    override fun onAttachmentPermissionsRequested() {
        callback.onAttachmentPermissionsRequested()
    }
}