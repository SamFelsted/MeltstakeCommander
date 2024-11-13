package com.meltstakecommander;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class SettingsLoader {

    private Map<String, Object> data;
    Yaml settings;
    Path path;

    public SettingsLoader() {
        settings = new Yaml();
        path = Paths.get("settings.yml");
        try {
            data = settings.load(new FileInputStream(path.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setDataAndSave(Map<String, Object> data) {
        this.data = data;
        try {
            settings.dump(data, new FileWriter(path.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
