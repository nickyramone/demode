package net.dbd.demode.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
@Getter
@ToString
public final class MultiPakSelection implements Iterable<SinglePakSelection> {
    private final List<SinglePakSelection> singlePakSelections = new ArrayList<>();
    private int totalFiles;
    private long totalBytes;

    public MultiPakSelection(Collection<SinglePakSelection> singlePakSelections) {
        singlePakSelections.forEach(this::addSinglePakSelection);
    }

    public void addSinglePakSelection(SinglePakSelection selection) {
        singlePakSelections.add(selection);
        totalFiles += selection.getTotalFiles();
        totalBytes += selection.getTotalBytes();
    }

    @Override
    public Iterator<SinglePakSelection> iterator() {
        return singlePakSelections.iterator();
    }
}
