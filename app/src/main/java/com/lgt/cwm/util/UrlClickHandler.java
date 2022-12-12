package com.lgt.cwm.util;

import android.text.style.URLSpan;
import android.view.View;

import androidx.annotation.NonNull;

public interface UrlClickHandler {

    /**
     * @return true if you have handled it, false if you want to allow the standard Url handling.
     */
    boolean handleOnClick(@NonNull URLSpan urlSpan, View widget);
}
