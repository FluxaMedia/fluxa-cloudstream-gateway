package android.util;

public final class Base64 {
    public static final int DEFAULT = 0;
    public static final int NO_PADDING = 1;
    public static final int NO_WRAP = 2;
    public static final int CRLF = 4;
    public static final int URL_SAFE = 8;

    private Base64() {
    }

    public static byte[] decode(String str, int flags) {
        return decode(str.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), flags);
    }

    public static byte[] decode(byte[] input, int flags) {
        java.util.Base64.Decoder d = (flags & URL_SAFE) != 0 ? java.util.Base64.getUrlDecoder() : java.util.Base64.getMimeDecoder();
        return d.decode(input);
    }

    public static byte[] decode(byte[] input, int offset, int len, int flags) {
        return decode(java.util.Arrays.copyOfRange(input, offset, offset + len), flags);
    }

    public static byte[] encode(byte[] input, int flags) {
        java.util.Base64.Encoder e = (flags & URL_SAFE) != 0 ? java.util.Base64.getUrlEncoder() : java.util.Base64.getEncoder();
        if ((flags & NO_PADDING) != 0) {
            e = e.withoutPadding();
        }
        return e.encode(input);
    }

    public static byte[] encode(byte[] input, int offset, int len, int flags) {
        return encode(java.util.Arrays.copyOfRange(input, offset, offset + len), flags);
    }

    public static String encodeToString(byte[] input, int flags) {
        return new String(encode(input, flags), java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    public static String encodeToString(byte[] input, int offset, int len, int flags) {
        return new String(encode(input, offset, len, flags), java.nio.charset.StandardCharsets.ISO_8859_1);
    }
}
