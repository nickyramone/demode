package net.dbd.demode.util.io;

import org.apache.commons.codec.binary.Hex;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static net.dbd.demode.util.ByteUtil.*;

/**
 * A random access file reader that uses buffered reads and that has some convenience methods like negative position
 * seeking.
 * Not thread-safe.
 *
 * @author Nicky Ramone
 */
public class RandomAccessFileReader implements AutoCloseable {

    private static final int BUFFER_SIZE = 16 * 1024; // TODO: try with 8192;

    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final boolean littleEndian;
    private final RandomAccessReadBufferedFile file;
    private final long fileSize;


    public RandomAccessFileReader(String file) throws IOException {
        this(new File(file), false);
    }

    public RandomAccessFileReader(File file) throws IOException {
        this(file, false);
    }


    /**
     * @param file       The file to read
     * @param endianness Endianness of the file to read
     */
    public RandomAccessFileReader(File file, boolean littleEndian) throws IOException {
        this.file = new RandomAccessReadBufferedFile(file);
        this.fileSize = this.file.length();
        this.littleEndian = littleEndian;
    }


    public void seek(long pos) throws IOException {
        long correctedPos;

        if (pos >= 0) {
            correctedPos = Math.min(pos, fileSize);
        } else {
            correctedPos = file.getPosition() + pos;
            correctedPos = correctedPos < 0 ? 0 : correctedPos;
        }
        file.seek(correctedPos);
    }

    public void eof() throws IOException {
        file.seek(file.length());
    }

    /**
     * @param byteSeq is assumed big-endian
     */
    public boolean find(int byteSeq) throws IOException {
        byte[] bytesToSearch = littleEndian ? toLEByteArray(byteSeq) : toByteArray(byteSeq);

        return search(bytesToSearch);
    }

    public void skip(int length) throws IOException {
        file.skip(length);
    }

    private boolean search(byte[] needle) throws IOException {
        byte[] buffer = new byte[needle.length];
        boolean found = false;
        file.skip(needle.length - 1);

        do {
            file.rewind(needle.length - 1);
            file.read(buffer);
            found = Arrays.compare(buffer, needle) == 0;
        }
        while (!found && fileSize - file.getPosition() >= needle.length);

        return found;
    }


    public byte read() throws IOException {
        return (byte) file.read();
    }

    public byte[] read(int length) throws IOException {
        byte[] buffer = new byte[length];
        read(buffer, length);

        return buffer;
    }

    public String readString(int length) throws IOException {
        byte[] buffer;
        if (BUFFER_SIZE >= length) {
            buffer = this.buffer;
        } else {
            buffer = new byte[length];
        }
        read(buffer, length);

        return new String(buffer, 0, length, StandardCharsets.US_ASCII);
    }

    public String readHexString(int length) throws IOException {
        byte[] buffer = read(length);

        return Hex.encodeHexString(buffer);
    }

    public void read(byte[] bytes, int size) throws IOException {
        int read = 0;

        do {
            read += file.read(bytes, read, size - read);
        }
        while (read != 0 && read < size);
    }

    public int readInt() throws IOException {
        read(buffer, Integer.BYTES);

        return littleEndian ? fromLEByteArrayToInt(buffer) : fromByteArrayToInt(buffer);
    }

    public long readLong() throws IOException {
        read(buffer, Long.BYTES);

        return littleEndian ? fromLEByteArrayToLong(buffer) : fromByteArrayToLong(buffer);
    }

    public long getPosition() throws IOException {
        return file.getPosition();
    }

    public long getFileSize() {
        return fileSize;
    }

    public void close() throws IOException {
        file.close();
    }

}
