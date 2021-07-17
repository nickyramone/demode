package net.dbd.demode.service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Nicky Ramone
 */
public class UserSettings {

    private Map<String, String> settings = new HashMap<>();


    public UserSettings() {
        settings.put("dbd_dir", "d:\\SteamLibrary\\steamapps\\common\\Dead by Daylight\\");
        settings.put("unpack_output_dir", "D:\\tmp\\dbd-unpacked\\DeadByDaylight\\Content\\Paks");
    }


    public String getDbdDirectory() {
        return settings.get("dbd_dir");
    }

    public void setDbdDirectory(String dir) {
        settings.put("dbd_dir", dir);
    }

    public String getUnpackOutputDirectory() {
        return settings.get("unpack_output_dir");
    }

}
