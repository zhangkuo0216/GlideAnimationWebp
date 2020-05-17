package com.moji.glide.webp.glide;

import com.bumptech.glide.load.Option;

public class WebpStrategy {

    public static final Option<WebpFrameCacheStrategy> FRAME_CACHE_STRATEGY = Option.memory(
            "com.moji.glide.webp.glide.webp.CacheStrategy", WebpFrameCacheStrategy.AUTO);

    public static final Option<WebpFramePlayStrategy> FRAME_PLAY_STRATEGY = Option.memory(
            "com.moji.glide.webp.glide.webp.PlayStrategy", WebpFramePlayStrategy.SEQUENCE);

}
