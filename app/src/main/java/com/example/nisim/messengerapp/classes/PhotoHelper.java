package com.example.nisim.messengerapp.classes;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.activities.ProfileActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class PhotoHelper {
    /*
    Public Methods
     */

    //creating an image file and saving it in cache
    public static File createCacheImageFile(Context context) {
        String imageFileName = "JPEG_" + UUID.randomUUID().toString();
        File storageDir = context.getCacheDir();
        File imageFile = null;
        try {
            imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Toast.makeText(context, "Error: couldn't create an image file.", Toast.LENGTH_SHORT).show();
        }

        return imageFile;
    }

    //save bitmap file to cache
    public static File saveBitmapToCache(Context context, Bitmap bitmapImage) {
        File imageFile = createCacheImageFile(context);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return imageFile;
    }

    //get the "real" uri path, in case the path is "external://" for example
    public static Uri getUriRealPath(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = 0;
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();

            String realPath = cursor.getString(column_index);
            File file = new File(realPath);
            return Uri.fromFile(file);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    //fix image rotation, which might be wrong after taking a photo in some devices
    public static Bitmap fixImageRotation(String filename, Bitmap bitmap) {
        ExifInterface ei;
        try {
            ei = new ExifInterface(filename);
        } catch (IOException e) {
            e.printStackTrace();
            return bitmap;
        }
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = rotateImage(bitmap, 90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = rotateImage(bitmap, 180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = rotateImage(bitmap, 270);
                break;

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                rotatedBitmap = bitmap;
        }

        return rotatedBitmap;
    }

    //create a thumbnail file and save it to cache (to save/load photos efficiently)
    public static Uri createThumbnailFromUri(Context context, Uri uri, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(uri.getPath(), options);
        options.inSampleSize = calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(uri.getPath(), options);
        return Uri.fromFile(PhotoHelper.saveBitmapToCache(context, bitmap));
    }

    //convert from dp unit to pixels according to device's resolution
    public static int dpsToPixels(Context context, double dps) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

    /*
    Private Methods
     */

    //rotate an image according to given angle
    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    //calculating a sample size according to photo size and screen size
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
