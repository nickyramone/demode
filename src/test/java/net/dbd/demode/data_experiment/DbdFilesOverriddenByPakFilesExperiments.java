package net.dbd.demode.data_experiment;

import net.dbd.demode.Factory;
import net.dbd.demode.service.DbdPakManager;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * @author Nicky Ramone
 */
public class DbdFilesOverriddenByPakFilesExperiments {

    private DbdPakManager dbdPakManager;

    @Before
    public void setUp() {
        dbdPakManager = Factory.newDbdPakManager(Defaults.DBD_HOME);
    }


    /**
     * Conclusion: Files inside .pak files do not override any files in DBD home.
     */
    @Test
    public void findPackagedFilesThatAlsoAppearInTheDbdDir() throws Exception {
        Set<Path> packedFiles = dbdPakManager.getPackedFiles();

        Set<Path> dbdDirFiles = Files.walk(Defaults.DBD_HOME)
                .filter(Files::isRegularFile)
                .map(Defaults.DBD_HOME::relativize)
                .collect(toSet());

        Set<Path> intersection = new HashSet<>(dbdDirFiles);
        intersection.retainAll(packedFiles);

        System.out.println("packed files : " + packedFiles.size());
        System.out.println("dbd dir files: " + dbdDirFiles.size());
        System.out.println("intersection : " + intersection.size());
    }


}
