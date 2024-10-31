package torrent.network.client.torrententity;

import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import torrent.network.client.torrentexception.ExceptionHandler;

public class FileEntity {
    private String length;
    private Object paths;

    protected FileEntity() {
    }

    public String getLength() {
        return length;
    }

    public List<List<String>> getPaths() {
        try {
            Gson gson = new Gson();

            JsonElement jsonElement = gson.toJsonTree(paths);
            Object[][] array = gson.fromJson(jsonElement, Object[][].class);

            List<List<String>> paths = new ArrayList<>();

            for (Object[] path : array) {
                List<String> pathList = new ArrayList<>();
                for (Object p : path) {
                    pathList.add(p.toString());
                }
                paths.add(pathList);
            }
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }

    public static FileEntity from(Object file) {
        try {
            Gson gson = new Gson();

            JsonElement jsonElement = gson.toJsonTree(file);
            return gson.fromJson(jsonElement, FileEntity.class);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return null;
    }
}
