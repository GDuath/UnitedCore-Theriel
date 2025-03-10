package org.unitedlands.items;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.unitedlands.items.armours.*;
import org.unitedlands.items.saplings.*;
import org.unitedlands.items.tools.*;
import org.unitedlands.items.util.GenericLocation;
import org.unitedlands.items.util.Logger;
import org.unitedlands.items.util.SerializableData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ItemDetector implements Listener {

    private final Map<String, CustomArmour> armourSets;
    private final Map<String, CustomTool> toolSets;
    private final Map<String, CustomSapling> saplingSets;
    private final Map<Location, CustomSapling> saplingMap = new HashMap<>();
    private static final int ONE_YEAR_TICKS = 630720000;

    public ItemDetector(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        armourSets = new HashMap<>();
        toolSets = new HashMap<>();
        saplingSets = new HashMap<>();

        armourSets.put("nutcracker", new NutcrackerArmour());
        armourSets.put("gamemaster", new GamemasterArmour(plugin, config));

        toolSets.put("gamemaster", new GamemasterTools(plugin, config));
        toolSets.put("amethyst", new AmethystPickaxe());
        toolSets.put("barkbinder", new BarkbinderAxe());

        saplingSets.put("ancient_oak_sapling", new AncientOak());
        saplingSets.put("avocado_sapling", new Avocado());
        saplingSets.put("banana_sapling", new Banana());
        saplingSets.put("lemon_sapling", new Lemon());
        saplingSets.put("mango_sapling", new Mango());
        saplingSets.put("midas_jungle_sapling", new MidasJungle());
        saplingSets.put("midas_oak_sapling", new MidasOak());
        saplingSets.put("olive_sapling", new Olive());
        saplingSets.put("orange_sapling", new Orange());
        saplingSets.put("pear_sapling", new Pear());

        // Add more sets here...
    }

    // Detect if the player is wearing a full set of a registered armour.
    private CustomArmour detectArmourSet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        for (Map.Entry<String, CustomArmour> entry : armourSets.entrySet()) {
            String setId = entry.getKey();
            if (isFullSet(helmet, chestplate, leggings, boots, setId)) {
                return entry.getValue();
            }
        }

        // No matching set found
        return null;
    }

    // Check if all pieces of the set match the given setId.
    private boolean isFullSet(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots, String setId) {
        return isCustomArmourPiece(helmet, setId) &&
                isCustomArmourPiece(chestplate, setId) &&
                isCustomArmourPiece(leggings, setId) &&
                isCustomArmourPiece(boots, setId);
    }

    // Check if an individual armour piece matches the setId.
    private boolean isCustomArmourPiece(ItemStack item, String setId) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        CustomStack customStack = CustomStack.byItemStack(item);
        return customStack != null && customStack.getId().contains(setId);
    }

    private void applyEffectsIfWearingArmor(Player player) {
        CustomArmour armour = detectArmourSet(player);
        if (armour != null) {
            armour.applyEffects(player);
        } else {
            removeAllEffects(player);
        }
    }

    private void removeAllEffects(Player player) {
        if (detectArmourSet(player) == null) {
            // Remove only the potion effects that were applied by our custom armour.
            for (CustomArmour armour : armourSets.values()) {
                for (PotionEffectType effectType : armour.getAppliedEffects()) {
                    if (player.hasPotionEffect(effectType)) {
                        PotionEffect current = player.getPotionEffect(effectType);
                        // Remove the effect the duration is above one year.
                        if (current != null && current.getDuration() >= ONE_YEAR_TICKS) {
                            player.removePotionEffect(effectType);
                        }
                    }
                }
            }
        }
    }

    // Detect if the player is holding a registered tool.
    private CustomTool detectTool(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        for (Map.Entry<String, CustomTool> entry : toolSets.entrySet()) {
            String toolId = entry.getKey();
            if (isCustomTool(itemInHand, toolId)) {
                return entry.getValue();
            }
        }

        // No matching tool found
        return null;
    }

    // Check if the item in hand matches the toolId.
    private boolean isCustomTool(ItemStack item, String toolId) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        CustomStack customStack = CustomStack.byItemStack(item);
        return customStack != null && customStack.getId().contains(toolId);
    }

    // Apply effects for the detected tool.
    private void applyEffectsIfHoldingTool(Player player) {
        CustomTool tool = detectTool(player);
        if (tool != null) {
            tool.applyEffects(player);
        } else {
            removeToolEffects(player);
        }
    }

    private void removeToolEffects(Player player) {
        CustomTool detectedTool = detectTool(player); // Detect currently held tool
        if (detectedTool != null) {
            // Remove only the effects applied by the detected tool
            for (PotionEffectType effectType : detectedTool.getAppliedEffects()) {
                if (player.hasPotionEffect(effectType)) {
                    player.removePotionEffect(effectType);
                }
            }
        } else {
            // Fallback: Remove effects that could belong to any tool in the registry
            toolSets.values().forEach(tool -> {
                for (PotionEffectType effectType : tool.getAppliedEffects()) {
                    if (player.hasPotionEffect(effectType)) {
                        player.removePotionEffect(effectType);
                    }
                }
            });
        }
    }

    // Detect if a held item is a custom sapling.
    public CustomSapling detectSapling(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            Logger.log("&cNo item detected.");
            return null;
        }

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) {
            Logger.log("&cItem is not a custom stack.");
            return null;
        }

        String saplingId = customStack.getId().trim().toLowerCase();
        Logger.log("&aDetected sapling ID: " + saplingId);

        CustomSapling sapling = saplingSets.get(saplingId);
        if (sapling != null) {
            Logger.log("&aSapling found: " + sapling.getId());
        } else {
            Logger.log("&cNo matching sapling for ID: " + saplingId);
        }

        return sapling;
    }

    @SuppressWarnings("unchecked")
    public void loadSaplings() {
        HashMap<GenericLocation, String> loadedSaplings = SerializableData.Farming.readFromDatabase("sapling.dat", HashMap.class);
        if (loadedSaplings == null || loadedSaplings.isEmpty()) {
            Logger.log("&aNo cached saplings found.");
            return;
        }

        Logger.log(String.format("&aLoading cached saplings &6[&e%d&6]...", loadedSaplings.size()));
        loadedSaplings.forEach((genericLocation, saplingId) -> {
            Location location = genericLocation.getLocation();
            if (location != null) {
                CustomSapling sapling = saplingSets.get(saplingId.toLowerCase());
                if (sapling != null) {
                    saplingMap.put(location, sapling);
                }
            }
        });
    }

    public Map<GenericLocation, String> getSerializableSaplings() {
        Map<GenericLocation, String> serializedSaplings = new HashMap<>();
        saplingMap.forEach((location, sapling) -> {
            if (location != null) {
                serializedSaplings.put(new GenericLocation(location), sapling.getId());
            }
        });
        return serializedSaplings;
    }

    public void removeMappedLocation(Location location) {
        CustomSapling sapling = saplingMap.remove(location);
        if (sapling != null) {
            location.getWorld().dropItemNaturally(location, sapling.getSeedItem());
            location.getBlock().setType(Material.AIR);
        }
    }

    // Handle sapling placement.
    public boolean handleSaplingInteraction(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getItem() == null) {
            return false;
        }

        CustomSapling sapling = detectSapling(event.getItem());
        if (sapling == null) {
            return false;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null ||
              !(clickedBlock.getType() == Material.GRASS_BLOCK ||
                clickedBlock.getType() == Material.DIRT ||
                clickedBlock.getType() == Material.PODZOL ||
                clickedBlock.getType() == Material.SHORT_GRASS ||
                clickedBlock.getType() == Material.TALL_GRASS ||
                clickedBlock.getType() == Material.DEAD_BUSH ||
                clickedBlock.getType() == Material.SNOW)) {
            return false;
        }

        // Check if the clicked block is short grass or tall grass.
        if (clickedBlock.getType() == Material.SHORT_GRASS ||
            clickedBlock.getType() == Material.TALL_GRASS ||
            clickedBlock.getType() == Material.DEAD_BUSH ||
            clickedBlock.getType() == Material.SNOW) {
            clickedBlock.setType(Material.AIR); // Remove the grass
            clickedBlock = clickedBlock.getRelative(0, -1, 0); // Get the block below/
        }

        // Ensure sapling can only be planted on valid ground.
        if (!(clickedBlock.getType() == Material.GRASS_BLOCK || clickedBlock.getType() == Material.DIRT || clickedBlock.getType() == Material.PODZOL)) {
            return false;
        }

        Biome biome = clickedBlock.getBiome();
        if (!sapling.canGrowInBiome(biome)) {
            event.setCancelled(true);
            return true;
        }

        Block above = clickedBlock.getRelative(0, 1, 0);
        if (!above.getType().equals(Material.AIR)) {
            return false;
        }

        // Plant the sapling.
        above.setType(sapling.getVanillaSapling());
        saplingMap.put(above.getLocation(), sapling);

        // Reduce the item count.
        event.getItem().setAmount(event.getItem().getAmount() - 1);

        // Cancel the event to prevent further processing.
        event.setCancelled(true);
        return true;
    }

    @EventHandler
    // Check block breaks for use of custom tools.
    public void onNormalBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        CustomTool tool = detectTool(player);
        // Delegate the block breaking logic to the specific tool.
        if (tool != null) {
            tool.handleBlockBreak(player, event);
        }
    }

    @EventHandler
    // Check block interactions for use of custom tools.
    public void handleInteract(PlayerInteractEvent event) {
        // Check if it's a sapling first.
        if (handleSaplingInteraction(event)) {
            return;
        }
        // Continue to tool processing.
        Player player = event.getPlayer();
        CustomTool tool = detectTool(player);
        if (tool != null) {
            tool.handleInteract(player, event);
        }
    }

    @EventHandler
    // Check player damage events for use of custom armour.
    public void handlePlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        CustomArmour armour = detectArmourSet(player);
        if (armour != null) {
            armour.handlePlayerDamage(player, event);
        }
    }

    @EventHandler
    // Check entity damage for use of custom tools.
    public void handleEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            CustomTool tool = detectTool(player);
            if (tool != null) {
                tool.handleEntityDamage(player, event);
            }
        }
    }

    @EventHandler
    // Handle experience pickups.
    public void handleExpPickup(PlayerPickupExperienceEvent event) {
        Player player = event.getPlayer();
        ExperienceOrb orb = event.getExperienceOrb();
        CustomArmour armour = detectArmourSet(player);
        if (armour != null) {
            armour.handleExpPickup(player, orb);
        }
    }

    @EventHandler
    // Handle armour changes.
    public void onPlayerArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UnitedItems")), () -> applyEffectsIfWearingArmor(player));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Apply or remove effects when a player joins.
        applyEffectsIfWearingArmor(event.getPlayer());
    }

    @EventHandler
    // Check if tools has been moved when interacting with the inventory.
    public void onInventoryClick(InventoryClickEvent event) {
        Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UnitedItems")), () -> {
            if (event.getWhoClicked() instanceof Player player) {
                applyEffectsIfHoldingTool(player);
            }
        });
    }

    @EventHandler
    // Check if the armour has broken when taking damage.
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UnitedItems")), () -> applyEffectsIfWearingArmor(player));
        }
    }

    @EventHandler
    // Removes effects on player death.
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        detectArmourSet(player);
        Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UnitedItems")), () -> removeAllEffects(player));
    }

    @EventHandler
    // Checks when a player switches their held item.
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UnitedItems")), () -> applyEffectsIfHoldingTool(player));
    }

    @EventHandler
    // Checks if the dropped item is a registered tool.
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        for (Map.Entry<String, CustomTool> entry : toolSets.entrySet()) {
            String toolId = entry.getKey();
            if (isCustomTool(droppedItem, toolId)) {
                // If the dropped item is a registered tool, remove its effects.
                Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UnitedItems")), () -> removeToolEffects(player));
                break; // Stop checking further since the tool has been identified.
            }
        }
    }

    @EventHandler
    // Checks if the player picks up a registered tool.
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        // Check if the entity picking up the item is a player.
        if (event.getEntity() instanceof Player player) {
            ItemStack pickedUpItem = event.getItem().getItemStack();
            // Check if the picked up item is a registered tool.
            for (Map.Entry<String, CustomTool> entry : toolSets.entrySet()) {
                String toolId = entry.getKey();
                if (isCustomTool(pickedUpItem, toolId)) {
                    // If the picked up item is a registered tool, apply its effects.
                    Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UnitedItems")), () -> applyEffectsIfHoldingTool(player));
                    break; // Stop checking further since the tool has been identified.
                }
            }
        }
    }

    @EventHandler
    // Handle tree grow events (creating the custom tree).
    public void onGrow(StructureGrowEvent event) {
        Location location = event.getLocation().toBlockLocation();
        CustomSapling sapling = saplingMap.get(location);

        if (sapling != null) {
            Biome biome = location.getBlock().getBiome();
            if (!sapling.canGrowInBiome(biome)) {
                event.setCancelled(true);
                return;
            }

            for (BlockState block : event.getBlocks()) {
                Location blockLocation = block.getLocation().toBlockLocation();

                // Log construction.
                if (block.getBlockData().getMaterial().toString().endsWith("_LOG")) {
                    if (sapling.isUsingVanillaStem()) {
                        blockLocation.getBlock().setType(sapling.getStemBlock());
                    } else if (sapling.getStemReplaceBlockName() != null) {
                        CustomBlock placedBlock = CustomBlock.place(sapling.getStemReplaceBlockName(), blockLocation);

                        if (placedBlock == null) {
                            blockLocation.getBlock().setType(sapling.getStemBlock());
                        }
                    }
                }

                // Leaf construction.
                else if (block.getType() == Material.OAK_LEAVES || block.getType() == Material.JUNGLE_LEAVES) {
                    if (sapling.getCustomLeavesName() != null) {
                        block.setType(Material.AIR); // Remove vanilla leaves first

                        // Get defined fruited block.
                        String leafType = sapling.isSuccessful() ? sapling.getFruitedLeavesName() : sapling.getCustomLeavesName();
                        CustomBlock.place(leafType, blockLocation);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDecay(LeavesDecayEvent event) {
        CustomSapling sapling = saplingMap.get(event.getBlock().getLocation());
        if (sapling != null) {
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
        }
    }

    @EventHandler
    public void onTreeBlockBreak(BlockBreakEvent event) {
        removeMappedLocation(event.getBlock().getLocation());
    }
}