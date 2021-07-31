package net.dbd.demode.pak;

import lombok.experimental.UtilityClass;
import net.dbd.demode.pak.domain.*;
import net.dbd.demode.util.io.RandomAccessFileWriter;
import net.dbd.demode.util.lang.LittleEndianByteArrayOutputStream;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Helper for reading pak file metadata.
 *
 * @author Nicky Ramone
 */
@UtilityClass
public class PakMetaWriter {


    public void writeMetadata(PakFile pakFile) throws Exception {
        File file = pakFile.getFile();

        // TODO: improve: use a buffered writer
        try (RandomAccessFileWriter raf = new RandomAccessFileWriter(file, RandomAccessFileWriter.Endianness.LITTLE_ENDIAN)) {
            writeIndex(raf, pakFile.getPak());
            raf.writeNullBytes(PakConstants.BYTES_BETWEEN_INDEX_AND_PAK_INFO);
            writePakInfo(raf, pakFile.getPak());
        }

    }


    public void writeIndex(RandomAccessFileWriter raf, Pak pak) throws IOException, DecoderException {
        PakInfo pakInfo = pak.getInfo();
        PakIndex index = pak.getIndex();

        raf.seek(pakInfo.getIndexOffset());
        raf.truncate();
        byte[] data = serializeIndex(index);
        raf.write(data);

        // TODO: improve: we can calculate the sha1 on the fly
        byte[] sha1 = calculateSha1Hash(data);

        pak.getInfo().setIndexSize(data.length);
        pak.getInfo().setIndexHash(Hex.encodeHexString(sha1));
    }

    private byte[] serializeIndex(PakIndex index) throws DecoderException {
        String mountPoint = index.getMountPoint().toString().replace('\\', '/') + '/';

        var byteStream = new LittleEndianByteArrayOutputStream();
        byteStream.writeInt(mountPoint.length() + 1);
        byteStream.writeAsciiString(mountPoint);
        byteStream.write(0);
        byteStream.writeInt(index.getEntries().size());
        serializeIndexEntries(index.getEntries(), byteStream);

        return byteStream.toByteArray();
    }

    private void serializeIndexEntries(List<PakEntry> entries, LittleEndianByteArrayOutputStream byteStream) throws DecoderException {

        for (PakEntry entry : entries) {
            String filePath = toPakPath(entry.getFilePath());
            byteStream.writeInt(filePath.length() + 1);
            byteStream.writeAsciiString(filePath);
            byteStream.write(0);
            byteStream.writeLong(entry.getOffset());
            byteStream.writeLong(entry.getCompressedSize());
            byteStream.writeLong(entry.getSize());
            byteStream.writeInt(entry.isCompressed() ? 1 : 0);
            byteStream.writeHexString(entry.getHash());

            if (entry.isCompressed()) {
                serializeIndexEntryCompressedBlocks(entry.getBlocks(), byteStream);
            }

            byteStream.write(entry.isEncrypted() ? 1 : 0);
            byteStream.writeInt(entry.getBlockSize());
        }
    }

    private void serializeIndexEntryCompressedBlocks(List<PakCompressedBlock> blocks, LittleEndianByteArrayOutputStream byteStream) {
        byteStream.writeInt(blocks.size());

        for (PakCompressedBlock block : blocks) {
            byteStream.writeLong(block.getOffsetStart());
            byteStream.writeLong(block.getOffsetEnd());
        }
    }


    public void writePakInfo(RandomAccessFileWriter raf, Pak pak) throws IOException {
        long currentPos = raf.position();
        raf.truncate();
        raf.writeNullBytes(PakConstants.PAK_INFO_OFFSET_FROM_EOF * -1);
        raf.seek(currentPos);

        PakInfo pakInfo = pak.getInfo();
        raf.writeInt(PakConstants.PAK_MAGIC);
        raf.writeInt(pakInfo.getVersion());
        raf.writeLong(pakInfo.getIndexOffset());
        raf.writeLong(pakInfo.getIndexSize());
        raf.writeHexString(pakInfo.getIndexHash());
        raf.writeByte(0);
        raf.writeAsciiString("Zlib");
    }


    private byte[] calculateSha1Hash(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Invalid hash algorithm.", e);
        }
    }

    private String toPakPath(Path path) {
        return path.toString().replace('\\', '/');
    }

}
