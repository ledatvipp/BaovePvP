package org.ledat.baovePvP;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;


public class BaovePvP extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private final HashMap<UUID, Long> pvpProtection = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        pvpProtection.clear();
    }

    // Lắng nghe lệnh mở GUI
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().substring(1); // Bỏ dấu '/'
        if (config.getStringList("gui-commands").contains(command)) {
            openGui(player);
            event.setCancelled(true);
        }
    }

    // Mở GUI
    private void openGui(Player player) {
        int rows = config.getInt("gui.rows", 3);
        Inventory gui = Bukkit.createInventory(null, rows * 9, ChatColor.GREEN + "Bảo Vệ PvP");

        List<Map<String, Object>> items = config.getMapList("gui.items").stream()
                .map(item -> (Map<String, Object>) item)
                .collect(Collectors.toList());
        for (Map<String, Object> itemConfig : items) {
            int slot = (int) itemConfig.get("slot");
            Material type = Material.valueOf((String) itemConfig.get("type"));
            int amount = (int) itemConfig.get("amount");
            String displayName = ChatColor.translateAlternateColorCodes('&', (String) itemConfig.get("display_name"));
            List<String> loreConfig = (List<String>) itemConfig.get("lore");
            List<String> lore = loreConfig.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());

            ItemStack item = new ItemStack(type);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            item.setItemMeta(meta);

            gui.setItem(slot, item);
        }

        player.openInventory(gui);
    }

    // Xử lý khi người chơi click trong GUI
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().equals(ChatColor.GREEN + "Bảo Vệ PvP")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                List<Map<?, ?>> items = config.getMapList("gui.items");
                for (Map<?, ?> itemConfig : items) {
                    Material type = Material.valueOf((String) itemConfig.get("type"));
                    int amount = (int) itemConfig.get("amount");
                    int time = (int) itemConfig.get("time");

                    if (clickedItem.getType() == type) {
                        if (player.getInventory().containsAtLeast(new ItemStack(type), amount)) {
                            player.getInventory().removeItem(new ItemStack(type, amount));
                            activatePvpProtection(player, time);
                            player.closeInventory();
                            return;
                        } else {
                            player.sendMessage(ChatColor.RED + "Bạn không đủ tài nguyên!");
                        }
                    }
                }
            }
        }
    }

    // Kích hoạt bảo vệ PvP
    private void activatePvpProtection(Player player, int time) {
        pvpProtection.put(player.getUniqueId(), System.currentTimeMillis() + (time * 1000L));

        new BukkitRunnable() {
            @Override
            public void run() {
                long timeLeft = (pvpProtection.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
                if (timeLeft <= 0) {
                    pvpProtection.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                String actionBar = ChatColor.translateAlternateColorCodes('&',
                        config.getString("action-bar-message", "&aBạn được bảo vệ khỏi PvP &7(%timeleft%s)")
                                .replace("%timeleft%", String.valueOf(timeLeft)));
                player.sendActionBar(actionBar);
            }
        }.runTaskTimer(this, 0, 20); // 20 ticks = 1 giây

        player.sendMessage(ChatColor.GREEN + "Bạn đã được bảo vệ PvP trong " + time / 60 + " phút!");
    }

    // Kiểm tra người chơi có đang được bảo vệ PvP không
    public boolean isPvpProtected(Player player) {
        return pvpProtection.containsKey(player.getUniqueId()) &&
                pvpProtection.get(player.getUniqueId()) > System.currentTimeMillis();
    }
}