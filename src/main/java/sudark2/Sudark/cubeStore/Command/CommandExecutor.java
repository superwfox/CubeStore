package sudark2.Sudark.cubeStore.Command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import sudark2.Sudark.cubeStore.CubeStore;
import sudark2.Sudark.cubeStore.File.FileManager;
import sudark2.Sudark.cubeStore.Util.Cargo;
import sudark2.Sudark.cubeStore.Util.Good;

public class CommandExecutor implements org.bukkit.command.CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(CubeStore.text("§f你§7没有权限§f使用§e/store§f。"));
            return true;
        }
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "add" -> handleAdd(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "reload" -> {
                Cargo.reload();
                sender.sendMessage(CubeStore.text("§f已§7重载§f §e" + Cargo.CargoMap.size() + "§f 件商品。"));
            }
            default -> usage(sender);
        }
        return true;
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(CubeStore.text("§f用法§7: §e/store delete §b<name>"));
            return;
        }
        String name = args[1];
        int live = Cargo.countByName(name);
        if (live == 0) {
            sender.sendMessage(CubeStore.text("§f未找到§7商品§7: §e" + name));
            return;
        }
        int removed = Cargo.removeByName(name);
        int onDisk = FileManager.removeByName(name);
        sender.sendMessage(CubeStore.text("§f已§7删除§f §e" + name + "§f §b×" + removed + "§f §7(磁盘§f §b×" + onDisk + "§7)§f。"));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 8) {
            sender.sendMessage(CubeStore.text("§f用法§7: §e/store create §b<name> <Material> <amount> <singlePrice> <x> <y> <z> §f[world]"));
            return;
        }
        String name = args[1];
        Material mat = Material.matchMaterial(args[2]);
        if (mat == null || mat == Material.AIR) {
            sender.sendMessage(CubeStore.text("§f材料§7无效§7: §e" + args[2]));
            return;
        }
        int amount, singlePrice, x, y, z;
        try {
            amount = Integer.parseInt(args[3]);
            singlePrice = Integer.parseInt(args[4]);
            x = Integer.parseInt(args[5]);
            y = Integer.parseInt(args[6]);
            z = Integer.parseInt(args[7]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(CubeStore.text("§f数字§7参数有误§f。"));
            return;
        }
        if (amount <= 0 || singlePrice <= 0) {
            sender.sendMessage(CubeStore.text("§e数量§f与§e单价§f必须§7大于 0§f。"));
            return;
        }
        World world;
        if (args.length >= 9) {
            world = Bukkit.getWorld(args[8]);
            if (world == null) {
                sender.sendMessage(CubeStore.text("§f世界§7不存在§7: §e" + args[8]));
                return;
            }
        } else if (sender instanceof Player p) {
            world = p.getWorld();
        } else {
            sender.sendMessage(CubeStore.text("§f控制台§7必须指定§f §eworld§f 参数。"));
            return;
        }
        Location loc = new Location(world, x + 0.5, y, z + 0.5);
        Good good = new Good(name, mat, amount, singlePrice, loc);
        FileManager.addGood(good);
        Cargo.transToDisplay(good);
        sender.sendMessage(CubeStore.text("§f已§7创建§f §e" + name + "§f §b×" + amount + "§f @ §b" + singlePrice + "§f Lv/件。"));
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(CubeStore.text("§e/store add§f 仅§7玩家§f可用。"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(CubeStore.text("§f用法§7: §e/store add §b<name> <amount> <singlePrice>"));
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            sender.sendMessage(CubeStore.text("§f主手§7为空§f, 无法§7推断§f材料§7。"));
            return;
        }
        String name = args[1];
        int amount, singlePrice;
        try {
            amount = Integer.parseInt(args[2]);
            singlePrice = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(CubeStore.text("§f数字§7参数有误§f。"));
            return;
        }
        if (amount <= 0 || singlePrice <= 0) {
            sender.sendMessage(CubeStore.text("§e数量§f与§e单价§f必须§7大于 0§f。"));
            return;
        }
        Location pl = p.getLocation();
        Location loc = new Location(pl.getWorld(), pl.getBlockX() + 0.5, pl.getBlockY(), pl.getBlockZ() + 0.5);
        Good good = new Good(name, hand.getType(), amount, singlePrice, loc);
        FileManager.addGood(good);
        Cargo.transToDisplay(good);
        sender.sendMessage(CubeStore.text("§f已§7创建§f §e" + name + "§f §b×" + amount + "§f @ §b" + singlePrice + "§f Lv/件。"));
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(CubeStore.text("§f用法§7:"));
        sender.sendMessage(CubeStore.text(" §e/store create §b<name> <Material> <amount> <singlePrice> <x> <y> <z> §f[world]"));
        sender.sendMessage(CubeStore.text(" §e/store add §b<name> <amount> <singlePrice>"));
        sender.sendMessage(CubeStore.text(" §e/store delete §b<name>"));
        sender.sendMessage(CubeStore.text(" §e/store reload"));
    }
}
