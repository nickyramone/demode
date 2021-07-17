package net.dbd.demode.pak.domain;

import lombok.Data;

/**
 * Compressed files are divided into blocks designed for streaming.
 * Offsets are relative to the index entry.
 *
 * @author Nicky Ramone
 */
@Data
public class PakCompressedBlock {
    final long offsetStart;
    final long offsetEnd;

    public int size() {
        return (int) (offsetEnd - offsetStart);
    }
}
