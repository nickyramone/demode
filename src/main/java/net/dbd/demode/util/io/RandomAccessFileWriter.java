package net.dbd.demode.util.io;

import net.dbd.demode.util.ByteUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class RandomAccessFileWriter implements AutoCloseable {

    public enum Endianness {
        BIG_ENDIAN, LITTLE_ENDIAN
    }

    private static final Endianness DEFAULT_ENDIANNESS = Endianness.BIG_ENDIAN;

    private final boolean bigEndian;
    private RandomAccessFile file;


    public RandomAccessFileWriter(File file) throws IOException {
        this(file, DEFAULT_ENDIANNESS);
    }

    public RandomAccessFileWriter(File file, Endianness endianness) throws IOException {
        this.bigEndian = endianness.equals(Endianness.BIG_ENDIAN);
        this.file = new RandomAccessFile(file, "rw");
    }


    public void skip(int numBytes) throws IOException {
        file.skipBytes(numBytes);
    }

    public void writeByte(int b) throws IOException {
        file.write(b);
    }

    public void writeInt(int num) throws IOException {
        int intToWrite = bigEndian ? num : ByteUtil.flipEndianness(num);

        file.writeInt(intToWrite);
    }

    public void writeLong(long num) throws IOException {
        long longToWrite = bigEndian ? num : ByteUtil.flipEndianness(num);

        file.writeLong(longToWrite);
    }

    public void writeAsciiString(String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.US_ASCII);
        write(bytes);
    }

    public void write(byte[] bytes) throws IOException {
        write(bytes, bytes.length);
    }

    public void write(byte[] bytes, int len) throws IOException {
        file.write(bytes, 0, len);
    }

    public void writeNullBytes(int numBytes) throws IOException {
        for (int i = 0; i < numBytes; i++) {
            writeByte(0);
        }
    }

    public void writeHexString(String hexString) throws IOException {
        try {
            write(Hex.decodeHex(hexString));
        } catch (DecoderException e) {
            throw new RuntimeException("Invalid hex string.", e);
        }

    }

    public void seek(long position) throws IOException {
        file.seek(position);
    }

    public long position() throws IOException {
        return file.getFilePointer();
    }

    public long size() throws IOException {
        return file.length();
    }


    public void truncate() throws IOException {
        file.setLength(file.getFilePointer());
    }


    @Override
    public void close() throws Exception {
        file.close();
    }

}
