package com.moji.glide.webp.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.resource.drawable.DrawableResource;

public class WebpDrawableResource extends DrawableResource<WebpDrawable> implements Initializable {
    // Public API.
    @SuppressWarnings("WeakerAccess")
    public WebpDrawableResource(WebpDrawable drawable) {
        super(drawable);
    }

    @NonNull
    @Override
    public Class<WebpDrawable> getResourceClass() {
        return WebpDrawable.class;
    }

    @Override
    public int getSize() {
        return drawable.getSize();
    }

    @Override
    public void recycle() {
        drawable.stop();
        drawable.recycle();
    }

    @Override
    public void initialize() {
        drawable.getFirstFrame().prepareToDraw();
    }
}
