package com.lgt.cwm.business.media.audio;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lgt.cwm.ui.components.voice.VoiceNoteDraft;
import com.lgt.cwm.util.NoExternalStorageException;
import com.lgt.cwm.util.StreamUtil;
import com.lgt.cwm.util.ThreadUtil;
import com.lgt.cwm.util.concurrent.ListenableFuture;
import com.lgt.cwm.util.concurrent.SettableFuture;
import com.lgt.cwm.util.concurrent.SignalExecutors;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;


public class AudioRecorder {

    private static final String TAG = AudioRecorder.class.getSimpleName();

    private static final ExecutorService executor = SignalExecutors.newCachedSingleThreadExecutor("signal-AudioRecorder");

    private final Context context;

    private Recorder recorder;
    private Uri      captureUri;

    public AudioRecorder(@NonNull Context context) {
        this.context = context;
    }

    public void startRecording() {
        Log.i(TAG, "startRecording()");

        executor.execute(() -> {
            Log.i(TAG, "Running startRecording() + " + Thread.currentThread().getId());
            try {
                if (recorder != null) {
                    throw new AssertionError("We can only record once at a time.");
                }

                ParcelFileDescriptor[] fds = ParcelFileDescriptor.createPipe();
                ParcelFileDescriptor.AutoCloseInputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(fds[0]);
                String fileName = "record_" + System.currentTimeMillis() + ".aac";

                OutputStream fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);

                SignalExecutors.UNBOUNDED.execute(() -> {
                    try {
                        StreamUtil.copy(inputStream, fileOutputStream);
                    } catch (IOException e) {
                        Log.w(TAG, "Error during write!", e);
                    }
                });

                File outputFile = new File(context.getFilesDir(), fileName);
                captureUri = Uri.fromFile(outputFile);

                recorder = Build.VERSION.SDK_INT >= 26 ? new MediaRecorderWrapper() : new AudioCodec();
                recorder.start(context, fds[1]);
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        });
    }

    public @NonNull ListenableFuture<VoiceNoteDraft> stopRecording() {
        Log.i(TAG, "stopRecording()");

        final SettableFuture<VoiceNoteDraft> future = new SettableFuture<>();

        executor.execute(() -> {
            if (recorder == null) {
                sendToFuture(future, new IOException("MediaRecorder was never initialized successfully!"));
                return;
            }

            recorder.stop();

            long size = 0;

            sendToFuture(future, new VoiceNoteDraft(captureUri, size));

            recorder   = null;
            captureUri = null;
        });

        return future;
    }

    private <T> void sendToFuture(final SettableFuture<T> future, final Exception exception) {
        ThreadUtil.runOnMain(() -> future.setException(exception));
    }

    private <T> void sendToFuture(final SettableFuture<T> future, final T result) {
        ThreadUtil.runOnMain(() -> future.set(result));
    }

}
