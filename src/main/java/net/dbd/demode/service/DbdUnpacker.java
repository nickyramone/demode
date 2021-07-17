package net.dbd.demode.service;

import lombok.*;
import net.dbd.demode.pak.ExtractionStats;
import net.dbd.demode.pak.PakExtractor;
import net.dbd.demode.pak.PakMetaReader;
import net.dbd.demode.pak.domain.Pak;
import net.dbd.demode.util.event.EventListener;
import net.dbd.demode.util.event.EventSupport;
import net.dbd.demode.util.lang.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

import static java.util.stream.Collectors.toList;

/**
 * @author Nicky Ramone
 */
@RequiredArgsConstructor
public class DbdUnpacker {

    private static final String PAK_FILE_REGEX = "pakchunk(\\d+)-WindowsNoEditor.pak";
    private static final Pattern PAK_FILE_PATTERN = Pattern.compile(PAK_FILE_REGEX);
    private static final String PAKS_RELATIVE_PATH = "DeadByDaylight/Content/Paks";
    private static final String FILE_ATTR__ORIGINAL = "user.demode.original";


    public enum EventType {
        UNPACK_BEGIN,
        PAK_EXTRACT_BEGIN,
        FILE_EXTRACTED,
        BYTES_EXTRACTED,
        PAK_EXTRACT_FINISH,
        UNPACK_FINISH,
        ABORTED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class PaksSelection {
        private int numFiles;
        private long totalSize;
        private List<Pak> paks = new ArrayList<>();

        public void addPaks(List<Pak> paks) {
            for (Pak pak : paks) {
                addPak(pak);
            }
        }

        public void addPak(Pak pak) {
            this.paks.add(pak);
            numFiles += pak.getPakIndex().getNumEntries();
            totalSize += pak.getPakIndex().getTotalEntriesSize();
        }

        public void removePak(int index) {
            Pak removed = paks.remove(index);
            numFiles -= removed.getPakIndex().getNumEntries();
            totalSize -= removed.getPakIndex().getTotalEntriesSize();
        }
    }


    @Getter
    public class UnpackStatus {

        @Setter
        private int currentPak;
        @Setter
        private int totalPaks;
        @Setter
        private PakExtractor pakExtractor;

        private final ExtractionStats currentPakStats = new ExtractionStats();
        private final ExtractionStats totalStats = new ExtractionStats();
        private final EventSupport eventSupport = new EventSupport();


        public void abort() {
            pakExtractor.abort();
        }

    }


    private final PakMetaReader pakMetaReader;
    //    private final PakExtractor pakExtractor;
    private final EventSupport eventSupport = new EventSupport();
    private final Path paksPath;

    @Getter
    private final List<Pak> paks;

    private boolean aborted;


    public DbdUnpacker(PakMetaReader pakMetaReader, String dbdDirectory)
            throws IOException {

        this.pakMetaReader = pakMetaReader;
        this.paksPath = Path.of(dbdDirectory, PAKS_RELATIVE_PATH);
        this.paks = parsePakFiles(paksPath);
    }


    public PaksSelection selectAllPaks() {
        PaksSelection selection = new PaksSelection();
        selection.addPaks(paks);

        return selection;
    }

    public PaksSelection selectPaks(int... indexes) {
        PaksSelection selection = new PaksSelection();

        for (int i = 0; i < indexes.length; i++) {
            selection.addPak(paks.get(indexes[i]));
        }


        return selection;
    }


    public List<String> listPakFiles(Path paksPath) throws IOException {

        return Stream.of(Optional.ofNullable(paksPath.toFile().listFiles()).orElse(new File[0]))
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

        return new Pair<>(Integer.parseInt(matcher.group(1)), filename);
    }

    private List<Pak> parsePakFiles(Path paksBasePath) throws IOException {

        return listPakFiles(this.paksPath).stream()
                .map(filename -> paksBasePath.resolve(filename).normalize().toFile())
//                .map(pakPath -> pakParser.parse(pakPath, Path.of(PAKS_RELATIVE_PATH)))
                .map(pakPath -> pakMetaReader.parse(pakPath))
                .collect(toList());
    }


    public void unpack(PaksSelection selection, String outputPath) throws DataFormatException, IOException {
        UnpackStatus unpackStatus = new UnpackStatus();
        unpackStatus.setTotalPaks(selection.getPaks().size());
        unpackStatus.getTotalStats().start(selection.numFiles, selection.totalSize);
        fireEvent(EventType.UNPACK_BEGIN, unpackStatus);

        for (Pak pak : selection.getPaks()) {

            if (!unpackPak(pak, Path.of(outputPath), unpackStatus)) {
                return;
            }
        }

        unpackStatus.getTotalStats().stop();
        fireEvent(EventType.UNPACK_FINISH, unpackStatus);
    }


    private boolean unpackPak(Pak pak, Path outputPath, UnpackStatus unpackStatus) throws DataFormatException, IOException {
        ExtractionStats currentPakStats = unpackStatus.getCurrentPakStats();
        unpackStatus.currentPak++;
        currentPakStats.start(pak.getPakIndex().getEntries().size(), pak.getPakIndex().getTotalEntriesSize());
        fireEvent(EventType.PAK_EXTRACT_BEGIN, pak);

        PakExtractor pakExtractor = new PakExtractor();
        pakExtractor.registerListener(PakExtractor.EventType.FILE_EXTRACTED, e -> handleFileExtractedEvent(unpackStatus, (File) e.getValue()));
        pakExtractor.registerListener(PakExtractor.EventType.BYTES_EXTRACTED, e -> handleBytesExtractedEvent(unpackStatus, (long) e.getValue()));
        pakExtractor.registerListener(PakExtractor.EventType.PAK_EXTRACTED, e -> handlePakExtractedEvent(unpackStatus));
        pakExtractor.registerListener(PakExtractor.EventType.ABORTED, e -> handleExtractAbort());
        unpackStatus.setPakExtractor(pakExtractor);


        boolean finished = pakExtractor.extract(pak, outputPath);

        if (!finished) {
            unpackStatus.getCurrentPakStats().stop();
            unpackStatus.getTotalStats().stop();
            fireEvent(EventType.ABORTED);
        }

        return finished;
    }


    public void abortUnpacking() {
        aborted = true;
    }


    private void handleFileExtractedEvent(UnpackStatus unpackStatus, File file) {
        unpackStatus.getCurrentPakStats().incrementFilesExtracted();
        unpackStatus.getTotalStats().incrementFilesExtracted();
        markFileAsOriginal(file);
        fireEvent(EventType.FILE_EXTRACTED, unpackStatus);
    }

    private void handleBytesExtractedEvent(UnpackStatus unpackStatus, long bytes) {
        unpackStatus.getCurrentPakStats().incrementBytesExtracted(bytes);
        unpackStatus.getTotalStats().incrementBytesExtracted(bytes);
        fireEvent(EventType.BYTES_EXTRACTED, unpackStatus);
    }

    private void handlePakExtractedEvent(UnpackStatus unpackStatus) {
        unpackStatus.getCurrentPakStats().stop();
        fireEvent(EventType.PAK_EXTRACT_FINISH, unpackStatus.currentPakStats.elapsed());
    }

    private void handleExtractAbort() {
        abortUnpacking();
        fireEvent(EventType.ABORTED);
    }


    private void markFileAsOriginal(File file) {
        UserDefinedFileAttributeView attributeView = Files.getFileAttributeView(file.toPath(), UserDefinedFileAttributeView.class);
        try {
            attributeView.write(FILE_ATTR__ORIGINAL, ByteBuffer.allocate(0));
        } catch (IOException e) {
            throw new RuntimeException("Failed to mark file as original: " + file.toPath());
        }
    }

    public void registerListener(EventType eventType, EventListener eventListener) {
        eventSupport.registerListener(eventType, eventListener);
    }

    private void fireEvent(EventType eventType) {
        eventSupport.fireEvent(eventType);
    }

    private void fireEvent(EventType eventType, Object eventValue) {
        eventSupport.fireEvent(eventType, eventValue);
    }

}
