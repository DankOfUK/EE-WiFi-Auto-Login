package managers;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final String FOLDER_NAME = "EE-Auto-Login";
    private static final String FILE_NAME = FOLDER_NAME + File.separator + "config.yml";
    private Map<String, Object> configData = new HashMap<>();

    private void saveDefaultConfig() {
        configData.put("username", "");
        configData.put("password", "");
        configData.put("id_user", "username");
        configData.put("id_pass", "password");
        configData.put("id_button", "login-button");
        configData.put("auto_loop", false);
        configData.put("check_internet", true);
        configData.put("auto_reset", true);
        configData.put("hidden_mode", false); // Default for the debug button
        saveConfig();
    }

    public void loadConfig() {
        File folder = new File(FOLDER_NAME);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(FILE_NAME);
        if (!file.exists()) {
            saveDefaultConfig();
            return;
        }

        try (InputStream input = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> loaded = yaml.load(input);
            if (loaded != null) {
                configData = loaded;
            } else {
                saveDefaultConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(FILE_NAME)) {
            yaml.dump(configData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getString(String key) {
        Object val = configData.get(key);
        return (val == null) ? "" : val.toString();
    }

    public boolean getBoolean(String key) {
        Object val = configData.get(key);
        return (val instanceof Boolean) ? (Boolean) val : false;
    }

    public void set(String key, Object value) { configData.put(key, value); }
}