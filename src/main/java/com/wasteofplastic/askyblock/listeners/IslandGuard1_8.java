/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.wasteofplastic.askyblock.listeners;

import java.util.Optional;

import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.Flags;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento Provides protection to islands - handles newer events that
 *         may not exist in older servers
 */
public class IslandGuard1_8 {
	private final ASkyBlock plugin;
	private final static boolean DEBUG = false;

	public IslandGuard1_8(final ASkyBlock plugin) {
		this.plugin = plugin;

	}

	/**
	 * Handle interaction with armor stands V1.8 Note - some armor stand
	 * protection is done in IslandGuard.java, e.g. against projectiles.
	 * 
	 * @param e
	 */
	@Listener
	public void onPlayerInteract(final InteractEntityEvent e) {
		Optional<Player> player = e.getCause().first(Player.class);
		if (!player.isPresent()) {
			return;
		}
		Player p = player.get();
		
		

		if (DEBUG) {
			plugin.getLogger().info("1.8 " + e.getEventName());
		}
		if (!IslandGuard.inWorld(p)) {
			return;
		}
		// This permission bypasses protection
		if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
			return;
		}
		if (e.getRightClicked() != null && e.getRightClicked().getType().equals(EntityTypes.ARMOR_STAND)) {
			// Check island
			Island island = plugin.getGrid().getIslandAt(e.getRightClicked().getLocation());
			if (island != null) {
				if (island.isSpawn()) {
					if (Settings.allowSpawnArmorStandUse) {
						return;
					}
				} else {
					if (island.getMembers().contains(e.getPlayer().getUniqueId())
							|| island.getIgsFlag(Flags.allowArmorStandUse)) {
						return;
					}
				}
			}
			// plugin.getLogger().info("1.8 " + "DEBUG: Armor stand clicked off
			// island");
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		}
	}

	/**
	 * Handle V1.8 blocks that need special treatment Tilling of coarse dirt
	 * into dirt Usually prevented because it could lead to an endless supply of
	 * dirt with gravel
	 * 
	 * @param e
	 */
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerInteract(final PlayerInteractEvent e) {
		if (DEBUG) {
			plugin.getLogger().info("1.8 " + e.getEventName());
		}
		if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		if (!IslandGuard.inWorld(e.getPlayer())) {
			return;
		}
		if (e.getPlayer().isOp()) {
			return;
		}
		// This permission bypasses protection
		if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
				|| VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "craft.dirt")) {
			return;
		}
		// Prevents tilling of coarse dirt into dirt
		ItemStack inHand = e.getPlayer().getItemInHand();
		if (inHand.getType() == Material.WOOD_HOE || inHand.getType() == Material.IRON_HOE
				|| inHand.getType() == Material.GOLD_HOE || inHand.getType() == Material.DIAMOND_HOE
				|| inHand.getType() == Material.STONE_HOE) {
			// plugin.getLogger().info("1.8 " + "DEBUG: hoe in hand");
			Block block = e.getClickedBlock();
			// plugin.getLogger().info("1.8 " + "DEBUG: block is " +
			// block.getType() +
			// ":" + block.getData());
			// Check if coarse dirt
			if (block.getType() == Material.DIRT && block.getData() == (byte) 1) {
				// plugin.getLogger().info("1.8 " + "DEBUG: hitting coarse
				// dirt!");
				e.setCancelled(true);
			}
		}
	}

	// Armor stand events
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	void placeArmorStandEvent(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if (DEBUG) {
			plugin.getLogger().info("1.8 " + "Armor stand place " + e.getEventName());
		}
		if (!IslandGuard.inWorld(p)) {
			return;
		}
		if (p.isOp() || VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
			// You can do anything if you are Op
			return;
		}

		// Check if they are holding armor stand
		ItemStack inHand = e.getPlayer().getItemInHand();
		if (inHand != null && inHand.getType().equals(Material.ARMOR_STAND)) {
			// Check island
			Island island = plugin.getGrid().getIslandAt(e.getPlayer().getLocation());
			if (island != null
					&& (island.getMembers().contains(p.getUniqueId()) || island.getIgsFlag(Flags.allowPlaceBlocks))) {
				// plugin.getLogger().info("1.8 " + "DEBUG: armor stand place
				// check");
				if (Settings.limitedBlocks.containsKey("ARMOR_STAND")
						&& Settings.limitedBlocks.get("ARMOR_STAND") > -1) {
					// plugin.getLogger().info("1.8 " + "DEBUG: count armor
					// stands");
					int count = island.getTileEntityCount(Material.ARMOR_STAND);
					// plugin.getLogger().info("1.8 " + "DEBUG: count is " +
					// count + " limit is " +
					// Settings.limitedBlocks.get("ARMOR_STAND"));
					if (Settings.limitedBlocks.get("ARMOR_STAND") <= count) {
						e.getPlayer().sendMessage(ChatColor.RED
								+ (plugin.myLocale(e.getPlayer().getUniqueId()).entityLimitReached.replace("[entity]",
										Util.prettifyText(Material.ARMOR_STAND.toString()))).replace("[number]",
												String.valueOf(Settings.limitedBlocks.get("ARMOR_STAND"))));
						e.setCancelled(true);
						return;
					}
				}
				return;
			}
			// plugin.getLogger().info("1.8 " + "DEBUG: stand place cancelled");
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.getPlayer().updateInventory();
		}

	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void ArmorStandDestroy(EntityDamageByEntityEvent e) {
		if (DEBUG) {
			plugin.getLogger().info("1.8 " + "IslandGuard New " + e.getEventName());
		}
		if (!(e.getEntity() instanceof LivingEntity)) {
			return;
		}
		if (!IslandGuard.inWorld(e.getEntity())) {
			return;
		}
		final LivingEntity livingEntity = (LivingEntity) e.getEntity();
		if (!livingEntity.getType().equals(EntityType.ARMOR_STAND)) {
			return;
		}
		if (e.getDamager() instanceof Player) {
			Player p = (Player) e.getDamager();
			if (p.isOp() || VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
				return;
			}
			// Check if on island
			if (plugin.getGrid().playerIsOnIsland(p)) {
				return;
			}
			// Check island
			Island island = plugin.getGrid().getIslandAt(e.getEntity().getLocation());
			if (island != null && island.isSpawn() && Settings.allowSpawnBreakBlocks) {
				return;
			}
			if (island != null && island.getIgsFlag(Flags.allowBreakBlocks)) {
				return;
			}
			p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).islandProtected);
			e.setCancelled(true);
		}
	}

}