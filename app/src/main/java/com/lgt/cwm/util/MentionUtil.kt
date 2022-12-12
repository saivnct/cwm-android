package com.lgt.cwm.util

import com.google.gson.Gson
import com.lgt.cwm.activity.conversation.fragments.models.Mention
import java.util.regex.Pattern

object MentionUtil {
    const val MENTION_STARTER = '@'
    const val MENTION_PLACEHOLDER = "\uFFFC"


    private fun invalidMention(body: CharSequence, mention: Mention): Boolean {
        val start: Int = mention.start
        val length: Int = mention.length
        return start < 0 || length < 0 || start + length > body.length
    }

    fun addMentionsToBody(body: String, mentions: List<Mention>): String {
        var content = body
        for (mention in mentions) {
            val jsonString = Gson().toJson(mention)
            content = content.replaceFirst(
                Pattern.quote("@${mention.displayName}").toRegex(),
                "<tag>$jsonString</tag>"
            )
        }
        return content
    }

    fun extractMentionsFromContent(content: String): BodyAndMentions {
        val regex = "<tag>(.+?)</tag>".toRegex()
        var body = content
        val mentions: MutableList<Mention> = mutableListOf()
        regex.findAll(content).forEach {
            if (it.groupValues.size >= 2) {
                val tagMention = it.groupValues[0]
                val mentionJson = it.groupValues[1]
                val result = try {
                    Gson().fromJson(mentionJson, Mention::class.java)
                }catch (e: Throwable){ null }

                result?.let { mention ->
                    mentions.add(mention)
                    body = body.replaceFirst(tagMention, "$MENTION_STARTER${mention.displayName}")
                }
            }

        }
//        Log.d("ConversationFragment", "Mention body extract ${body}")
        return BodyAndMentions(body, mentions)
    }

    data class BodyAndMentions(val body: String, val mentions: List<Mention>) {
    }
}