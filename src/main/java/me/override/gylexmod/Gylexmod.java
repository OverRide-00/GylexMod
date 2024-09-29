package me.override.gylexmod;

import me.override.gylexmod.Block.ModBlocks;
import me.override.gylexmod.Commands.Permit;
import net.fabricmc.api.ModInitializer;
import me.override.gylexmod.Item.ModItemGroups;
import me.override.gylexmod.Item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Gylexmod implements ModInitializer {
    public static final String MOD_ID = "gylexmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItemGroups.registerItemGroups();

        ModItems.registerModItems();
        ModBlocks.registerModBlocks();

        Permit.registerCommands();
    }
}
