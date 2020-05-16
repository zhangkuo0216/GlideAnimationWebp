package com.moji.glide.webp.io;

import java.io.IOException;

/**
 * @Description: APNG4Android
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-12
 */
public interface Writer extends AutoCloseable {
    void reset(int size);

    void putByte(byte b);

    void putBytes(byte[] b);

    int position();

    void skip(int length);

    byte[] toByteArray();

    void close() throws IOException;
}
