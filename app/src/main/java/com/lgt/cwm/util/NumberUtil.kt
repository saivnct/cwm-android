package com.lgt.cwm.util

import android.content.Context
import android.util.DisplayMetrics
import java.nio.ByteBuffer


/**
 * Created by giangtpu on 30/07/2022.
 */

fun ByteArray.toLong(): Long {
    val buffer: ByteBuffer = ByteBuffer.allocate(Long.SIZE_BYTES)
    buffer.put(this)
    buffer.flip() //need flip

    return buffer.getLong()
}

fun Long.toByteArray(): ByteArray {
    val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
    buffer.putLong(this)
    return buffer.array()
}

object NumberUtil {

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.getResources()
            .getDisplayMetrics().densityDpi as Float / DisplayMetrics.DENSITY_DEFAULT)
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    fun convertPixelsToDp(px: Float, context: Context): Float {
        return px / (context.getResources()
            .getDisplayMetrics().densityDpi as Float / DisplayMetrics.DENSITY_DEFAULT)
    }
}