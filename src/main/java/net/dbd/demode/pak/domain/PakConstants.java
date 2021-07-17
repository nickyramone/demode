package net.dbd.demode.pak.domain;

import lombok.experimental.UtilityClass;

/**
 * @author Nicky Ramone
 */
@UtilityClass
public class PakConstants {

    public static final int PAK_MAGIC = 0x5a6f12e1;
    public static final int PAK_INFO_OFFSET_FROM_EOF = -205;
    public static final int BYTES_BETWEEN_INDEX_AND_PAK_INFO = 17;

}
