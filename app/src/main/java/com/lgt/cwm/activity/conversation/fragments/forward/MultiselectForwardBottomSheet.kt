package com.lgt.cwm.activity.conversation.fragments.forward

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lgt.cwm.R
import com.lgt.cwm.databinding.MultiselectBottomSheetBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
open class MultiselectForwardBottomSheet : BottomSheetDialogFragment(), MultiselectForwardFragment.Callback {
    companion object {
        const val ARG_MESSAGES = "multiselect.forward.fragment.messages"
        const val RESULT_KEY = "result_key"
        const val RESULT_THREADS_SELECTION = "result_selection_threads"
        const val RESULT_MSGS_SELECTION = "result_selection_msgIds"
    }


    val peekHeightPercentage: Float = 1f

    @ColorInt
    protected var backgroundColor: Int = Color.TRANSPARENT

    lateinit var binding: MultiselectBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = MultiselectBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.peekHeight = (resources.displayMetrics.heightPixels * peekHeightPercentage).toInt()
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if (savedInstanceState == null) {
            val fragment = MultiselectForwardFragment()
            fragment.arguments = requireArguments()

            childFragmentManager.beginTransaction()
                .replace(R.id.multiselect_container, fragment)
                .commitAllowingStateLoss()

            fragment.setCallBack(this)
        }
    }

    override fun getContainer(): ViewGroup {
        return requireView().parent.parent.parent as ViewGroup
    }

    override fun getDialogBackgroundColor(): Int {
        return backgroundColor
    }

    override fun setResult(bundle: Bundle) {
        setFragmentResult(MultiselectForwardBottomSheet.RESULT_KEY, bundle)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    override fun onFinishForwardAction() {
    }

    override fun exitFlow() {
        dismissAllowingStateLoss()
    }

    override fun onSearchInputFocused() {
        (requireDialog() as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

}
