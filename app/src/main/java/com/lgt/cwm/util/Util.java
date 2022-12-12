package com.lgt.cwm.util;


import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.Telephony;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import com.lgt.cwm.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Util {
    private static final String TAG = Util.class.getSimpleName();

    private static final long BUILD_LIFESPAN = TimeUnit.DAYS.toMillis(90);

    public static <T> List<T> asList(T... elements) {
        List<T> result = new LinkedList<>();
        Collections.addAll(result, elements);
        return result;
    }

    public static String join(String[] list, String delimiter) {
        return join(Arrays.asList(list), delimiter);
    }

    public static <T> String join(Collection<T> list, String delimiter) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        for (T item : list) {
            result.append(item);

            if (++i < list.size())
                result.append(delimiter);
        }

        return result.toString();
    }

    public static String join(long[] list, String delimeter) {
        List<Long> boxed = new ArrayList<>(list.length);

        for (int i = 0; i < list.length; i++) {
            boxed.add(list[i]);
        }

        return join(boxed, delimeter);
    }

    public static String join(List<Long> list, String delimeter) {
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < list.size(); j++) {
            if (j != 0) sb.append(delimeter);
            sb.append(list.get(j));
        }

        return sb.toString();
    }

    public static String rightPad(String value, int length) {
        if (value.length() >= length) {
            return value;
        }

        StringBuilder out = new StringBuilder(value);
        while (out.length() < length) {
            out.append(" ");
        }

        return out.toString();
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(@Nullable CharSequence charSequence) {
        return charSequence == null || charSequence.length() == 0;
    }

    public static boolean hasItems(@Nullable Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    public static <K, V> V getOrDefault(@NonNull Map<K, V> map, K key, V defaultValue) {
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    public static String getFirstNonEmpty(String... values) {
        for (String value : values) {
            if (!Util.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    public static @NonNull String emptyIfNull(@Nullable String value) {
        return value != null ? value : "";
    }

    public static @NonNull CharSequence emptyIfNull(@Nullable CharSequence value) {
        return value != null ? value : "";
    }

    public static CharSequence getBoldedString(String value) {
        SpannableString spanned = new SpannableString(value);
        spanned.setSpan(new StyleSpan(Typeface.BOLD), 0,
                spanned.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spanned;
    }


    public static void wait(Object lock, long timeout) {
        try {
            lock.wait(timeout);
        } catch (InterruptedException ie) {
            throw new AssertionError(ie);
        }
    }

    @RequiresPermission(anyOf = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_NUMBERS
    })


    public static @NonNull <T> T firstNonNull(@Nullable T optional, @NonNull T fallback) {
        return optional != null ? optional : fallback;
    }

    @SafeVarargs
    public static @NonNull <T> T firstNonNull(T ... ts) {
        for (T t : ts) {
            if (t != null) {
                return t;
            }
        }

        throw new IllegalStateException("All choices were null.");
    }

    public static <T> List<List<T>> partition(List<T> list, int partitionSize) {
        List<List<T>> results = new LinkedList<>();

        for (int index=0;index<list.size();index+=partitionSize) {
            int subListSize = Math.min(partitionSize, list.size() - index);

            results.add(list.subList(index, index + subListSize));
        }

        return results;
    }

    public static List<String> split(String source, String delimiter) {
        List<String> results = new LinkedList<>();

        if (TextUtils.isEmpty(source)) {
            return results;
        }

        String[] elements = source.split(delimiter);
        Collections.addAll(results, elements);

        return results;
    }

    public static byte[][] split(byte[] input, int firstLength, int secondLength) {
        byte[][] parts = new byte[2][];

        parts[0] = new byte[firstLength];
        System.arraycopy(input, 0, parts[0], 0, firstLength);

        parts[1] = new byte[secondLength];
        System.arraycopy(input, firstLength, parts[1], 0, secondLength);

        return parts;
    }

    public static byte[] combine(byte[]... elements) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            for (byte[] element : elements) {
                baos.write(element);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] trim(byte[] input, int length) {
        byte[] result = new byte[length];
        System.arraycopy(input, 0, result, 0, result.length);

        return result;
    }

    @SuppressLint("NewApi")
    public static boolean isDefaultSmsProvider(Context context){
        return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
    }


    public static int getManifestApkVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] getSecretBytes(int size) {
        return getSecretBytes(new SecureRandom(), size);
    }

    public static byte[] getSecretBytes(@NonNull SecureRandom secureRandom, int size) {
        byte[] secret = new byte[size];
        secureRandom.nextBytes(secret);
        return secret;
    }

    public static <T> T getRandomElement(T[] elements) {
        return elements[new SecureRandom().nextInt(elements.length)];
    }

    public static <T> T getRandomElement(List<T> elements) {
        return elements.get(new SecureRandom().nextInt(elements.size()));
    }

    public static boolean equals(@Nullable Object a, @Nullable Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static int hashCode(@Nullable Object... objects) {
        return Arrays.hashCode(objects);
    }

    public static @Nullable Uri uri(@Nullable String uri) {
        if (uri == null) return null;
        else             return Uri.parse(uri);
    }

    @TargetApi(VERSION_CODES.KITKAT)
    public static boolean isLowMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        return (VERSION.SDK_INT >= VERSION_CODES.KITKAT && activityManager.isLowRamDevice()) ||
                activityManager.getLargeMemoryClass() <= 64;
    }

    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static long clamp(long value, long min, long max) {
        return Math.min(Math.max(value, min), max);
    }

    public static float clamp(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * Returns half of the difference between the given length, and the length when scaled by the
     * given scale.
     */
    public static float halfOffsetFromScale(int length, float scale) {
        float scaledLength = length * scale;
        return (length - scaledLength) / 2;
    }

    public static @Nullable String readTextFromClipboard(@NonNull Context context) {
        {
            ClipboardManager clipboardManager = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);

            if (clipboardManager.hasPrimaryClip() && clipboardManager.getPrimaryClip().getItemCount() > 0) {
                return clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
            } else {
                return null;
            }
        }
    }

    public static void writeTextToClipboard(@NonNull Context context, @NonNull String text) {
        writeTextToClipboard(context, context.getString(R.string.app_name), text);
    }

    public static void writeTextToClipboard(@NonNull Context context, @NonNull String label, @NonNull String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }

    public static int toIntExact(long value) {
        if ((int)value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int)value;
    }

    public static boolean isEquals(@Nullable Long first, long second) {
        return first != null && first == second;
    }

    public static String getPrettyFileSize(long sizeBytes) {
        return MemoryUnitFormat.formatBytes(sizeBytes);
    }

    public static boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int parseInt(String integer, int defaultValue) {
        try {
            return Integer.parseInt(integer);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
