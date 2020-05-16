package com.moji.glide.webp;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.module.LibraryGlideModule;
import com.moji.glide.webp.glide.ByteBufferWebpDecoder;
import com.moji.glide.webp.glide.StreamWebpDecoder;
import com.moji.glide.webp.glide.WebpDrawable;

import java.io.InputStream;
import java.nio.ByteBuffer;

@GlideModule
public class WebpGlideLibraryModule extends LibraryGlideModule {

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        super.registerComponents(context, glide, registry);

        final BitmapPool bitmapPool = glide.getBitmapPool();
        final ArrayPool arrayPool = glide.getArrayPool();

        ByteBufferWebpDecoder byteBufferWebpDecoder =
                new ByteBufferWebpDecoder(context,arrayPool,bitmapPool);

        registry.prepend(ByteBuffer.class, WebpDrawable.class, byteBufferWebpDecoder)
                .prepend(InputStream.class, WebpDrawable.class, new StreamWebpDecoder(byteBufferWebpDecoder));
    }
}
