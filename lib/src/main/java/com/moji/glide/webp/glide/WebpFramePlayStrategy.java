package com.moji.glide.webp.glide;

public interface WebpFramePlayStrategy {

    WebpFramePlayStrategy SEQUENCE = new Sequence();
    WebpFramePlayStrategy REPEAT = new Repeat();
    WebpFramePlayStrategy REVERT = new Revert();

    int getNextFrameIndex(int frameCount);

    class Sequence implements WebpFramePlayStrategy {

        private int mIndex = -1;

        @Override
        public int getNextFrameIndex(int frameCount) {
            mIndex = (mIndex + 1) % frameCount;
            return mIndex;
        }
    }

    class Repeat implements WebpFramePlayStrategy {

        private int mIndex = -1;
        private int mAddend = 1;

        @Override
        public int getNextFrameIndex(int frameCount) {
            if (mIndex == frameCount -1) {
                mAddend = -1;
            } else if (mIndex == 0) {
                mAddend = 1;
            }
            mIndex += mAddend;
            return mIndex;
        }
    }

    class Revert implements WebpFramePlayStrategy {

        private int mIndex = 0;

        @Override
        public int getNextFrameIndex(int frameCount) {
            if (mIndex == 0) {
                mIndex = frameCount;
            }
            mIndex -= 1;
            return mIndex;
        }
    }
}

