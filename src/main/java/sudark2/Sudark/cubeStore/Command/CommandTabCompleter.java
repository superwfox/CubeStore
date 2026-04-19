package sudark2.Sudark.cubeStore.Command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "reload"), args[0]);
        }
        if (!"create".equalsIgnoreCase(args[0])) return List.of();
        return switch (args.length) {
            case 2 -> List.of("<name>");
            case 3 -> containsFilter(Arrays.stream(Material.values())
                    .filter(m -> m.isItem() && m != Material.AIR)
                    .map(Enum::name).toList(), args[2]);
            case 4 -> List.of("<amount>");
            case 5 -> List.of("<singlePrice>");
            case 6 -> sender instanceof Player p ? List.of(String.valueOf(p.getLocation().getBlockX())) : List.of("<x>");
            case 7 -> sender instanceof Player p ? List.of(String.valueOf(p.getLocation().getBlockY())) : List.of("<y>");
            case 8 -> sender instanceof Player p ? List.of(String.valueOf(p.getLocation().getBlockZ())) : List.of("<z>");
            case 9 -> filter(Bukkit.getWorlds().stream().map(World::getName).toList(), args[8]);
            default -> List.of();
        };
    }

    private List<String> filter(List<String> src, String prefix) {
        String lp = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : src) if (s.toLowerCase().startsWith(lp)) out.add(s);
        return out;
    }

    private List<String> containsFilter(List<String> src, String query) {
        String lq = query.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : src) if (s.toLowerCase().contains(lq)) out.add(s);
        return out;
    }
}
