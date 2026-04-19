package sudark2.Sudark.cubeStore.File;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import sudark2.Sudark.cubeStore.CubeStore;
import sudark2.Sudark.cubeStore.Util.Good;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private static File file() {
        CubeStore plugin = CubeStore.getInstance();
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        return new File(plugin.getDataFolder(), "goods.yml");
    }

    public static List<Good> loadAll() {
        List<Good> list = new ArrayList<>();
        File f = file();
        if (!f.exists()) return list;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        for (String key : cfg.getKeys(false)) {
            ConfigurationSection s = cfg.getConfigurationSection(key);
            if (s == null) continue;
            Good g = Good.fromConfig(s);
            if (g == null) {
                CubeStore.getInstance().getLogger().warning("Skipping invalid good entry: " + key);
                continue;
            }
            list.add(g);
        }
        return list;
    }

    public static void saveAll(List<Good> goods) {
        YamlConfiguration cfg = new YamlConfiguration();
        int i = 0;
        for (Good g : goods) {
            g.writeTo(cfg.createSection(String.valueOf(i++)));
        }
        try {
            cfg.save(file());
        } catch (IOException e) {
            CubeStore.getInstance().getLogger().warning("Failed to save goods.yml: " + e.getMessage());
        }
    }

    public static void addGood(Good good) {
        List<Good> all = loadAll();
        all.add(good);
        saveAll(all);
    }
}
