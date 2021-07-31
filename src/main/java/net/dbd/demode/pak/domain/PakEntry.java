package net.dbd.demode.pak.domain;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * An index entry.
 *
 * @author Nicky Ramone
 */
@Data
public class PakEntry {

    private Path filePath;         // path to the filename (relative to the mount point)
    private long offset;           // absolute offset where the file data begins
    private long compressedSize;
    private long size;
    private boolean compressed;
    private String hash;
    private final List<PakCompressedBlock> blocks = new ArrayList<>();
    private boolean encrypted;
    private int blockSize;         /* If there is no compression, it will be 0.
                                      If not, normally it's going to be 65536 (64 KiB) if the uncompressed size is bigger
                                      or equal than this; otherwise it will be equal to the uncompressed size. */

}
