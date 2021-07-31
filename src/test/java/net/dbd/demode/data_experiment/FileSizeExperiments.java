package net.dbd.demode.data_experiment;

import net.dbd.demode.Factory;
import net.dbd.demode.pak.domain.PakEntry;
import net.dbd.demode.pak.domain.PakIndex;
import net.dbd.demode.service.DbdPakManager;
import org.junit.Test;

/**
 * @author Nicky Ramone
 */
public class FileSizeExperiments {

    /**
     * Conclusion: largest file is ~170 MB
     */
    @Test
    public void showLargestFileOnEachPak() {
        DbdPakManager dbdPakManager = Factory.newDbdPakManager(Defaults.DBD_HOME);

        dbdPakManager.getPakFiles().stream()
                .map(p -> findLargestFile(p.getIndex()))
                .forEach(e -> System.out.printf("%s | %d bytes | %s\n",
                        e.getFilePath(), e.getSize(), e.getBlocks().isEmpty() ? "" : " chunked"));

    }

    private PakEntry findLargestFile(PakIndex pakIndex) {

        return pakIndex.getEntries().stream()
                .reduce(pakIndex.getEntries().get(0), (e1, e2) -> e2.getSize() >= e1.getSize() ? e2 : e1);
    }

}
