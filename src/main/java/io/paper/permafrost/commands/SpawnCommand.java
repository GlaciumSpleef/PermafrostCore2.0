package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefFFAGame;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefQueueManager;
import io.paper.permafrost.core.game.SpleefSpectator;
import io.paper.permafrost.utilities.SpleefMode;

@SuppressWarnings("unused")
public class SpawnCommand implements CommandExecutor {
	private Main main;

	public SpawnCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return true;
		} else {
			Player player = (Player) sender;
			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
			if (sp != null) {
				if (sp.getInGame() || sp.getInTeamGame()) {
					if (!main.getDataLoader().getSpectatorsForGame(sp.getGame()).contains(sp.getSpectator())) {
						sender.sendMessage("This command cannot be run in game.");
						return true;
					}
				}
			}
			if (command.getName().equalsIgnoreCase("spawn")) {
				Location teleportLocation = new Location(Bukkit.getWorld("lobby"), 9012.5,194,8287.5);
				teleportLocation.setYaw(180f);
				player.getPlayer().teleport(teleportLocation);
				player.getPlayer().getInventory().clear();
				player.setGameMode(GameMode.SURVIVAL);
				
				if(main.getSpleggQueueManager().getPlayers().containsKey(player.getUniqueId())) {
					if (main.getSpleggQueueManager().getPlayers().containsKey(player.getUniqueId())) {
						main.getSpleggQueueManager().removePlayer(player.getUniqueId());
					}
					if (main.getSpleggQueueManager().getPlayersAliveInGame().containsKey(player.getUniqueId())) {
						main.getSpleggQueueManager().removePlayerFromGame(player.getUniqueId());
					}
					if(main.getSpleggQueueManager().getPlayersRemaining() == 1) {
						main.getSpleggQueueManager().endFFAGame();
					}
					Bukkit.dispatchCommand(
						    Bukkit.getConsoleSender(),
						    "hologram edit &6Splegg setline 4 &fPlayers: &e" + main.getSpleggQueueManager().getPlayerCount()
						);
				}
				
				SpleefQueueManager spleefManager = main.getSpleefQueueManager();
				SpleefFFAGame spleefGame = spleefManager.getPlayerFFA(player.getUniqueId());

				if (spleefGame != null) {

					if (spleefGame.isAlive(player.getUniqueId())) {
						spleefGame.setDead(player.getUniqueId());

						int remaining = spleefGame.getPlayersRemaining();

						if (remaining == 2) {
							spleefGame.setTopThree(player.getUniqueId());
						} else if (remaining == 1) {
							spleefGame.setTopTwo(player.getUniqueId());
						}

						if (spleefGame.isInGame() && !spleefGame.isGameEnding() && remaining <= 1) {
							spleefGame.endGame(); // or endFFAGame() if that is what you named it
						}
					}

					spleefGame.removePlayer(player.getUniqueId());

					Bukkit.dispatchCommand(
							Bukkit.getConsoleSender(),
							"hologram edit &6Spleef setline 4 &fPlayers: &e" + spleefManager.getTotalPlayersInQueue()
					);
				}
				
				if(main.getRankedSpleggManager().getPlayersInQueue().contains(player.getUniqueId())) {
					if(main.getRankedSpleggManager().getPlayersInQueue().contains(player.getUniqueId())) {
						main.getRankedSpleggManager().removePlayerFromQueue(player.getUniqueId());
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hologram edit RankedSplegg setline 4 &fIn Queue: &e"
								+ main.getRankedSpleggManager().getPlayersInQueue().size());
					}
				}
				
				if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF).contains(player.getUniqueId())) {
					main.getRankedSpleefManager().removePlayerFromQueue(player.getUniqueId(), SpleefMode.POWERSPLEEF);
					Bukkit.dispatchCommand(
			                Bukkit.getConsoleSender(),
			                "hologram edit RankedSpleef setline 4 &fIn Queue: &e" + main.getRankedSpleefManager().getTotalPlayersInQueue()
			        );
				}
				if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF).contains(player.getUniqueId())) {
					main.getRankedSpleefManager().removePlayerFromQueue(player.getUniqueId(), SpleefMode.AIRSPLEEF);
					Bukkit.dispatchCommand(
			                Bukkit.getConsoleSender(),
			                "hologram edit RankedSpleef setline 4 &fIn Queue: &e" + main.getRankedSpleefManager().getTotalPlayersInQueue()
			        );
				}
				if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF).contains(player.getUniqueId())) {
					main.getRankedSpleefManager().removePlayerFromQueue(player.getUniqueId(), SpleefMode.MINISPLEEF);
					Bukkit.dispatchCommand(
			                Bukkit.getConsoleSender(),
			                "hologram edit RankedSpleef setline 4 &fIn Queue: &e" + main.getRankedSpleefManager().getTotalPlayersInQueue()
			        );
				}
				
				if(main.getSpleggQueueManager().getPlayersSpectating().contains(player.getUniqueId())) {
					main.getSpleggQueueManager().getPlayersSpectating().remove(player.getUniqueId());
				}
				
				ItemStack stats = new ItemStack(Material.MACE);
				ItemMeta statsMeta = stats.getItemMeta();

				statsMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Your Stats");

				List<String> statsLore = new ArrayList<>();
				statsLore.add(ChatColor.DARK_GRAY + "────────────────────");
				statsLore.add(ChatColor.GRAY + "View your stats");
				statsLore.add(ChatColor.GRAY + "for all gamemodes.");
				statsLore.add("");
				statsLore.add(ChatColor.GREEN + "▶ Click to open stats menu");
				statsLore.add(ChatColor.DARK_GRAY + "────────────────────");

				statsMeta.setLore(statsLore);

				statsMeta.addEnchant(Enchantment.EFFICIENCY, 1, true);
				statsMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

				statsMeta.setUnbreakable(true);

				stats.setItemMeta(statsMeta);
				player.getInventory().setItem(7, stats);

				ItemStack item = new ItemStack(Material.NETHER_STAR);
				ItemMeta meta = item.getItemMeta();

				meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Select Powers");

				List<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_GRAY + "────────────────────");
				lore.add(ChatColor.GRAY + "Choose your abilities");
				lore.add(ChatColor.GRAY + "before the game begins.");
				lore.add("");
				lore.add(ChatColor.GREEN + "▶ Click to open menu");
				lore.add(ChatColor.DARK_GRAY + "────────────────────");

				meta.setLore(lore);

				meta.addEnchant(Enchantment.EFFICIENCY, 1, true);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

				meta.setUnbreakable(true);

				item.setItemMeta(meta);
				player.getInventory().setItem(8, item);

				if (sp != null) {
					SpleefSpectator spectator = sp.getSpectator();
					if (spectator != null) {
						if (spectator.isInGame() || spectator.isInTeamGame()) {
							SpleefGame game = sp.getGame();

							if (game != null) {
								main.getDataLoader().getSpectatorsForGame(game).remove(spectator);
							}

							spectator.setGame(null);
							spectator.setInGame(false);
							spectator.setInTeamGame(false);

							sp.setSpectator(null);
							sp.setGame(null);
						}
					}
				}

				return true;
			}
		}
		return false;
	}
}
