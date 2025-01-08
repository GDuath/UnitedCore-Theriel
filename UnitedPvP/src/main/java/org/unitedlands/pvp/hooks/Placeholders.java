package org.unitedlands.pvp.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.unitedlands.pvp.player.PvpPlayer;
import org.unitedlands.pvp.player.Status;

public class Placeholders extends PlaceholderExpansion {

    @Override
    public String getAuthor() {
        return "Maroon28";
    }

    @Override
    public String getIdentifier() {
        return "unitedpvp";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player.getPlayer() != null) {
            PvpPlayer pvpPlayer = new PvpPlayer((Player) player);
            if (params.equalsIgnoreCase("status")) {
                int hostility = pvpPlayer.getHostility();
                return pvpPlayer.getIconHex(hostility) + pvpPlayer.getStatus().getIcon();
            } else if (params.equalsIgnoreCase("status-string")) {
                return String.valueOf(pvpPlayer.getStatus());
            } else if (params.equalsIgnoreCase("is-immune")) {
                return String.valueOf(pvpPlayer.isImmune());
            }
        }

        return null;
    }
}
