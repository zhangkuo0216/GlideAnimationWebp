package com.moji.glide.webp.glide;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.moji.glide.webp.Utils;
import com.moji.glide.webp.WebpImage;
import com.moji.glide.webp.io.ByteBufferReader;
import com.moji.glide.webp.parser.WebPParser;


import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteBufferWebpDecoder implements ResourceDecoder<ByteBuffer, WebpDrawable> {

    private final Context mContext;
    private final GifBitmapProvider mProvider;

    public ByteBufferWebpDecoder(Context context, ArrayPool byteArrayPool, BitmapPool bitmapPool) {
        this.mContext = context.getApplicationContext();
        this.mProvider = new GifBitmapProvider(bitmapPool, byteArrayPool);
    }

    @Override
    public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
        return WebPParser.isAWebP(new ByteBufferReader(source));
    }

    @Nullable
    @Override
    public Resource<WebpDrawable> decode(@NonNull ByteBuffer source, int width, int height, @NonNull Options options) throws IOException {

        int length = source.remaining();
        byte[] data = new byte[length];
        source.get(data, 0, length);

        WebpImage webp = WebpImage.createFromSource(source);
        int sampleSize = Utils.getSampleSize(webp.getWidth(), webp.getHeight(), width, height);
        WebpFrameCacheStrategy cacheStrategy = options.get(WebpStrategy.FRAME_CACHE_STRATEGY);
        WebpFramePlayStrategy playStrategy = options.get(WebpStrategy.FRAME_PLAY_STRATEGY);
        WebpDecoder webpDecoder = new WebpDecoder(mProvider, webp, source, cacheStrategy,playStrategy, sampleSize);
        webpDecoder.advance();
        Bitmap firstFrame = webpDecoder.getNextFrame();
        if (firstFrame == null) {
            return null;
        }

        Transformation<Bitmap> unitTransformation = UnitTransformation.get();

        return new WebpDrawableResource(new WebpDrawable(mContext, webpDecoder, unitTransformation, width, height,
                firstFrame));
    }
}
