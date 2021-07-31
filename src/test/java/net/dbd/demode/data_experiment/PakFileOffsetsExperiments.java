package net.dbd.demode.data_experiment;

import net.dbd.demode.Factory;
import net.dbd.demode.pak.PakFile;
import net.dbd.demode.pak.domain.PakConstants;
import net.dbd.demode.pak.domain.PakEntry;
import net.dbd.demode.pak.domain.PakInfo;
import net.dbd.demode.service.DbdPakManager;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author Nicky Ramone
 */
public class PakFileOffsetsExperiments {

    private DbdPakManager dbdPakManager = Factory.newDbdPakManager(Defaults.DBD_HOME);


    @Test
    public void showOffsetsBetweenPakInfoAndIndex() {

        dbdPakManager.getPakFiles()
                .forEach(this::printOffsetsInfo);
    }


    private void printOffsetsInfo(PakFile pakFile) {

        long minOffset = Long.MAX_VALUE;
        long minOffsetEntryIndex = -1;
        long maxOffset = -1;
        long maxOffsetEntryIndex = -1;
        List<PakEntry> entries = pakFile.getIndex().getEntries();
        PakEntry maxEntry = null;

        for (int i = 0, n = entries.size(); i < n; i++) {
            PakEntry entry = entries.get(i);
            long offset = entry.getOffset();

            if (offset < minOffset) {
                minOffset = offset;
                minOffsetEntryIndex = i + 1;
            }
            if (offset > maxOffset) {
                maxOffset = offset;
                maxOffsetEntryIndex = i + 1;
                maxEntry = entry;
            }
        }

        PakInfo pakInfo = pakFile.getInfo();
        long indexOffset = pakInfo.getIndexOffset();
        long indexSize = pakInfo.getIndexSize();
        long pakFileSize = pakFile.getFile().length();

        System.out.printf("%s -->\n"
                        + "                                    pak size: %d; index offset: %d; index size: %d;\n"
                        + "                                    total entries: %d; min entry offset (entry#%d): %d; max entry offset (entry#%d): %d\n"
                        + "                                    max entry offset + size: %d; diff with index offset: %d; chunked?: %b\n"
                        + "                                    index offset+size: %d; footer (pak info): %d; diff: %d\n",
                pakFile.getFile().getName(), pakFileSize, indexOffset, indexSize,
                pakFile.getIndex().getEntries().size(), minOffsetEntryIndex, minOffset, maxOffsetEntryIndex, maxOffset,
                maxOffset + maxEntry.getSize(), indexOffset - (maxOffset + maxEntry.getCompressedSize()), maxEntry.isCompressed(),
                indexOffset + indexSize, pakFileSize + PakConstants.PAK_INFO_OFFSET_FROM_EOF,
                (pakFileSize + PakConstants.PAK_INFO_OFFSET_FROM_EOF) - (indexOffset + indexSize));
    }


    /**
     * Conclusion: Blocks for a single file are all contiguous.
     */
    @Test
    public void showBlockSeparationOffsets() {
        var blockLists = dbdPakManager.getPakFiles().stream()
                .map(pakFile -> pakFile.getIndex().getEntries())
                .flatMap(Collection::stream)
                .filter(PakEntry::isCompressed)
                .map(PakEntry::getBlocks)
                .collect(toList());

        Long lastBlockEndOffset = null;

        for (var blockList : blockLists) {

            for (var block : blockList) {

                if (lastBlockEndOffset != null) {
                    var delta = block.getOffsetStart() - lastBlockEndOffset;

                    if (delta != 0) {
                        System.out.println("Block separation: " + delta);
                    }
                }
                lastBlockEndOffset = block.getOffsetEnd();
            }

            lastBlockEndOffset = null;
        }
    }

}
