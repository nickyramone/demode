package net.dbd.demode.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Path;

/**
 * @author Nicky Ramone
 */
public class Gson {

    private final com.google.gson.Gson gson;

    public Gson() {
        var builder = new com.google.gson.GsonBuilder();
        builder.setPrettyPrinting();
        builder.enableComplexMapKeySerialization();
        builder.registerTypeAdapter(File.class, new FileTypeAdapter());
        builder.registerTypeHierarchyAdapter(Path.class, new PathHierarchyAdapter());

        gson = builder.create();
    }

    public String toJson(Object src) {
        return gson.toJson(src);
    }


    public static class PathHierarchyAdapter implements JsonSerializer<Path> {

        @Override
        public JsonElement serialize(Path src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString().replace('\\', '/'));
        }
    }

    public static class FileTypeAdapter implements JsonSerializer<File> {

        @Override
        public JsonElement serialize(File src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString().replace('\\', '/'));
        }
    }

}