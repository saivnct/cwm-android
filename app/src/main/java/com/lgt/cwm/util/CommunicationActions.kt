package com.lgt.cwm.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Browser
import android.text.style.URLSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import com.lgt.cwm.R

object CommunicationActions {

    @JvmStatic
    fun openBrowserLink(context: Context, link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                R.string.CommunicationActions_no_browser_found,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @JvmStatic
    fun openEmail(context: Context, address: String, subject: String?, body: String?) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        intent.putExtra(Intent.EXTRA_SUBJECT, StringUtil.emptyIfNull(subject))
        intent.putExtra(Intent.EXTRA_TEXT, StringUtil.emptyIfNull(body))
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.CommunicationActions_send_email)
            )
        )
    }

    @JvmStatic
    fun openActionView(span: URLSpan, context: Context) {
        val uri = Uri.parse(span.url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("URLSpan", "Actvity was not found for intent, $intent")
        }
    }

}