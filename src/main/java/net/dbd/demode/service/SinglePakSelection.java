package net.dbd.demode.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.dbd.demode.pak.PakFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Getter
@ToString
public final class SinglePakSelection {
    private final PakFile pakFile;
    private final List<Path> filePaths = new ArrayList<>();
    private int totalFiles;
    private long totalBytes;


    public SinglePakSelection(PakFile pakFile, Collection<Path> filePaths) {
        this.pakFile = pakFile;
        filePaths.forEach(this::addFile);
    }

    public void addFile(Path path) {
        filePaths.add(path);
        totalFiles++;
        totalBytes += pakFile.getFileSize(path);
    }

}
