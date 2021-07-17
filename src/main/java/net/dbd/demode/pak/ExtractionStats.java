package net.dbd.demode.pak;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;

/**
 * @author Nicky Ramone
 */
@Data
public class ExtractionStats {
    private Instant startTime;
    private Instant endTime;
    private int filesExtracted;
    private long bytesExtracted;
    private int filesToExtract;
    private long bytesToExtract;


    public void incrementFilesExtracted() {
        this.filesExtracted++;
    }

    public void incrementBytesExtracted(long bytes) {
        this.bytesExtracted += bytes;
    }

    public void start(int filesToExtract, long bytesToExtract) {
        startTime = Instant.now();
        endTime = null;
        this.filesToExtract = filesToExtract;
        this.bytesToExtract = bytesToExtract;
        this.filesExtracted = 0;
        this.bytesExtracted = 0;
    }

    public void stop() {
        endTime = Instant.now();
    }

    public int elapsed() {
        Instant endInstant = this.endTime != null ? this.endTime : Instant.now();
        return (int) Duration.between(startTime, endInstant).toSeconds();
    }

    public int eta() {
        return (int) ((double) bytesToExtract * elapsed() / bytesExtracted);
    }
}
