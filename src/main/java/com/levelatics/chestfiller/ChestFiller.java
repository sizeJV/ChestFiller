package com.levelatics.chestfiller;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ChestFiller extends JavaPlugin implements CommandExecutor, Listener {

    private FileConfiguration lootTablesConfig;
    private Random random = new Random();
    private String prefix = "§8[§6§lChest Filler§8] §e"; // Prefix for messages
    private ItemStack chestFillerStick = new ItemStack(Material.STICK);
    private ItemMeta chestFillerStickMeta;
    private String currentLootTable = "default"; // Default loot table

    @Override
    public void onEnable() {
        getLogger().info("ChestFiller has been enabled!");
        getCommand("chestfill").setExecutor(this);
        getCommand("reloadchestfiller").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this); // Register the listener
        saveDefaultConfig();
        lootTablesConfig = getConfig();

        chestFillerStickMeta = chestFillerStick.getItemMeta();
        assert chestFillerStickMeta != null;
        chestFillerStickMeta.setDisplayName("§6§lChest Filler");
        chestFillerStick.setItemMeta(chestFillerStickMeta);
    }

    @Override
    public void onDisable() {
        getLogger().info("ChestFiller has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("reloadchestfiller")) {
            if (!sender.hasPermission("chestfiller.reload")) {
                sender.sendMessage(prefix + "You do not have permission to use this command.");
                return true;
            }

            reloadConfig();
            lootTablesConfig = getConfig();
            sender.sendMessage(prefix + "Config has been reloaded.");
            return true;
        }
        if (!(sender instanceof Player) || !command.getName().equalsIgnoreCase("chestfill")) {
            return false;
        }

        Player player = (Player) sender;
        player.getInventory().addItem(chestFillerStick);
        sendMessage(player, "You received the Chest Filler Stick!");
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT_CLICK")) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!heldItem.isSimilar(chestFillerStick)) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) {
            return;
        }

        event.setCancelled(true);
        Chest chest = (Chest) block.getState();
        fillChestRandomly(chest.getInventory(), currentLootTable);
        sendMessage(player, "Chest has been filled randomly with items from the loot table!");
    }

    @EventHandler
    public void onPlayerPunch(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("LEFT_CLICK")) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!heldItem.isSimilar(chestFillerStick)) {
            return;
        }

        switchLootTable();
        sendMessage(player, "Switched to loot table:§a§l " + currentLootTable);
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().isSimilar(chestFillerStick)) {
            event.setCancelled(true);
        }
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(prefix + message);
    }

    private void fillChestRandomly(Inventory chestInventory, String lootTable) {
        if (lootTablesConfig == null) {
            getLogger().warning("Config is null!");
            return;
        }

        List<String> materials = lootTablesConfig.getStringList("loot." + lootTable);
        if (materials == null || materials.isEmpty()) {
            getLogger().warning("Loot table 'loot." + lootTable + "' does not exist or is empty!");
            return;
        }

        for (int slot = 0; slot < chestInventory.getSize(); slot++) {
            if (random.nextFloat() > 0.5) {
                chestInventory.setItem(slot, new ItemStack(Material.AIR));
                continue;
            }

            String randomItemName = materials.get(random.nextInt(materials.size()));
            Material material = Material.getMaterial(randomItemName);
            if (material != null) {
                int amount = 1; // You can customize the amount of items
                ItemStack itemStack = new ItemStack(material, amount);
                chestInventory.setItem(slot, itemStack);
            }
        }
    }

    private void switchLootTable() {
        Set<String> lootTableKeys = lootTablesConfig.getConfigurationSection("loot").getKeys(false);
        List<String> lootTableList = new ArrayList<>(lootTableKeys);
        int currentIndex = lootTableList.indexOf(currentLootTable);
        int nextIndex = (currentIndex + 1) % lootTableList.size();
        currentLootTable = lootTableList.get(nextIndex);
    }
}