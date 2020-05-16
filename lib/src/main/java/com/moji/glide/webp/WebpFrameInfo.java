package com.moji.glide.webp;



import com.moji.glide.webp.parser.ANMFChunk;
import com.moji.glide.webp.parser.BaseChunk;


public class WebpFrameInfo {
    private int mFrameWidth;
    private int mFrameHeight;
    private int mFrameX;
    private int mFrameY;
    int mFrameDuration;
    private boolean mBlendPreviousFrame;
    private boolean mDisposeBackgroundColor;

    private final int imagePayloadOffset;

    private final int imagePayloadSize;
    private boolean useAlpha;
    public boolean isBlendPreviousFrame() {
        return mBlendPreviousFrame;
    }

    public boolean isDisposeBackgroundColor() {
        return mDisposeBackgroundColor;
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }

    public int getFrameX() {
        return mFrameX;
    }

    public int getFrameY() {
        return mFrameY;
    }

    public int getImagePayloadOffset() {
        return imagePayloadOffset;
    }

    public int getImagePayloadSize() {
        return imagePayloadSize;
    }

    public boolean isUseAlpha() {
        return useAlpha;
    }

    static WebpFrameInfo createFromChunk(ANMFChunk anmfChunk){
        return new WebpFrameInfo(anmfChunk);
    }

    private WebpFrameInfo(ANMFChunk anmfChunk) {

        this.mFrameWidth = anmfChunk.frameWidth;
        this.mFrameHeight = anmfChunk.frameHeight;
        this.mFrameDuration = anmfChunk.frameDuration;
        this.mFrameX = anmfChunk.frameX * 2;
        this.mFrameY = anmfChunk.frameY * 2;
        this.mBlendPreviousFrame = anmfChunk.blendingMethod();
        this.mDisposeBackgroundColor = anmfChunk.disposalMethod();
        this.imagePayloadOffset = anmfChunk.offset + BaseChunk.CHUNCK_HEADER_OFFSET + 16;
        this.imagePayloadSize = anmfChunk.payloadSize - 16 + (anmfChunk.payloadSize & 1);
        this.useAlpha = anmfChunk.alphChunk != null;
    }

}

