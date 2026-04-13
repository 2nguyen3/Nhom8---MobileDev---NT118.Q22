package com.example.heami.ai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageProxyUtils {

    private ImageProxyUtils() {}

    public static Bitmap imageProxyToBitmap(@NonNull ImageProxy imageProxy) {
        byte[] nv21 = yuv420ToNv21(imageProxy);

        YuvImage yuvImage = new YuvImage(
                nv21,
                ImageFormat.NV21,
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                null
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        yuvImage.compressToJpeg(
                new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()),
                90,
                outputStream
        );

        byte[] jpegBytes = outputStream.toByteArray();

        Bitmap bitmap = BitmapFactory.decodeByteArray(
                jpegBytes,
                0,
                jpegBytes.length
        );

        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

        return rotateBitmap(bitmap, rotationDegrees);
    }

    public static Bitmap cropFace(Bitmap source, Rect faceBounds) {
        if (source == null || faceBounds == null) {
            return source;
        }

        int padding = Math.round(faceBounds.width() * 0.20f);

        int left = Math.max(faceBounds.left - padding, 0);
        int top = Math.max(faceBounds.top - padding, 0);
        int right = Math.min(faceBounds.right + padding, source.getWidth());
        int bottom = Math.min(faceBounds.bottom + padding, source.getHeight());

        int width = right - left;
        int height = bottom - top;

        if (width <= 0 || height <= 0) {
            return source;
        }

        return Bitmap.createBitmap(source, left, top, width, height);
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (bitmap == null || rotationDegrees == 0) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);

        return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
        );
    }

    private static byte[] yuv420ToNv21(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = imageProxy.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = imageProxy.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);

        byte[] vBytes = new byte[vSize];
        byte[] uBytes = new byte[uSize];

        vBuffer.get(vBytes);
        uBuffer.get(uBytes);

        int position = ySize;
        int chromaSize = Math.min(vBytes.length, uBytes.length);

        for (int i = 0; i < chromaSize; i++) {
            nv21[position++] = vBytes[i];
            nv21[position++] = uBytes[i];

            if (position >= nv21.length) {
                break;
            }
        }

        return nv21;
    }
}