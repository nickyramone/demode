package net.dbd.demode.manual;

import net.dbd.demode.Factory;
import net.dbd.demode.pak.domain.PakEntry;
import net.dbd.demode.pak.domain.PakIndex;
import net.dbd.demode.service.DbdUnpacker;
import org.junit.Ignore;
import org.junit.Test;

import static java.util.stream.Collectors.toList;

public class LargestFilesManualTest {


    @Test
    @Ignore
    public void showLargestFileOnEachPak() throws Exception {

        DbdUnpacker dbdUnpacker = new DbdUnpacker(Factory.pakMetaReader(), Defaults.DBD_DIR);

        dbdUnpacker.getPaks().stream()
                .map(p -> findLargestFile(p.getPakIndex()))
                .peek(e -> System.out.printf("%s | %d bytes | %s\n",
                        e.getFilename(), e.getSize(), e.getBlocks().isEmpty() ? "" : " chunked"))
                .collect(toList());
    }

    private PakEntry findLargestFile(PakIndex pakIndex) {

        return pakIndex.getEntries().stream()
                .reduce(pakIndex.getEntries().get(0), (e1, e2) -> e2.getSize() >= e1.getSize() ? e2 : e1);
    }

}
