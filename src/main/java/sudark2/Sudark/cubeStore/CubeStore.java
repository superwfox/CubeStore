package sudark2.Sudark.cubeStore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import sudark2.Sudark.cubeStore.Command.CommandExecutor;
import sudark2.Sudark.cubeStore.Command.CommandTabCompleter;
import sudark2.Sudark.cubeStore.Listener.PlayerPurchaseListener;
import sudark2.Sudark.cubeStore.Util.Cargo;

public final class CubeStore extends JavaPlugin {

    private static CubeStore instance;

    public static CubeStore getInstance() {
        return instance;
    }

    public static Component text(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new PlayerPurchaseListener(), this);
        PluginCommand cmd = getCommand("store");
        if (cmd != null) {
            cmd.setExecutor(new CommandExecutor());
            cmd.setTabCompleter(new CommandTabCompleter());
        }
        Cargo.reload();
    }

    @Override
    public void onDisable() {
        Cargo.cleanupAll();
    }
}
