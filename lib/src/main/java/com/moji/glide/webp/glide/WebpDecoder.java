package com.moji.glide.webp.glide;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.util.LruCache;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.moji.glide.webp.WebpFrameInfo;
import com.moji.glide.webp.WebpImage;
import com.moji.glide.webp.io.WebPWriter;
import com.moji.glide.webp.parser.BaseChunk;
import com.moji.glide.webp.parser.VP8XChunk;


import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


/**
 * 集成webp部分代码
 * 用于支持Animated Webp图片展示
 */
public class WebpDecoder implements GifDecoder {
    private static final String TAG = "WebpDecoder";
    // 缓存最近的Bitmap帧用于渲染当前帧
    private static final int STANDARD_FRAME_BITMAP_CACHE_SIZE = 6;

    /** Raw WebP data from input source. */
    private ByteBuffer rawData;
    /** WebpImage instance */
    private WebpImage mWebPImage;
    private final GifDecoder.BitmapProvider mBitmapProvider;
    private int mFramePointer = -1;
    private final int[] mFrameDurations;
    private int sampleSize;
    private int downsampledHeight;
    private int downsampledWidth;
    private final Paint mTransparentFillPaint;

    private WebpFrameCacheStrategy mCacheStrategy;

    private Bitmap.Config mBitmapConfig = Bitmap.Config.ARGB_8888;
    // 动画每一帧渲染后的Bitmap缓存
    private final LruCache<Integer, Bitmap> mFrameBitmapCache;


    public WebpDecoder(GifDecoder.BitmapProvider provider,
                       WebpImage webPImage,
                       ByteBuffer rawData,
                       WebpFrameCacheStrategy webpFrameCacheStrategy,
                       int sampleSize) {
        mBitmapProvider = provider;
        mWebPImage = webPImage;
        mFrameDurations = webPImage.getFrameDurations();

        mTransparentFillPaint = new Paint();
        mTransparentFillPaint.setColor(Color.TRANSPARENT);
        mTransparentFillPaint.setStyle(Paint.Style.FILL);
        mTransparentFillPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        this.mCacheStrategy = webpFrameCacheStrategy;
        int maxCacheSize = 0;
        if (mCacheStrategy.cacheAll()) {
            maxCacheSize = webPImage.getFrameCount();
        } else if(mCacheStrategy.cacheAuto()) {
            maxCacheSize = Math.min(STANDARD_FRAME_BITMAP_CACHE_SIZE,webPImage.getFrameCount());
        }else {
            maxCacheSize = Math.max(maxCacheSize, mCacheStrategy.getCacheSize());
        }
        mFrameBitmapCache = new LruCache<Integer, Bitmap>(maxCacheSize) {
            @Override
            protected void entryRemoved(boolean evicted, Integer key, Bitmap oldValue, Bitmap newValue) {
                // Return the cached frame bitmap to the provider
                if (oldValue != null) {
                    mBitmapProvider.release(oldValue);
                }
            }
        };

        setData(new GifHeader(), rawData, sampleSize);
    }

    @Override
    public int getWidth() {
        return mWebPImage.getWidth();
    }

    @Override
    public int getHeight() {
        return mWebPImage.getHeight();
    }

    @Override
    public ByteBuffer getData() {
        return rawData;
    }

    @Override
    public int getStatus() {
        return STATUS_OK;
    }

    @Override
    public void advance() {
        mFramePointer = (mFramePointer + 1) % mWebPImage.getFrameCount();
    }

    @Override
    public int getDelay(int n) {
        int delay = -1;
        if ((n >= 0) && (n < mFrameDurations.length)) {
            delay = mFrameDurations[n];
        }
        return delay;
    }

    @Override
    public int getNextDelay() {
        if (mFrameDurations.length == 0 || mFramePointer < 0) {
            return 0;
        }

        return getDelay(mFramePointer);
    }

    @Override
    public int getFrameCount() {
        return mWebPImage.getFrameCount();
    }

    @Override
    public int getCurrentFrameIndex() {
        return mFramePointer;
    }

    @Override
    public void resetFrameIndex() {
        mFramePointer = -1;
    }

    @Override
    public int getLoopCount() {
        return mWebPImage.getLoopCount();
    }

    @Override
    public int getNetscapeLoopCount() {
        return mWebPImage.getLoopCount();
    }

    @Override
    public int getTotalIterationCount() {
        if (mWebPImage.getLoopCount() == 0) {
            return TOTAL_ITERATION_COUNT_FOREVER;
        }
        return mWebPImage.getLoopCount();
    }

    @Override
    public int getByteSize() {
        return rawData.limit();
    }

    /** @Override Added in Glide 4.4.0 */
    public void setDefaultBitmapConfig(Bitmap.Config config) {
        if (config != Bitmap.Config.ARGB_8888) {
            throw new IllegalArgumentException("Unsupported format: " + config
                    + ", must be one of " + Bitmap.Config.ARGB_8888);
        }

        mBitmapConfig = config;
    }

    @Override
    public Bitmap getNextFrame() {
        int frameNumber = getCurrentFrameIndex();
        // Get the target Bitmap for Canvas
        Bitmap bitmap = mBitmapProvider.obtain(downsampledWidth, downsampledHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);

        if (!mCacheStrategy.noCache()) {
            Bitmap cache = mFrameBitmapCache.get(frameNumber);
            if (cache != null) {
                // hit from memory cache
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "hit frame bitmap from memory cache, frameNumber=" + frameNumber);
                }
                canvas.drawBitmap(cache, 0, 0, null);
                return bitmap;
            }
        }

        int nextIndex;
        // if blending is required, prepare the canvas with the nearest cached frame
        if (!isKeyFrame(frameNumber)) {
            // Blending is required, nextIndex points to the next index to render into the canvas
            nextIndex = prepareCanvasWithBlending(frameNumber - 1, canvas);
        } else {
            nextIndex = frameNumber;
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "frameNumber=" + frameNumber + ", nextIndex=" + nextIndex);
        }

        for (int index = nextIndex; index < frameNumber; index++) {
            WebpFrameInfo frameInfo = mWebPImage.getFrame(index);
            if (!frameInfo.isBlendPreviousFrame()) {
                disposeToBackground(canvas, frameInfo);
            }

            // render the previous frame
            renderFrame(index, canvas);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "renderFrame, index=" + index + ", blend=" + frameInfo.isBlendPreviousFrame()
                        + ", dispose=" + frameInfo.isDisposeBackgroundColor());
            }

            if (frameInfo.isBlendPreviousFrame()) {
                disposeToBackground(canvas, frameInfo);
            }
        }

        WebpFrameInfo frameInfo = mWebPImage.getFrame(frameNumber);
        if (!frameInfo.isBlendPreviousFrame()) {
            disposeToBackground(canvas, frameInfo);
        }

        // Finally, we render the current frame. We don't dispose it.
        renderFrame(frameNumber, canvas);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "renderFrame, index=" + frameNumber + ", blend=" + frameInfo.isBlendPreviousFrame()
                    + ", dispose=" + frameInfo.isDisposeBackgroundColor());
        }
        // Then put the rendered frame into the BitmapCache
        cacheFrameBitmap(frameNumber, bitmap);

        return bitmap;
    }

    private void renderFrame(int frameNumber, Canvas canvas) {

        WebpFrameInfo frameInfo = mWebPImage.getFrame(frameNumber);

        int targetWidth = frameInfo.getFrameWidth() / sampleSize;
        int targetHeight = frameInfo.getFrameHeight() / sampleSize;
        int xOffset = frameInfo.getFrameX() / sampleSize;
        int yOffset = frameInfo.getFrameY() / sampleSize;

        try {
            Bitmap frameBitmap = mBitmapProvider.obtain(targetWidth, targetHeight, mBitmapConfig);
            frameBitmap.eraseColor(Color.TRANSPARENT);

            mWebPImage.renderFrame(targetWidth,targetHeight,frameInfo,frameBitmap);
            canvas.drawBitmap(frameBitmap,xOffset,yOffset,null);
            mBitmapProvider.release(frameBitmap);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Rendering of frame failed. Frame number: " + frameNumber);
        }
    }

    private void cacheFrameBitmap(int frameNumber, Bitmap bitmap) {
        // Release the old cached bitmap
        mFrameBitmapCache.remove(frameNumber);

        // Create a new copy and put it into the cache
        Bitmap cache = mBitmapProvider.obtain(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        cache.eraseColor(Color.TRANSPARENT);

        Canvas canvas = new Canvas(cache);
        canvas.drawBitmap(bitmap, 0, 0, null);

        mFrameBitmapCache.put(frameNumber, cache);
    }

    @Override
    public int read(InputStream inputStream, int i) {
        return GifDecoder.STATUS_OK;
    }

    @Override
    public void clear() {
        mWebPImage.dispose();
        mWebPImage = null;
        mFrameBitmapCache.evictAll();
        rawData = null;
    }

    @Override
    public void setData(GifHeader header, byte[] data) {
        setData(header, ByteBuffer.wrap(data));
    }

    @Override
    public void setData(GifHeader header, ByteBuffer buffer) {
        setData(header, buffer, 1);
    }

    @Override
    public void setData(GifHeader header, ByteBuffer buffer, int sampleSize) {
        if (sampleSize <= 0) {
            throw new IllegalArgumentException("Sample size must be >=0, not: " + sampleSize);
        }
        // Make sure sample size is a power of 2.
        sampleSize = Integer.highestOneBit(sampleSize);

        // Initialize the raw data buffer.
        rawData = buffer.asReadOnlyBuffer();
        rawData.position(0);

        this.sampleSize = sampleSize;
        downsampledWidth = mWebPImage.getWidth() / sampleSize;
        downsampledHeight = mWebPImage.getHeight() / sampleSize;
    }

    @Override
    public int read(byte[] bytes) {
        return GifDecoder.STATUS_OK;
    }


    private int prepareCanvasWithBlending(int previousFrameNumber, Canvas canvas) {
        for (int index = previousFrameNumber; index >= 0; index--) {
            WebpFrameInfo frameInfo = mWebPImage.getFrame(index);
            if (!frameInfo.isDisposeBackgroundColor() || !isFullFrame(frameInfo)) {
                // need to draw this frame
                Bitmap bitmap = mFrameBitmapCache.get(index);
                if (bitmap != null && !bitmap.isRecycled()) {

                    canvas.drawBitmap(bitmap, 0, 0, null);
                    if (frameInfo.isDisposeBackgroundColor()) {
                        disposeToBackground(canvas, frameInfo);
                    }
                    return index + 1;
                } else if (isKeyFrame(index)) {
                    return index;
                } /* else keep going */
            } else {
                return index + 1;
            }
        }
        return 0;
    }


    /**
     * 使用透明色填充帧的显示区域
     *
     * @param canvas
     * @param frameInfo
     */
    private void disposeToBackground(Canvas canvas, WebpFrameInfo frameInfo) {
        final float left = frameInfo.getFrameX() / sampleSize;
        final float top = frameInfo.getFrameY() / sampleSize;
        final float right = (frameInfo.getFrameX() + frameInfo.getFrameWidth()) / sampleSize;
        final float bottom = (frameInfo.getFrameY() + frameInfo.getFrameHeight()) / sampleSize;
        canvas.drawRect(left, top, right, bottom, mTransparentFillPaint);
    }

    /**
     * 当前帧是否是关键帧
     *
     * @param index
     * @return
     */
    private boolean isKeyFrame(int index) {
        if (index == 0) {
            // first Frame
            return true;
        }

        WebpFrameInfo curFrameInfo = mWebPImage.getFrame(index);
        WebpFrameInfo prevFrameInfo = mWebPImage.getFrame(index-1);
        if (!curFrameInfo.isBlendPreviousFrame() && isFullFrame(curFrameInfo)) {
            return true;
        } else {
            return prevFrameInfo.isDisposeBackgroundColor() && isFullFrame(prevFrameInfo);
        }
    }

    /**
     * 当前帧是否充满画布
     *
     * @param frameInfo
     * @return
     */
    private boolean isFullFrame(WebpFrameInfo frameInfo) {
        return frameInfo.getFrameX() == 0 &&
                frameInfo.getFrameY() == 0 &&
                frameInfo.getFrameWidth() == mWebPImage.getWidth() &&
                frameInfo.getFrameHeight() == mWebPImage.getHeight();
    }

}
