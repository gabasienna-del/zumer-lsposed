package com.gab.zumer.hook;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class CameraIntentHook implements IXposedHookLoadPackage {

    private static final String TAG = "ZumerFakeCam";
    private static final String FAKE_PHOTO_PATH = "/sdcard/LSPosedFakeCamera/photo.jpg";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {

        findAndHookMethod(Activity.class, "startActivityForResult",
                Intent.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        hookCamera(param);
                    }
                });

        findAndHookMethod(Activity.class, "startActivityForResult",
                Intent.class, int.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        hookCamera(param);
                    }
                });
    }

    private void hookCamera(XC_MethodHook.MethodHookParam param) {
        try {
            Intent intent = (Intent) param.args[0];
            if (intent == null) return;

            if (!MediaStore.ACTION_IMAGE_CAPTURE.equals(intent.getAction()))
                return;

            Activity act = (Activity) param.thisObject;
            int req = (int) param.args[1];

            Intent resultIntent = new Intent();

            Uri outUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);

            if (outUri != null) {
                writeFakePhoto(act, outUri);
                resultIntent.setData(outUri);
            } else {
                Bitmap bmp = BitmapFactory.decodeFile(FAKE_PHOTO_PATH);
                if (bmp != null) {
                    resultIntent.putExtra("data", bmp);
                    Uri saved = saveToMediaStore(act.getContentResolver(), bmp);
                    if (saved != null)
                        resultIntent.setData(saved);
                }
            }

            act.runOnUiThread(() -> act.onActivityResult(req, Activity.RESULT_OK, resultIntent));

            param.setResult(null);

        } catch (Throwable e) {
            Log.e(TAG, "hook error", e);
        }
    }

    private boolean writeFakePhoto(Activity act, Uri uri) {
        try {
            ContentResolver cr = act.getContentResolver();
            try (OutputStream out = cr.openOutputStream(uri);
                 InputStream in = new java.io.FileInputStream(FAKE_PHOTO_PATH)) {

                if (out == null) return false;

                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1)
                    out.write(buf, 0, r);

                out.flush();
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writeFakePhoto failed", e);
            return false;
        }
    }

    private Uri saveToMediaStore(ContentResolver resolver, Bitmap bmp) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME,
                    "fake_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return null;

            try (OutputStream out = resolver.openOutputStream(uri)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
            return uri;
        } catch (Exception e) {
            Log.e(TAG, "saveToMediaStore error", e);
            return null;
        }
    }
}
