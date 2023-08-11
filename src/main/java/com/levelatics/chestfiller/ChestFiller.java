package com.levelatics.chestfiller;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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

import java.util.*;

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
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("chestfill")) {
                // Give the player the chestFillerStick
                player.getInventory().addItem(chestFillerStick);
                player.sendMessage(prefix + "You received the Chest Filler Stick!");
                return true;
            }
        }
        return false;
    }

    // Handle right-clicking chests
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            Player player = event.getPlayer();

            // Check if the player is holding the chestFillerStick
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem.isSimilar(chestFillerStick)) {
                Block block = event.getClickedBlock();
                if (block != null && block.getType() == Material.CHEST) {
                    event.setCancelled(true); // Prevent chest interaction
                    Chest chest = (Chest) block.getState();
                    fillChestRandomly(chest.getInventory(), currentLootTable);
                    player.sendMessage(prefix + "Chest has been filled randomly with items from the loot table!");
                }
            }
        }
    }

    // Handle punching with the ChestFillerStick to switch loot tables
    @EventHandler
    public void onPlayerPunch(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("LEFT_CLICK")) {
            Player player = event.getPlayer();
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem.isSimilar(chestFillerStick)) {
                // Switch to the next loot table
                switchLootTable();
                player.sendMessage(prefix + "Switched to loot table:§a§l " + currentLootTable);
                event.setCancelled(true); // Prevent block interaction
            }
        }
    }

    // Prevent breaking chests while holding the ChestFillerStick
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().isSimilar(chestFillerStick)) {
            event.setCancelled(true);
        }
    }

    private void switchLootTable() {
        Set<String> lootTableKeys = Objects.requireNonNull(lootTablesConfig.getConfigurationSection("loot")).getKeys(false);
        List<String> lootTableList = new ArrayList<>(lootTableKeys);
        int currentIndex = lootTableList.indexOf(currentLootTable);
        int nextIndex = (currentIndex + 1) % lootTableList.size();
        currentLootTable = lootTableList.get(nextIndex);
    }

    private void fillChestRandomly(Inventory chestInventory, String lootTable) {
        if(lootTablesConfig == null) {
            // Config ist null ausgeben
            getServer().getOnlinePlayers().forEach(player -> player.sendMessage("Config is null"));
            return;
        }
        getServer().getOnlinePlayers().forEach(player -> player.sendMessage(lootTablesConfig.getConfigurationSection("loot").getValues(true).toString()));

        ConfigurationSection lootTableSection = lootTablesConfig.getConfigurationSection("loot." + lootTable);

        if (lootTableSection != null) {
            List<String> lootItems = new ArrayList<>(lootTableSection.getKeys(false));
            for (int slot = 0; slot < chestInventory.getSize(); slot++) {
                if (random.nextFloat() <= 0.5) { // Adjust probability as needed
                    String randomItemName = lootItems.get(random.nextInt(lootItems.size()));
                    Material material = Material.getMaterial(randomItemName);
                    if (material != null) {
                        int amount = 1; // You can customize the amount of items
                        ItemStack itemStack = new ItemStack(material, amount);
                        chestInventory.setItem(slot, itemStack);
                    }
                } else {
                    chestInventory.setItem(slot, new ItemStack(Material.AIR));
                }
            }
        }
    }
}