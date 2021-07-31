package net.dbd.demode.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dbd.demode.util.event.EventListener;
import net.dbd.demode.util.event.EventSupport;
import net.dbd.demode.util.lang.OperationAbortedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author Nicky Ramone
 */
@RequiredArgsConstructor
public class FileCleaner {

    public enum EventType {
        FILE_SCANNED
    }

    public static final class CleanerMonitor {
        private final EventSupport eventSupport = new EventSupport();
        private Supplier<CompletableFuture<Void>> action;
        @Getter
        private int totalFiles;
        @Getter
        private int filesDeleted;
        @Getter
        private long bytesFreed;
        private boolean abort;


        public CompletableFuture<Void> start() {
            return action.get();
        }

        public void abort() {
            abort = true;
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


    private final FileMetadataManager fileMetadataManager;


    public CleanerMonitor clean(Path basePath) {
        var monitor = new CleanerMonitor();
        monitor.action = () -> cleanFiles(basePath, monitor);

        return monitor;
    }

    private CompletableFuture<Void> cleanFiles(Path basePath, CleanerMonitor monitor) {

        return CompletableFuture.runAsync(() -> {
            try {
                Files.walk(basePath)
                        .filter(Files::isRegularFile)
                        .forEach(filePath -> processFile(filePath, monitor));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void processFile(Path filePath, CleanerMonitor monitor) {
        if (monitor.abort) {
            throw new OperationAbortedException();
        }

        monitor.totalFiles++;
        if (fileMetadataManager.readHash(filePath) != null) {
            File file = filePath.toFile();
            long fileSize = file.length();

            if (file.delete()) {
                monitor.filesDeleted++;
                monitor.bytesFreed += fileSize;
            }
        }
        monitor.fireEvent(EventType.FILE_SCANNED);
    }

}
