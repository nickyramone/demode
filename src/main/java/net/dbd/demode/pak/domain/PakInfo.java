package net.dbd.demode.pak.domain;

import lombok.Data;

/**
 * Entry point of a pak file.
 *
 * @author Nicky Ramone
 */
@Data
public class PakInfo {
    private int version;
    private long indexOffset;
    private long indexSize;
    private String indexHash;
}
