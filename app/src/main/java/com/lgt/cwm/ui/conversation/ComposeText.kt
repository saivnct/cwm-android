package com.lgt.cwm.ui.conversation

import android.content.Context
import android.graphics.Color
import android.text.*
import android.text.Annotation
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import com.lgt.cwm.activity.conversation.fragments.models.Mention
import com.lgt.cwm.business.media.mention.MentionAnnotation
import com.lgt.cwm.business.media.mention.MentionDeleter
import com.lgt.cwm.business.media.mention.MentionValidatorWatcher
import com.lgt.cwm.util.MentionUtil.MENTION_STARTER
import com.vanniktech.emoji.EmojiEditText

class ComposeText: EmojiEditText {

    private var mentionQueryChangedListener: MentionQueryChangedListener? = null
    private lateinit var mentionValidatorWatcher: MentionValidatorWatcher

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs) {
        initialize()
    }

    override fun onSelectionChanged(selectionStart: Int, selectionEnd: Int) {
        super.onSelectionChanged(selectionStart, selectionEnd)

        text?.let {
            val selectionChanged = changeSelectionForPartialMentions(it, selectionStart, selectionEnd)
            if (selectionChanged) {
                return
            }
            if (selectionStart == selectionEnd) {
                doAfterCursorChange(it)
            } else {
                updateQuery(null)
            }
        }

    }


    private fun initialize() {
        addTextChangedListener(MentionDeleter())
        mentionValidatorWatcher = MentionValidatorWatcher()
        addTextChangedListener(mentionValidatorWatcher)
    }

    fun getTextTrimmed(): CharSequence {
        val text = text ?: return ""
        return text.trim()
    }

    fun setMentionQueryChangedListener(listener: MentionQueryChangedListener?) {
        mentionQueryChangedListener = listener
    }

    fun setMentionValidator(mentionValidator: MentionValidatorWatcher.MentionValidator?) {
        mentionValidatorWatcher.setMentionValidator(mentionValidator)
    }

    fun hasMentions(): Boolean {
        text?.let {
            MentionAnnotation.getMentionAnnotations(it).isNotEmpty()
        }
        return false
    }

    fun getMentions(): List<Mention> {
        return MentionAnnotation.getMentionsFromAnnotations(text)
    }

    private fun changeSelectionForPartialMentions(spanned: Spanned, selectionStart: Int, selectionEnd: Int): Boolean {
        val annotations = spanned.getSpans(0, spanned.length, Annotation::class.java)
        for (annotation in annotations) {
            if (MentionAnnotation.isMentionAnnotation(annotation)) {
                val spanStart = spanned.getSpanStart(annotation)
                val spanEnd = spanned.getSpanEnd(annotation)
                val startInMention = selectionStart in (spanStart + 1) until spanEnd
                val endInMention = selectionEnd in (spanStart + 1) until spanEnd
                if (startInMention || endInMention) {
                    if (selectionStart == selectionEnd) {
                        setSelection(spanEnd, spanEnd)
                    } else {
                        val newStart = if (startInMention) spanStart else selectionStart
                        val newEnd = if (endInMention) spanEnd else selectionEnd
                        setSelection(newStart, newEnd)
                    }
                    return true
                }
            }
        }
        return false
    }

    private fun doAfterCursorChange(text: Editable) {
        if (enoughToFilter(text)) {
            performFiltering(text)
        } else {
            updateQuery(null)
        }
    }

    private fun performFiltering(text: Editable) {
        val end = selectionEnd
        val start: Int = findQueryStart(text, end)
        val query = text.subSequence(start, end)
        updateQuery(query.toString())
    }

    private fun updateQuery(query: String?) {
        mentionQueryChangedListener?.onQueryChanged(query)
    }

    private fun enoughToFilter(text: Editable): Boolean {
        val end = selectionEnd
        return if (end < 0) {
            false
        } else findQueryStart(text, end) != -1
    }

    fun replaceTextWithMention(displayName: String, recipientId: String) {
        val text = text ?: return
        clearComposingText()
        val end = selectionEnd
        val start: Int = findQueryStart(text, end) - 1
        text.replace(start, end, createReplacementToken(displayName, recipientId))
    }

    private fun createReplacementToken(text: CharSequence, recipientId: String): CharSequence {
        val builder: SpannableStringBuilder = SpannableStringBuilder().append(MENTION_STARTER)
        if (text is Spanned) {
            val spannableString = SpannableString("$text ")
            TextUtils.copySpansFrom(
                text, 0, text.length,
                Any::class.java, spannableString, 0
            )
            builder.append(spannableString)
        } else {
            builder.append(text).append(" ")
        }
        builder.setSpan(
            MentionAnnotation.mentionAnnotationForRecipientId(recipientId, text.toString()),
            0,
            builder.length - 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.setSpan(
            ForegroundColorSpan(Color.BLUE),
            1,
            builder.length - 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return builder
    }

    private fun findQueryStart(text: CharSequence, inputCursorPosition: Int): Int {
        if (inputCursorPosition == 0) {
            return -1
        }
        var delimiterSearchIndex = inputCursorPosition - 1
        while (delimiterSearchIndex >= 0 && text[delimiterSearchIndex] != MENTION_STARTER && text[delimiterSearchIndex] != ' ') {
            delimiterSearchIndex--
        }
        return if (delimiterSearchIndex >= 0 && text[delimiterSearchIndex] == MENTION_STARTER) {
            delimiterSearchIndex + 1
        } else -1
    }


    interface MentionQueryChangedListener {
        fun onQueryChanged(query: String?)
    }
}