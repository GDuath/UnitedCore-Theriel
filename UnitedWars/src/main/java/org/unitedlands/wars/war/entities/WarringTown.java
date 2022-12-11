package org.unitedlands.wars.war.entities;

import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.Utils;
import org.unitedlands.wars.war.War;
import org.unitedlands.wars.war.WarDatabase;
import org.unitedlands.wars.war.health.WarHealth;

import java.util.*;

public class WarringTown implements WarringEntity {
    private final UUID townUUID;
    private final WarHealth warHealth;
    private final HashSet<UUID> warringResidents;
    private final UUID warUUID;

    public WarringTown(Town town, WarHealth warHealth, List<Resident> warringResidents, War war) {
        this.townUUID = town.getUUID();
        this.warHealth = warHealth;
        this.warringResidents = Utils.toUUID(warringResidents);
        this.warUUID = war.getUuid();
        if (townUUID != null && warHealth != null && warUUID != null) {
            WarDatabase.addWarringTown(this);
        }
    }


    public War getWar() {
        return WarDatabase.getWar(warUUID);
    }

    public WarHealth getWarHealth() {
        return warHealth;
    }

    @Override
    public HashSet<Resident> getWarParticipants() {
        HashSet<Resident> residents = new HashSet<>();
        warringResidents.forEach(uuid -> residents.add(Utils.getTownyResident(uuid)));
        return residents;
    }

    @Override
    public HashSet<Player> getOnlinePlayers() {
        HashSet<Player> players = new HashSet<>();
        getWarParticipants().forEach(resident -> {
            if (resident.getPlayer() != null) {
                players.add(resident.getPlayer());
            }
        });
        return players;
    }

    @Override
    public Government getGovernment() {
        return getTown();
    }

    @Override
    public Resident getLeader() {
        return getTown().getMayor();
    }

    public UUID getUUID() {
        return townUUID;
    }

    public String getPath() {
        return "warring-towns";
    }

    @Override
    public String name() {
        return getTown().getFormattedName();
    }

    public Town getTown() {
        return UnitedWars.TOWNY_API.getTown(townUUID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WarringTown that = (WarringTown) o;

        return Objects.equals(townUUID, that.townUUID);
    }

    @Override
    public int hashCode() {
        return townUUID != null ? townUUID.hashCode() : 0;
    }
}