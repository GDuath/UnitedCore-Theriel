package org.unitedlands.skills.safarinets;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import de.Linus122.SafariNet.API.Listener;
import de.Linus122.SafariNet.API.Status;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.unitedlands.skills.Utils;
import org.unitedlands.skills.skill.Skill;
import org.unitedlands.skills.skill.SkillType;

import java.util.List;

public class WranglerListener implements Listener {
    @Override
    public void playerCatchEntity(Player player, Entity entity, Status status) {
        status.setCancelled(!canUse(player, entity));
    }

    @Override
    public void playerReleaseEntity(Player player, Entity entity, Status status) {
        status.setCancelled(!canUse(player, entity));
    }

    private boolean canUse(Player player, Entity entity) {
        if (!isFarmer(player)) {
            player.sendMessage(Utils.getMessage("must-be-farmer"));
            return false;
        }
        Skill wrangler = new Skill(player, SkillType.WRANGLER);
        return wrangler.getLevel() != 0 && isPassive(entity);
    }

    private boolean isPassive(Entity entity) {
        FileConfiguration config = Utils.getUnitedSkills().getConfig();
        List<String> passiveEntities = config.getStringList("wrangler-mobs");
        for (String entityType : passiveEntities) {
            if (entity.getType().toString().equals(entityType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFarmer(Player player) {
        JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        for (JobProgression job : jobsPlayer.getJobProgression()) {
            return job.getJob().getName().equals("Farmer");
        }
        return false;
    }
}