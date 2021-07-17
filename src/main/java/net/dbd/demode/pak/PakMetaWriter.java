package net.dbd.demode.pak;

import net.dbd.demode.pak.domain.*;
import net.dbd.demode.util.io.RandomAccessFileWriter;
import net.dbd.demode.util.lang.LittleEndianByteArrayOutputStream;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * TODO: Finish reverse-engineering the index location relative to the compressed blocks for the case of compressed files.
 * TODO: use buffers for writing metadata
 *
 * @author Nicky Ramone
 */
public class PakMetaWriter {

    private static final byte[] SPACE_BETWEEN_INDEX_AND_PAK_INFO = new byte[PakConstants.BYTES_BETWEEN_INDEX_AND_PAK_INFO];


    public void writeMetadata(Pak pak) throws Exception {
        File file = pak.getFile();

        try (RandomAccessFileWriter raf = new RandomAccessFileWriter(file, RandomAccessFileWriter.Endianness.LITTLE_ENDIAN)) {
            writeIndex(raf, pak);
            raf.write(SPACE_BETWEEN_INDEX_AND_PAK_INFO);
            writePakInfo(raf, pak);
        }

    }


    public void writeIndex(RandomAccessFileWriter raf, Pak pak) throws IOException, DecoderException {
        PakInfo pakInfo = pak.getPakInfo();
        PakIndex index = pak.getPakIndex();

        raf.seek(pakInfo.getIndexOffset());
        byte[] data = serialize(index);
        raf.write(data);
        byte[] sha1 = calculateSha1Hash(data);
        pak.getPakInfo().setIndexHash(Hex.encodeHexString(sha1));

//        raf.seek(pak.getFooter().getTocOffset());
//        raf.writeLong(index.getMountPath().toString().length());
//        raf.writeAsciiString(index.getMountPath().toString().replace('\\', '/'));
//        raf.writeByte(0);
//        raf.writeInt(index.getEntries().size());
//        raf.writeLong(index.getTotalEntriesSize());

    }

    private byte[] serialize(PakIndex index) throws DecoderException {
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
            byteStream.writeInt(entry.getFilename().length() + 1);
            byteStream.writeAsciiString(entry.getFilename());
            byteStream.write(0);
            byteStream.writeLong(entry.getOffset());
            byteStream.writeLong(entry.getCompressedSize());
            byteStream.writeLong(entry.getSize());
            byteStream.writeInt(entry.isCompressed() ? 1 : 0);
            byteStream.writeHexString(entry.getHash());
            byteStream.write(entry.isEncrypted() ? 1 : 0);
            byteStream.writeInt(entry.getChunkSize());

            if (entry.isCompressed()) {
                serializeIndexEntryCompressedBlocks(entry.getBlocks(), byteStream);
            }
        }
    }

    private void serializeIndexEntryCompressedBlocks(List<PakCompressedBlock> blocks, LittleEndianByteArrayOutputStream byteStream) {
        for (PakCompressedBlock block : blocks) {
            byteStream.writeLong(block.getOffsetStart());
            byteStream.writeLong(block.getOffsetEnd());
        }
    }


    public void writePakInfo(RandomAccessFileWriter raf, Pak pak) throws IOException, NoSuchAlgorithmException {

        PakInfo pakInfo = pak.getPakInfo();
        raf.writeInt(PakConstants.PAK_MAGIC);
        raf.writeInt(pakInfo.getVersion());
        raf.writeLong(pakInfo.getIndexOffset());
        raf.writeLong(pakInfo.getIndexSize());
        raf.writeHexString(pakInfo.getIndexHash());
    }


    private byte[] calculateSha1Hash(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Invalid hash algorithm.", e);
        }
    }


}
