package net.dbd.demode.pak;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dbd.demode.pak.domain.*;
import net.dbd.demode.util.io.RandomAccessFileWriter;
import org.apache.commons.codec.binary.Hex;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * @author Nicky Ramone
 */
@RequiredArgsConstructor
public class PakFile {

    private static final int BUFFER_SIZE = 8 * 1024;
    private static final int SHA1_LENGTH = 20;

    @Getter
    private final File file;

    @Getter(AccessLevel.PACKAGE)
    private final Pak pak;
    private final transient byte[] buffer = new byte[BUFFER_SIZE];
    private final transient Map<Path, PakEntry> index = new LinkedHashMap<>(); /* we are interested in keeping the keys
                                                                                  in order of insertion. */

    public PakFile(File file) {
        this.file = file;
        this.pak = PakMetaReader.parse(file);

        initIndex();
    }

    private void initIndex() {
        Path mountPoint = pak.getIndex().getMountPoint();

        for (var pakEntry : pak.getIndex().getEntries()) {
            Path filePath = mountPoint.resolve(pakEntry.getFilePath()).normalize();
            index.put(filePath, pakEntry);
        }
    }


    public void softDeleteFiles(List<Path> filePaths) {

        var entriesToRemove = filePaths.stream()
                .map(index::get)
                .collect(toList());

        pak.getIndex().getEntries().removeAll(entriesToRemove);
    }

    public void renameFilesWithSuffix(List<Path> filePaths, String suffix) {

        for (var filePath : filePaths) {
            PakEntry pakEntry = index.get(filePath);
            pakEntry.setFilePath(pakEntry.getFilePath().resolve(suffix));
        }
    }

    public void replaceFile(File inputFile, Path packedFilePath) throws Exception {
        PakEntry fileEntry = index.get(packedFilePath);

        if (fileEntry.isCompressed()) {
            throw new IllegalArgumentException("Compressed files not supported yet.");
        }

        if (inputFile.length() > fileEntry.getSize()) {
            throw new IllegalArgumentException("File to replace cannot be bigger than existing file.");
        }

        String hash;

        try (RandomAccessFileWriter raf = new RandomAccessFileWriter(this.file, RandomAccessFileWriter.Endianness.LITTLE_ENDIAN)) {
            raf.seek(fileEntry.getOffset());
            hash = writeFile(inputFile, raf);
        }

        fileEntry.setSize(inputFile.length());
        fileEntry.setCompressedSize(fileEntry.getSize());
        fileEntry.setHash(hash);
    }

    public void appendFile(File inputFile, Path targetPath) throws Exception {
        if (!targetPath.startsWith(pak.getIndex().getMountPoint())) {
            throw new IllegalArgumentException("Target path must start with the mount point.");
        }

        long offset;
        String sha1;

        try (RandomAccessFileWriter raf = new RandomAccessFileWriter(this.file, RandomAccessFileWriter.Endianness.LITTLE_ENDIAN)) {
            offset = seekAppendPosition(raf);
            sha1 = writeFile(inputFile, raf);
        }

        PakEntry newEntry = new PakEntry();
        newEntry.setFilePath(targetPath);
        newEntry.setSize(inputFile.length());
        newEntry.setCompressedSize(inputFile.length());
        newEntry.setCompressed(false);
        newEntry.setHash(sha1);
        newEntry.setOffset(offset);

        PakIndex pakIndex = pak.getIndex();
        pakIndex.addEntry(newEntry);

        pak.getInfo().setIndexOffset(offset + newEntry.getCompressedSize() + PakConstants.BYTES_BETWEEN_LAST_ENTRY_AND_INDEX);
    }


    private String writeFile(File inputFile, RandomAccessFileWriter raf) throws IOException, DigestException {
        byte[] sha1 = new byte[SHA1_LENGTH];
        MessageDigest digest = createDigest();

        try (BufferedInputStream bif = new BufferedInputStream(new FileInputStream(inputFile))) {
            int bytesRead;
            do {
                bytesRead = bif.read(buffer, 0, BUFFER_SIZE);
                if (bytesRead > 0) {
                    raf.write(buffer, bytesRead);
                    digest.update(buffer, 0, bytesRead);
                }
            }
            while (bytesRead > 0);
        }

        digest.digest(sha1, 0, SHA1_LENGTH);

        return Hex.encodeHexString(sha1);
    }

    private long seekAppendPosition(RandomAccessFileWriter raf) throws IOException {
        List<PakEntry> entries = pak.getIndex().getEntries();

        if (entries.isEmpty()) {
            return 0;
        }

        var lastEntry = entries.get(entries.size() - 1);
        long offset;

        if (!lastEntry.isCompressed()) {
            offset = lastEntry.getOffset() + lastEntry.getCompressedSize();
        } else {
            offset = lastEntry.getOffset() + lastEntry.getSize();
        }
        raf.seek(offset);

        return offset;
    }

    private MessageDigest createDigest() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Invalid algorithm for calculating sha1 signature.", e);
        }
        return digest;
    }

    public void writeMetadata() throws Exception {
        PakMetaWriter.writeMetadata(this);
    }

    public Set<Path> getFilePaths() {
        return index.keySet();
    }

    public PakInfo getInfo() {
        return pak.getInfo();
    }

    public PakIndex getIndex() {
        return pak.getIndex();
    }

    public PakEntry getEntry(Path filePath) {
        return index.get(filePath);
    }

    public long getFileSize(Path filePath) {
        return index.get(filePath).getSize();
    }

    public String getFileHash(Path filePath) {
        return index.get(filePath).getHash();
    }

}
