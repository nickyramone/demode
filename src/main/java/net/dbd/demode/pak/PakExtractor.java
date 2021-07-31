package net.dbd.demode.pak;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dbd.demode.pak.domain.PakCompressedBlock;
import net.dbd.demode.pak.domain.PakEntry;
import net.dbd.demode.pak.domain.PakIndex;
import net.dbd.demode.util.event.EventListener;
import net.dbd.demode.util.event.EventSupport;
import net.dbd.demode.util.lang.OperationAbortedException;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static java.lang.Math.min;

/**
 * TODO: Consider removing or having it being a helper to @{@link PakFile}.
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

    @RequiredArgsConstructor
    @Getter
    public static final class ExtractedFileInfo {
        final Path relativeFilePath;
        final File file;
        final String hash;
    }

    private static final int KiB = 1024;
    private static final int DEFAULT_BUFFER_SIZE = 8 * KiB;
    private static final int ZLIB_BUFFER_SIZE = 64 * KiB;

    private final byte[] zlibBuffer = new byte[ZLIB_BUFFER_SIZE];
    private final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    private final EventSupport eventSupport = new EventSupport();

    private boolean abort;


    public void extract(PakFile pakFile, List<Path> includedFilePaths, Path outputPath)
            throws IOException, DataFormatException, OperationAbortedException {
        abort = false;

        try (RandomAccessFile raf = new RandomAccessFile(pakFile.getFile(), "r")) {
            PakIndex pakIndex = pakFile.getIndex();
            Path mountPoint = pakIndex.getMountPoint();

            for (Path includedFilePath : includedFilePaths) {
                if (abort) {
                    throw new OperationAbortedException();
                }

                PakEntry pakEntry = pakFile.getEntry(includedFilePath);
                extractSingleFile(pakEntry, outputPath, mountPoint, raf);
            }
        }

        eventSupport.fireEvent(EventType.PAK_EXTRACTED, pakFile);
    }


    private void extractSingleFile(PakEntry indexEntry, Path outputPath, Path mountPoint, RandomAccessFile raf)
            throws IOException, DataFormatException, OperationAbortedException {

        Path filePath = mountPoint.resolve(indexEntry.getFilePath()).normalize();
        File outputFile = outputPath.resolve(filePath).normalize().toFile();
        outputFile.getParentFile().mkdirs();

        try (var outStream = new FileOutputStream(outputFile)) {

            if (indexEntry.getBlocks().isEmpty()) {
                raf.seek(indexEntry.getOffset());
                copy(raf, (int) indexEntry.getSize(), outStream);
            } else {
                unpackSplitFile(indexEntry, raf, outStream);
            }
        }

        ExtractedFileInfo extractedFileInfo = new ExtractedFileInfo(filePath, outputFile, indexEntry.getHash());
        eventSupport.fireEvent(EventType.FILE_EXTRACTED, extractedFileInfo);
        eventSupport.fireEvent(EventType.BYTES_EXTRACTED, indexEntry.getSize());
    }


    private void copy(RandomAccessFile raf, int length, OutputStream outStream) throws IOException, OperationAbortedException {
        int totalRead = 0;

        do {
            if (abort) {
                throw new OperationAbortedException();
            }

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
            throws IOException, DataFormatException, OperationAbortedException {

        long baseOffset = entry.getOffset();

        for (PakCompressedBlock block : entry.getBlocks()) {
            if (abort) {
                throw new OperationAbortedException();
            }
            int blockSize = block.size();

            if (blockSize > ZLIB_BUFFER_SIZE) {
                throw new RuntimeException(String.format("File '%s' chunk is too big.", entry.getFilePath()));
            }

            raf.seek(baseOffset + block.getOffsetStart());
            raf.read(zlibBuffer, 0, blockSize);
            decompressWithZlib(zlibBuffer, blockSize, outStream);
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
