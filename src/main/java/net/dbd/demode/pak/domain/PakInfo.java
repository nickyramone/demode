package net.dbd.demode.pak.domain;

import lombok.Data;

/**
 * Header (it's actually the footer) of a pak file.
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
