package net.dbd.demode.service;

import lombok.Getter;
import net.dbd.demode.pak.PakFile;
import net.dbd.demode.util.event.EventListener;
import net.dbd.demode.util.event.EventSupport;
import net.dbd.demode.util.lang.OperationAbortedException;
import net.dbd.demode.util.lang.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.*;

/**
 * @author Nicky Ramone
 */
public class DbdPakManager {

    public static class PakSelectionMonitor {

        public enum EventType {BEGIN, FILE_PROCESSED, ABORTED}

        private final EventSupport eventSupport = new EventSupport();

        private boolean abort;


        public void registerListener(EventType eventType, EventListener eventListener) {
            eventSupport.registerListener(eventType, eventListener);
        }

        public void abort() {
            abort = true;
        }

        private void fireBeginEvent(int totalFiles) {
            eventSupport.fireEvent(EventType.BEGIN, totalFiles);
        }

        private void fireFileProcessedEvent() {
            eventSupport.fireEvent(EventType.FILE_PROCESSED);
        }

        private void fireAbortEvent() {
            eventSupport.fireEvent(EventType.ABORTED);
        }

        private void fireFinishEvent() {
            eventSupport.fireEvent(EventType.FILE_PROCESSED);
        }

    }


    private final Map<Path, PakFile> index = new HashMap<>();

    @Getter
    private final List<PakFile> pakFiles = new ArrayList<>();

    private final FileMetadataManager fileMetadataManager;
    private final DbdPathService dbdPathService;
    private final Path dbdAbsolutePath;


    public DbdPakManager(FileMetadataManager fileMetadataManager, DbdPathService dbdPathService, Path dbdAbsolutePath)
            throws InvalidDbdHomePathException {

        this.fileMetadataManager = fileMetadataManager;
        this.dbdPathService = dbdPathService;
        this.dbdAbsolutePath = dbdAbsolutePath;

        dbdPathService.verifyDbdHomePath(dbdAbsolutePath);
        instantiatePakFiles();
        reindexPaks();
    }


    private void instantiatePakFiles() {
        dbdPathService.getAvailablePakFilenames(dbdAbsolutePath).forEach(f -> {
            var file = dbdPathService.getPakFilePath(dbdAbsolutePath, f).toFile();
            pakFiles.add(new PakFile(file));
        });
    }

    private void reindexPaks() {
        index.clear();
        pakFiles.forEach(this::reindexPak);
    }

    private void reindexPak(PakFile pakFile) {
        pakFile.getFilePaths().forEach(filePath -> index.put(filePath, pakFile));
    }


    public MultiPakSelection selectAllFiles() {

        return new MultiPakSelection(pakFiles.stream()
                .map(pakFile -> new SinglePakSelection(pakFile, pakFile.getFilePaths()))
                .collect(toList()));
    }


    public CompletableFuture<MultiPakSelection> selectMissingAndUnverified(PakSelectionMonitor progressMonitor) {

        return CompletableFuture.supplyAsync(() -> {
            progressMonitor.fireBeginEvent(index.size());
            var multiPakSelection = new MultiPakSelection();

            for (var pakFile : pakFiles) {
                SinglePakSelection singlePakSelection;
                try {
                    singlePakSelection = selectMissingAndUnverified(pakFile, progressMonitor);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (singlePakSelection.getTotalFiles() > 0) {
                    multiPakSelection.addSinglePakSelection(singlePakSelection);
                }
            }
            progressMonitor.fireFinishEvent();

            return multiPakSelection;
        });
    }


    private SinglePakSelection selectMissingAndUnverified(PakFile pakFile, PakSelectionMonitor progressMonitor)
            throws OperationAbortedException, IOException {

        Path mountPoint = pakFile.getIndex().getMountPoint();
        var singlePakSelection = new SinglePakSelection(pakFile);

        for (var pakEntry : pakFile.getIndex().getEntries()) {

            if (progressMonitor.abort) {
                throw new OperationAbortedException();
            }

            Path filePath = mountPoint.resolve(pakEntry.getFilePath());

            if (isFileMissingOrUnverified(pakFile, filePath)) {
                singlePakSelection.addFile(filePath);
            }

            progressMonitor.fireFileProcessedEvent();
        }

        return singlePakSelection;
    }

    private boolean isFileMissingOrUnverified(PakFile pakFile, Path relativeTargetFilePath) throws IOException {
        Path targetFilePath = pakFile.getFile().getParentFile().toPath().resolve(relativeTargetFilePath).normalize();

        if (!targetFilePath.toFile().exists()) {
            return true;
        }

        String hash = fileMetadataManager.readHash(targetFilePath);

        return hash == null || !pakFile.getFileHash(relativeTargetFilePath).equals(hash);
    }


    public MultiPakSelection selectFiles(Collection<Path> filePaths) {

        Set<Path> relativizedPaths = filePaths.stream()
                .map(dbdPathService.getCommonPrefixPath()::resolve)
                .collect(toSet());

        Set<Path> invalidPaths = relativizedPaths.stream()
                .filter(p -> !index.containsKey(p))
                .map(dbdPathService.getCommonPrefixPath()::relativize)
                .collect(toSet());

        if (!invalidPaths.isEmpty()) {
            throw new IllegalArgumentException("Invalid file paths provided: " + invalidPaths);
        }

        return createMultiPakSelection(relativizedPaths);
    }


    /**
     * We guarantee that the paks will be ordered by pak number, and within each pak, files will be sorted
     * corresponding to the order in which they appear inside the pak file.
     */
    private MultiPakSelection createMultiPakSelection(Set<Path> relativizedPaths) {
        Map<PakFile, Set<Path>> filesByPak = relativizedPaths.stream()
                .map(path -> Pair.of(index.get(path), path))
                .collect(
                        groupingBy(Pair::left, mapping(Pair::right, toSet()))
                );

        MultiPakSelection result = new MultiPakSelection();

        dbdPathService.sortPakFiles(filesByPak.keySet()).forEach(pakFile ->
                result.addSinglePakSelection(createSinglePakSelection(pakFile, filesByPak.get(pakFile))));

        return result;
    }

    /**
     * We guarantee the order of the paths to match the same order as inside the pak file.
     */
    private SinglePakSelection createSinglePakSelection(PakFile pakFile, Set<Path> filePaths) {
        SinglePakSelection selection = new SinglePakSelection(pakFile);

        pakFile.getFilePaths().stream()
                .filter(filePaths::contains)
                .forEach(selection::addFile);

        return selection;
    }


    public PakFile getPakFileForPackedFile(Path packedFilePath) {
        return index.get(dbdPathService.getCommonPrefixPath().resolve(packedFilePath));
    }

    public Set<Path> getPackedFiles() {
        return index.keySet();
    }
}
