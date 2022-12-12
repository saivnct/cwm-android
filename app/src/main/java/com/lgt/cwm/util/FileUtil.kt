package com.lgt.cwm.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.lgt.cwm.models.FileMetaData
import cwmSignalMsgPb.CwmSignalMsg
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

/**
 * Created by giangtpu on 22/08/2022.
 */
fun Uri.getName(resolver: ContentResolver): String? {
    val returnCursor = resolver.query(this, null, null, null, null)
    returnCursor?.let {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        val fileName = it.getString(nameIndex)
        it.close()
        return fileName
    }
    return null
}

object FileUtil {
    fun getFileMetaData(applicationContext: Context, uri: Uri, isGallery: Boolean): FileMetaData? {
        val contentResolver = applicationContext.getContentResolver()

        contentResolver.openInputStream(uri).use { inputStream ->
            inputStream?.let {
                var mimeType = contentResolver.getType(uri)
                if (mimeType.isNullOrEmpty()){
                    val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())

                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))?.let {
                        mimeType = it
                    }

                    if (mimeType.isNullOrEmpty()) {
                        return null
                    }
                }

                val mediaType = getFileTypeFromMimeType(mimeType!!) ?: return null
//                val data = readBytes(inputStream)
                val checksum = inputStream.md5()

                return FileMetaData(
                    uri = uri,
                    checksum = checksum,
                    mimeType = mimeType!!,
                    mediaType = mediaType,
                    isGallery = isGallery
                )
            }
        }

        return null
    }

    fun readBytes(inputStream: InputStream): ByteArray {
        // this dynamically extends to take the bytes you read
        val byteBuffer = ByteArrayOutputStream()

        // this is storage overwritten on each iteration with bytes
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)

        // we need to know how may bytes were read to write them to the byteBuffer
        var len = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray()
    }

    fun getFileTypeFromMimeType(mimeType: String): CwmSignalMsg.SIGNAL_MEDIA_TYPE?{
        val m = mimeType.lowercase()

        if (m.startsWith("image",true)){
            return CwmSignalMsg.SIGNAL_MEDIA_TYPE.IMAGE
        }else if (m.startsWith("video",true)){
            return CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO
        }else if (m.startsWith("audio",true)){
            return CwmSignalMsg.SIGNAL_MEDIA_TYPE.AUDIO
        }else if (
            m.equals("application/pdf") ||
            m.equals("application/x-pdf") ||
            m.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
            m.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
            m.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation") ||
            m.equals("application/msword") ||
            m.equals("application/vnd.ms-word") ||
            m.equals("application/vnd.ms-powerpoint") ||
            m.equals("application/mspowerpoint") ||
            m.equals("application/vnd.ms-excel") ||
            m.equals("application/msexcel") ||
            m.equals("application/vnd.oasis.opendocument.text") ||
            m.equals("application/x-vnd.oasis.opendocument.text") ||
            m.equals("application/vnd.oasis.opendocument.text-template") ||
            m.equals("application/x-vnd.oasis.opendocument.text-template") ||
            m.equals("application/vnd.oasis.opendocument.spreadsheet") ||
            m.equals("application/x-vnd.oasis.opendocument.spreadsheet") ||
            m.equals("application/vnd.oasis.opendocument.spreadsheet-template") ||
            m.equals("application/x-vnd.oasis.opendocument.spreadsheet-template") ||
            m.equals("application/vnd.oasis.opendocument.presentation") ||
            m.equals("application/x-vnd.oasis.opendocument.presentation") ||
            m.equals("application/vnd.oasis.opendocument.presentation-template") ||
            m.equals("application/x-vnd.oasis.opendocument.presentation-template")
        ){
            return CwmSignalMsg.SIGNAL_MEDIA_TYPE.DOC
        }else if (m.isNullOrEmpty()){
            return CwmSignalMsg.SIGNAL_MEDIA_TYPE.FILE
        }

        return null
    }

}