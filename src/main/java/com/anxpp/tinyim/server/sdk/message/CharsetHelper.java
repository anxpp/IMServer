package com.anxpp.tinyim.server.sdk.message;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class CharsetHelper {
    public static final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
    private static final String ENCODE_CHARSET = "UTF-8";
    private static final String DECODE_CHARSET = "UTF-8";

    public static String getString(byte[] b, int len) {
        try {
            return new String(b, 0, len, DECODE_CHARSET);
        }
        // 如果是不支持的字符类型则按默认字符集进行解码
        catch (UnsupportedEncodingException e) {
            return new String(b, 0, len);
        }
    }

    public static String getString(byte[] b, int start, int len) {
        try {
            return new String(b, start, len, DECODE_CHARSET);
        }
        // 如果是不支持的字符类型则按默认字符集进行解码
        catch (UnsupportedEncodingException e) {
            return new String(b, start, len);
        }
    }

    public static byte[] getBytes(String str) {
        if (str != null) {
            try {
                return str.getBytes(ENCODE_CHARSET);
            }
            // 如果是不支持的字符类型则按默认字符集进行编码
            catch (UnsupportedEncodingException e) {
                return str.getBytes();
            }
        } else
            return new byte[0];
    }
}