package net.dbd.demode.pak.domain;

import lombok.Data;

import java.io.File;

/**
 * Based lightly on the original C++ UnrealEngine pak library:
 * https://docs.unrealengine.com/4.26/en-US/API/Runtime/PakFile/
 *
 * @author Nicky Ramone
 */
@Data
public class Pak {

    private File file;
    private PakInfo pakInfo;
    private PakIndex pakIndex;

}
