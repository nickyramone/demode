package net.dbd.demode.manual;

import net.dbd.demode.Factory;
import net.dbd.demode.pak.domain.Pak;
import net.dbd.demode.pak.domain.PakEntry;
import net.dbd.demode.pak.domain.PakIndex;
import net.dbd.demode.service.DbdUnpacker;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toList;

public class FilenameFrequencyManualTest {

    private static DbdUnpacker dbdUnpacker;
    private static Map<String, Integer> frequencies = new TreeMap<>();


    @BeforeClass
    public static void setUp() throws Exception {
        dbdUnpacker = new DbdUnpacker(Factory.pakMetaReader(), Defaults.DBD_DIR);

        for (Pak pak : dbdUnpacker.getPaks()) {
            updateFrequency(pak.getPakIndex());
        }
    }

    private static void updateFrequency(PakIndex pakIndex) {

        for (PakEntry entry : pakIndex.getEntries()) {
            String filename = new File(entry.getFilename()).getName();

            Integer frequency = frequencies.get(filename);
            if (frequency == null) {
                frequency = 0;
            }

            frequency++;
            frequencies.put(filename, frequency);
        }
    }

    @Test
    @Ignore
    public void showDuplicateFilenames() throws Exception {
        var dupeFilenames = frequencies.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted(reverseOrder(comparingByValue()))
                .collect(toList());

        System.out.println(dupeFilenames);
    }

    @Test
    @Ignore
    public void showAllPathsForFilename() {
        String filename = "MI_MSHead00_01.uexp";

        List<String> filePaths = dbdUnpacker.getPaks().stream()
                .map(p -> p.getPakIndex().getEntries())
                .flatMap(Collection::stream)
                .map(PakEntry::getFilename)
                .filter(f -> new File(f).getName().equals(filename))
                .toList();

        System.out.println(filePaths);
    }


}
