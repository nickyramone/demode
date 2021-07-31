package net.dbd.demode.data_experiment;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;

@UtilityClass
public class Defaults {

    final String DBD_DIR = "d:/SteamLibrary/steamapps/common/Dead by Daylight";
    final Path DBD_HOME = Path.of(DBD_DIR);

}
