package net.dbd.demode.util.lang;

import net.dbd.demode.util.ByteUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.nio.charset.StandardCharsets;

/**
 * Just an extension to @{@link ByteArrayOutputStream} with extra convenience methods.
 *
 * @author Nicky Ramone
 */
public class LittleEndianByteArrayOutputStream extends ByteArrayOutputStream {

    private final byte[] buffer = new byte[32];


    public void writeInt(int num) {
        ByteUtil.toLittleEndian(num, buffer);
        write(buffer, 0, Integer.BYTES);
    }

    public void writeLong(long num) {
        ByteUtil.toLittleEndian(num, buffer);
        write(buffer, 0, Long.BYTES);
    }

    public void writeAsciiString(String string) {
        byte[] bytes = string.getBytes(StandardCharsets.US_ASCII);
        write(bytes, 0, bytes.length);
    }

    public void writeHexString(String hex) throws DecoderException {
        byte[] bytes = Hex.decodeHex(hex);
        write(bytes, 0, bytes.length);
    }

}
