package com.lgt.cwm.business.media.mention

import android.text.Annotation
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import com.annimon.stream.Stream
import com.lgt.cwm.activity.conversation.fragments.models.Mention

/**
 * This wraps an Android standard [Annotation] so it can leverage the built in
 * span parceling for copy/paste. The annotation span contains the mentioned recipient's
 * id (in numerical form).
 *
 * Note: Do not extend Annotation or the parceling behavior will be lost.
 */
object MentionAnnotation {
    const val MENTION_ANNOTATION = "mention_"

    fun mentionAnnotationForRecipientId(id: String, displayName: String): Annotation {
        return Annotation("$MENTION_ANNOTATION$displayName", id)
    }

    fun isMentionAnnotation(annotation: Annotation): Boolean {
        return annotation.key.startsWith(MENTION_ANNOTATION)
    }

    fun setMentionAnnotations(body: SpannableString, mentions: List<Mention>) {
        for (mention in mentions) {
            body.setSpan(
                mentionAnnotationForRecipientId(mention.recipientId, mention.displayName),
                mention.start,
                mention.start + mention.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun getMentionsFromAnnotations(text: CharSequence?): List<Mention> {
        if (text is Spanned) {
            return Stream.of(getMentionAnnotations(text))
                .map { annotation: Annotation ->
                    val spanStart = text.getSpanStart(annotation)
                    val spanLength = text.getSpanEnd(annotation) - spanStart
                    val displayName = annotation.key.replace(MENTION_ANNOTATION, "")
                    Mention(annotation.value, displayName, spanStart, spanLength)
                }
                .toList()

        }
        return emptyList()
    }

    fun getMentionAnnotations(spanned: Spanned): List<Annotation> {
        return getMentionAnnotations(spanned, 0, spanned.length)
    }

    fun getMentionAnnotations(spanned: Spanned, start: Int, end: Int): List<Annotation> {
        return Stream.of(*spanned.getSpans(start, end, Annotation::class.java))
            .filter { annotation: Annotation ->
                isMentionAnnotation(annotation)
            }
            .toList()
    }
}