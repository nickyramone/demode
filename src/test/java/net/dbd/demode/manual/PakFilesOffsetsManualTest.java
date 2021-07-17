package net.dbd.demode.manual;

import net.dbd.demode.Factory;
import net.dbd.demode.pak.domain.Pak;
import net.dbd.demode.pak.domain.PakConstants;
import net.dbd.demode.pak.domain.PakEntry;
import net.dbd.demode.pak.domain.PakInfo;
import net.dbd.demode.service.DbdUnpacker;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class PakFilesOffsetsManualTest {

    private DbdUnpacker dbdUnpacker;

    @Before
    public void setUp() throws Exception {
        dbdUnpacker = new DbdUnpacker(Factory.pakMetaReader(), Defaults.DBD_DIR);
    }


    @Test
    public void showPakFooterLocation() throws Exception {
        dbdUnpacker.getPaks().stream()
                .peek(this::printOffsetsInfo)
                .collect(toList());
    }


    private void printOffsetsInfo(Pak pak) {

        long minOffset = Long.MAX_VALUE;
        long minOffsetEntryIndex = -1;
        long maxOffset = -1;
        long maxOffsetEntryIndex = -1;
        List<PakEntry> entries = pak.getPakIndex().getEntries();
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

        PakInfo pakInfo = pak.getPakInfo();
        long indexOffset = pakInfo.getIndexOffset();
        long indexSize = pakInfo.getIndexSize();
        long pakFileSize = pak.getFile().length();

        System.out.printf("%s -->\n"
                        + "                                    pak size: %d; index offset: %d; index size: %d;\n"
                        + "                                    total entries: %d; min entry offset (entry#%d): %d; max entry offset (entry#%d): %d\n"
                        + "                                    max entry offset + size: %d; diff with index offset: %d; chunked?: %b\n"
                        + "                                    index offset+size: %d; footer (pak info): %d; diff: %d\n",
                pak.getFile().getName(), pakFileSize, indexOffset, indexSize,
                pak.getPakIndex().getEntries().size(), minOffsetEntryIndex, minOffset, maxOffsetEntryIndex, maxOffset,
                maxOffset + maxEntry.getSize(), indexOffset - (maxOffset + maxEntry.getCompressedSize()), maxEntry.isCompressed(),
                indexOffset + indexSize, pakFileSize + PakConstants.PAK_INFO_OFFSET_FROM_EOF,
                (pakFileSize + PakConstants.PAK_INFO_OFFSET_FROM_EOF) - (indexOffset + indexSize));
    }


}
