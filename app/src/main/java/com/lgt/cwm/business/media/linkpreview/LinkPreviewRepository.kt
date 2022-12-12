package com.lgt.cwm.business.media.linkpreview

import android.util.Log
import androidx.core.util.Consumer
import com.lgt.cwm.http.connection.CallRequestController
import com.lgt.cwm.http.connection.OkHttpUtil
import com.lgt.cwm.http.connection.RequestController
import com.lgt.cwm.util.ByteUnit
import com.lgt.cwm.util.LinkUtil
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor

import java.io.IOException
class LinkPreviewRepository {

    companion object {
        private val TAG: String = LinkPreviewRepository::class.java.simpleName
        private val NO_CACHE = CacheControl.Builder().noCache().build()
        private val FAILSAFE_MAX_TEXT_SIZE: Long = ByteUnit.MEGABYTES.toBytes(2)
        private val FAILSAFE_MAX_IMAGE_SIZE: Long = ByteUnit.MEGABYTES.toBytes(2)
    }

    private var client: OkHttpClient

    init {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        client = OkHttpClient.Builder()
            .cache(null)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(UserAgentInterceptor("WhatsApp/2"))
            .build()
    }

    fun getLinkPreview(url: String, callback: Callback): RequestController {
        var metadataController: RequestController = CallRequestController()
        if (!LinkUtil.isValidPreviewUrl(url)) {
            Log.w(TAG, "Tried to get a link preview for a non-whitelisted domain.")
            callback.onError(Error.PREVIEW_NOT_AVAILABLE)
            return metadataController
        }

        metadataController = fetchMetadata(url) { metadata: Metadata ->
            if (metadata.isEmpty) {
                callback.onError(Error.PREVIEW_NOT_AVAILABLE)
                return@fetchMetadata
            }
            if (metadata.imageUrl == null) {
                callback.onSuccess(LinkPreview(url, metadata.title.orEmpty(), metadata.description.orEmpty(), metadata.date.toInt(), null))
                return@fetchMetadata
            }

            callback.onSuccess(LinkPreview(url, metadata.title.orEmpty(), metadata.description.orEmpty(), metadata.date.toInt(), metadata.imageUrl))
        }
        return metadataController
    }

    private fun fetchMetadata(url: String, callback: Consumer<Metadata>): RequestController {
        val call = client.newCall(Request.Builder().url(url).cacheControl(NO_CACHE).build())

        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Request failed.", e)
                callback.accept(Metadata.empty())
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.w(TAG, "Non-successful response. Code: " + response.code())
                    callback.accept(Metadata.empty())
                    return
                } else if (response.body() == null) {

                    Log.w(TAG, "No response body.")
                    callback.accept(Metadata.empty())
                    return
                }
                val body: String = OkHttpUtil.readAsString(response.body()!!, FAILSAFE_MAX_TEXT_SIZE)
                val openGraph: LinkPreviewUtil.OpenGraph = LinkPreviewUtil.parseOpenGraphFields(body)
                val title: String? = openGraph.title
                val description: String? = openGraph.description
                var imageUrl: String? = openGraph.imageUrl
                val date: Long = openGraph.date

                imageUrl?.let {
                    if (!LinkUtil.isValidPreviewUrl(it)) {
                        Log.i(TAG, "Image URL was invalid or for a non-whitelisted domain. Skipping.")
                        imageUrl = null
                    }
                }

                callback.accept(Metadata(title, description, date, imageUrl))
            }
        })
        return CallRequestController(call)
    }

    private data class Metadata constructor(
        val title: String?,
        val description: String?,
        val date: Long,
        val imageUrl: String?
    ) {
        companion object {
            fun empty() = Metadata(null, null, 0, null)
        }

        val isEmpty = (title == null && imageUrl == null)
    }

    interface Callback {
        fun onSuccess(linkPreview: LinkPreview)
        fun onError(error: Error)
    }

    enum class Error {
        PREVIEW_NOT_AVAILABLE
    }

}