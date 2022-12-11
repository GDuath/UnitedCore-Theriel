package org.unitedlands.wars.war;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.Utils;
import org.unitedlands.wars.events.WarEndEvent;
import org.unitedlands.wars.war.entities.WarringEntity;
import org.unitedlands.wars.war.entities.WarringNation;
import org.unitedlands.wars.war.entities.WarringTown;
import org.unitedlands.wars.war.health.WarHealth;

import java.util.*;

import static net.kyori.adventure.text.Component.text;
import static org.unitedlands.wars.Utils.isBannedWorld;
import static org.unitedlands.wars.Utils.teleportPlayerToSpawn;

public class War {
    private static final UnitedWars plugin = UnitedWars.getInstance();
    private final List<WarringTown> warringTowns;
    private final List<WarringNation> warringNations;
    private final HashSet<UUID> residents;
    private final WarType warType;
    private UUID uuid = UUID.randomUUID();
    private WarTimer warTimer = null;
    private WarringEntity winner;
    private WarringEntity loser;

    public War(List<Town> warringTowns, List<Nation> warringNations, HashSet<Resident> residents, WarType warType) {
        this.warringTowns = generateWarringTownList(warringTowns);
        this.warringNations = generateWarringNationList(warringNations);
        this.residents = Utils.toUUID(residents);
        this.warType = warType;
        warTimer = new WarTimer(this);
        // Start the war immediately, since this is the first time.
        startWar();
        // Save war to database
        WarDatabase.addWar(this);
    }

    // Used for loading existing wars from the config.
    public War(List<Town> warringTowns, List<Nation> warringNations, HashSet<Resident> residents, WarType warType, UUID uuid) {
        this.warringTowns = generateWarringTownList(warringTowns);
        this.warringNations = generateWarringNationList(warringNations);
        this.residents = Utils.toUUID(residents);
        this.warType = warType;
        this.uuid = uuid;
        // Save war to internal database
        WarDatabase.addWar(this);
    }

    public List<WarringEntity> getWarringEntities() {
        List<WarringEntity> warringEntities = new ArrayList<>();
        if (warType == WarType.TOWNWAR)
            warringEntities.addAll(warringTowns);
        else
            warringEntities.addAll(warringNations);

        return warringEntities;
    }

    public List<WarringTown> getWarringTowns() {
        return warringTowns;
    }

    public List<WarringNation> getWarringNations() {
        return warringNations;
    }

    public HashSet<Resident> getWarParticipants() {
        HashSet<Resident> uuidResidents = new HashSet<>();
        residents.forEach(uuid -> uuidResidents.add(Utils.getTownyResident(uuid)));
        return uuidResidents;
    }

    public WarType getWarType() {
        return warType;
    }

    public UUID getUuid() {
        return uuid;
    }

    public WarTimer getWarTimer() {
        return warTimer;
    }

    public HashSet<Player> getOnlinePlayers() {
        HashSet<Player> players = new HashSet<>();
        getWarParticipants().forEach(resident -> {
            if (resident.isOnline())
                players.add(resident.getPlayer());
        });
        return players;
    }

    public void startWar() {
        // Start the war countdowns
        warTimer.startCountdown();

        // Start player procedures
        runPlayerProcedures();

        // Make the involved entities have an active war.
        toggleActiveWar(true);
    }

    public void endWar(WarringEntity winner, WarringEntity loser) {
        this.winner = winner;
        this.loser = loser;
        // Call event. Handle rewarding in WarListener
        WarEndEvent warEndEvent = new WarEndEvent(this, winner, loser);
        Bukkit.getServer().getPluginManager().callEvent(warEndEvent);

        // Remove health.
        hideHealth(winner);
        hideHealth(loser);

        // Notify entities
        notifyWin();
        notifyLoss();
        // Give rewards
        giveWarEarnings();

        // Toggle active war
        toggleActiveWar(false);
        saveLastWarTimes();

        // Clear from database.
        WarDatabase.removeWarringEntity(winner);
        WarDatabase.removeWarringEntity(loser);
        WarDatabase.removeWar(this);
    }

    public void surrenderWar(WarringEntity winner, WarringEntity loser) {
        this.winner = winner;
        this.loser = loser;
        // Call event. Handle rewarding in WarListener
        WarEndEvent warEndEvent = new WarEndEvent(this, winner, loser);
        Bukkit.getServer().getPluginManager().callEvent(warEndEvent);

        // Remove health.
        hideHealth(winner);
        hideHealth(loser);

        // Notify
        notifySurrenderAccepted();
        notifySurrendered();
        // Toggle active war
        toggleActiveWar(false);

        // Clear from database.
        WarDatabase.removeWarringEntity(winner);
        WarDatabase.removeWarringEntity(loser);
        WarDatabase.removeWar(this);
    }

    // Called inside WarTimer.
    public void endWarTimer() {
        for (Player player : getOnlinePlayers()) {
            // Remove for again for safe measures
            warTimer.removeViewer(player);

            // Show the health.
            WarringEntity warringEntity = WarDatabase.getWarringEntity(player);
            warringEntity.getWarHealth().show(player);
            // Teleport them to spawn
            teleportPlayerToSpawn(player);
        }
    }

    public void broadcast(Component message) {
        getOnlinePlayers().forEach(player -> player.sendMessage(message));
    }

    private void runPlayerProcedures() {
        for (Resident resident : getWarParticipants()) {
            // Set player lives
            WarDataController.setResidentLives(resident, 3);

            Player player = resident.getPlayer();
            // Player is offline, next
            if (player == null) continue;

            // Teleport players to appropriate places.
            if (isBannedWorld(player.getWorld().getName()))
                teleportPlayerToSpawn(player);

            // Run start war commands
            for (String command : plugin.getConfig().getStringList("commands-on-war-start"))
                player.performCommand(command);

        }
    }

    private void toggleActiveWar(boolean toggle) {
        for (WarringEntity entity : getWarringEntities()) {
            entity.getGovernment().setActiveWar(toggle);
            if (entity.getGovernment() instanceof Nation nation) {
                nation.getAllies().forEach(ally -> ally.setActiveWar(toggle));
            }
        }
    }

    private void saveLastWarTimes() {
        WarDataController.setLastWarTime(winner.getGovernment(), System.currentTimeMillis());
        WarDataController.setLastWarTime(loser.getGovernment(), System.currentTimeMillis());
    }

    public boolean hasActiveTimer() {
        if (warTimer == null)
            return false;
        else
            return warTimer.getRemainingSeconds() > 0;
    }

    private void hideHealth(WarringEntity warringEntity) {
        warringEntity.getOnlinePlayers().forEach(player -> warringEntity.getWarHealth().hide(player));
    }

    private void notifySurrendered() {
        Title title = Utils.getTitle("<dark_red><bold>WAR LOST!", "<red>You've surrendered from the war!");
        loser.getOnlinePlayers().forEach(player -> {
            player.showTitle(title);
            //   player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_7, 1F, 1F);
            player.sendMessage(Utils.getMessage("war-lost-surrender", getWonAndLostPlaceholders()));
        });
    }

    private void notifySurrenderAccepted() {
        Title title = Utils.getTitle("<dark_green><bold>VICTORY!", "<green>Your enemy has surrendered!");
        winner.getOnlinePlayers().forEach(player -> {
            player.showTitle(title);
            //   player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_1, 1F, 1F);
            player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
        });
    }

    private void notifyWin() {
        Title title = Utils.getTitle("<dark_green><bold>VICTORY!", "<green>The war has ended!");
        winner.getOnlinePlayers().forEach(player -> {
            player.showTitle(title);
         //   player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_1, 1F, 1F);
            player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
            player.sendMessage(Utils.getMessage("war-won", getWonAndLostPlaceholders()));
        });
    }

    private void notifyLoss() {
        Title title = Utils.getTitle("<dark_red><bold>WAR LOST!", "<red>The war has ended!");
        loser.getOnlinePlayers().forEach(player -> {
            player.showTitle(title);
         //   player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_7, 1F, 1F);
            player.sendMessage(Utils.getMessage("war-lost", getWonAndLostPlaceholders()));
        });
    }

    private TagResolver.Single[] getWonAndLostPlaceholders() {
        return new TagResolver.Single[] {
                Placeholder.component("money-amount", text(calculateWonMoney())),
                Placeholder.component("bonus-claims", text(calculateBonusBlocks())),
                Placeholder.component("winner", text(winner.name())),
                Placeholder.component("loser", text(loser.name()))
        };
    }


    private void giveWarEarnings() {
        winner.getGovernment().getAccount().deposit(calculateWonMoney(), "Won a war against" + loser.name());
        if (winner instanceof WarringTown town)
            town.getTown().addBonusBlocks(calculateBonusBlocks());
        else if (winner instanceof WarringNation nation)
            nation.getNation().getCapital().addBonusBlocks(calculateBonusBlocks());
    }

    private int calculateBonusBlocks() {
        return (int) (loser.getGovernment().getTownBlocks().size() * 0.25);
    }

    private double calculateWonMoney() {
        double total = 0;
        total += loser.getGovernment().getAccount().getHoldingBalance() * 0.5;

        if (loser.getGovernment() instanceof Nation nation) {
            for (Town town : nation.getTowns()) {
                total += town.getAccount().getHoldingBalance() * 0.5;
            }
        }
        return total;
    }
    private List<WarringTown> generateWarringTownList(List<Town> townList) {
        if (townList == null)
            return null;

        List<WarringTown> generatedList = new ArrayList<>();
        for (Town town : townList) {
            generatedList.add(new WarringTown(town, new WarHealth(town), town.getResidents(), this));
        }
        return generatedList;
    }

    private List<WarringNation> generateWarringNationList(List<Nation> nationList) {
        if (nationList == null)
            return null;

        List<WarringNation> generatedList = new ArrayList<>();
        for (Nation nation : nationList) {
            // Create a list with the nation residents
            List<Resident> warringResidents = new ArrayList<>(nation.getResidents());
            // add all the allies
            for (Nation ally : nation.getAllies()) {
                warringResidents.addAll(ally.getResidents());
            }
            generatedList.add(new WarringNation(nation, new WarHealth(nation), warringResidents, this));
        }
        return generatedList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        War war = (War) o;

        return Objects.equals(uuid, war.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}