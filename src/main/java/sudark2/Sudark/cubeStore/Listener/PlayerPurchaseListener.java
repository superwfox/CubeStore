package sudark2.Sudark.cubeStore.Listener;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import sudark2.Sudark.cubeStore.CubeStore;
import sudark2.Sudark.cubeStore.Util.Cargo;
import sudark2.Sudark.cubeStore.Util.Good;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerPurchaseListener implements Listener {

    private final Map<UUID, BukkitTask> loops = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        boolean right;
        switch (e.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> right = true;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> right = false;
            default -> {
                return;
            }
        }
        Map.Entry<UUID, Good> hit = Cargo.locateLookedAt(p);
        if (hit == null) return;
        e.setCancelled(true);
        if (loops.containsKey(p.getUniqueId())) return;

        boolean ok = handleTrade(p, right);
        if (!ok || !p.isSneaking()) return;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(CubeStore.getInstance(), () -> {
            if (!p.isOnline() || !p.isSneaking()) {
                cancelLoop(p);
                return;
            }
            Map.Entry<UUID, Good> h = Cargo.locateLookedAt(p);
            if (h == null) {
                cancelLoop(p);
                return;
            }
            boolean r = handleTrade(p, right);
            if (!r) cancelLoop(p);
        }, 4L, 4L);
        loops.put(p.getUniqueId(), task);
    }

    private boolean handleTrade(Player p, boolean rightClick) {
        Map.Entry<UUID, Good> hit = Cargo.locateLookedAt(p);
        if (hit == null) return false;
        return rightClick
                ? recycle(p, hit.getValue(), hit.getKey())
                : purchase(p, hit.getValue(), hit.getKey());
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) cancelLoop(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancelLoop(e.getPlayer());
    }

    private void cancelLoop(Player p) {
        BukkitTask t = loops.remove(p.getUniqueId());
        if (t != null) t.cancel();
    }

    private boolean purchase(Player p, Good good, UUID displayId) {
        int cost = good.getSinglePrice();
        if (p.getLevel() < cost) {
            fail(p, displayId, "购买失败", "经验不足");
            return false;
        }
        p.giveExpLevels(-cost);
        giveStoreItem(p, good);
        Cargo.animateShrink(displayId);
        p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, 1f, 1.2f);
        p.sendActionBar(CubeStore.text(
                "§7购买§f: §b" + good.getAmount() + " §fx§e" + good.getMaterial().name()
                        + "§f, §b-" + cost + " §fL"));
        return true;
    }

    private boolean recycle(Player p, Good good, UUID displayId) {
        NamespacedKey key = Cargo.amountKey();
        PlayerInventory inv = p.getInventory();
        ItemStack[] contents = inv.getStorageContents();
        int slot = -1;
        int pdcAmt = 0;
        for (int i = 0; i < contents.length; i++) {
            ItemStack s = contents[i];
            if (s == null || s.getType() != good.getMaterial()) continue;
            ItemMeta meta = s.getItemMeta();
            if (meta == null) continue;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!pdc.has(key, PersistentDataType.INTEGER)) continue;
            int cur = pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
            if (cur < good.getAmount()) continue;
            if (!meta.getEnchants().isEmpty()) continue;
            if (meta instanceof Damageable dmg && dmg.getDamage() > 0) continue;
            slot = i;
            pdcAmt = cur;
            break;
        }
        if (slot < 0) {
            fail(p, displayId, "回收失败", "无可回收物品");
            return false;
        }
        int remaining = pdcAmt - good.getAmount();
        if (remaining == 0) {
            inv.setItem(slot, null);
        } else {
            ItemStack s = contents[slot];
            writeStoreMeta(s, remaining);
            inv.setItem(slot, s);
        }
        int gain = good.getSinglePrice();
        p.giveExpLevels(gain);
        Cargo.animateGrow(displayId);
        p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, 1f, 1.2f);
        p.sendActionBar(CubeStore.text(
                "§7回收§f: §b" + good.getAmount() + " §fx§e" + good.getMaterial().name()
                        + "§f, §b+" + gain + " §fL"));
        return true;
    }

    private void fail(Player p, UUID displayId, String verb, String reason) {
        Cargo.animateShake(displayId);
        p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_DRIP, 1f, 0.8f);
        p.sendActionBar(CubeStore.text("§7" + verb + "§f: §e" + reason));
    }

    private void giveStoreItem(Player p, Good good) {
        PlayerInventory inv = p.getInventory();
        NamespacedKey key = Cargo.amountKey();
        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack s = contents[i];
            if (s == null || s.getType() != good.getMaterial()) continue;
            ItemMeta meta = s.getItemMeta();
            if (meta == null) continue;
            if (!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) continue;
            int cur = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
            writeStoreMeta(s, cur + good.getAmount());
            inv.setItem(i, s);
            return;
        }
        ItemStack fresh = new ItemStack(good.getMaterial(), 1);
        writeStoreMeta(fresh, good.getAmount());
        HashMap<Integer, ItemStack> leftover = inv.addItem(fresh);
        for (ItemStack drop : leftover.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), drop);
        }
    }

    private void writeStoreMeta(ItemStack stack, int pdcAmount) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(Cargo.amountKey(), PersistentDataType.INTEGER, pdcAmount);
        stack.setItemMeta(meta);
    }
}
