package net.dbd.demode.pak;

import net.dbd.demode.pak.domain.Pak;
import net.dbd.demode.pak.domain.PakCompressedBlock;
import net.dbd.demode.pak.domain.PakEntry;
import net.dbd.demode.pak.domain.PakIndex;
import net.dbd.demode.util.event.EventListener;
import net.dbd.demode.util.event.EventSupport;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static java.lang.Math.min;

/**
 * Non thread-safe.
 *
 * @author Nicky Ramone
 */
public class PakExtractor {

    public enum EventType {
        FILE_EXTRACTED,
        BYTES_EXTRACTED,
        PAK_EXTRACTED,
        ABORTED
    }

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int ZLIB_BUFFER_SIZE = 65536;

    private final byte[] zlibBuffer = new byte[ZLIB_BUFFER_SIZE];
    private final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    private final EventSupport eventSupport = new EventSupport();

    private boolean abort;


    public boolean extract(Pak pak, Path outputPath) throws IOException, DataFormatException {
        abort = false;

        try (RandomAccessFile raf = new RandomAccessFile(pak.getFile(), "r")) {
            PakIndex pakIndex = pak.getPakIndex();
            Path mountPath = outputPath.resolve(pakIndex.getMountPoint());

            for (PakEntry tocEntry : pakIndex.getEntries()) {
                if (abort) {
                    return false;
                }

                File extractedFile = extractSingleFile(tocEntry, mountPath, raf);
                eventSupport.fireEvent(EventType.FILE_EXTRACTED, extractedFile);
                eventSupport.fireEvent(EventType.BYTES_EXTRACTED, tocEntry.getSize());
            }
        }
        eventSupport.fireEvent(EventType.PAK_EXTRACTED, pak);

        return true;
    }


    private File extractSingleFile(PakEntry tocEntry, Path outputPath, RandomAccessFile raf)
            throws IOException, DataFormatException {

        File outputFile = outputPath.resolve(tocEntry.getFilename()).normalize().toFile();
        outputFile.getParentFile().mkdirs();

        try (var outStream = new FileOutputStream(outputFile)) {

            if (tocEntry.getBlocks().isEmpty()) {
                raf.seek(tocEntry.getOffset());
                copy(raf, (int) tocEntry.getSize(), outStream);
            } else {
                unpackSplitFile(tocEntry, raf, outStream);
            }
        }

        return outputFile;
    }


    public void copy(RandomAccessFile raf, int length, OutputStream outStream) throws IOException {
        int totalRead = 0;

        do {
            int numBytesToRead = min(length - totalRead, DEFAULT_BUFFER_SIZE);
            raf.read(buffer, 0, numBytesToRead);
            totalRead += numBytesToRead;
            outStream.write(buffer, 0, numBytesToRead);
        }
        while (totalRead < length);
    }


    /**
     * We cannot buffer the input into the output because -apparently- zlib compressed data cannot be arbitrarily split,
     * so we'll just decompress each chunk in a single go.
     * This is ok, though, since so far we haven't found any chunk bigger than our buffer size.
     */
    private void unpackSplitFile(PakEntry entry, RandomAccessFile raf, OutputStream outStream)
            throws IOException, DataFormatException {

        long baseOffset = entry.getOffset();

        for (PakCompressedBlock chunk : entry.getBlocks()) {
            int chunkSize = chunk.size();

            if (chunkSize > ZLIB_BUFFER_SIZE) {
                throw new RuntimeException(String.format("File '%s' chunk is too big.", entry.getFilename()));
            }

            raf.seek(baseOffset + chunk.getOffsetStart());
            raf.read(zlibBuffer, 0, chunkSize);
            decompressWithZlib(zlibBuffer, chunkSize, outStream);
        }
    }


    private void decompressWithZlib(byte[] data, int length, OutputStream outputStream)
            throws IOException, DataFormatException {

        Inflater inflater = new Inflater();
        inflater.setInput(data, 0, length);

        while (!inflater.finished()) {
            int numInflatedBytes = inflater.inflate(buffer);
            outputStream.write(buffer, 0, numInflatedBytes);
        }
    }

    public void abort() {
        abort = true;
    }


    public void registerListener(EventType eventType, EventListener eventListener) {
        eventSupport.registerListener(eventType, eventListener);
    }

}
