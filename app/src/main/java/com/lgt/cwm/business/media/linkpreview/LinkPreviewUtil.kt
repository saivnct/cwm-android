package com.lgt.cwm.business.media.linkpreview

import android.text.SpannableString
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.core.text.HtmlCompat
import androidx.core.text.util.LinkifyCompat
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.lgt.cwm.util.DateUtil
import com.lgt.cwm.util.LinkUtil
import com.lgt.cwm.util.Util
import okhttp3.HttpUrl
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object LinkPreviewUtil {
    private val OPEN_GRAPH_TAG_PATTERN: Pattern = Pattern.compile("<\\s*meta[^>]*property\\s*=\\s*\"\\s*og:([^\"]+)\"[^>]*/?\\s*>")
    private val ARTICLE_TAG_PATTERN: Pattern = Pattern.compile("<\\s*meta[^>]*property\\s*=\\s*\"\\s*article:([^\"]+)\"[^>]*/?\\s*>")
    private val OPEN_GRAPH_CONTENT_PATTERN: Pattern = Pattern.compile("content\\s*=\\s*\"([^\"]*)\"")
    private val TITLE_PATTERN: Pattern = Pattern.compile("<\\s*title[^>]*>(.*)<\\s*/title[^>]*>")
    private val FAVICON_PATTERN: Pattern = Pattern.compile("<\\s*link[^>]*rel\\s*=\\s*\".*icon.*\"[^>]*>")
    private val FAVICON_HREF_PATTERN: Pattern = Pattern.compile("href\\s*=\\s*\"([^\"]*)\"")

    fun getTopLevelDomain(urlString: String?): String? {
        if (!Util.isEmpty(urlString)) {
            val url: HttpUrl? = HttpUrl.parse(urlString!!)
            if (url != null) {
                return url.topPrivateDomain()
            }
        }
        return null
    }

    /**
     * @return All URLs allowed as previews in the source text.
     */
    fun findValidPreviewUrls(text: String): Links {
        val spannable = SpannableString(text)
        val found: Boolean = LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)
        if (!found) {
            return Links.EMPTY
        }

        return Links(Stream.of(*spannable.getSpans(0, spannable.length, URLSpan::class.java))
                .map { span: URLSpan -> Link(span.url, spannable.getSpanStart(span)) }
                .filter { link: Link -> LinkUtil.isValidPreviewUrl(link.url) }
                .toList()
        )
    }

    fun parseOpenGraphFields(html: String?): OpenGraph {
        if (html == null) {
            return OpenGraph(emptyMap(), null, null)
        }

        val openGraphTags: MutableMap<String, String> = HashMap()

        val openGraphMatcher: Matcher = OPEN_GRAPH_TAG_PATTERN.matcher(html)
        while (openGraphMatcher.find()) {
            val tag: String = openGraphMatcher.group()
            val property: String? = if (openGraphMatcher.groupCount() > 0) openGraphMatcher.group(1) else null
            if (property != null) {
                val contentMatcher: Matcher = OPEN_GRAPH_CONTENT_PATTERN.matcher(tag)
                if (contentMatcher.find() && contentMatcher.groupCount() > 0) {
                    val content: String = fromDoubleEncoded(contentMatcher.group(1) ?: "")
                    openGraphTags[property.lowercase(Locale.getDefault())] = content
                }
            }
        }

        val articleMatcher: Matcher = ARTICLE_TAG_PATTERN.matcher(html)
        while (articleMatcher.find()) {
            val tag: String = articleMatcher.group()
            val property: String? = if (articleMatcher.groupCount() > 0) articleMatcher.group(1) else null
            if (property != null) {
                val contentMatcher: Matcher = OPEN_GRAPH_CONTENT_PATTERN.matcher(tag)
                if (contentMatcher.find() && contentMatcher.groupCount() > 0) {
                    val content: String = fromDoubleEncoded(contentMatcher.group(1) ?: "")
                    openGraphTags[property.lowercase(Locale.getDefault())] = content
                }
            }
        }

        var htmlTitle: String? = ""
        var faviconUrl: String? = ""

        val titleMatcher: Matcher = TITLE_PATTERN.matcher(html)
        if (titleMatcher.find() && titleMatcher.groupCount() > 0) {
            htmlTitle = fromDoubleEncoded(titleMatcher.group(1) ?: "")
        }

        val faviconMatcher: Matcher = FAVICON_PATTERN.matcher(html)
        if (faviconMatcher.find()) {
            val faviconHrefMatcher: Matcher = FAVICON_HREF_PATTERN.matcher(faviconMatcher.group())
            if (faviconHrefMatcher.find() && faviconHrefMatcher.groupCount() > 0) {
                faviconUrl = faviconHrefMatcher.group(1)
            }
        }
        return OpenGraph(openGraphTags, htmlTitle, faviconUrl)
    }

    private fun fromDoubleEncoded(html: String): String {
        return HtmlCompat.fromHtml(HtmlCompat.fromHtml(html, 0).toString(), 0).toString()
    }

    class OpenGraph (private val values: Map<String, String>, private val htmlTitle: String?, private val faviconUrl: String?) {
        companion object {
            private const val KEY_TITLE: String = "title"
            private const val KEY_DESCRIPTION_URL: String = "description"
            private const val KEY_IMAGE_URL: String = "image"
            private const val KEY_PUBLISHED_TIME_1: String = "published_time"
            private const val KEY_PUBLISHED_TIME_2: String = "article:published_time"
            private const val KEY_MODIFIED_TIME_1: String = "modified_time"
            private const val KEY_MODIFIED_TIME_2: String = "article:modified_time"

            fun nullIfEmpty(value: String?): String? {
                return if (value == null || value.isEmpty()) {
                    null
                } else {
                    value
                }
            }
        }

        val title: String? = nullIfEmpty(Util.getFirstNonEmpty(values[KEY_TITLE], htmlTitle))

        val imageUrl: String? = nullIfEmpty(Util.getFirstNonEmpty(values[KEY_IMAGE_URL], faviconUrl))

        val date: Long = Stream.of(
            values[KEY_PUBLISHED_TIME_1],
            values[KEY_PUBLISHED_TIME_2],
            values[KEY_MODIFIED_TIME_1],
            values[KEY_MODIFIED_TIME_2])
            .map(DateUtil::parseIso8601)
            .filter { time -> time > 0 }
            .findFirst()
            .orElse(0) ?: 0

        val description: String? = nullIfEmpty(values[KEY_DESCRIPTION_URL])
    }

    class Links constructor(private val links: List<Link>) {
        private val urlSet: Set<String>

        companion object {
            val EMPTY: Links = Links(emptyList())
        }

        init {
            urlSet = Stream.of(links).map { link: Link -> trimTrailingSlash(link.url) }
                .collect(Collectors.toSet()) as Set<String>
        }

        fun findFirst(): Link? {
            return if (links.isEmpty()) null else links[0]
        }

        /**
         * Slightly forgiving comparison where it will ignore trailing '/' on the supplied url.
         */
        fun containsUrl(url: String): Boolean {
            return urlSet.contains(trimTrailingSlash(url))
        }

        private fun trimTrailingSlash(url: String): String {
            return if (url.endsWith("/")) url.substring(0, url.length - 1) else url
        }

        fun size(): Int {
            return links.size
        }
    }
}