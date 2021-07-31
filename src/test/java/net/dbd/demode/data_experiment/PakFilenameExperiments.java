package net.dbd.demode.data_experiment;

import net.dbd.demode.Factory;
import net.dbd.demode.pak.PakFile;
import net.dbd.demode.pak.domain.PakEntry;
import net.dbd.demode.pak.domain.PakIndex;
import net.dbd.demode.service.DbdPakManager;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @author Nicky Ramone
 */
public class PakFilenameExperiments {

    private static DbdPakManager dbdPakManager = Factory.newDbdPakManager(Path.of(Defaults.DBD_DIR));


    /**
     * Conclusion: Most files are sorted on each pak but some are not.
     */
    @Test
    public void showFilePathsThatAreNotSorted() {
        dbdPakManager.getPakFiles()
                .forEach(this::areAllFilePathsSorted);
    }

    private boolean areAllFilePathsSorted(PakFile pakFile) {
        boolean pathsAreSorted = true;
        String previousPathString = null;

        for (Path currentPath : pakFile.getFilePaths()) {
            String currentPathString = currentPath.toString().replace('\\', '/');

            if (previousPathString != null) {
                if (currentPathString.compareToIgnoreCase(previousPathString) < 0) {
                    System.out.printf("These paths are not sorted in pak file '%s':\n%s\n%s\n\n",
                            pakFile.getFile().getName(), previousPathString, currentPathString);
                    pathsAreSorted = false;
                }
            }

            previousPathString = currentPath.toString().replace('\\', '/');
        }

        return pathsAreSorted;
    }

    /**
     * Conclusion: There are ~1500 duplicate filenames.
     * Most are *DB.uasset and they might not be useful for modding.
     */
    @Test
    public void showDuplicateFilenames() {
        Map<String, Integer> frequencies = new TreeMap<>();

        for (PakFile pakFile : dbdPakManager.getPakFiles()) {
            updateFrequency(pakFile.getIndex(), frequencies);
        }

        var dupeFilenames = frequencies.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted(reverseOrder(comparingByValue()))
                .collect(toList());

        System.out.println(dupeFilenames.size() + " files with duplicates.");
        System.out.println(dupeFilenames);
    }

    private void updateFrequency(PakIndex pakIndex, Map<String, Integer> frequencies) {

        for (PakEntry entry : pakIndex.getEntries()) {
            String filename = entry.getFilePath().toFile().getName();

            Integer frequency = frequencies.get(filename);
            if (frequency == null) {
                frequency = 0;
            }

            frequency++;
            frequencies.put(filename, frequency);
        }
    }


    @Test
    public void locatePackagedFile() {
        var stringContainedInFilename = "T_DFHead00_01_BC.uasset";

        var filePaths = dbdPakManager.getPakFiles().stream()
                .map(PakFile::getFilePaths)
                .flatMap(Collection::stream)
                .filter(path -> path.getFileName().toString().contains(stringContainedInFilename))
                .map(path -> Path.of("../../..").relativize(path))
                .peek(System.out::println)
                .collect(toList());

        var selection = dbdPakManager.selectFiles(filePaths);

        selection.forEach(s -> s.getFilePaths().forEach(fp ->
                System.out.printf("file: %s; size: %d; pak: %s\n", fp, s.getPakFile().getEntry(fp).getSize(), s.getPakFile().getFile().getName())));

        System.out.println(selection);
    }

    /**
     * Conclusion: All mount points start with "../../../"
     */
    @Test
    public void showMountPoints() {
        dbdPakManager.getPakFiles().forEach(
                p -> System.out.println(p.getIndex().getMountPoint())
        );
    }

    @Test
    public void showFileExtensions() {
        var extensions = dbdPakManager.getPakFiles().stream()
                .map(PakFile::getFilePaths)
                .flatMap(Collection::stream)
                .map(p -> StringUtils.substringAfterLast(p.getFileName().toString(), "."))
                .collect(toSet());

        System.out.println("Found " + extensions.size() + " extensions: " + extensions);
    }

}
