package sudark2.Sudark.cubeStore.Util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import sudark2.Sudark.cubeStore.CubeStore;
import sudark2.Sudark.cubeStore.File.FileManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Cargo {

    public static ConcurrentHashMap<UUID, Good> CargoMap = new ConcurrentHashMap<>();

    public static final float BLOCK_SCALE = 0.7f;
    public static final float ITEM_SCALE = 1.2f;

    private static final Transformation itemTransformation = new Transformation(
            new Vector3f(0f, -0.5f, 0f),
            new AxisAngle4f(0, 0, 0, 0),
            new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
            new AxisAngle4f(0, 0, 0, 0)
    );

    private static final Transformation blockTransformation = new Transformation(
            new Vector3f(-BLOCK_SCALE / 2f, -0.5f - BLOCK_SCALE / 2f, -BLOCK_SCALE / 2f),
            new AxisAngle4f(0, 0, 0, 0),
            new Vector3f(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE),
            new AxisAngle4f(0, 0, 0, 0)
    );

    public static NamespacedKey amountKey() {
        return new NamespacedKey("multistack", "amount");
    }

    public static void transToDisplay(Good good) {
        if (good.getMaterial().isBlock())
            spawnInBlock(good);
        else
            spawnInItem(good);
    }

    public static void spawnInBlock(Good good) {
        Location spawnLoc = good.getLoc().clone().add(0, 1, 0);
        BlockDisplay display = spawnLoc.getWorld().spawn(spawnLoc, BlockDisplay.class);
        display.setBlock(good.getMaterial().createBlockData());
        display.setTransformation(blockTransformation);
        display.setDisplayWidth(1f);
        display.setDisplayHeight(1f);
        applyNameplate(display, good);
        CargoMap.put(display.getUniqueId(), good);
    }

    public static void spawnInItem(Good good) {
        Location spawnLoc = good.getLoc().clone().add(0, 1, 0);
        ItemDisplay display = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class);
        display.setItemStack(new ItemStack(good.getMaterial(), 1));
        display.setTransformation(itemTransformation);
        display.setDisplayWidth(1f);
        display.setDisplayHeight(1f);
        applyNameplate(display, good);
        CargoMap.put(display.getUniqueId(), good);
    }

    private static void applyNameplate(Display display, Good good) {
        display.customName(CubeStore.text(
                good.getName() + " x " + good.getAmount() + " : §e" + good.getSinglePrice()));
        display.setCustomNameVisible(true);
    }

    public static int countByName(String name) {
        int n = 0;
        for (Good g : CargoMap.values()) if (g.getName().equals(name)) n++;
        return n;
    }

    public static int removeByName(String name) {
        int removed = 0;
        for (Map.Entry<UUID, Good> entry : CargoMap.entrySet()) {
            if (!entry.getValue().getName().equals(name)) continue;
            Entity ent = Bukkit.getEntity(entry.getKey());
            if (ent != null) ent.remove();
            CargoMap.remove(entry.getKey());
            removed++;
        }
        return removed;
    }

    public static void cleanupAll() {
        for (UUID id : CargoMap.keySet()) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        CargoMap.clear();
    }

    public static void reload() {
        cleanupAll();
        for (Good g : FileManager.loadAll()) {
            transToDisplay(g);
        }
    }

    public static Map.Entry<UUID, Good> locateLookedAt(org.bukkit.entity.Player p) {
        Vector origin = p.getEyeLocation().toVector();
        Vector dir = p.getEyeLocation().getDirection();
        double bestDist = Double.MAX_VALUE;
        Map.Entry<UUID, Good> best = null;
        for (Map.Entry<UUID, Good> entry : CargoMap.entrySet()) {
            Good g = entry.getValue();
            if (!g.getLoc().getWorld().equals(p.getWorld())) continue;
            double cx = g.getLoc().getX();
            double cy = g.getLoc().getY() + 0.5;
            double cz = g.getLoc().getZ();
            double half = 0.5;
            BoundingBox box = BoundingBox.of(
                    new Vector(cx - half, cy - half, cz - half),
                    new Vector(cx + half, cy + half, cz + half));
            org.bukkit.util.RayTraceResult r = box.rayTrace(origin, dir, 5.0);
            if (r == null) continue;
            double d = r.getHitPosition().distanceSquared(origin);
            if (d < bestDist) {
                bestDist = d;
                best = entry;
            }
        }
        return best;
    }

    public static void animateShrink(UUID id) { bump(id, 0.7f); }

    public static void animateGrow(UUID id) { bump(id, 1.3f); }

    public static void animateShake(UUID id) {
        Entity e = Bukkit.getEntity(id);
        if (!(e instanceof Display d)) return;
        Good g = CargoMap.get(id);
        if (g == null) return;
        Transformation base = g.getMaterial().isBlock() ? blockTransformation : itemTransformation;
        Transformation shaken = new Transformation(
                base.getTranslation(),
                new Quaternionf(new AxisAngle4f(0.15f, 0f, 1f, 0f)),
                base.getScale(),
                base.getRightRotation());
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(2);
        d.setTransformation(shaken);
        Bukkit.getScheduler().runTaskLater(CubeStore.getInstance(), () -> restore(id), 2L);
    }

    private static void bump(UUID id, float factor) {
        Entity e = Bukkit.getEntity(id);
        if (!(e instanceof Display d)) return;
        Good g = CargoMap.get(id);
        if (g == null) return;
        Transformation base = g.getMaterial().isBlock() ? blockTransformation : itemTransformation;
        Vector3f s = base.getScale();
        Transformation bumped = new Transformation(
                base.getTranslation(),
                base.getLeftRotation(),
                new Vector3f(s.x * factor, s.y * factor, s.z * factor),
                base.getRightRotation());
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(2);
        d.setTransformation(bumped);
        Bukkit.getScheduler().runTaskLater(CubeStore.getInstance(), () -> restore(id), 2L);
    }

    private static void restore(UUID id) {
        Entity e = Bukkit.getEntity(id);
        if (!(e instanceof Display d)) return;
        Good g = CargoMap.get(id);
        if (g == null) return;
        Transformation base = g.getMaterial().isBlock() ? blockTransformation : itemTransformation;
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(2);
        d.setTransformation(base);
    }
}
