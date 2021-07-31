package net.dbd.demode.pak;

import lombok.experimental.UtilityClass;
import net.dbd.demode.pak.domain.*;
import net.dbd.demode.util.io.RandomAccessFileReader;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Helper for writing pak file metadata.
 *
 * @author Nicky Ramone
 */
@UtilityClass
public class PakMetaReader {

    private static final boolean IS_LITTLE_ENDIAN = true;


    public Pak parse(File pakFilename) {
        return parse(pakFilename, Path.of("./"));
    }

    public Pak parse(File pakFilename, Path mountPointsRoot) {
        Pak pak = new Pak();

        try {
            try (RandomAccessFileReader reader = new RandomAccessFileReader(pakFilename, IS_LITTLE_ENDIAN)) {
                pak.setInfo(readFooter(reader));
                pak.setIndex(readIndex(reader, pak.getInfo().getIndexOffset(), mountPointsRoot));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse file.", e);
        }

        return pak;
    }


    private PakInfo readFooter(RandomAccessFileReader reader) throws IOException {
        reader.eof();
        reader.seek(PakConstants.PAK_INFO_OFFSET_FROM_EOF);

        if (reader.readInt() != PakConstants.PAK_MAGIC) {
            throw new RuntimeException("Could not recognize file format.");
        }

        PakInfo pakInfo = new PakInfo();
        pakInfo.setVersion(reader.readInt());
        pakInfo.setIndexOffset(reader.readLong());
        pakInfo.setIndexSize(reader.readLong());
        pakInfo.setIndexHash(Hex.encodeHexString(reader.read(20)));

        return pakInfo;
    }


    private PakIndex readIndex(RandomAccessFileReader reader, long tocOffset, Path mountPointsRoot) throws IOException {
        PakIndex pakIndex = new PakIndex();
        reader.seek(tocOffset);
        readMountPoint(reader, pakIndex, mountPointsRoot);
        readIndexEntries(reader, pakIndex);

        return pakIndex;
    }

    private void readMountPoint(RandomAccessFileReader reader, PakIndex pakIndex, Path mountPointsRoot) throws IOException {
        int entrySize = reader.readInt() - 1;
        String entryName = reader.readString(entrySize);
        reader.skip(1); // null terminator

        Path normalizedMountPath = mountPointsRoot.resolve(entryName).normalize();
        pakIndex.setMountPoint(normalizedMountPath);
    }

    private void readIndexEntries(RandomAccessFileReader reader, PakIndex pakIndex) throws IOException {
        int numFiles = reader.readInt();

        for (int i = 0; i < numFiles; i++) {
            pakIndex.addEntry(readIndexEntry(reader));
        }
    }

    private PakEntry readIndexEntry(RandomAccessFileReader reader) throws IOException {
        PakEntry entry = new PakEntry();

        entry.setFilePath(Path.of(readPakString(reader)));
        entry.setOffset(reader.readLong());
        entry.setCompressedSize(reader.readLong());
        entry.setSize(reader.readLong());
        entry.setCompressed((readCompressionFlag(reader)));
        entry.setHash(reader.readHexString(20));

        if (entry.isCompressed()) {
            entry.getBlocks().addAll(readFileBlockDescriptors(reader));
        }

        entry.setEncrypted(reader.read() != 0);

        if (entry.isEncrypted()) {
            throw new RuntimeException("Unsupported file encryption.");
        }

        entry.setBlockSize(reader.readInt());

        return entry;
    }


    private boolean readCompressionFlag(RandomAccessFileReader reader) throws IOException {
        int compressionFlags = reader.readInt();

        if (compressionFlags != 0 && compressionFlags != 1) {
            throw new RuntimeException("Unsupported compression type.");
        }

        return compressionFlags == 1;
    }


    private String readPakString(RandomAccessFileReader reader) throws IOException {
        int length = reader.readInt() - 1;
        String string = reader.readString(length);
        reader.skip(1); // null terminator

        return string;
    }


    private List<PakCompressedBlock> readFileBlockDescriptors(RandomAccessFileReader reader) throws IOException {
        List<PakCompressedBlock> blocks = new ArrayList<>();
        int numBlocks = reader.readInt();

        for (int i = 0; i < numBlocks; i++) {
            long chunkOffsetStart = reader.readLong();
            long chunkOffsetEnd = reader.readLong();
            blocks.add(new PakCompressedBlock(chunkOffsetStart, chunkOffsetEnd));
        }

        return blocks;
    }

}
