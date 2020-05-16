package com.moji.glide.webp.glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.moji.glide.webp.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class StreamWebpDecoder implements ResourceDecoder<InputStream, WebpDrawable> {

    private ByteBufferWebpDecoder mByteBufferWebpDecoder;

    public StreamWebpDecoder(ByteBufferWebpDecoder byteBufferWebpDecoder){
        this.mByteBufferWebpDecoder = byteBufferWebpDecoder;
    }

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
        byte[] data = Utils.inputStreamToBytes(source);
        if (data == null) {
            return false;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        return mByteBufferWebpDecoder.handles(byteBuffer,options);
    }

    @Nullable
    @Override
    public Resource<WebpDrawable> decode(@NonNull InputStream inputStream, int width, int height, @NonNull Options options) throws IOException {
        byte[] data = Utils.inputStreamToBytes(inputStream);
        if (data == null) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);

        return mByteBufferWebpDecoder.decode(byteBuffer, width, height, options);
    }
}
