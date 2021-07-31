package net.dbd.demode.service;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;

/**
 * @author Nicky Ramone
 */
public class FileMetadataManager {

    private static final String FILE_ATTR__ORIGINAL = "user.demode.original";
    private static final String FILE_ATTR__HASH = "user.demode.hash";
    private static final int HASH_SIZE = 20;

    private final byte[] hashBuffer = new byte[HASH_SIZE];
    private final ByteBuffer hashByteBuffer = ByteBuffer.allocate(HASH_SIZE);


    public void writeHash(Path filePath, String hash) {

        UserDefinedFileAttributeView attributeView = Files.getFileAttributeView(filePath, UserDefinedFileAttributeView.class);
        try {
            attributeView.write(FILE_ATTR__ORIGINAL, ByteBuffer.allocate(0));
            ByteBuffer hashByteBuffer = ByteBuffer.wrap(Hex.decodeHex(hash));
            attributeView.write(FILE_ATTR__HASH, hashByteBuffer);
        } catch (IOException | DecoderException e) {
            throw new RuntimeException("Failed to write metadata to file: " + filePath);
        }
    }

    public String readHash(Path filePath) {
        UserDefinedFileAttributeView attributeView = Files.getFileAttributeView(filePath, UserDefinedFileAttributeView.class);

        try {
            if (!attributeView.list().contains(FILE_ATTR__ORIGINAL) || !attributeView.list().contains(FILE_ATTR__HASH)) {
                return null;
            }

            hashByteBuffer.clear();
            attributeView.read(FILE_ATTR__HASH, hashByteBuffer);
            hashByteBuffer.flip();
            hashByteBuffer.get(hashBuffer);

            return Hex.encodeHexString(hashBuffer);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
