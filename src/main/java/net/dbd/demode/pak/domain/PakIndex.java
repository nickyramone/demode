package net.dbd.demode.pak.domain;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Index (table of contents).
 *
 * @author Nicky Ramone
 */
@Data
public class PakIndex {
    private Path mountPoint;
    private List<PakEntry> entries = new ArrayList<>();

    public void addEntry(PakEntry entry) {
        entries.add(entry);
    }

}
