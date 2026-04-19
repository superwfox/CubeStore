package sudark2.Sudark.cubeStore.Util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class Good {

    private final String name;
    private final Material material;
    private final int amount;
    private final int singlePrice;
    private final Location loc;

    public Good(String name, Material material, int amount, int singlePrice, Location loc) {
        this.name = name;
        this.material = material;
        this.amount = amount;
        this.singlePrice = singlePrice;
        this.loc = loc;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public int getSinglePrice() {
        return singlePrice;
    }

    public Location getLoc() {
        return loc;
    }

    public int totalPrice() {
        return amount * singlePrice;
    }

    public static Good fromConfig(ConfigurationSection s) {
        Material m = Material.matchMaterial(s.getString("material", ""));
        if (m == null) return null;
        World world = Bukkit.getWorld(s.getString("world", ""));
        if (world == null) return null;
        Location loc = new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"));
        return new Good(s.getString("name", m.name()), m, s.getInt("amount"), s.getInt("singlePrice"), loc);
    }

    public void writeTo(ConfigurationSection s) {
        s.set("name", name);
        s.set("material", material.name());
        s.set("amount", amount);
        s.set("singlePrice", singlePrice);
        s.set("world", loc.getWorld().getName());
        s.set("x", loc.getX());
        s.set("y", loc.getY());
        s.set("z", loc.getZ());
    }
}
