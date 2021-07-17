package net.dbd.demode.util;

public class Gson {

    private com.google.gson.Gson gson;

    public Gson() {
        gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
    }

    public String toJson(Object src) {
        return gson.toJson(src);
    }
}
