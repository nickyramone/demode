package net.dbd.demode.util;

/**
 * @author Nicky Ramone
 */
public class ByteUtil {

    public static int flipEndianness(int num) {
        return num << 24 | num >> 8 & 0xff00 | num << 8 & 0xff0000 | num >>> 24;
    }

    public static long flipEndianness(long num) {
        return num << 56
                | num << 40 & 0x00ff000000000000L
                | num << 24 & 0x0000ff0000000000L
                | num << 8 & 0x000000ff00000000L
                | num >> 8 & 0x00000000ff000000L
                | num >> 24 & 0x0000000000ff0000L
                | num >> 40 & 0x000000000000ff00L
                | num >> 56;
    }

    public static byte[] toByteArray(int byteSeq) {
        return toByteArray(byteSeq, Integer.BYTES, false);
    }

    public static byte[] toLEByteArray(int byteSeq) {
        return toByteArray(byteSeq, Integer.BYTES, true);
    }

    public static void toLittleEndian(int num, byte[] result) {
        toByteArray(num, Integer.BYTES, true, result);
    }

    public static void toLittleEndian(long num, byte[] result) {
        toByteArray(num, Integer.BYTES, true, result);
    }

    private static byte[] toByteArray(long byteSeq, int numBytes, boolean toLittleEndian) {
        byte[] buffer = new byte[numBytes];
        toByteArray(byteSeq, numBytes, toLittleEndian, buffer);

        return buffer;
    }

    private static void toByteArray(long byteSeq, int numBytes, boolean toLittleEndian, byte[] result) {
        for (int i = 0; i < numBytes; i++) {
            byte b = (byte) ((byteSeq >> (8 * i)) & 0xff);
            int idx = toLittleEndian ? i : numBytes - i - 1;
            result[idx] = b;
        }
    }

    public static int fromByteArrayToInt(byte[] bytes) {
        return fromBytesToInt(bytes[0], bytes[1], bytes[2], bytes[3]);
    }

    public static long fromByteArrayToLong(byte[] bytes) {
        return fromBytesToLong(bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]);
    }

    public static int fromLEByteArrayToInt(byte[] bytes) {
        return fromBytesToInt(bytes[3], bytes[2], bytes[1], bytes[0]);
    }

    public static long fromLEByteArrayToLong(byte[] bytes) {
        return fromBytesToLong(bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
    }

    /**
     * Returns the {@code int} value whose byte representation is the given 4 bytes, in big-endian
     * order; equivalent to {@code Ints.fromByteArray(new byte[] {b1, b2, b3, b4})}.
     *
     * @since 7.0
     */
    private static int fromBytesToInt(byte b1, byte b2, byte b3, byte b4) {
        return b1 << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
    }

    private static long fromBytesToLong(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7, byte b8) {
        return (long) b1 << 56 | (b2 & 0xFFL) << 42 | (b3 & 0xFFL) << 40 | (b4 & 0xFFL) << 32
                | (b5 & 0xFFL) << 24 | (b6 & 0xFFL) << 16 | (b7 & 0xFFL) << 8 | (b8 & 0xFFL);
    }
}
