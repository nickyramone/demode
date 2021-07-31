package net.dbd.demode.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dbd.demode.util.event.EventListener;
import net.dbd.demode.util.event.EventSupport;
import net.dbd.demode.util.lang.OperationAbortedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
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
    }


    @RequiredArgsConstructor
    private final class CleanerFileVisitor extends SimpleFileVisitor<Path> {

        private final CleanerMonitor monitor;


        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (attrs.isRegularFile()) {
                deleteFileIfInstalledByDemode(file, monitor);
            }

            return super.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (Objects.requireNonNull(dir.toFile().listFiles()).length == 0) {
                Files.delete(dir);
            }

            return super.postVisitDirectory(dir, exc);
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
                Files.walkFileTree(basePath, new CleanerFileVisitor(monitor));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void deleteFileIfInstalledByDemode(Path filePath, CleanerMonitor monitor) {
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
