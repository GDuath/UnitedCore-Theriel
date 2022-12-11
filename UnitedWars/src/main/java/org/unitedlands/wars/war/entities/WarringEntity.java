package org.unitedlands.wars.war.entities;

import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.entity.Player;
import org.unitedlands.wars.war.War;
import org.unitedlands.wars.war.health.WarHealth;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public interface WarringEntity {
    War getWar();

    UUID getUUID();

    WarHealth getWarHealth();

    HashSet<Resident> getWarParticipants();
    HashSet<Player> getOnlinePlayers();

    Government getGovernment();
    Resident getLeader();

    String getPath();

    String name();
}