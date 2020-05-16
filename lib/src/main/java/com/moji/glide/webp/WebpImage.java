package com.moji.glide.webp;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.moji.glide.webp.io.ByteBufferReader;
import com.moji.glide.webp.io.WebPReader;
import com.moji.glide.webp.io.WebPWriter;
import com.moji.glide.webp.parser.ANIMChunk;
import com.moji.glide.webp.parser.ANMFChunk;
import com.moji.glide.webp.parser.BaseChunk;
import com.moji.glide.webp.parser.VP8XChunk;
import com.moji.glide.webp.parser.WebPParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class WebpImage {

    // See comment in fixFrameDuration below.
    private static final int MIN_FRAME_DURATION_MS = 20;
    private static final int FRAME_DURATION_MS_FOR_MIN = 100;

    private int mWidth;
    private int mHeight;

    private int mFrameCount;
    private int[] mFrameDurations;
    private int mLoopCount;
    private int mBackgroundColor;
    private ByteBuffer mSource;

    private List<WebpFrameInfo> mWebpFrames = new ArrayList<>();

    private WebpImage() {

    }

    public static WebpImage createFromSource(ByteBuffer source) throws IOException {
        try (WebPReader webPReader = new WebPReader(new ByteBufferReader(source))) {
            WebpImage image = new WebpImage();
            List<BaseChunk> chunks = WebPParser.parse(webPReader);
            for (BaseChunk chunk : chunks) {
                if (chunk instanceof VP8XChunk) {
                    image.mWidth = ((VP8XChunk) chunk).canvasWidth;
                    image.mHeight = ((VP8XChunk) chunk).canvasHeight;
                } else if (chunk instanceof ANIMChunk) {
                    image.mBackgroundColor = ((ANIMChunk) chunk).backgroundColor;
                    image.mLoopCount = ((ANIMChunk) chunk).loopCount;
                } else if (chunk instanceof ANMFChunk) {
                    image.mWebpFrames.add(WebpFrameInfo.createFromChunk((ANMFChunk) chunk));
                }
            }
            image.fixFrameDurations();
            image.mFrameCount = image.mWebpFrames.size();
            image.mSource = source;
            return image;
        }
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getFrameCount() {
        return mFrameCount;
    }

    public int[] getFrameDurations() {
        return mFrameDurations;
    }

    public int getLoopCount() {
        return mLoopCount;
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * Adjust the frame duration to respect logic for minimum frame duration times
     */
    private void fixFrameDurations() {
        mFrameDurations = new int[mWebpFrames.size()];
        for (int i = 0; i < mWebpFrames.size(); i++) {
            int frameDurationMs = mWebpFrames.get(i).mFrameDuration;
            if (frameDurationMs < MIN_FRAME_DURATION_MS) {
                frameDurationMs = FRAME_DURATION_MS_FOR_MIN;
            }
            mFrameDurations[i] = frameDurationMs;
        }
    }

    public WebpFrameInfo getFrame(int frameNumber) {
        return mWebpFrames.get(frameNumber);
    }

    public void dispose() {
        mWebpFrames.clear();
    }

    private int encodeFrame(WebPWriter writer, WebpFrameInfo frameInfo) {
        int vp8xPayloadSize = 10;
        int size = 12 + (BaseChunk.CHUNCK_HEADER_OFFSET + vp8xPayloadSize) + frameInfo.getImagePayloadSize();
        writer.reset(size);
        // Webp Header
        writer.putFourCC("RIFF");
        writer.putUInt32(size);
        writer.putFourCC("WEBP");

        //VP8X
        writer.putUInt32(VP8XChunk.ID);
        writer.putUInt32(vp8xPayloadSize);
        writer.putByte((byte) (frameInfo.isUseAlpha() ? 0x10 : 0));
        writer.putUInt24(0);
        writer.put1Based(frameInfo.getFrameWidth());
        writer.put1Based(frameInfo.getFrameHeight());

        //image data
        try (WebPReader reader = new WebPReader(new ByteBufferReader(mSource))) {
            reader.reset();
            reader.skip(frameInfo.getImagePayloadOffset());
            reader.read(writer.toByteArray(), writer.position(), frameInfo.getImagePayloadSize());
        } catch (IOException e) {

        }
        return size;
    }

    public void renderFrame(int targetWidth, int targetHeight, WebpFrameInfo frameInfo, Bitmap frameBitmap) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inBitmap = frameBitmap;
        options.outHeight = targetHeight;
        options.outWidth = targetWidth;

        try (WebPWriter webPWriter = new WebPWriter()) {
            int length = encodeFrame(webPWriter, frameInfo);
            byte[] bytes = webPWriter.toByteArray();
            BitmapFactory.decodeByteArray(bytes, 0, length, options);
        }
    }
}
