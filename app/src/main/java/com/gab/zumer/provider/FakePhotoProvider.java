package com.gab.zumer.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

public class FakePhotoProvider extends ContentProvider {
    private static final String TAG = "FakePhotoProvider";
    public static final String AUTHORITY = "com.gab.zumer.provider";
    private static final String PATH_IMAGES = "images";
    private static final int CODE_IMAGES = 1;

    private static final UriMatcher sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sMatcher.addURI(AUTHORITY, PATH_IMAGES + "/*", CODE_IMAGES);
    }

    private static final String STORAGE_DIR = "/sdcard/LSPosedFakeCamera";

    @Override
    public boolean onCreate() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) {
            boolean ok = dir.mkdirs();
            Log.i(TAG, "create storage dir: " + STORAGE_DIR + " -> " + ok);
        }
        return true;
    }

    private File fileForUri(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name == null || name.isEmpty()) {
            name = "photo_" + System.currentTimeMillis() + ".jpg";
        }
        return new File(STORAGE_DIR, name);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        int code = sMatcher.match(uri);
        if (code != CODE_IMAGES) {
            throw new FileNotFoundException("Unsupported URI: " + uri);
        }

        File file = fileForUri(uri);
        if (file == null) throw new FileNotFoundException("Bad uri: " + uri);

        int fileMode;
        if (mode.contains("w")) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            fileMode = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            fileMode = ParcelFileDescriptor.MODE_READ_ONLY;
        }

        return ParcelFileDescriptor.open(file, fileMode);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        File f = fileForUri(uri);
        if (f != null && f.exists() && f.delete()) return 1;
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
