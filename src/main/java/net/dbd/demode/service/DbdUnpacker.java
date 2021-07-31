package net.dbd.demode.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dbd.demode.pak.ExtractionStats;
import net.dbd.demode.pak.PakExtractor;
import net.dbd.demode.service.DbdPakManager.PakSelectionMonitor;
import net.dbd.demode.util.event.EventListener;
import net.dbd.demode.util.event.EventSupport;
import net.dbd.demode.util.lang.OperationAbortedException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.zip.DataFormatException;

/**
 * @author Nicky Ramone
 */
@RequiredArgsConstructor
@Slf4j
public class DbdUnpacker {

    public enum EventType {
        FILE_SELECT__BEGIN,
        FILE_SELECT__FILE_PROCESSED,
        FILE_SELECT__FINISH,

        UNPACK_BEGIN,
        PAK_EXTRACT_BEGIN,
        FILE_EXTRACTED,
        BYTES_EXTRACTED,
        PAK_EXTRACT_FINISH,
        UNPACK_FINISH,
        ABORTED
    }

    private static final long MiB = 1024 * 1024;
    private static final long GiB = 1024 * MiB;
    private static final long MIN_FREE_DISK_SPACE = 5 * GiB;

    private final FileMetadataManager fileMetadataManager;
    private final DbdPathService dbdPathService;
    private final DbdPakManager dbdPakManager;
    private final EventSupport eventSupport = new EventSupport();


    public static final class UnpackMonitor {
        private final EventSupport eventSupport = new EventSupport();
        @Getter
        private final ExtractionStats currentPakStats = new ExtractionStats();
        @Getter
        private final ExtractionStats totalStats = new ExtractionStats();
        @Getter
        private int currentPak;
        @Getter
        private int totalPaks;
        private PakSelectionMonitor pakSelectionMonitor;
        private PakExtractor pakExtractor;
        private Supplier<CompletableFuture<Void>> startAction;


        public UnpackMonitor() {
            pakSelectionMonitor = new PakSelectionMonitor();
            pakSelectionMonitor.registerListener(PakSelectionMonitor.EventType.BEGIN,
                    e -> fireEvent(EventType.FILE_SELECT__BEGIN, e.getValue()));
            pakSelectionMonitor.registerListener(PakSelectionMonitor.EventType.FILE_PROCESSED,
                    e -> fireEvent(EventType.FILE_SELECT__FILE_PROCESSED));
            pakSelectionMonitor.registerListener(PakSelectionMonitor.EventType.ABORTED,
                    e -> fireEvent(EventType.ABORTED));
        }

        private void setStartAction(Supplier<CompletableFuture<Void>> startAction) {
            this.startAction = startAction;
        }

        public CompletableFuture<Void> start() {
            return startAction.get();
        }

        public void abort() {
            if (pakSelectionMonitor != null) {
                pakSelectionMonitor.abort();
                return;
            }
            if (pakExtractor != null) {
                pakExtractor.abort();
            }
        }

        public UnpackMonitor registerListener(EventType eventType, EventListener eventListener) {
            eventSupport.registerListener(eventType, eventListener);
            return this;
        }

        public void fireEvent(Object eventType) {
            eventSupport.fireEvent(eventType);
        }

        public void fireEvent(Object eventType, Object eventValue) {
            eventSupport.fireEvent(eventType, eventValue);
        }
    }


    public UnpackMonitor unpackAll(Path outputPath) {
        UnpackMonitor monitor = new UnpackMonitor();
        monitor.setStartAction(() -> unpackAll(outputPath, monitor));

        return monitor;
    }

    private CompletableFuture<Void> unpackAll(Path outputPath, UnpackMonitor monitor) {
        return CompletableFuture.runAsync(() -> {
            monitor.pakSelectionMonitor = null;
            var selection = dbdPakManager.selectAllFiles();
            try {
                unpackSelection(selection, outputPath, monitor);
            } catch (DataFormatException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public UnpackMonitor unpackMissingAndUnverified(Path outputPath) {
        UnpackMonitor monitor = new UnpackMonitor();
        monitor.setStartAction(() -> unpackMissingAndUnverified(outputPath, monitor));

        return monitor;
    }

    private CompletableFuture<Void> unpackMissingAndUnverified(Path outputPath, UnpackMonitor unpackMonitor) {

        return dbdPakManager.selectMissingAndUnverified(unpackMonitor.pakSelectionMonitor).thenAccept(selection -> {
            unpackMonitor.pakSelectionMonitor = null;
            unpackMonitor.fireEvent(EventType.FILE_SELECT__FINISH, selection);
            try {
                unpackSelection(selection, outputPath, unpackMonitor);
            } catch (DataFormatException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void unpackSelection(MultiPakSelection selection, Path outputPath, UnpackMonitor unpackMonitor)
            throws DataFormatException, IOException {

        if (selection.getTotalFiles() == 0) {
            return;
        }

        verifyFreeDiskSpace(outputPath, selection.getTotalBytes() + MIN_FREE_DISK_SPACE);

        unpackMonitor.totalPaks = selection.getSinglePakSelections().size();
        unpackMonitor.totalStats.start(selection.getTotalFiles(), selection.getTotalBytes());
        unpackMonitor.fireEvent(EventType.UNPACK_BEGIN);

        for (SinglePakSelection singlePakSelection : selection) {

            if (!unpackPak(singlePakSelection, outputPath.resolve(dbdPathService.getPaksRelativePath()), unpackMonitor)) {
                return;
            }
        }

        unpackMonitor.totalStats.stop();
        unpackMonitor.fireEvent(EventType.UNPACK_FINISH);
    }

    private boolean unpackPak(SinglePakSelection pakSelection, Path outputPath, UnpackMonitor unpackMonitor)
            throws DataFormatException, IOException {

        unpackMonitor.currentPak++;
        unpackMonitor.currentPakStats.start(pakSelection.getTotalFiles(), pakSelection.getTotalBytes());
        unpackMonitor.fireEvent(EventType.PAK_EXTRACT_BEGIN, pakSelection.getPakFile());

        PakExtractor pakExtractor = new PakExtractor();
        pakExtractor.registerListener(PakExtractor.EventType.FILE_EXTRACTED,
                e -> handleFileExtractedEvent(unpackMonitor, (PakExtractor.ExtractedFileInfo) e.getValue()));
        pakExtractor.registerListener(PakExtractor.EventType.BYTES_EXTRACTED,
                e -> handleBytesExtractedEvent(unpackMonitor, (long) e.getValue()));
        pakExtractor.registerListener(PakExtractor.EventType.PAK_EXTRACTED,
                e -> handlePakExtractedEvent(unpackMonitor));
        pakExtractor.registerListener(PakExtractor.EventType.ABORTED,
                e -> handleExtractAbort());
        unpackMonitor.pakExtractor = pakExtractor;

        try {
            pakExtractor.extract(pakSelection.getPakFile(), pakSelection.getFilePaths(), outputPath);
        } catch (OperationAbortedException e) {
            unpackMonitor.currentPakStats.stop();
            unpackMonitor.totalStats.stop();
            unpackMonitor.fireEvent(EventType.ABORTED);
            return false;
        }

        return true;
    }


    private void verifyFreeDiskSpace(Path targetPath, long sizeInBytes) {
        if (targetPath.toFile().getFreeSpace() < sizeInBytes) {
            throw new InsufficientDiskSpaceException(sizeInBytes);
        }
    }

    private void handleFileExtractedEvent(UnpackMonitor unpackMonitor, PakExtractor.ExtractedFileInfo extractedFileInfo) {
        unpackMonitor.currentPakStats.incrementFilesExtracted();
        unpackMonitor.totalStats.incrementFilesExtracted();
        fileMetadataManager.writeHash(extractedFileInfo.getFile().toPath(), extractedFileInfo.getHash());
        unpackMonitor.fireEvent(EventType.FILE_EXTRACTED);
    }

    private void handleBytesExtractedEvent(UnpackMonitor unpackMonitor, long bytes) {
        unpackMonitor.currentPakStats.incrementBytesExtracted(bytes);
        unpackMonitor.totalStats.incrementBytesExtracted(bytes);
        unpackMonitor.fireEvent(EventType.BYTES_EXTRACTED);
    }

    private void handlePakExtractedEvent(UnpackMonitor unpackMonitor) {
        unpackMonitor.currentPakStats.stop();
        unpackMonitor.fireEvent(EventType.PAK_EXTRACT_FINISH, unpackMonitor.currentPakStats.elapsed());
    }

    private void handleExtractAbort() {
        fireEvent(EventType.ABORTED);
    }

    private void fireEvent(EventType eventType) {
        eventSupport.fireEvent(eventType);
    }

}
