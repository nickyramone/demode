package net.dbd.demode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class Main {

    private int count = 0;

    public static void main(String[] args) throws Exception {
//        String file = "dummy2.pak";
//        String file = "pakchunk7.pak";
//        String file = "pakchunk8.pak"; // 40 sec
//        String file = "pakchunk0.pak"; // 528 sec ~ 9 min ; 395 ~ 7 min (buffering)
//        String fil e = "pakchunk25.pak"; // 587 sec ~ 10 min (buffering)
//        PakReader pakReader = new PakReader(file);
//        pakReader.close();

        String dbdDir = "d:/SteamLibrary/steamapps/common/Dead by Daylight";
        String outDir = "d:/tmp/DeadByDaylight/Content/Paks";

//        PakParser pakParser = new PakParser();
//        DbdUnpacker unpacker = new DbdUnpacker(pakParser, dbdDir);
//        unpacker.unpack(outDir);


        Main main = new Main();
        Path file = Path.of("d:\\tmp\\dummy.txt");
        UserDefinedFileAttributeView view = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
//        main.statAllFiles();
//        main.deleteAttr(view);
//        main.readAttr(view);
//        main.writeAttr(view);
//        main.writeBoolenAttr(view);
//        view.write("user.demode.original", ByteBuffer.wrap(new byte[0]));

        System.out.println("Done!");

    }

    private void readAttr(UserDefinedFileAttributeView view) throws IOException {
        ByteBuffer readbuf = ByteBuffer.allocate(4);
        view.read("user.demode.original", readbuf);
        readbuf.flip();
//        Integer flag = readbuf.getInt(0);
//        System.out.println("flag: " + flag);
        String value = Charset.defaultCharset().decode(readbuf).toString();
        System.out.println("value: " + value);
    }


    private void writeAttr(UserDefinedFileAttributeView view) throws IOException {
        view.write("user.demode.original", Charset.defaultCharset().encode("test"));
    }

    private void writeBoolenAttr(UserDefinedFileAttributeView view) throws IOException {
        view.write("user.demode.original", ByteBuffer.allocate(0));
    }

    private void deleteAttr(UserDefinedFileAttributeView view) throws IOException {
        view.delete("user.demode.original");
    }


    private void statAllFiles() throws IOException {

        String path = "d:\\tmp\\DeadByDaylight\\Content\\Characters\\Slashers\\Crooked\\AnimSequences";
//        String dbdDir = "d:/SteamLibrary/steamapps/common/Dead by Daylight";

        Instant start = Instant.now();

        Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .forEach(this::doStuff);

        long elapsed = Duration.between(Instant.now(), start).toSeconds();
        System.out.println();
        System.out.println("elapsed: " + elapsed);

    }

    private void doStuff(Path file) {
        try {
            System.out.printf("%s                                     \n", file);
//            System.out.printf("%d   \r", ++count);

            UserDefinedFileAttributeView view = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
            Optional<String> attr = view.list().stream().filter(name -> name.equals("user.demode.original")).findFirst();
            if (attr.isPresent()) {
                System.out.println("Found attribute!");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        String name = "user.mimetype";
//        ByteBuffer buf = ByteBuffer.allocate(view.size(name));
//        view.read(name, buf);
//        buf.flip();

    }

}
