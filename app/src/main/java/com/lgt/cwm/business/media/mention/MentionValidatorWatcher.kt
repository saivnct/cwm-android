package com.lgt.cwm.business.media.mention

import android.text.Annotation
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher

/**
 * Provides a mechanism to validate mention annotations set on an edit text. This enables
 * removing invalid mentions if the user mentioned isn't in the group.
 */
class MentionValidatorWatcher : TextWatcher {
    private var invalidMentionAnnotations: List<Annotation>? = null
    private var mentionValidator: MentionValidator? = null

    override fun onTextChanged(sequence: CharSequence, start: Int, before: Int, count: Int) {
        mentionValidator?.let {
            if (count > 1 && sequence is Spanned) {
                val mentionAnnotations = MentionAnnotation.getMentionAnnotations(sequence, start, start + count)
                if (mentionAnnotations.isNotEmpty()) {
                    invalidMentionAnnotations = it.getInvalidMentionAnnotations(mentionAnnotations)
                }
            }
        }
    }

    override fun afterTextChanged(editable: Editable) {
        if (invalidMentionAnnotations == null) {
            return
        }
        val invalidMentions: List<Annotation> = invalidMentionAnnotations!!
        invalidMentionAnnotations = null
        for (annotation in invalidMentions) {
            editable.removeSpan(annotation)
        }
    }

    fun setMentionValidator(mentionValidator: MentionValidator?) {
        this.mentionValidator = mentionValidator
    }

    override fun beforeTextChanged(sequence: CharSequence, start: Int, count: Int, after: Int) {}

    interface MentionValidator {
        fun getInvalidMentionAnnotations(mentionAnnotations: List<Annotation>): List<Annotation>
    }
}