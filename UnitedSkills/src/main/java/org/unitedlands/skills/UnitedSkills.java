package org.unitedlands.skills;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.skills.brewer.BlendingGui;
import org.unitedlands.skills.brewer.BrewerListener;
import org.unitedlands.skills.commands.BlendCommand;
import org.unitedlands.skills.farmer.FarmerListener;
import org.unitedlands.skills.miner.MinerListener;

public final class UnitedSkills extends JavaPlugin {
    @Override
    public void onEnable() {
        getCommand("blend").setExecutor(new BlendCommand(new BlendingGui(this)));
        registerListeners();
        saveDefaultConfig();
    }

    private void registerListeners() {
        final BrewerListener brewerListener = new BrewerListener(this);
        final FarmerListener farmerListener = new FarmerListener(this);
        final MinerListener minerListener = new MinerListener(this, getCoreProtect());
        final PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(brewerListener, this);
        pluginManager.registerEvents(farmerListener, this);
        pluginManager.registerEvents(minerListener, this);
    }

    public CoreProtectAPI getCoreProtect() {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (!(plugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
        if (!CoreProtect.isEnabled()) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 9) {
            return null;
        }

        return CoreProtect;
    }
}
