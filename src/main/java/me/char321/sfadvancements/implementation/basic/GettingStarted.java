package me.char321.sfadvancements.implementation.basic;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.events.AncientAltarCraftEvent;
import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockInteractEvent;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.EnhancedCraftingTable;
import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.api.Advancement;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class GettingStarted extends Advancement implements Listener {
    public GettingStarted(ItemStack display, String id) {
        super(display, id);
        Bukkit.getPluginManager().registerEvents(this, SFAdvancements.instance());
    }

    @EventHandler
    public void onCraft(MultiBlockInteractEvent e) {
        if(e.getMultiBlock().getSlimefunItem() instanceof EnhancedCraftingTable) {
            complete(e.getPlayer());
        }
    }

}
