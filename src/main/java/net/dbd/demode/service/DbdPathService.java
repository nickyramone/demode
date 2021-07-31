package net.dbd.demode.service;

import net.dbd.demode.pak.PakFile;
import net.dbd.demode.util.lang.Pair;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * @author Nicky Ramone
 */
public class DbdPathService {

    private static final List<String> TYPICAL_STEAM_PATHS = List.of(
            "/Program Files (x86)/Steam",
            "/Program Files/Steam",
            "/Steam",
            "/SteamLibrary"
    );
    private static final String TYPICAL_DBD_INSTALLATION_PATH = "steamapps/common/Dead By Daylight";
    private static final String DBD_EXE_FILE = "DeadByDaylight.exe";
    private static final String PAK_FILE_REGEX = "pakchunk(\\d+)-WindowsNoEditor.pak";
    private static final Pattern PAK_FILE_PATTERN = Pattern.compile(PAK_FILE_REGEX);
    private static final Path PAKS_RELATIVE_PATH = Path.of("DeadByDaylight/Content/Paks");
    private static final Path COMMON_PREFIX_PATH = Path.of("../".repeat(PAKS_RELATIVE_PATH.getNameCount()));

    private final List<Path> pathsToCheckForDbd;


    public DbdPathService() {
        var typicalSteamPaths = TYPICAL_STEAM_PATHS.stream().map(Path::of).collect(toList());
        var fileDrives = Arrays.stream(File.listRoots())
                .map(File::toPath)
                .collect(toList());

        pathsToCheckForDbd = fileDrives.stream()
                .map(drive -> typicalSteamPaths.stream()
                        .map(path -> path.resolve(TYPICAL_DBD_INSTALLATION_PATH))
                        .map(drive::resolve)
                        .collect(toList())
                )
                .flatMap(Collection::stream)
                .collect(toList());
    }


    public Optional<Path> findDbdInstallationPath() {

        return pathsToCheckForDbd.stream()
                .filter(this::isValidDbdHomePath)
                .findAny();
    }


    public void verifyDbdHomePath(Path path) throws InvalidDbdHomePathException {
        if (!isValidDbdHomePath(path)) {
            throw new InvalidDbdHomePathException();
        }
    }


    public boolean isValidDbdHomePath(Path path) {
        return path.toFile().isDirectory()
                && path.resolve(DBD_EXE_FILE).toFile().exists()
                && !getAvailablePakFilenames(path).isEmpty();
    }


    public List<String> getAvailablePakFilenames(Path dbdHomePath) {
        Path paksAbsolutePath = dbdHomePath.resolve(PAKS_RELATIVE_PATH);

        return Stream.of(Optional.ofNullable(paksAbsolutePath.toFile().listFiles()).orElse(new File[0]))
                .map(File::getName)
                .filter(s -> s.matches(PAK_FILE_REGEX))
                .map(this::parsePakFilename)
                .sorted(Comparator.comparingInt(Pair::left))
                .map(Pair::right)
                .collect(toList());
    }

    private Pair<Integer, String> parsePakFilename(String filename) {
        Matcher matcher = PAK_FILE_PATTERN.matcher(filename);
        matcher.find();

        return Pair.of(Integer.parseInt(matcher.group(1)), filename);
    }

    public Path getPaksRelativePath() {
        return PAKS_RELATIVE_PATH;
    }

    public Path getCommonPrefixPath() {
        return COMMON_PREFIX_PATH;
    }

    public Path getPakFilePath(Path dbdHomePath, String filename) {
        return dbdHomePath.resolve(PAKS_RELATIVE_PATH).resolve(filename);
    }

    public List<PakFile> sortPakFiles(Collection<PakFile> pakFiles) {
        return pakFiles.stream()
                .sorted(comparing(pf -> parsePakFilename(pf.getFile().getName()).left()))
                .collect(toList());
    }

}
