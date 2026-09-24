package io.paper.permafrost.core.game.tasks;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.FlowerPot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.gui.ShovelPowersManager;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;
import io.paper.permafrost.utilities.SpleefMode;

public class PowersListener implements Listener {
	private final Main main;
	private final Set<Projectile> trackedSnowballs = new HashSet<>();
	private final Map<UUID, Long> lastShot = new HashMap<>();
	private Map<UUID, Integer> playerGunTick = new HashMap<>();
	private Map<UUID, Boolean> gunAuto = new HashMap<>();
	private Map<UUID, Boolean> gunHoldAuto = new HashMap<>();
	private Map<UUID, Boolean> isAutoFiring = new HashMap<>();
	private Map<UUID, Boolean> isHoldAutoFiring = new HashMap<>();
	private Map<UUID, BukkitRunnable> timerTasks = new HashMap<>();
	private Map<UUID, BukkitRunnable> holdTimerTasks = new HashMap<>();
	private static final int RAPID_CLICK_THRESHOLD = 3; // spam clicking
	private static final int HOLD_CLICK_THRESHOLD = 6; // classify event as hold
	private static final int HOLD_TIMEOUT = 10; // how long hold task stays alive without new interact
	private static final int FAST_FIRE_INTERVAL = 3; // ~6.67 cps
	private static final int HOLD_FIRE_INTERVAL = 4; // 5 cps
	private static final int HOLD_SHOOT_CUTOFF = 2;
	private final Map<UUID, Integer> lastPowerSwapTick = new HashMap<>();
	private final Map<UUID, ShovelPowers> lastPowerBeforeSwap = new HashMap<>();
	private final Map<UUID, ShovelPowers> lastPowerAfterSwap = new HashMap<>();
	private final Map<UUID, Integer> playerFastShotTick = new HashMap<>();
	private final Map<UUID, Integer> playerHoldShotTick = new HashMap<>();

	public PowersListener(Main main) {
		this.main = main;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player))
			return;

		String title = event.getView().getTitle();

		if (title.equals(ChatColor.AQUA + "Select Training Type")) {
			event.setCancelled(true);

			if (event.getRawSlot() == 13) {
				ItemStack clicked = event.getCurrentItem();
				if (clicked == null || !clicked.hasItemMeta())
					return;

				String plainName = null;
				ItemMeta meta = clicked.getItemMeta();
				if (meta.hasDisplayName()) {
					String displayName = meta.getDisplayName();
					plainName = ChatColor.stripColor(displayName);
					plainName = plainName.replaceAll("[^A-Za-z0-9 ]", "").trim();
				}

				if (plainName != null && !plainName.isEmpty()) {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.15f);
					player.performCommand("training" + plainName.toUpperCase());
				}
			}

		} else if (title.equals("" + ChatColor.BLUE + ChatColor.BOLD + ChatColor.UNDERLINE + "Select Your First Power!")
				|| title.equals(
						"" + ChatColor.BLUE + ChatColor.BOLD + ChatColor.UNDERLINE + "Select Your Second Power!")) {

			event.setCancelled(true);

			// Prevent bottom inventory clicks
			if (event.getRawSlot() >= event.getView().getTopInventory().getSize())
				return;

			ItemStack clicked = event.getCurrentItem();
			if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.65f, 0.8f);
				return;
			}

			String plainName = null;
			ItemMeta meta = clicked.getItemMeta();
			if (meta.hasDisplayName()) {
				String displayName = meta.getDisplayName();
				plainName = ChatColor.stripColor(displayName);
				plainName = plainName.replaceAll("[^A-Za-z0-9 ]", "").trim();
			}

			if (plainName == null || plainName.isEmpty()) {
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.65f, 0.8f);
				return;
			}

			boolean validPower = false;
			switch (plainName.toUpperCase()) {
			case "SPEED":
			case "WINDCHARGE":
			case "BUILDER":
			case "ICEGRENADE":
			case "DOUBLEJUMP":
			case "RECALL":
			case "BLACKGOO":
			case "LEVITATION":
			case "SWAP":
			case "DECAYGRENADE":
			case "TNTRUN":
			case "SILENCER":
			case "SNARE":
			case "SMOKEGRENADE":
			case "VORTEXGRENADE":
			case "ROULETTE":
			case "PURGINGSHIELD":
			case "ENDERPEARL":
			case "JUMPPAD":
				validPower = true;
				break;
			default:
				break;
			}

			if (!validPower) {
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.65f, 0.8f);
				return;
			}

			player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
			player.performCommand("powers set " + plainName.toUpperCase());

			if (main.getDataLoader().getPowerInt(player.getUniqueId()) == 1) {
				player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.35f);
				main.getShovelPowersManager().openPower2Menu(player);
			} else if (main.getDataLoader().getPowerInt(player.getUniqueId()) == 2) {
				String sql = "SELECT power1, power2 FROM player_powers WHERE uuid = ?";
				String power1 = null;
				String power2 = null;

				try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
					ps.setString(1, player.getUniqueId().toString());

					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							power1 = rs.getString("power1");
							power2 = rs.getString("power2");
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}

				player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
				player.sendMessage(ChatColor.GRAY + "You chose the " + power1 + " and " + power2
						+ " powers! Your shovel will now be augmented with them in the games you play.");
				if (ShovelPowers.valueOf(power1) == ShovelPowers.ROULETTE
						&& ShovelPowers.valueOf(power2) == ShovelPowers.ROULETTE) {
					String column = "power2";
					sql = "UPDATE player_powers SET " + column + " = ? WHERE uuid = ?";
					try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
						ps.setString(1, "ROULETTE2");
						ps.setString(2, player.getUniqueId().toString());
						int rows = ps.executeUpdate();
						if (rows == 0) {
							sql = "INSERT INTO player_powers (uuid, " + column + ") VALUES (?, ?)";
							try (PreparedStatement insertPs = main.getConnection().prepareStatement(sql)) {
								insertPs.setString(1, player.getUniqueId().toString());
								insertPs.setString(2, "ROULETTE2");
								insertPs.executeUpdate();
							}
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} else if (ShovelPowers.valueOf(power1) == ShovelPowers.valueOf(power2)) {
					player.sendMessage(ChatColor.RED
							+ "WARNING: You chose two of the same power, but only ROULETTE can be chosen twice. You will only have one working power in game.");
				}
				player.closeInventory();
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerDrop(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
		Material type = event.getItemDrop().getItemStack().getType();
		UUID uuid = player.getUniqueId();
		ShovelPowers[] powers = main.getDataLoader().getCachedPowers(uuid);

		// Add all inventory presets here
		if (type == Material.NETHER_STAR || type == Material.DIAMOND_SHOVEL || type == Material.MACE) {
			event.setCancelled(true);
		}

		if (player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
		}

		if (powers == null || powers[0] == null || powers[1] == null) {
			return;
		}

		ShovelPowers power1 = powers[0];
		ShovelPowers power2 = powers[1];
		if (type == Material.DIAMOND_SHOVEL) {
			if (sp.getInGame() || sp.getInTeamGame() || (main.getSpleefQueueManager().isAliveInAnyFFA(uuid)
					&& main.getSpleefQueueManager().getFFAMode(uuid).equals(SpleefFFAMode.POWERSPLEEF))) {
				if (power1 != null && power2 != null) {
					if (sp.getSelectedPower() == power1) {
						sp.setSelectedPower(power2);
						sp.setDeselectedPower(power1);
					} else if (sp.getSelectedPower() == power2) {
						sp.setSelectedPower(power1);
						sp.setDeselectedPower(power2);
					}
					int tick = Bukkit.getCurrentTick();

					lastPowerSwapTick.put(uuid, tick);
					lastPowerAfterSwap.put(uuid, sp.getSelectedPower());
					lastPowerBeforeSwap.put(uuid, sp.getDeselectedPower());

					player.sendTitle("", ChatColor.GREEN + sp.getSelectedPower().toString(), 5, 10, 5);
					return;
				}
			}
		}
		if ((type == Material.GOLDEN_SHOVEL && !player.getGameMode().equals(GameMode.CREATIVE))
				|| (type == Material.DIAMOND_SHOVEL && !player.getGameMode().equals(GameMode.CREATIVE))) {
			event.setCancelled(true);
		}
		if (sp.isOnCooldown() && sp.getSelectedPower() != sp.getCooldown1()) {
			if (sp.getSelectedPower() != sp.getCooldown2()) {
				sp.getPlayer().setLevel(0);
			}
		}
		if (main.getDataLoader().getTrainingSession(uuid) != null) {
			if (type == Material.DIAMOND_SHOVEL) {
				main.getDataLoader().getTrainingSession(uuid).startTraining();
				main.getDataLoader().setBlocksToBreak(player.getUniqueId(), 0);
				main.getDataLoader().getTrainingSession(uuid).stopTimer(true);
			}
		}
	}

	@EventHandler
	public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getOffHandItem();
		if (item == null) {
			item = event.getMainHandItem();
		}
		if (item == null || item.getType() == null || player == null
				|| main.getDataLoader().getSpleefPlayer(player) == null) {
			return;
		}
		event.setCancelled(true);
		Material mat = item.getType();
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
		UUID uuid = player.getUniqueId();
		if (mat == Material.DIAMOND_SHOVEL) {
			if (sp.getGame() != null || (main.getSpleefQueueManager().isAliveInAnyFFA(uuid)
					&& main.getSpleefQueueManager().getFFAMode(uuid).equals(SpleefFFAMode.POWERSPLEEF))) {
				// If player is in a SPLEEF GAME, dash logic
				if ((sp.getInGame() || sp.getInTeamGame()
						|| (main.getSpleefQueueManager().isAliveInAnyFFA(uuid)
								&& main.getSpleefQueueManager().getFFAMode(uuid).equals(SpleefFFAMode.POWERSPLEEF)))
						&& !sp.isSilenced() && !sp.isRoundStarting()) {
					if (sp.getGame() != null && sp.getGame().getSpleefMode().equals(SpleefMode.POWERSPLEEF)) {
						if (!sp.isOnDashCooldown()) {
							new DashCooldownTask(main, sp, 3.5);
							Location current = player.getLocation();
							Vector direction = current.getDirection().normalize();
							player.setVelocity(direction.multiply(0.9));
							player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.2, 0.2, 0.2, 0);
							player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
						}
					} else if (main.getSpleefQueueManager().isAliveInAnyFFA(uuid)
							&& main.getSpleefQueueManager().getFFAMode(uuid).equals(SpleefFFAMode.POWERSPLEEF)) {
						if (!sp.isOnDashCooldown()) {
							new DashCooldownTask(main, sp, 3.5);
							Location current = player.getLocation();
							Vector direction = current.getDirection().normalize();
							player.setVelocity(direction.multiply(0.9));
							player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.2, 0.2, 0.2, 0);
							player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
						}
					}
				}
			}

		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		Material mat = event.getMaterial();
		ItemStack item = event.getItem();
		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		Location hit = event.getInteractionPoint();
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);

		if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
			return;
		if (mat == null)
			return;

		// Nether star
		if (mat == Material.NETHER_STAR) {
			if (!player.getGameMode().equals(GameMode.CREATIVE)) {
				if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta()
						.getDisplayName().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "Select Powers")) {
					player.performCommand("powers");
				}
			}

			if (main.getDataLoader().getTrainingSession(uuid) != null) {
				// training menu stuff here if needed
			}

			return;
		}
		if (mat == Material.MACE) {
			if (!player.getGameMode().equals(GameMode.CREATIVE)) {
				if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta()
						.getDisplayName().equals(ChatColor.YELLOW + "" + ChatColor.BOLD + "Your Stats")) {
					player.performCommand("stats");
				}
			}
		}
		if (block != null)
			if (block.getType() == Material.GRASS_BLOCK) {
				if (mat == Material.WOODEN_SHOVEL || mat == Material.STONE_SHOVEL || mat == Material.IRON_SHOVEL
						|| mat == Material.GOLDEN_SHOVEL || mat == Material.DIAMOND_SHOVEL
						|| mat == Material.NETHERITE_SHOVEL) {
					event.setCancelled(true);
				}
			}

		// Training start item
		if (mat == Material.GREEN_CONCRETE) {
			if (main.getDataLoader().getTrainingSession(uuid) != null) {
				main.getDataLoader().getTrainingSession(uuid).startTraining();
				main.getDataLoader().setBlocksToBreak(player.getUniqueId(), 0);
			}
			return;
		}

		if (block != null) {
			if (!player.getGameMode().equals(GameMode.CREATIVE)) {
				if (block.getState() instanceof Sign sign) {
					event.setCancelled(true);
				}
				if (block.getState() instanceof Boat boat) {
					event.setCancelled(true);
				}
				if (isPlant(block)) {
					event.setCancelled(true);
				}
				if (block.getType() == Material.FARMLAND) {
					event.setCancelled(true);
				}
				if (block.getState() instanceof Painting painting) {
					event.setCancelled(true);
				}
				if (block.getState() instanceof ItemFrame itemFrame) {
					event.setCancelled(true);
				}
				if (block.getState() instanceof TrapDoor trapDoor) {
					event.setCancelled(true);
				}
			}
		}

		// Slime ball
		if (mat == Material.SLIME_BALL) {
			if (main.getSpleggQueueManager().getPlayers().containsKey(uuid)) {
				player.performCommand("spawn");
			} else if (main.getRankedSpleggManager().getPlayersInQueue().contains(uuid)) {
				player.performCommand("spawn");
			} else if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF)
					.contains(player.getUniqueId())) {
				player.performCommand("spawn");
			} else if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF)
					.contains(player.getUniqueId())) {
				player.performCommand("spawn");
			} else if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF)
					.contains(player.getUniqueId())) {
				player.performCommand("spawn");
			} else if (main.getSpleefQueueManager().getInFFA(uuid)) {
				player.performCommand("spawn");
			}
			return;
		}

		// Golden shovel

		if (mat == Material.GOLDEN_SHOVEL) {
			if (main.getSpleggQueueManager().getPlayersAliveInGame().containsKey(uuid)) {
				int currentTick = Bukkit.getServer().getCurrentTick();
				int lastInteractTick = playerGunTick.getOrDefault(uuid, -1000);
				int delta = currentTick - lastInteractTick;

				playerGunTick.put(uuid, currentTick);

				if (main.getSpleggQueueManager().isGameEnding())
					return;
				if (!main.getSpleggQueueManager().isDead(uuid))
					return;

				// spam clicking -> fast auto fire (~6.67 cps cap)
				if (delta <= RAPID_CLICK_THRESHOLD) {
					gunAuto.put(uuid, true);
					gunHoldAuto.put(uuid, false);

					stopHoldAutoFire(uuid);

					if (!isAutoFiring.getOrDefault(uuid, false)) {
						startAutoFire(uuid);
					}
				}

				// holding / slower repeated interact -> hold auto fire (5 cps)
				else if (delta <= HOLD_CLICK_THRESHOLD) {
					gunAuto.put(uuid, false);
					gunHoldAuto.put(uuid, true);

					stopAutoFire(uuid);

					if (!isHoldAutoFiring.getOrDefault(uuid, false)) {
						startHoldAutoFire(uuid);
					}
				}

				// first click / slow click -> single shot only
				else {
					gunAuto.put(uuid, false);
					gunHoldAuto.put(uuid, false);

					stopAutoFire(uuid);
					stopHoldAutoFire(uuid);

					shootEgg(player);
					playerFastShotTick.put(uuid, currentTick);
					playerHoldShotTick.put(uuid, currentTick);
				}
			} else if (sp != null) {
				if (sp.getInTeamGame()) {
					if (!sp.isSpectating() && !sp.isRoundStarting()) {
						if (sp.getGame().isSplegg()) {
							// if (!sp.getGame().isGameEnding() && !sp.getGame().isRoundEnding()) {
							int currentTick = Bukkit.getServer().getCurrentTick();
							int lastInteractTick = playerGunTick.getOrDefault(uuid, -1000);
							int delta = currentTick - lastInteractTick;

							playerGunTick.put(uuid, currentTick);

							// spam clicking -> fast auto fire (~6.67 cps cap)
							if (delta <= RAPID_CLICK_THRESHOLD) {
								gunAuto.put(uuid, true);
								gunHoldAuto.put(uuid, false);

								stopHoldAutoFire(uuid);

								if (!isAutoFiring.getOrDefault(uuid, false)) {
									startAutoFire(uuid);
								}
							}

							// holding / slower repeated interact -> hold auto fire (5 cps)
							else if (delta <= HOLD_CLICK_THRESHOLD) {
								gunAuto.put(uuid, false);
								gunHoldAuto.put(uuid, true);

								stopAutoFire(uuid);

								if (!isHoldAutoFiring.getOrDefault(uuid, false)) {
									startHoldAutoFire(uuid);
								}
							}

							// first click / slow click -> single shot only
							else {
								gunAuto.put(uuid, false);
								gunHoldAuto.put(uuid, false);

								stopAutoFire(uuid);
								stopHoldAutoFire(uuid);

								shootEgg(player);
								playerFastShotTick.put(uuid, currentTick);
								playerHoldShotTick.put(uuid, currentTick);
							}

							// }
						}
					}
				}
			}
			return;
		}

		if (sp.getGame() != null || (main.getSpleefQueueManager().isAliveInAnyFFA(uuid)
				&& main.getSpleefQueueManager().getFFAMode(uuid).equals(SpleefFFAMode.POWERSPLEEF))) {
			if (sp.getGame() != null && sp.getGame().isSplegg()) {
				if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
					return;

				// Egg Dynamics
			} else {
				// If player is in a SPLEEF GAME, power logic
				String[] powers = getPlayerPowers(uuid);
				String power1 = powers[0];
				String power2 = powers[1];
				if (mat == Material.DIAMOND_SHOVEL) {
					int tick = Bukkit.getCurrentTick();
					int lastSwapTick = lastPowerSwapTick.getOrDefault(uuid, -999999);
					int ticksSinceSwap = tick - lastSwapTick;

				}
				if (main.getDataLoader().getRecallActive(uuid) && mat == Material.DIAMOND_SHOVEL
						&& !sp.isRoundStarting()
						&& (sp.getInGame() || sp.getInTeamGame() || (main.getSpleefQueueManager().isAliveInAnyFFA(uuid)
								&& main.getSpleefQueueManager().getFFAMode(uuid).equals(SpleefFFAMode.POWERSPLEEF)))
						&& !sp.isSilenced()) {

					if (sp.getSelectedPower().equals(ShovelPowers.RECALL)
							|| sp.getSelectedPower().equals(ShovelPowers.ROULETTE)
							|| sp.getSelectedPower().equals(ShovelPowers.ROULETTE2)) {

						main.getDataLoader().setRecallActive(uuid, false);
					} else {

					}
				} else if (sp.isSilenced() && (sp.getSelectedPower().equals(ShovelPowers.RECALL)
						|| (sp.getSelectedPower().equals(ShovelPowers.ROULETTE)
								&& sp.getRandomPower().equals(ShovelPowers.RECALL))
						|| (sp.getSelectedPower().equals(ShovelPowers.ROULETTE2)
								&& sp.getRandomPower().equals(ShovelPowers.RECALL)))) {
					sp.getPlayer().sendMessage(ChatColor.GRAY + "You cannot recall while silenced!");
				}
				if (mat == Material.DIAMOND_SHOVEL
						&& (sp.getInGame() || sp.getInTeamGame()
								|| (main.getSpleefQueueManager().isAliveInAnyFFA(uuid) && main.getSpleefQueueManager()
										.getFFAMode(uuid).equals(SpleefFFAMode.POWERSPLEEF)))
						&& !sp.isSilenced() && !sp.isRoundStarting() && power1 != null && power2 != null) {
					if (sp.getGame() != null) {
						if (sp.getGame().getBan1() != null && sp.getGame().getBan2() != null) {
							if (sp.getSelectedPower().equals(sp.getGame().getBan1())
									|| sp.getSelectedPower().equals(sp.getGame().getBan2())) {
								sp.getPlayer().sendMessage(ChatColor.RED + "You cannot use a banned power!");
								return;
							}
						}
					}
					if (!sp.isOnCooldown()) {
						switch (sp.getSelectedPower()) {
						case ShovelPowers.NONE:
							break;
						case ShovelPowers.SPEED: {
							useSpeed(player, sp, false, false);
							break;
						}
						case ShovelPowers.WINDCHARGE: {
							useWindCharge(player, sp, false, false);
							break;
						}
						case ShovelPowers.BLACKGOO: {
							useBlackGoo(player, sp, false, false);
							break;
						}
						case ShovelPowers.LEVITATION: {
							useLevitation(player, sp, false, false);
							break;
						}
						case ShovelPowers.ICEGRENADE: {
							useIceGrenade(player, sp, false, false);
							break;
						}
						case ShovelPowers.BUILDER: {
							useBuilder(player, uuid, sp, false, false);
							break;
						}
						case ShovelPowers.TNTRUN: {
							useTntRun(player, sp, false, false);
							break;
						}
						case ShovelPowers.DECAYGRENADE: {
							useDecayGrenade(player, sp, false, false);
							break;
						}
						case ShovelPowers.SWAP: {
							useSwap(player, sp, false, false);
							break;
						}
						case ShovelPowers.SILENCER: {
							useSilencer(player, sp, false, false);
							break;
						}
						case ShovelPowers.DOUBLEJUMP: {
							useDoubleJump(player, sp, false, false);
							break;
						}
						case ShovelPowers.SNARE: {
							useSnare(player, sp, hit, false, false);
							break;
						}
						case ShovelPowers.RECALL: {
							useRecall(player, sp, false, false);
							break;
						}
						case ShovelPowers.SMOKEGRENADE: {
							useSmokeGrenade(player, sp, false, false);
							break;
						}
						case ShovelPowers.ROULETTE: {
							useRoulette(player, sp, hit);
							break;
						}
						case ShovelPowers.ROULETTE2: {
							useRoulette2(player, sp, hit);
							break;
						}
						case ShovelPowers.VORTEXGRENADE: {
							useVortexGrenade(player, sp, false, false);
							break;
						}
						case ShovelPowers.PURGINGSHIELD: {
							usePurgingShield(player, sp, false, false);
							break;
						}
						case ShovelPowers.ENDERPEARL: {
							useEnderPearl(player, sp, false, false);
							break;
						}
						}

					} else if (sp.isOnCooldown() && sp.getSelectedPower() != sp.getCooldown1()) {
						if (sp.getSelectedPower() != sp.getCooldown2()) {
							switch (sp.getSelectedPower()) {
							case ShovelPowers.NONE:
								break;
							case ShovelPowers.SPEED: {
								useSpeed(player, sp, false, false);
								break;
							}
							case ShovelPowers.WINDCHARGE: {
								useWindCharge(player, sp, false, false);
								break;
							}
							case ShovelPowers.BLACKGOO: {
								useBlackGoo(player, sp, false, false);
								break;
							}
							case ShovelPowers.LEVITATION: {
								useLevitation(player, sp, false, false);
								break;
							}
							case ShovelPowers.ICEGRENADE: {
								useIceGrenade(player, sp, false, false);
								break;
							}
							case ShovelPowers.BUILDER: {
								useBuilder(player, uuid, sp, false, false);
								break;
							}
							case ShovelPowers.TNTRUN: {
								useTntRun(player, sp, false, false);
								break;
							}
							case ShovelPowers.DECAYGRENADE: {
								useDecayGrenade(player, sp, false, false);
								break;
							}
							case ShovelPowers.SWAP: {
								useSwap(player, sp, false, false);
								break;
							}
							case ShovelPowers.SILENCER: {
								useSilencer(player, sp, false, false);
								break;
							}
							case ShovelPowers.DOUBLEJUMP: {
								useDoubleJump(player, sp, false, false);
								break;
							}
							case ShovelPowers.SNARE: {
								useSnare(player, sp, hit, false, false);
								break;
							}
							case ShovelPowers.RECALL: {
								useRecall(player, sp, false, false);
								break;
							}
							case ShovelPowers.SMOKEGRENADE: {
								useSmokeGrenade(player, sp, false, false);
								break;
							}
							case ShovelPowers.ROULETTE: {
								useRoulette(player, sp, hit);
								break;
							}
							case ShovelPowers.ROULETTE2: {
								useRoulette2(player, sp, hit);
								break;
							}
							case ShovelPowers.VORTEXGRENADE: {
								useVortexGrenade(player, sp, false, false);
								break;
							}
							case ShovelPowers.PURGINGSHIELD: {
								usePurgingShield(player, sp, false, false);
								break;
							}
							case ShovelPowers.ENDERPEARL: {
								useEnderPearl(player, sp, false, false);
								break;
							}
							}
						} else {
							sp.getPlayer().sendMessage("§cYou are on cooldown for " + sp.getTimeLeft() + " seconds.");
						}
					}
				} else if (sp.isSilenced()) {
					sp.getPlayer().sendMessage("§cYou are silenced!");
				} else if (sp.isRoundStarting()) {
					sp.getPlayer().sendMessage("§cYou cannot use your abilities during round start.");
				}
			}
		}
	}

	// POWER METHODS

	public void useSpeed(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		new CooldownTask(main, sp, 8, ShovelPowers.SPEED, roulette, roulette2);
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 1));
		Attribute reachAttr = resolveBlockReachAttr();
		AttributeInstance inst = player.getAttribute(reachAttr);
		if (inst != null) {
			inst.setBaseValue(5.5);

			new BukkitRunnable() {
				@Override
				public void run() {
					inst.setBaseValue(4.5);
				}
			}.runTaskLater(main, 100L);
		}
	}

	public void useWindCharge(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		int windCount = 0;
		if (sp.getGame() != null) {
			for (UUID uuid : sp.getTeam().getPlayers()) {
				Player p = Bukkit.getPlayer(uuid);
				SpleefPlayer sp2 = main.getDataLoader().getSpleefPlayer(p);
				if (sp2.getSelectedPower().equals(ShovelPowers.WINDCHARGE)
						|| sp2.getDeselectedPower().equals(ShovelPowers.WINDCHARGE)) {
					if (!sp2.isDead())
						windCount++;
				} else if (sp2.getSelectedPower().equals(ShovelPowers.ROULETTE)
						|| sp2.getDeselectedPower().equals(ShovelPowers.ROULETTE)) {
					if (sp2.getRandomPower().equals(ShovelPowers.WINDCHARGE)) {
						if (!sp2.isDead())
							windCount++;
					}
				} else if (sp2.getSelectedPower().equals(ShovelPowers.ROULETTE2)
						|| sp2.getDeselectedPower().equals(ShovelPowers.ROULETTE2)) {
					if (sp2.getRandomPower2().equals(ShovelPowers.WINDCHARGE)) {
						if (!sp2.isDead())
							windCount++;
					}
				}
			}
		}
		if (windCount >= 2) {
			new CooldownTask(main, sp, 4, ShovelPowers.WINDCHARGE, roulette, roulette2);
		} else {
			new CooldownTask(main, sp, 2.5, ShovelPowers.WINDCHARGE, roulette, roulette2);
		}
		WindCharge windCharge = player.launchProjectile(WindCharge.class,
				player.getLocation().getDirection().multiply(2.0));
		player.getWorld().playSound(player.getLocation(), "entity.wind_charge.shoot", 1.0f, 1.0f);
	}

	public void useBlackGoo(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		if (main.getAmmoManger().getAmmo(sp, ShovelPowers.BLACKGOO) == 0) {
			sp.getPlayer().sendMessage("§c You are out of ammo!");
		} else {
			World world = player.getWorld();
			Location eyeLoc = player.getEyeLocation();
			Vector direction = eyeLoc.getDirection().normalize();
			double maxDistance = 20;
			double step = 0.5;
			boolean playerFound = false;
			Player pf = null;
			for (double i = 0; i <= maxDistance; i += step) {
				Location particleLoc = eyeLoc.clone().add(direction.clone().multiply(i));
				world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
						new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f));
				for (Player target : Bukkit.getOnlinePlayers()) {
					if (target.equals(player))
						continue;
					if (!target.getWorld().equals(world))
						continue;
					if (target.getLocation().distance(particleLoc) <= 2.0
							|| target.getEyeLocation().distance(particleLoc) <= 2.0) {
						pf = target;
						playerFound = true;
					}
				}
			}
			if (sp.getGame() != null) {
				if (playerFound == true
						&& !main.getDataLoader().getSpectatorsForGame(sp.getGame())
								.contains(main.getDataLoader().getSpleefPlayer(pf).getSpectator())
						&& !main.getDataLoader().getSpleefPlayer(pf).isDead()) {
					if (isPlayerInShield(pf)) {
						player.sendMessage("§aThis player is protected by their purging shield!");
						return;
					}
					new AmmoRefillTask(main, sp, 10, ShovelPowers.BLACKGOO);
					main.getAmmoManger().consume(ShovelPowers.BLACKGOO, sp);
					main.getAmmoManger().showAmmo(player, ShovelPowers.BLACKGOO,
							main.getAmmoManger().getAmmo(sp, ShovelPowers.BLACKGOO));
					pf.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
					pf.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 2));
					player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
				} else {
					player.sendMessage("§cYou missed!");
				}
			} else if ((main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId()) && main
					.getSpleefQueueManager().getFFAMode(player.getUniqueId()).equals(SpleefFFAMode.POWERSPLEEF))) {
				if (playerFound == true
						&& !main.getSpleefQueueManager().getAllSpectators().contains(pf.getUniqueId())) {
					if (isPlayerInShield(pf)) {
						player.sendMessage("§aThis player is protected by their purging shield!");
						return;
					}
					new AmmoRefillTask(main, sp, 10, ShovelPowers.BLACKGOO);
					main.getAmmoManger().consume(ShovelPowers.BLACKGOO, sp);
					main.getAmmoManger().showAmmo(player, ShovelPowers.BLACKGOO,
							main.getAmmoManger().getAmmo(sp, ShovelPowers.BLACKGOO));
					pf.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
					pf.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 2));
					player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
				} else {
					player.sendMessage("§cYou missed!");
				}
			}
		}
	}

	public void useLevitation(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		if (main.getAmmoManger().getAmmo(sp, ShovelPowers.LEVITATION) == 0) {
			sp.getPlayer().sendMessage("§cYou are out of ammo!");
		} else {
			World world = player.getWorld();
			Location eyeLoc = player.getEyeLocation();
			Vector direction = eyeLoc.getDirection().normalize();
			double maxDistance = 20;
			double step = 0.5;
			boolean playerFound = false;
			Player pf = null;
			for (double i = 0; i <= maxDistance; i += step) {
				Location particleLoc = eyeLoc.clone().add(direction.clone().multiply(i));
				world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
						new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f));
				for (Player target : Bukkit.getOnlinePlayers()) {
					if (target.equals(player))
						continue;
					if (!target.getWorld().equals(world))
						continue;
					if (target.getLocation().distance(particleLoc) <= 2.0
							|| target.getEyeLocation().distance(particleLoc) <= 2.0) {
						pf = target;
						playerFound = true;
					}
				}
			}
			if (sp.getGame() != null) {
				if (playerFound == true
						&& !main.getDataLoader().getSpectatorsForGame(sp.getGame())
								.contains(main.getDataLoader().getSpleefPlayer(pf).getSpectator())
						&& !main.getDataLoader().getSpleefPlayer(pf).isDead()) {
					if (isPlayerInShield(pf)) {
						player.sendMessage("§aThis player is protected by their purging shield!");
						return;
					}
					new AmmoRefillTask(main, sp, 10, ShovelPowers.LEVITATION);
					main.getAmmoManger().consume(ShovelPowers.LEVITATION, sp);
					main.getAmmoManger().showAmmo(player, ShovelPowers.LEVITATION,
							main.getAmmoManger().getAmmo(sp, ShovelPowers.LEVITATION));
					pf.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 25, 1));
					player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
				} else {
					player.sendMessage("§cYou missed!");
				}
			} else if ((main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId()) && main
					.getSpleefQueueManager().getFFAMode(player.getUniqueId()).equals(SpleefFFAMode.POWERSPLEEF))) {
				if (playerFound == true
						&& !main.getSpleefQueueManager().getAllSpectators().contains(pf.getUniqueId())) {
					if (isPlayerInShield(pf)) {
						player.sendMessage("§aThis player is protected by their purging shield!");
						return;
					}
					new AmmoRefillTask(main, sp, 10, ShovelPowers.LEVITATION);
					main.getAmmoManger().consume(ShovelPowers.LEVITATION, sp);
					main.getAmmoManger().showAmmo(player, ShovelPowers.LEVITATION,
							main.getAmmoManger().getAmmo(sp, ShovelPowers.LEVITATION));
					pf.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 25, 1));
					player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
				} else {
					player.sendMessage("§cYou missed!");
				}
			}
		}
	}

	public void useIceGrenade(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		new CooldownTask(main, sp, 5.5, ShovelPowers.ICEGRENADE, roulette, roulette2);
		NamespacedKey key = new NamespacedKey(main, "snowball_power");
		Snowball snowball = player.launchProjectile(Snowball.class, player.getLocation().getDirection().multiply(2.0));
		snowball.getPersistentDataContainer().set(key, PersistentDataType.STRING, "ICEGRENADE");
		new RemoveIceTask(main, sp, 5);
	}

	public void useBuilder(Player player, UUID uuid, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		Block target = player.getTargetBlockExact(5);
		if (target == null) {
			return;
		}
		if (sp.getGame() != null) {
			if (target.getY() >= (sp.getGame().getArena().getCorner1().getBlockY() + 7)) {
				player.sendMessage("§cYou cannot build above 7 blocks from the arena.");
				return;
			}
			int count = 0;
			for (TeamSpleefTeam team : sp.getGame().getTeamSpleefTeams()) {
				for (UUID uuid2 : team.getPlayers()) {
					count++;
				}
			}
			if (count > 4) {
				Location corner1 = sp.getGame().getArena().getCorner1().clone();
				Location corner2 = sp.getGame().getArena().getCorner2().clone();
				corner1.setX(corner1.getX() - Math.floor((count - 4) / 2));
				corner1.setZ(corner1.getZ() + Math.floor((count - 4) / 2));
				corner2.setX(corner2.getX() + Math.floor((count - 4) / 2));
				corner2.setZ(corner2.getZ() - Math.floor((count - 4) / 2));
				int x = target.getX();
				int y = target.getY();
				int z = target.getZ();

				int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
				int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());

				int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
				int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());

				int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
				int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

				if (!(x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ)) {
					player.sendMessage("§cYou cannot build outside of the arena.");
					return;
				}
			} else {
				Location corner1 = sp.getGame().getArena().getCorner1().clone();
				Location corner2 = sp.getGame().getArena().getCorner2().clone();
				int x = target.getX();
				int y = target.getY();
				int z = target.getZ();

				int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
				int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());

				int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
				int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());

				int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
				int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

				if (!(x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ)) {
					player.sendMessage("§cYou cannot build outside of the arena.");
					return;
				}
			}

		}
		if (main.getSpleefQueueManager().isAliveInAnyFFA(uuid))
			if (target.getY() >= (main.getSpleefQueueManager().getGame(SpleefFFAMode.POWERSPLEEF).getArena()
					.getCorner1().getBlockY() + 7)) {
				player.sendMessage("§cYou cannot build above 7 blocks from the arena.");
				return;
			}
		if (target != null) {
			if (target.getType() == Material.SNOW_BLOCK || target.getType() == Material.ORANGE_CONCRETE_POWDER
					|| target.getType() == Material.RED_CONCRETE_POWDER || target.getType() == Material.BLUE_ICE
					|| target.getType() == Material.ICE || target.getType() == Material.PACKED_ICE) {
				new CooldownTask(main, sp, 4, ShovelPowers.BUILDER, roulette, roulette2);
			} else {
				player.sendMessage("§cInvalid placement.");
			}
			if (target != null) {
				float yaw = player.getLocation().getYaw();
				yaw = (yaw % 360 + 360) % 360;
				BlockFace face;
				if (yaw >= 45 && yaw < 135) {
					face = BlockFace.SOUTH;
				} else if (yaw >= 135 && yaw < 225) {
					face = BlockFace.WEST;
				} else if (yaw >= 225 && yaw < 315) {
					face = BlockFace.NORTH;
				} else {
					face = BlockFace.EAST;
				}

				if (face == BlockFace.EAST || face == BlockFace.WEST) {
					for (int x = -1; x <= 1; x++) {
						for (int y = 1; y <= 3; y++) {
							Location loc = new Location(target.getWorld(), target.getX() + x, target.getY() + y,
									target.getZ());
							Block b = loc.getBlock();
							b.setType(Material.SNOW_BLOCK);
							main.getDataLoader().addSnowSpawned(uuid, b.getLocation());
						}
					}
				} else {
					for (int z = -1; z <= 1; z++) {
						for (int y = 1; y <= 3; y++) {
							Location loc = new Location(target.getWorld(), target.getX(), target.getY() + y,
									target.getZ() + z);
							Block b = loc.getBlock();
							b.setType(Material.SNOW_BLOCK);
							main.getDataLoader().addSnowSpawned(uuid, b.getLocation());
						}
					}
				}
			}
		}
	}

	public void useTntRun(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		World world = player.getWorld();
		Location eyeLoc = player.getEyeLocation();
		Vector direction = eyeLoc.getDirection().normalize();
		double maxDistance = 20;
		double step = 0.5;
		boolean playerFound = false;
		Player pf = null;
		for (double i = 0; i <= maxDistance; i += step) {
			Location particleLoc = eyeLoc.clone().add(direction.clone().multiply(i));
			world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
					new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f));
			for (Player target : Bukkit.getOnlinePlayers()) {
				if (target.equals(player))
					continue;
				if (!target.getWorld().equals(world))
					continue;
				if (target.getLocation().distance(particleLoc) <= 2.0
						|| target.getEyeLocation().distance(particleLoc) <= 2.0) {
					pf = target;
					playerFound = true;
				}
			}
		}

		if (pf != null) {
			if (isPlayerInShield(pf)) {
				player.sendMessage("§aThis player is protected by their purging shield!");
				return;
			}
			if (main.getDataLoader().getPlayersAffectedByTntRun().contains(pf.getUniqueId())) {
				player.sendMessage("§cThis player is already affected by TNT run.");
				return;
			} else {
				if (sp.getGame() != null) {
					if (playerFound == true
							&& !main.getDataLoader().getSpectatorsForGame(sp.getGame())
									.contains(main.getDataLoader().getSpleefPlayer(pf).getSpectator())
							&& !main.getDataLoader().getSpleefPlayer(pf).isDead()) {
						if (isPlayerInShield(pf)) {
							player.sendMessage("§aThis player is protected by their purging shield!");
							return;
						}
						new CooldownTask(main, sp, 10, ShovelPowers.TNTRUN, roulette, roulette2);
						applyTntRunDebuff(pf, 5.2);
						main.getDataLoader().addPlayerAffectedByTntRun(pf.getUniqueId());
						player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
					} else {
						player.sendMessage("§cYou missed!");
					}
				} else if ((main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId()) && main
						.getSpleefQueueManager().getFFAMode(player.getUniqueId()).equals(SpleefFFAMode.POWERSPLEEF))) {
					if (playerFound == true
							&& !main.getSpleefQueueManager().getAllSpectators().contains(pf.getUniqueId())) {
						if (isPlayerInShield(pf)) {
							player.sendMessage("§aThis player is protected by their purging shield!");
							return;
						}
						new CooldownTask(main, sp, 10, ShovelPowers.TNTRUN, roulette, roulette2);
						applyTntRunDebuff(pf, 5.2);
						main.getDataLoader().addPlayerAffectedByTntRun(pf.getUniqueId());
						player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
					} else {
						player.sendMessage("§cYou missed!");
					}
				}
			}
		}
	}

	public void useDecayGrenade(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		new CooldownTask(main, sp, 5.5, ShovelPowers.DECAYGRENADE, roulette, roulette2);
		NamespacedKey key = new NamespacedKey(main, "snowball_power");
		Snowball snowball = player.launchProjectile(Snowball.class, player.getLocation().getDirection().multiply(2.0));
		snowball.getPersistentDataContainer().set(key, PersistentDataType.STRING, "DECAYGRENADE");
		new RemoveDecayTask(main, sp, 4);
	}

	public void useSwap(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		World world = player.getWorld();
		Location eyeLoc = player.getEyeLocation();
		Vector direction = eyeLoc.getDirection().normalize();
		double maxDistance = 20;
		double step = 0.5;
		boolean playerFound = false;
		Player pf = null;

		for (double i = 0; i <= maxDistance; i += step) {
			Location particleLoc = eyeLoc.clone().add(direction.clone().multiply(i));
			world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
					new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.0f));

			for (Player target : Bukkit.getOnlinePlayers()) {
				if (target.equals(player))
					continue;

				if (!target.getWorld().equals(world))
					continue;

				if (target.getLocation().distance(particleLoc) <= 2.0
						|| target.getEyeLocation().distance(particleLoc) <= 2.0) {
					pf = target;
					playerFound = true;
				}
			}
		}

		if (sp.getGame() != null) {
			if (playerFound == true
					&& !main.getDataLoader().getSpectatorsForGame(sp.getGame())
							.contains(main.getDataLoader().getSpleefPlayer(pf).getSpectator())
					&& !main.getDataLoader().getSpleefPlayer(pf).isDead()) {

				if (isPlayerInShield(pf)) {
					player.sendMessage("§aThis player is protected by their purging shield!");
					return;
				}

				SpleefPlayer targetted = main.getDataLoader().getSpleefPlayer(pf);

				if (targetted.getGame() != null && targetted.getSpectator() != null
						&& Main.getInstance().getDataLoader().getSpectatorsForGame(targetted.getGame()) != null
						&& Main.getInstance().getDataLoader().getSpectatorsForGame(targetted.getGame())
								.contains(targetted.getSpectator())) {
					return;
				}

				new CooldownTask(main, sp, 10, ShovelPowers.SWAP, roulette, roulette2);

				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
				pf.sendMessage("§4You are being swapped with...");
				player.sendMessage("§4You are swapping with " + pf.getName() + ", switch powers to cancel.");

				final ShovelPowers requiredPower = roulette ? ShovelPowers.ROULETTE
						: roulette2 ? ShovelPowers.ROULETTE2 : ShovelPowers.SWAP;

				new BukkitRunnable() {
					int ticks = 0;

					@Override
					public void run() {
						ticks++;

						if (sp.isRoundStarting()) {
							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
							return;
						}

						if (targetted.isDead()) {
							player.sendMessage("§cSwap cancelled because " + targetted.getName() + " died.");
							targetted.getPlayer().sendMessage("§cSwap cancelled because you died.");
							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
							return;
						}

						if (sp.isDead()) {
							player.sendMessage("§cSwap cancelled because you died.");
							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
							return;
						}

						if (targetted.getGame() != null && targetted.getSpectator() != null
								&& Main.getInstance().getDataLoader().getSpectatorsForGame(targetted.getGame()) != null
								&& Main.getInstance().getDataLoader().getSpectatorsForGame(targetted.getGame())
										.contains(targetted.getSpectator())) {
							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
							return;
						}

						if (sp.getSelectedPower() != requiredPower) {
							player.sendMessage("§cYou have swapped powers, canceling swap channeling.");
							targetted.getPlayer()
									.sendMessage("§4" + player.getName() + " has cancelled their swap on you.");
							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
							return;
						}

						if (main.getDataLoader().getSpectatorsForGame(sp.getGame()).contains(sp.getSpectator())) {
							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
							return;
						}

						if (ticks <= 60) {
							Location start = player.getEyeLocation();
							Location end = targetted.getPlayer().getEyeLocation();

							Vector dir = end.toVector().subtract(start.toVector()).normalize();
							double distance = start.distance(end);

							for (double i = 0; i < distance; i += 0.2) {
								Location point = start.clone().add(dir.clone().multiply(i));
								player.getWorld().spawnParticle(Particle.ASH, point, 1, 0, 0, 0, 0);
							}

							return;
						} else if (ticks > 60) {
							if (targetted.isDead() || sp.isDead()) {
								player.removePotionEffect(PotionEffectType.SLOWNESS);
								cancel();
								return;
							}

							Location playerLoc = player.getLocation().clone();

							if (((playerLoc.getY() - 1) > sp.getGame().getArena().getCorner1().getY())
									|| ((playerLoc.getY() - 1) < sp.getGame().getArena().getCorner1().getY())) {
								player.sendMessage("§cYou cannot swap in the air.");
								player.removePotionEffect(PotionEffectType.SLOWNESS);
								cancel();
								return;
							}

							Location targetLoc = targetted.getPlayer().getLocation().clone();
							if (!isPlayerInShield(targetted.getPlayer())) {
								targetted.getPlayer().teleport(playerLoc);
								targetted.getPlayer()
										.sendMessage("§4" + player.getName() + " swapped locations with you.");
							}
							player.teleport(targetLoc);

							player.sendMessage("§4You swapped with " + targetted.getName() + ".");

							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
						}
					}
				}.runTaskTimer(main, 0L, 1L);

				player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
			} else {
				player.sendMessage("§cYou missed!");
			}
		} else if ((main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId())
				&& main.getSpleefQueueManager().getFFAMode(player.getUniqueId()).equals(SpleefFFAMode.POWERSPLEEF))) {

			if (playerFound == true && !main.getSpleefQueueManager().getAllSpectators().contains(player.getUniqueId())
					&& !main.getSpleefQueueManager().getAllSpectators().contains(pf.getUniqueId())) {

				if (isPlayerInShield(pf)) {
					player.sendMessage("§aThis player is protected by their purging shield!");
					return;
				}

				SpleefPlayer targetted = main.getDataLoader().getSpleefPlayer(pf);

				if (main.getSpleefQueueManager().getAllSpectators().contains(targetted.getPlayer().getUniqueId())) {
					return;
				}

				new CooldownTask(main, sp, 10, ShovelPowers.SWAP, roulette, roulette2);

				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
				pf.sendMessage("§4You are being swapped with...");
				player.sendMessage("§4You are swapping with " + pf.getName() + ", switch powers to cancel.");

				final ShovelPowers requiredPower = roulette ? ShovelPowers.ROULETTE
						: roulette2 ? ShovelPowers.ROULETTE2 : ShovelPowers.SWAP;

				new BukkitRunnable() {
					int ticks = 0;

					@Override
					public void run() {
						ticks++;

						if (sp.isRoundStarting()) {
							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
							return;
						}

						if (main.getSpleefQueueManager().getAllSpectators()
								.contains(targetted.getPlayer().getUniqueId())
								|| main.getSpleefQueueManager().getAllSpectators().contains(player.getUniqueId())) {
							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
							return;
						}

						if (sp.getSelectedPower() != requiredPower) {
							player.sendMessage("§cYou have swapped powers, canceling swap channeling.");
							targetted.getPlayer()
									.sendMessage("§4" + player.getName() + " has cancelled their swap on you.");
							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
							return;
						}

						if (ticks <= 60) {
							Location start = player.getEyeLocation();
							Location end = targetted.getPlayer().getEyeLocation();

							Vector dir = end.toVector().subtract(start.toVector()).normalize();
							double distance = start.distance(end);

							for (double i = 0; i < distance; i += 0.2) {
								Location point = start.clone().add(dir.clone().multiply(i));
								player.getWorld().spawnParticle(Particle.ASH, point, 1, 0, 0, 0, 0);
							}

							return;
						} else if (ticks > 60) {
							if (main.getSpleefQueueManager().getAllSpectators()
									.contains(targetted.getPlayer().getUniqueId())
									|| main.getSpleefQueueManager().getAllSpectators().contains(player.getUniqueId())) {
								player.removePotionEffect(PotionEffectType.SLOWNESS);
								cancel();
								return;
							}

							Location playerLoc = player.getLocation().clone();
							Location targetLoc = targetted.getPlayer().getLocation().clone();

							if (((playerLoc.getY() - 1) > main.getSpleefQueueManager()
									.getGame(SpleefFFAMode.POWERSPLEEF).getArena().getCorner1().getY())
									|| ((playerLoc.getY() - 1) < main.getSpleefQueueManager()
											.getGame(SpleefFFAMode.POWERSPLEEF).getArena().getCorner1().getY())) {
								player.sendMessage("§cYou cannot swap in the air.");
								player.removePotionEffect(PotionEffectType.SLOWNESS);
								cancel();
								return;
							}

							if (!isPlayerInShield(targetted.getPlayer())) {
								targetted.getPlayer().teleport(playerLoc);
								targetted.getPlayer()
										.sendMessage("§4" + player.getName() + " swapped locations with you.");
							}
							player.teleport(targetLoc);
							player.sendMessage("§4You swapped with " + targetted.getName() + ".");

							player.removePotionEffect(PotionEffectType.SLOWNESS);
							cancel();
						}
					}
				}.runTaskTimer(main, 0L, 1L);

				player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
			} else {
				player.sendMessage("§cYou missed!");
			}
		}
	}

	public void useSilencer(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		World world = player.getWorld();
		Location eyeLoc = player.getEyeLocation();
		Vector direction = eyeLoc.getDirection().normalize();
		double maxDistance = 20;
		double step = 0.5;
		boolean playerFound = false;
		Player pf = null;
		for (double i = 0; i <= maxDistance; i += step) {
			Location particleLoc = eyeLoc.clone().add(direction.clone().multiply(i));
			world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
					new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.0f));
			for (Player target : Bukkit.getOnlinePlayers()) {
				if (target.equals(player))
					continue;
				if (!target.getWorld().equals(world))
					continue;
				if (target.getLocation().distance(particleLoc) <= 2.0
						|| target.getEyeLocation().distance(particleLoc) <= 2.0) {
					pf = target;
					playerFound = true;
				}
			}
		}
		if (sp.getGame() != null) {
			if (playerFound == true
					&& !main.getDataLoader().getSpectatorsForGame(sp.getGame())
							.contains(main.getDataLoader().getSpleefPlayer(pf).getSpectator())
					&& !main.getDataLoader().getSpleefPlayer(pf).isDead()) {
				if (isPlayerInShield(pf)) {
					player.sendMessage("§aThis player is protected by their purging shield!");
					return;
				}
				new CooldownTask(main, sp, 8, ShovelPowers.SILENCER, roulette, roulette2);
				SpleefPlayer targetted = main.getDataLoader().getSpleefPlayer(pf);
				pf.sendMessage("§4 You are being silenced...");
				new BukkitRunnable() {
					int ticks = 0;

					@Override
					public void run() {
						ticks++;
						if (ticks == 7) {
							targetted.getPlayer().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK,
									2.0f, 1.0f);
						} else if (ticks == 13) {
							targetted.getPlayer().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK,
									2.0f, 1.0f);
						} else if (ticks == 20) {
							targetted.setSilenced(true);
							targetted.getPlayer().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f,
									1.0f);
							targetted.getPlayer().sendMessage("§4" + sp.getName() + " has silenced you!");
						} else if (ticks >= 100) {
							if (targetted != null && targetted.isOnline()) {
								targetted.setSilenced(false);
								targetted.getPlayer().sendMessage("§aYou are no longer silenced!");
								targetted.getPlayer().playSound(player.getLocation(),
										Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.0f);
							}
							cancel();
						}
					}
				}.runTaskTimer(main, 0L, 1L);
				player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
			} else {
				player.sendMessage("§cYou missed!");
			}
		} else if ((main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId())
				&& main.getSpleefQueueManager().getFFAMode(player.getUniqueId()).equals(SpleefFFAMode.POWERSPLEEF))) {
			if (playerFound == true && !main.getSpleefQueueManager().getAllSpectators().contains(pf.getUniqueId())) {
				if (isPlayerInShield(pf)) {
					player.sendMessage("§aThis player is protected by their purging shield!");
					return;
				}
				SpleefPlayer targetted = main.getDataLoader().getSpleefPlayer(pf);
				if (main.getSpleefQueueManager().getAllSpectators().contains(targetted.getPlayer().getUniqueId())) {
					return;
				}
				new CooldownTask(main, sp, 8, ShovelPowers.SILENCER, roulette, roulette2);

				pf.sendMessage("§4 You are being silenced...");
				new BukkitRunnable() {
					int ticks = 0;

					@Override
					public void run() {
						ticks++;
						if (ticks == 7) {
							targetted.getPlayer().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK,
									2.0f, 1.0f);
						} else if (ticks == 13) {
							targetted.getPlayer().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK,
									2.0f, 1.0f);
						} else if (ticks == 20) {
							targetted.setSilenced(true);
							targetted.getPlayer().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f,
									1.0f);
							targetted.getPlayer().sendMessage("§4" + sp.getName() + " has silenced you!");
						} else if (ticks >= 100) {
							if (targetted != null && targetted.isOnline()) {
								targetted.setSilenced(false);
								targetted.getPlayer().sendMessage("§aYou are no longer silenced!");
								targetted.getPlayer().playSound(player.getLocation(),
										Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.0f);
							}
							cancel();
						}
					}
				}.runTaskTimer(main, 0L, 1L);
				player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
			} else {
				player.sendMessage("§cYou missed!");
			}
		}
	}

	public void useDoubleJump(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		new CooldownTask(main, sp, 2.5, ShovelPowers.DOUBLEJUMP, roulette, roulette2);
		Vector velocity = player.getVelocity();
		velocity.setY(0.9);
		player.setVelocity(velocity);
		// player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 10,
		// 15));
		player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.2, 0.2, 0.2, 0);
		player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
	}

	public void useSnare(Player player, SpleefPlayer sp, Location cursor, boolean roulette, boolean roulette2) {
		Location loc;
		Block target = player.getTargetBlockExact(5);

		if (cursor != null) {
			loc = cursor.clone().add(0, 0.25, 0);
		} else {
			if (target == null) {
				player.sendMessage("§cInvalid placement.");
				return;
			}

			loc = target.getLocation().add(0.5, 1.25, 0.5);
		}

		if (target == null) {
			player.sendMessage("§cInvalid placement.");
			return;
		}

		if (target.getType() == Material.SNOW_BLOCK || target.getType() == Material.ORANGE_CONCRETE_POWDER
				|| target.getType() == Material.RED_CONCRETE_POWDER || target.getType() == Material.BLUE_ICE
				|| target.getType() == Material.ICE || target.getType() == Material.PACKED_ICE) {

			new CooldownTask(main, sp, 10, ShovelPowers.SNARE, roulette, roulette2);

		} else {
			player.sendMessage("§cInvalid placement.");
			return;
		}

		String cleanUUID = player.getUniqueId().toString().replace("-", "");

		String snareTag = "Snare" + cleanUUID + "_" + System.nanoTime();
		String snareOwnerTag = "SnareOwner" + cleanUUID;

		main.getDataLoader().setSnareUsed(player.getUniqueId(), true);

		String command = "execute in minecraft:voidworld run summon block_display " + loc.getX() + " " + loc.getY()
				+ " " + loc.getZ() + " {Tags:[\"" + snareTag + "\",\"" + snareOwnerTag + "\"],Passengers:["
				+ "{id:\"minecraft:block_display\",Tags:[\"" + snareTag + "\",\"" + snareOwnerTag
				+ "\"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.75f,-0.25f,-0.75f],scale:[1.5f,1f,1.5f]},block_state:{Name:\"minecraft:stone_pressure_plate\",Properties:{powered:\"false\"}}},"
				+ "{id:\"minecraft:block_display\",Tags:[\"" + snareTag + "\",\"" + snareOwnerTag
				+ "\"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.75f,-0.1875f,-0.75f],scale:[1.5f,0.4375f,1.5f]},block_state:{Name:\"minecraft:cobweb\"}}"
				+ "],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.5f,-0.28125f,-0.5f],scale:[1f,0.5625f,1f]},block_state:{Name:\"minecraft:chain\",Properties:{waterlogged:\"false\",axis:\"y\"}}}";

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

		new SnareTask(main, player.getUniqueId(), loc, snareTag);
	}

	public void useRecall(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {

		new CooldownTask(main, sp, 10, ShovelPowers.RECALL, roulette, roulette2);
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0));

		Location loc = player.getLocation();
		main.getDataLoader().getRecallLocations().put(player.getUniqueId(), player.getLocation().clone());

		player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Recall Anchor Set! " + ChatColor.GRAY
				+ "Use Recall again within 5 seconds to return.");

		String command = "execute in minecraft:voidworld run summon item_display " + loc.getX() + " " + loc.getY() + " "
				+ loc.getZ() + " {Tags:[\"Recall" + player.getUniqueId() + "\"],"
				+ "item:{id:\"minecraft:totem_of_undying\",count:1}," + "transformation:{"
				+ "translation:[0f,0.35f,0f]," + "left_rotation:[0f,0f,0f,1f]," + "right_rotation:[0f,0f,0f,1f],"
				+ "scale:[0.45f,0.45f,0.45f]" + "}" + "}";

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

		main.getDataLoader().setRecallActive(player.getUniqueId(), true);

		new BukkitRunnable() {

			int ticks = 0;

			@Override
			public void run() {

				ticks++;

				World world = loc.getWorld();
				if (world == null)
					return;

				/* SPIRAL PARTICLES */
				if (ticks % 2 == 0) {
					double radius = 0.7;
					double baseAngle = ticks * 0.18;

					for (double y = 0; y <= 1.5; y += 0.15) {

						double angle = baseAngle + (y * 2.4);

						double x = loc.getX() + Math.cos(angle) * radius;
						double z = loc.getZ() + Math.sin(angle) * radius;

						Location particleLoc = new Location(world, x, loc.getY() + y, z);

						world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(Color.GREEN, 1.2F));
					}
				}

				// Player activated recall
				if (!main.getDataLoader().getRecallActive(player.getUniqueId())) {
					if (sp.getGame() != null) {
						if (!main.getDataLoader().getSpectatorsForGame(sp.getGame())
								.contains(main.getDataLoader().getSpleefPlayer(player).getSpectator())) {
							player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Recalled! " + ChatColor.GRAY
									+ "You returned to your anchor.");

							player.teleport(loc);
							player.removePotionEffect(PotionEffectType.SPEED);

							Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
									"kill @e[type=item_display,tag=Recall" + player.getUniqueId() + "]");

							cancel();
							return;
						}
					} else if ((main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId())
							&& main.getSpleefQueueManager().getFFAMode(player.getUniqueId())
									.equals(SpleefFFAMode.POWERSPLEEF))) {
						player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Recalled! " + ChatColor.GRAY
								+ "You returned to your anchor.");

						player.teleport(loc);
						player.removePotionEffect(PotionEffectType.SPEED);

						Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
								"kill @e[type=item_display,tag=Recall" + player.getUniqueId() + "]");

						cancel();
						return;
					}
					main.getDataLoader().removeRecallLocation(player.getUniqueId());
				}

				// Recall expired
				if (ticks == 100 && main.getDataLoader().getRecallActive(player.getUniqueId())) {
					main.getDataLoader().removeRecallLocation(player.getUniqueId());
					main.getDataLoader().setRecallActive(player.getUniqueId(), false);

					player.sendMessage(ChatColor.GRAY + "Your recall anchor faded.");

					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
							"kill @e[type=item_display,tag=Recall" + player.getUniqueId() + "]");

					cancel();
				}
				if (sp.isRoundStarting()) {
					main.getDataLoader().removeRecallLocation(player.getUniqueId());
					main.getDataLoader().setRecallActive(player.getUniqueId(), false);

					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
							"kill @e[type=item_display,tag=Recall" + player.getUniqueId() + "]");

					cancel();
					return;
				}
			}

		}.runTaskTimer(main, 0L, 1L);
	}

	public void useSmokeGrenade(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		new CooldownTask(main, sp, 10, ShovelPowers.SMOKEGRENADE, roulette, roulette2);
		NamespacedKey key = new NamespacedKey(main, "snowball_power");
		Snowball snowball = player.launchProjectile(Snowball.class, player.getLocation().getDirection().multiply(2.0));
		snowball.getPersistentDataContainer().set(key, PersistentDataType.STRING, "SMOKEGRENADE");
		Bukkit.getScheduler().runTaskLater(main, () -> {
			if (main.getDataLoader().getAllSmokeSpawned().containsKey(player.getUniqueId())) {
				main.getDataLoader().removeAllSmokeSpawned(player.getUniqueId());
				if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
					player.removePotionEffect(PotionEffectType.INVISIBILITY);
					if (sp.getGame() != null)
						sp.getGame().giveColoredLeatherTunic(player, sp.getTeam().getColor());
				}
			}
		}, 20L * 5);
	}

	public void useRoulette(Player player, SpleefPlayer sp, Location cursor) {
		switch (sp.getRandomPower()) {
		case ShovelPowers.NONE: {
			break;
		}
		case ShovelPowers.ROULETTE: {
			break;
		}
		case ShovelPowers.SPEED: {
			useSpeed(player, sp, true, false);
			break;
		}
		case ShovelPowers.WINDCHARGE: {
			useWindCharge(player, sp, true, false);
			break;
		}
		case ShovelPowers.BLACKGOO: {
			useBlackGoo(player, sp, true, false);
			break;
		}
		case ShovelPowers.LEVITATION: {
			useLevitation(player, sp, true, false);
			break;
		}
		case ShovelPowers.ICEGRENADE: {
			useIceGrenade(player, sp, true, false);
			break;
		}
		case ShovelPowers.BUILDER: {
			useBuilder(player, player.getUniqueId(), sp, true, false);
			break;
		}
		case ShovelPowers.TNTRUN: {
			useTntRun(player, sp, true, false);
			break;
		}
		case ShovelPowers.DECAYGRENADE: {
			useDecayGrenade(player, sp, true, false);
			break;
		}
		case ShovelPowers.SWAP: {
			useSwap(player, sp, true, false);
			break;
		}
		case ShovelPowers.SILENCER: {
			useSilencer(player, sp, true, false);
			break;
		}
		case ShovelPowers.DOUBLEJUMP: {
			useDoubleJump(player, sp, true, false);
			break;
		}
		case ShovelPowers.SNARE: {
			useSnare(player, sp, cursor, true, false);
			break;
		}
		case ShovelPowers.RECALL: {
			useRecall(player, sp, true, false);
			break;
		}
		case ShovelPowers.SMOKEGRENADE: {
			useSmokeGrenade(player, sp, true, false);
			break;
		}
		case ShovelPowers.VORTEXGRENADE: {
			useVortexGrenade(player, sp, true, false);
			break;
		}
		case ShovelPowers.PURGINGSHIELD: {
			usePurgingShield(player, sp, true, false);
			break;
		}
		case ShovelPowers.ENDERPEARL: {
			useEnderPearl(player, sp, true, false);
			break;
		}
		}
	}

	public void useRoulette2(Player player, SpleefPlayer sp, Location cursor) {
		switch (sp.getRandomPower2()) {
		case ShovelPowers.NONE: {
			break;
		}
		case ShovelPowers.ROULETTE2: {
			break;
		}
		case ShovelPowers.SPEED: {
			useSpeed(player, sp, false, true);
			break;
		}
		case ShovelPowers.WINDCHARGE: {
			useWindCharge(player, sp, false, true);
			break;
		}
		case ShovelPowers.BLACKGOO: {
			useBlackGoo(player, sp, false, true);
			break;
		}
		case ShovelPowers.LEVITATION: {
			useLevitation(player, sp, false, true);
			break;
		}
		case ShovelPowers.ICEGRENADE: {
			useIceGrenade(player, sp, false, true);
			break;
		}
		case ShovelPowers.BUILDER: {
			useBuilder(player, player.getUniqueId(), sp, false, true);
			break;
		}
		case ShovelPowers.TNTRUN: {
			useTntRun(player, sp, false, true);
			break;
		}
		case ShovelPowers.DECAYGRENADE: {
			useDecayGrenade(player, sp, false, true);
			break;
		}
		case ShovelPowers.SWAP: {
			useSwap(player, sp, false, true);
			break;
		}
		case ShovelPowers.SILENCER: {
			useSilencer(player, sp, false, true);
			break;
		}
		case ShovelPowers.DOUBLEJUMP: {
			useDoubleJump(player, sp, false, true);
			break;
		}
		case ShovelPowers.SNARE: {
			useSnare(player, sp, cursor, false, true);
			break;
		}
		case ShovelPowers.RECALL: {
			useRecall(player, sp, false, true);
			break;
		}
		case ShovelPowers.SMOKEGRENADE: {
			useSmokeGrenade(player, sp, false, true);
			break;
		}
		case ShovelPowers.VORTEXGRENADE: {
			useVortexGrenade(player, sp, false, true);
			break;
		}
		case ShovelPowers.PURGINGSHIELD: {
			usePurgingShield(player, sp, false, true);
			break;
		}
		case ShovelPowers.ENDERPEARL: {
			useEnderPearl(player, sp, false, true);
			break;
		}
		}
	}

	public void useVortexGrenade(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		new CooldownTask(main, sp, 10, ShovelPowers.VORTEXGRENADE, roulette, roulette2);
		NamespacedKey key = new NamespacedKey(main, "snowball_power");
		Snowball snowball = player.launchProjectile(Snowball.class, player.getLocation().getDirection().multiply(2.0));
		snowball.getPersistentDataContainer().set(key, PersistentDataType.STRING, "VORTEXGRENADE");
		Bukkit.getScheduler().runTaskLater(main, () -> {
			if (main.getDataLoader().getAllVortexSpawned().containsKey(player.getUniqueId())) {
				main.getDataLoader().removeAllVortexSpawned(player.getUniqueId());
				List<UUID> affectedPlayers = main.getDataLoader().getPlayersAffectedByVortex(player.getUniqueId());

				for (UUID affectedUUID : affectedPlayers) {
					Player affected = Bukkit.getPlayer(affectedUUID);

					if (affected != null && affected.isOnline()) {
						setPlayerGravity(affected, 0.08);
					}

					main.getDataLoader().removeGravityAffected(affectedUUID);
				}
			}
		}, 20L * 3);
	}

	public void usePurgingShield(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		new CooldownTask(main, sp, 15, ShovelPowers.PURGINGSHIELD, roulette, roulette2);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_BOTTLE_THROW, 1.0f, 1.0f);
		player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
		main.getDataLoader().addProtectedPlayer(player.getUniqueId());
		// remove all negative effects
		for (PotionEffect effect : sp.getPlayer().getActivePotionEffects()) {
			if (effect.getType() != PotionEffectType.NIGHT_VISION && effect.getType() != PotionEffectType.JUMP_BOOST
					&& effect.getType() != PotionEffectType.SPEED)
				sp.getPlayer().removePotionEffect(effect.getType());
		}
		if (sp.isSilenced()) {
			sp.setSilenced(false);
		}
		player.sendMessage(ChatColor.GREEN + "You feel protected and purged of all negative effects!");
		Bukkit.getScheduler().runTaskLater(main, () -> {
			player.sendMessage(ChatColor.RED + "You are no longer being protected by your shield!");
			player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
			main.getDataLoader().removeProtectedPlayer(player.getUniqueId());
		}, 20L * 5);
	}

	public void useEnderPearl(Player player, SpleefPlayer sp, boolean roulette, boolean roulette2) {
		new CooldownTask(main, sp, 7, ShovelPowers.ENDERPEARL, roulette, roulette2);
		EnderPearl enderPearl = player.launchProjectile(EnderPearl.class);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.0f);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
		if (sp == null || sp.getPlayer() == null)
			return;
		if (!(sp.getInGame() || sp.getInTeamGame()
				|| (main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId()) && main.getSpleefQueueManager()
						.getFFAMode(player.getUniqueId()).equals(SpleefFFAMode.POWERSPLEEF))))
			return;
		Block feetBlock = player.getLocation().subtract(0, 1, 0).getBlock();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (feetBlock.getType() != Material.BLUE_ICE)
				continue;
			UUID uuid = p.getUniqueId();
			List<Location> ices = main.getDataLoader().getIceSpawned(uuid);
			if (ices == null)
				continue;
			Location feetBlockLoc = feetBlock.getLocation().toBlockLocation();
			Iterator<Location> it = ices.iterator();
			while (it.hasNext()) {
				Location ice = it.next();
				if (feetBlockLoc.equals(ice.toBlockLocation())) {
					new IceDecayTask(main, main.getDataLoader().getSpleefPlayer(p), 2, ice);
					it.remove();
				}
			}
		}
		for (Player p : Bukkit.getOnlinePlayers()) {
			UUID uuid = p.getUniqueId();
			Location feetBlockLoc = feetBlock.getLocation().toBlockLocation();
			if (main.getDataLoader().getAllSmokeSpawned().containsKey(uuid)) {
				if (main.getDataLoader().getSmokeSpawned(uuid).contains(feetBlockLoc)
						&& player.getUniqueId().equals(uuid)) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0, false, false, false));
					player.getInventory().setChestplate(null);
				}
				if (!main.getDataLoader().getSmokeSpawned(uuid).contains(feetBlockLoc)
						&& player.getUniqueId().equals(uuid)) {
					player.removePotionEffect(PotionEffectType.INVISIBILITY);
					if (sp.getGame() != null)
						sp.getGame().giveColoredLeatherTunic(p, sp.getTeam().getColor());
				}
			}
			if (main.getDataLoader().getAllVortexSpawned().containsKey(uuid)) {
				if ((main.getDataLoader().getVortexSpawned(uuid).contains(feetBlockLoc)
						|| main.getDataLoader().getVortexSpawned(uuid).contains(feetBlockLoc.clone().add(0, -1, 0))
						|| main.getDataLoader().getVortexSpawned(uuid).contains(feetBlockLoc.clone().add(0, -2, 0)))
						&& !player.getUniqueId().equals(uuid)) {
					if (!isPlayerInShield(player)) {
						main.getDataLoader().addGravityAffected(player.getUniqueId(), // affected player
								uuid // vortex owner
						);
						setPlayerGravity(player, 0.24);
					}
				}
				if (!(main.getDataLoader().getVortexSpawned(uuid).contains(feetBlockLoc)
						|| main.getDataLoader().getVortexSpawned(uuid).contains(feetBlockLoc.clone().add(0, -1, 0))
						|| main.getDataLoader().getVortexSpawned(uuid).contains(feetBlockLoc.clone().add(0, -2, 0)))
						&& !player.getUniqueId().equals(uuid)) {
					setPlayerGravity(player, 0.08);
				}
			}
		}
	}

	public Attribute getGravityAttribute() {
		try {
			return Attribute.valueOf("GENERIC_GRAVITY"); // Paper 1.21
		} catch (IllegalArgumentException ignored) {
		}

		try {
			return Attribute.valueOf("GRAVITY"); // newer API naming
		} catch (IllegalArgumentException ignored) {
		}

		return null;
	}

	public void setPlayerGravity(Player player, double value) {
		if (player == null || !player.isOnline()) {
			return;
		}

		Attribute gravityAttribute = getGravityAttribute();

		if (gravityAttribute == null) {
			return;
		}

		AttributeInstance gravity = player.getAttribute(gravityAttribute);

		if (gravity == null) {
			return;
		}

		gravity.setBaseValue(value);
		if (value != 0.08) {
			Bukkit.getScheduler().runTaskLater(main, () -> {
				setPlayerGravity(player, 0.08);
			}, 20L * 4);
		}
	}

	public void startAutoFire(UUID uuid) {
		Player player = Bukkit.getPlayer(uuid);
		if (player == null)
			return;

		BukkitRunnable oldTask = timerTasks.remove(uuid);
		if (oldTask != null) {
			oldTask.cancel();
		}

		BukkitRunnable timerTask = new BukkitRunnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(uuid);
				if (player == null) {
					timerTasks.remove(uuid);
					isAutoFiring.put(uuid, false);
					this.cancel();
					return;
				}

				isAutoFiring.put(uuid, true);

				int currentTick = Bukkit.getServer().getCurrentTick();
				int lastInteractTick = playerGunTick.getOrDefault(uuid, -1000);

				// stop fast auto if player stopped spam clicking
				if (currentTick - lastInteractTick > RAPID_CLICK_THRESHOLD) {
					timerTasks.remove(uuid);
					isAutoFiring.put(uuid, false);
					this.cancel();
					return;
				}

				if (!main.getDataLoader().getSpleefPlayer(Bukkit.getPlayer(uuid)).getInGame()
						&& !main.getDataLoader().getSpleefPlayer(Bukkit.getPlayer(uuid)).getInTeamGame()
						&& main.getSpleggQueueManager().getPlayers().containsKey(uuid)) {
					if (main.getSpleggQueueManager().isGameEnding())
						return;
					if (!main.getSpleggQueueManager().isDead(uuid))
						return;
				}

				int lastShotTick = playerFastShotTick.getOrDefault(uuid, -1000);
				if (currentTick - lastShotTick >= FAST_FIRE_INTERVAL) {
					shootEgg(player);
					playerFastShotTick.put(uuid, currentTick);
				}
			}
		};

		timerTask.runTaskTimer(main, 0L, 1L);
		timerTasks.put(uuid, timerTask);
	}

	public void stopAutoFire(UUID uuid) {
		isAutoFiring.put(uuid, false);
		BukkitRunnable task = timerTasks.remove(uuid);
		if (task != null) {
			task.cancel();
		}
	}

	public void startHoldAutoFire(UUID uuid) {
		Player player = Bukkit.getPlayer(uuid);
		if (player == null)
			return;

		BukkitRunnable oldTask = holdTimerTasks.get(uuid);
		if (oldTask != null) {
			return;
		}

		BukkitRunnable timerTask = new BukkitRunnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(uuid);
				if (player == null) {
					holdTimerTasks.remove(uuid);
					isHoldAutoFiring.put(uuid, false);
					this.cancel();
					return;
				}

				isHoldAutoFiring.put(uuid, true);

				int currentTick = Bukkit.getServer().getCurrentTick();
				int lastInteractTick = playerGunTick.getOrDefault(uuid, -1000);
				int timeSinceInteract = currentTick - lastInteractTick;

				// kill the task fully if input has been gone for too long
				if (timeSinceInteract > HOLD_TIMEOUT) {
					holdTimerTasks.remove(uuid);
					isHoldAutoFiring.put(uuid, false);
					this.cancel();
					return;
				}

				if (!main.getDataLoader().getSpleefPlayer(Bukkit.getPlayer(uuid)).getInGame()
						&& !main.getDataLoader().getSpleefPlayer(Bukkit.getPlayer(uuid)).getInTeamGame()
						&& main.getSpleggQueueManager().getPlayers().containsKey(uuid)) {
					if (main.getSpleggQueueManager().isGameEnding())
						return;
					if (!main.getSpleggQueueManager().isDead(uuid))
						return;
				}

				// DO NOT fire unless the interact was extremely recent
				if (timeSinceInteract > HOLD_SHOOT_CUTOFF) {
					return;
				}

				int lastShotTick = playerHoldShotTick.getOrDefault(uuid, -1000);
				if (currentTick - lastShotTick >= HOLD_FIRE_INTERVAL) {
					shootEgg(player);
					playerHoldShotTick.put(uuid, currentTick);
				}
			}
		};

		timerTask.runTaskTimer(main, 0L, 1L);
		holdTimerTasks.put(uuid, timerTask);
	}

	public void stopHoldAutoFire(UUID uuid) {
		isHoldAutoFiring.put(uuid, false);
		BukkitRunnable task = holdTimerTasks.remove(uuid);
		if (task != null) {
			task.cancel();
		}
	}

	private void shootEgg(Player player) {
		/*
		 * createPlayerStatsIfNotExists(main.getConnection(), player.getUniqueId());
		 * addEggsFired(main.getConnection(), player.getUniqueId(), 1);
		 * 
		 * Vector dir = player.getEyeLocation().getDirection();
		 * 
		 * Location spawnLoc = player.getEyeLocation().clone().add(dir.multiply(0.3));
		 * spawnLoc.setY(spawnLoc.getY() + 0.05);
		 * 
		 * if (player.getWorld().getBlockAt(spawnLoc).getType().isSolid()) { spawnLoc =
		 * player.getEyeLocation().clone().add(dir.multiply(0.15));
		 * spawnLoc.setY(spawnLoc.getY() + 0.05); }
		 * 
		 * float yaw = (float) 10; spawnLoc.setYaw(spawnLoc.getYaw() + yaw);
		 * 
		 * Egg egg = player.getWorld().spawn(spawnLoc, Egg.class);
		 * egg.setShooter(player); egg.setVelocity(dir.multiply(4.8));
		 * 
		 * player.playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1, 2);
		 */
		createPlayerStatsIfNotExists(main.getConnection(), player.getUniqueId());
		addEggsFired(main.getConnection(), player.getUniqueId(), 1);

		Egg egg = player.launchProjectile(Egg.class);
		egg.setSilent(true);
		Vector direction = player.getLocation().getDirection();
		egg.setVelocity(direction.multiply(1.5));
		player.playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1, 2);
	}

	public boolean isPlayerInShield(Player player) {
		if (player == null || !player.isOnline()) {
			return false;
		}
		if (main.getDataLoader().getProtectedPlayers().contains(player.getUniqueId())) {
			return true;
		}
		return false;
	}

	private boolean sameBlock(Location a, Location b) {
		if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
			return false;
		}

		return a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX() && a.getBlockZ() == b.getBlockZ();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Attribute reachAttr = resolveBlockReachAttr();
		AttributeInstance inst = player.getAttribute(reachAttr);
		inst.setBaseValue(4.5);
	}

	// Logic for snowballs

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		if (event.getEntity() instanceof Snowball snowball) {
			handleSnowballHit(event, snowball);
			return;
		}
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
			return;
		}

		Location destination = event.getTo();

		if (destination == null) {
			return;
		}

		Block blockBelow = getBlockBelowPearlDestination(destination);

		if (blockBelow == null) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("§cYou can't teleport outside of the arena.");
			return;
		}

		event.getPlayer().sendMessage("Found block: " + blockBelow.getType() + " at Y=" + blockBelow.getY());

		if (blockBelow.getType() != Material.SNOW_BLOCK && blockBelow.getType() != Material.ICE
				&& blockBelow.getType() != Material.BLUE_ICE && blockBelow.getType() != Material.PACKED_ICE
				&& blockBelow.getType() != Material.ORANGE_CONCRETE_POWDER
				&& blockBelow.getType() != Material.RED_CONCRETE_POWDER
				&& blockBelow.getType() != Material.GREEN_CONCRETE_POWDER) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("§cYou can't teleport outside of the arena.");
		}
	}

	private Block getBlockBelowPearlDestination(Location destination) {
		World world = destination.getWorld();

		if (world == null) {
			return null;
		}

		// Check from slightly below the destination down several blocks.
		for (double yOffset = 0.1; yOffset <= 3.0; yOffset += 0.25) {
			Location check = destination.clone().subtract(0, yOffset, 0);
			Block block = world.getBlockAt(check);

			if (!block.getType().isAir()) {
				return block;
			}
		}

		return null;
	}

	private void handleSnowballHit(ProjectileHitEvent event, Snowball snowball) {
		String power = getSnowballPower(snowball);
		if (power == null) {
			return;
		}

		Player shooter = getShooter(event);
		if (shooter == null) {
			return;
		}

		if (event.getHitEntity() instanceof Player hitPlayer) {
			handleSnowballPlayerHit(event, power, shooter, hitPlayer);
			return;
		}

		Block hitBlock = event.getHitBlock();
		if (hitBlock == null) {
			return;
		}

		if (!isValidSnowballPowerBlock(hitBlock.getType())) {
			return;
		}

		handleSnowballBlockHit(event, power, shooter, hitBlock);
	}

	private void handleSnowballPlayerHit(ProjectileHitEvent event, String power, Player shooter, Player hitPlayer) {
		SpleefPlayer hitSp = main.getDataLoader().getSpleefPlayer(hitPlayer);
		if (hitSp == null) {
			return;
		}

		if (!isValidPowerTarget(hitPlayer, hitSp)) {
			return;
		}

		if (isSpectatorTarget(hitPlayer, hitSp)) {
			return;
		}

		if (isPlayerInShield(hitPlayer)) {
			handlePlayerPowerBlockedByBarrier(shooter, hitPlayer);
			return;
		}

		Block hitBlock = hitPlayer.getLocation().clone().add(0, -1, 0).getBlock();
		handlePowerAtBlock(event, power, shooter, hitBlock);
	}

	private void handleSnowballBlockHit(ProjectileHitEvent event, String power, Player shooter, Block hitBlock) {
		handlePowerAtBlock(event, power, shooter, hitBlock);
	}

	private void handlePowerAtBlock(ProjectileHitEvent event, String power, Player shooter, Block hitBlock) {
		if (hitBlock == null) {
			return;
		}

		SpleefPlayer shooterSp = main.getDataLoader().getSpleefPlayer(shooter);
		if (shooterSp == null) {
			return;
		}

		switch (power) {
		case "VORTEXGRENADE":
			useVortexGrenade(event, shooter, shooterSp, hitBlock);
			break;

		case "SMOKEGRENADE":
			useSmokeGrenade(event, shooter, shooterSp, hitBlock);
			break;

		case "ICEGRENADE":
			useIceGrenade(event, shooter, shooterSp, hitBlock);
			break;
			
		case "DECAYGRENADE":
			useDecayGrenade(event, shooter, shooterSp, hitBlock);
		}
	}

	private void useVortexGrenade(ProjectileHitEvent event, Player shooter, SpleefPlayer shooterSp, Block hitBlock) {
		World world = hitBlock.getWorld();
		int cx = hitBlock.getX();
		int cy = hitBlock.getY();
		int cz = hitBlock.getZ();

		int[][] offsets = getGrenadeOffsets();

		spawnPurpleBlackSwirl(main, hitBlock, 2.5, Particle.SMOKE, Particle.WITCH, shooterSp);

		for (int[] offset : offsets) {
			int x = offset[0];
			int z = offset[1];

			Location loc = new Location(world, cx + x, cy, cz + z);
			main.getDataLoader().addVortexSpawned(shooter.getUniqueId(), loc);
		}

		event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
	}

	private void useSmokeGrenade(ProjectileHitEvent event, Player shooter, SpleefPlayer shooterSp, Block hitBlock) {
		World world = hitBlock.getWorld();
		int cx = hitBlock.getX();
		int cy = hitBlock.getY();
		int cz = hitBlock.getZ();

		int[][] offsets = getGrenadeOffsets();

		spawnSmokeColumn(main, hitBlock, 3.5, shooterSp);

		for (int[] offset : offsets) {
			int x = offset[0];
			int z = offset[1];

			Location loc = new Location(world, cx + x, cy, cz + z);
			main.getDataLoader().addSmokeSpawned(shooter.getUniqueId(), loc);
		}

		event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
	}

	private void useIceGrenade(ProjectileHitEvent event, Player shooter, SpleefPlayer shooterSp, Block hitBlock) {
		World world = hitBlock.getWorld();
		int cx = hitBlock.getX();
		int cy = hitBlock.getY();
		int cz = hitBlock.getZ();

		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				if (x * x + z * z > 3 * 3) {
					continue;
				}

				int count = ((int) ((Math.random() * 100) + 1));
				if (count < 75) {
					continue;
				}

				Location loc = new Location(world, cx + x, cy, cz + z);
				tryPlaceIce(shooter, shooterSp, loc);
			}
		}

		event.getEntity().getWorld().spawnParticle(Particle.EXPLOSION, event.getEntity().getLocation(), 1);
		event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f,
				1.0f);
	}

	private void tryPlaceIce(Player shooter, SpleefPlayer shooterSp, Location loc) {
		if (shooter == null || shooterSp == null || loc == null || loc.getWorld() == null) {
			return;
		}

		Block block = loc.getWorld().getBlockAt(loc);
		Material type = block.getType();

		if (type != Material.SNOW_BLOCK && type != Material.AIR) {
			block.setType(Material.BLUE_ICE);
			main.getDataLoader().addIceSpawned(shooter.getUniqueId(), loc);
			return;
		}

		if (shooterSp.getGame() != null) {
			if (!isInsideTeamArenaBounds(shooterSp.getGame(), loc)) {
				return;
			}

			block.setType(Material.BLUE_ICE);
			main.getDataLoader().addIceSpawned(shooter.getUniqueId(), loc);
			return;
		}

		if (isShooterInPowerSpleefFFA(shooter)) {
			block.setType(Material.BLUE_ICE);
			main.getDataLoader().addIceSpawned(shooter.getUniqueId(), loc);
		}
	}

	private void useDecayGrenade(ProjectileHitEvent event, Player shooter, SpleefPlayer shooterSp, Block hitBlock) {
		if (hitBlock == null) {
			return;
		}

		World world = hitBlock.getWorld();
		int cx = hitBlock.getX();
		int cy = hitBlock.getY();
		int cz = hitBlock.getZ();

		int[][] offsets = getGrenadeOffsets();

		spawnFireVeil(main, hitBlock, 2.75, 5.0, Particle.FLAME);

		for (int[] offset : offsets) {
			int x = offset[0];
			int z = offset[1];

			Location loc = new Location(world, cx + x, cy, cz + z);
			Material type = world.getBlockAt(loc).getType();

			if (type == Material.SNOW_BLOCK || type == Material.AIR) {
				shooter.spawnParticle(Particle.LAVA, loc, 5);
				main.getDataLoader().addDecaySpawned(shooter.getUniqueId(), loc);
			}
		}

		event.getEntity().getWorld().spawnParticle(Particle.EXPLOSION, event.getEntity().getLocation(), 1);
		event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
	}

	private String getSnowballPower(Snowball snowball) {
		NamespacedKey key = new NamespacedKey(main, "snowball_power");
		return snowball.getPersistentDataContainer().get(key, PersistentDataType.STRING);
	}

	private Player getShooter(ProjectileHitEvent event) {
		if (event.getEntity().getShooter() instanceof Player shooter) {
			return shooter;
		}

		return null;
	}

	private boolean isValidPowerTarget(Player player, SpleefPlayer sp) {
		if (sp.getInGame() || sp.getInTeamGame()) {
			return true;
		}

		return main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId())
				&& main.getSpleefQueueManager().getFFAMode(player.getUniqueId()) == SpleefFFAMode.POWERSPLEEF;
	}

	private boolean isSpectatorTarget(Player player, SpleefPlayer sp) {
		if (sp.getGame() != null) {
			return main.getDataLoader().getSpectatorsForGame(sp.getGame()) != null
					&& main.getDataLoader().getSpectatorsForGame(sp.getGame()).contains(sp.getSpectator());
		}

		if (main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId())
				&& main.getSpleefQueueManager().getFFAMode(player.getUniqueId()) == SpleefFFAMode.POWERSPLEEF) {
			return main.getSpleefQueueManager().getAllSpectators().contains(player.getUniqueId());
		}

		return false;
	}

	private boolean isShooterInPowerSpleefFFA(Player shooter) {
		return main.getSpleefQueueManager().isAliveInAnyFFA(shooter.getUniqueId())
				&& main.getSpleefQueueManager().getFFAMode(shooter.getUniqueId()) == SpleefFFAMode.POWERSPLEEF;
	}

	private boolean isValidSnowballPowerBlock(Material type) {
		return type == Material.SNOW_BLOCK || type == Material.RED_CONCRETE_POWDER
				|| type == Material.ORANGE_CONCRETE_POWDER || type == Material.BLUE_ICE || type == Material.ICE
				|| type == Material.PACKED_ICE;
	}

	private void handlePlayerPowerBlockedByBarrier(Player shooter, Player hitPlayer) {
		if (shooter == null || hitPlayer == null) {
			return;
		}

		shooter.sendMessage("§aYour power was blocked by a purging shield!");

		for (Player p : Bukkit.getOnlinePlayers()) {
			if (!main.getDataLoader().getProtectedPlayers().contains(p.getUniqueId())) {
				continue;
			} else {
				if (p == hitPlayer) {
					p.sendMessage("§a" + shooter.getName() + "§a's power was blocked by your purging shield!");
				}
			}
		}
	}

	private boolean isInsideTeamArenaBounds(SpleefGame game, Location loc) {
		if (game == null || game.getArena() == null || loc == null) {
			return false;
		}

		Location corner1 = game.getArena().getCorner1().clone();
		Location corner2 = game.getArena().getCorner2().clone();

		int playerCount = 0;

		for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
			playerCount += team.getPlayers().size();
		}

		if (playerCount > 4) {
			double expand = Math.floor((playerCount - 4) / 2.0);

			corner1.setX(corner1.getX() - expand);
			corner1.setZ(corner1.getZ() + expand);

			corner2.setX(corner2.getX() + expand);
			corner2.setZ(corner2.getZ() - expand);
		}

		int x = loc.getBlockX();
		int z = loc.getBlockZ();

		int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
		int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());

		int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
		int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

		return x >= minX - 3 && x <= maxX + 3 && z >= minZ - 3 && z <= maxZ + 3;
	}

	private int[][] getGrenadeOffsets() {
		return new int[][] { { 0, 0 },

				{ -2, 0 }, { -1, 0 }, { 1, 0 }, { 2, 0 }, { 0, -2 }, { 0, -1 }, { 0, 1 }, { 0, 2 },

				{ -2, -1 }, { -2, 1 }, { 2, -1 }, { 2, 1 }, { -1, -2 }, { 1, -2 }, { -1, 2 }, { 1, 2 },

				{ -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 } };
	}

	/*
	 * private Block getSnowBlockBelowPlayer(Player player) { Location loc =
	 * player.getLocation();
	 * 
	 * World world = player.getWorld();
	 * 
	 * double radius = 0.31; // player width is about 0.6, so this checks slightly
	 * around their feet double yOffset = 0.15; // checks slightly below feet
	 * 
	 * Location[] checks = new Location[] { loc.clone().add(0, -yOffset, 0), //
	 * center loc.clone().add(radius, -yOffset, 0), // east loc.clone().add(-radius,
	 * -yOffset, 0), // west loc.clone().add(0, -yOffset, radius), // south
	 * loc.clone().add(0, -yOffset, -radius), // north loc.clone().add(radius,
	 * -yOffset, radius), loc.clone().add(radius, -yOffset, -radius),
	 * loc.clone().add(-radius, -yOffset, radius), loc.clone().add(-radius,
	 * -yOffset, -radius) };
	 * 
	 * for (Location check : checks) { Block block = world.getBlockAt(check);
	 * 
	 * if (block.getType() == Material.SNOW_BLOCK) { return block; } }
	 * 
	 * return null; }
	 */

	private Block getSnowBlockBelowPlayer(Player player) {
		Location location = player.getLocation().clone();
		World world = player.getWorld();
		int[][] offsets = { { 0, 0 }, // directly below

				{ 1, 0 }, // east
				{ -1, 0 }, // west
				{ 0, 1 }, // south
				{ 0, -1 }, // north

				{ 1, 1 }, // southeast
				{ 1, -1 }, // northeast
				{ -1, 1 }, // southwest
				{ -1, -1 } // northwest
		};

		for (int[] offset : offsets) {
			Block block = location.clone().add(offset[0], -1, offset[1]).getBlock();

			if (block.getType() == Material.SNOW_BLOCK) {
				return block;
			}
		}
		return player.getLocation().clone().add(0, -1, 0).getBlock();
	}

	@EventHandler
	public void onWindChargeHit(ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof WindCharge windCharge)) {
			return;
		}

		Player shooter = null;

		if (windCharge.getShooter() instanceof Player playerShooter) {
			shooter = playerShooter;
		}

		// Hit a player
		if (event.getHitEntity() instanceof Player player) {
			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);

			if (sp != null && (sp.getInGame() || sp.getInTeamGame())) {
				if (isPlayerInShield(player)) {
					if (shooter != null) {
						shooter.sendMessage("§aThis player is protected by their purging shield!");
					}

					main.getDataLoader().addBlockedWindCharge(windCharge, shooter);
					event.setCancelled(true);
					windCharge.remove();

					return;
				}
			}
		}
	}

	@EventHandler
	public void onWindChargeExplode(EntityExplodeEvent event) {
		if (event.getEntityType() != EntityType.WIND_CHARGE) {
			return;
		}

		if (!(event.getEntity() instanceof WindCharge windCharge)) {
			return;
		}

		if (!main.getDataLoader().isBlockedWindCharge(windCharge)) {
			return;
		}

		event.setCancelled(true);
		event.blockList().clear();

		main.getDataLoader().removeBlockedWindCharge(windCharge);
	}

	@EventHandler
	public void onSnowballLaunch(ProjectileLaunchEvent event) {
		if (!(event.getEntity() instanceof Snowball))
			return;
		if (event.getEntity() instanceof Snowball) {
			Snowball snowball = (Snowball) event.getEntity();
			trackedSnowballs.add(snowball);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!snowball.isValid() || snowball.isOnGround() || snowball.isDead()) {
						trackedSnowballs.remove(snowball);
						cancel();
						return;
					}

					snowball.getWorld().spawnParticle(Particle.SNOWFLAKE, snowball.getLocation(), 3, 0.1, 0.1, 0.1, 0);
				}
			}.runTaskTimer(main, 0L, 1L);
		}
	}

	public String[] getPlayerPowers(UUID uuid) {
		String[] powers = new String[2];
		String sql = "SELECT power1, power2 FROM player_powers WHERE uuid = ?";
		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					powers[0] = rs.getString("power1");
					powers[1] = rs.getString("power2");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return powers;
	}

	public void applyTntRunDebuff(Player target, double seconds) {
		target.sendMessage(
				ChatColor.RED + "You have been affected by TNTRun! " + ChatColor.GRAY + "Watch the ground under you.");
		UUID id = target.getUniqueId();
		new BukkitRunnable() {
			int ticks = (int) seconds * 20;

			@Override
			public void run() {
				if (ticks <= 0 || !target.isOnline()) {
					main.getDataLoader().removePlayerAffectedByTntRun(target.getUniqueId());
					cancel();
					return;
				}

				SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(target);
				if (sp != null) {
					if (sp.getGame() == null && !(main.getSpleefQueueManager().isAliveInAnyFFA(target.getUniqueId())
							&& main.getSpleefQueueManager().getFFAMode(target.getUniqueId())
									.equals(SpleefFFAMode.POWERSPLEEF))) {
						main.getDataLoader().removePlayerAffectedByTntRun(target.getUniqueId());
						cancel();
						return;
					}
					if (sp.isRoundStarting()) {
						main.getDataLoader().removePlayerAffectedByTntRun(target.getUniqueId());
						cancel();
						return;
					}
					if (sp.isDead() || main.getSpleefQueueManager().getGame(SpleefFFAMode.POWERSPLEEF)
							.getPlayersSpectating().contains(target.getUniqueId())) {
						main.getDataLoader().removePlayerAffectedByTntRun(target.getUniqueId());
						cancel();
						return;
					}
				}

				Location loc = target.getLocation().subtract(0, 1, 0);
				Block block = loc.getBlock();

				if (block.getType() != Material.AIR) {
					if (main.getDataLoader().getSpleefPlayer(target) != null) {
						if (sp.getInGame() || sp.getInTeamGame()
								|| (main.getSpleefQueueManager().isAliveInAnyFFA(target.getUniqueId())
										&& main.getSpleefQueueManager().getFFAMode(target.getUniqueId())
												.equals(SpleefFFAMode.POWERSPLEEF))) {
							if (!sp.isRoundStarting()
									&& !sp.isDead() & !main.getSpleefQueueManager().getGame(SpleefFFAMode.POWERSPLEEF)
											.getPlayersSpectating().contains(target.getUniqueId())) {
								
								int y = sp.getPlayer().getLocation().getBlockY();
								block = null;
								for (int i  = 0; i <= 3; i++) {
								block = getBlockUnderPlayer(y-i, sp.getPlayer().getLocation());
								if (block !=null) {
								break;
								}
								}

								if (block !=null) {
									new BlockDecayTask(main, sp, 3, block.getLocation());
								}
								//new BlockDecayTask(main, sp, 3, getSnowBlockBelowPlayer(sp.getPlayer()).getLocation());
							} else {
								main.getDataLoader().removePlayerAffectedByTntRun(target.getUniqueId());
								this.cancel();
							}
						} else {
							main.getDataLoader().removePlayerAffectedByTntRun(target.getUniqueId());
							this.cancel();
						}
					}
				}

				ticks--;
			}
		}.runTaskTimer(main, 0L, 1L);
	}
	
	private Block getBlockUnderPlayer(int y, Location location) {
        World world = location.getWorld();
        double x = location.getX();
        double z = location.getZ();
        Block b11 = getBlockAt(world, x + 0.3d, y, z - 0.3d);
        if (b11.getType() != Material.AIR) {
            return b11;
        }
        Block b12 = getBlockAt(world, x - 0.3d, y, z + 0.3d);
        if (b12.getType() != Material.AIR) {
            return b12;
        }
        Block b21 = getBlockAt(world, x + 0.3d, y, z + 0.3d);
        if (b21.getType() != Material.AIR) {
            return b21;
        }
        Block b22 = getBlockAt(world, x - 0.3d, y, z - 0.3d);
        if (b22.getType() != Material.AIR) {
            return b22;
        }
        return null;
    }

    private Block getBlockAt(World world, double x, int y, double z) {
        return world.getBlockAt(NumberConversions.floor(x), y, NumberConversions.floor(z));
    }

	public void useAspectOfTheEnd(Player player, ItemStack item) {
		if (!player.getGameMode().equals(GameMode.CREATIVE))
			if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta()
					.getDisplayName().equals(ChatColor.BLUE + "" + ChatColor.BOLD + "Aspect of the End")) {
				Location eye = player.getEyeLocation();
				Vector dir = eye.getDirection().normalize();
				double range = 8.0;

				RayTraceResult r = player.getWorld().rayTraceBlocks(eye, dir, range, FluidCollisionMode.NEVER, true);

				Location target;
				if (r != null && r.getHitPosition() != null) {
					target = r.getHitPosition().toLocation(player.getWorld()).subtract(dir.multiply(0.6));
				} else {
					target = eye.clone().add(dir.multiply(range));
				}

				// feet position
				target.subtract(0, 1.62, 0);

				// keep facing
				Location base = player.getLocation();
				target.setYaw(base.getYaw());
				target.setPitch(base.getPitch());

				// ---- SAFE SPACE CHECK (allows midair) ----
				World w = target.getWorld();
				int x = target.getBlockX();
				int z = target.getBlockZ();
				int y = target.getBlockY();

				for (int i = 0; i < 8; i++) { // move up a bit if clipped
					boolean feetAir = w.getBlockAt(x, y, z).isPassable();
					boolean headAir = w.getBlockAt(x, y + 1, z).isPassable();

					if (feetAir && headAir) {
						target.setX(x + 0.5);
						target.setY(y);
						target.setZ(z + 0.5);
						player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
						player.teleport(target);
						player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.35f);
						return;
					}
					y++;
				}
				// if no space found, do nothing
			}
	}

	public void spawnFireVeil(Main plugin, Block centerBlock, double radius, double heightSeconds, Particle particle) {
		final World world = centerBlock.getWorld();
		final Location center = centerBlock.getLocation().add(0.5, 0.0, 0.5);

		final int period = 2;
		final int totalTicks = 20 * 2;

		new BukkitRunnable() {
			int tick = 0;

			@Override
			public void run() {
				if (tick++ >= totalTicks) {
					cancel();
					return;
				}

				double prog = (double) tick / totalTicks;

				for (int i = 0; i < 72; i++) {
					double theta = (2 * Math.PI) * i / 72;

					double x = center.getX() + Math.cos(theta) * radius;
					double z = center.getZ() + Math.sin(theta) * radius;

					double wave = 0.15 * Math.sin(theta * 2.0 + prog * Math.PI * 2);
					Location p = new Location(world, x, center.getY() + 1, z);
					world.spawnParticle(particle, p, 1, 0.02, 0.02, 0.02, 0.0);

				}

				if (tick % 10 == 0) {
					world.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.35f, 1.05f);
				}
			}
		}.runTaskTimer(plugin, 0L, period);
	}

	public void spawnSmokeColumn(Main plugin, Block centerBlock, double radius, SpleefPlayer sp) {
		final World world = centerBlock.getWorld();
		final Location center = centerBlock.getLocation().add(0.5, 0.0, 0.5);

		final int period = 4; // was 2, now spawns less often
		final int totalTicks = (20 * 5) / period;
		final double height = 4.0;

		new BukkitRunnable() {
			int tick = 0;

			@Override
			public void run() {
				if (sp.isRoundStarting()) {
					cancel();
					return;
				}
				if (tick++ >= totalTicks) {
					cancel();
					return;
				}

				double prog = (double) tick / totalTicks;

				// Bigger y step = fewer vertical layers
				for (double y = 0.25; y <= height; y += 0.75) {

					double driftX = Math.sin(prog * Math.PI * 4 + y) * 0.12;
					double driftZ = Math.cos(prog * Math.PI * 4 + y) * 0.12;

					// Bigger r step = fewer rings
					for (double r = 0; r <= radius; r += 0.85) {
						int points = Math.max(4, (int) (18 * (r / radius)));

						for (int i = 0; i < points; i++) {
							double theta = (2 * Math.PI) * i / points;

							double swirl = prog * Math.PI * 2 + y * 0.45;
							double x = center.getX() + Math.cos(theta + swirl) * r + driftX;
							double z = center.getZ() + Math.sin(theta + swirl) * r + driftZ;

							Location p = new Location(world, x, center.getY() + y, z);

							world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p, 1, 0.05, 0.05, 0.05, 0.005);
						}
					}
				}

				if (tick % 20 == 0) {
					world.playSound(center, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.25f, 1.0f);
				}
			}
		}.runTaskTimer(plugin, 0L, period);
	}

	public void spawnFullFireVeil(Main plugin, Block centerBlock, double radius, double heightSeconds,
			Particle particle) {
		final World world = centerBlock.getWorld();
		final Location center = centerBlock.getLocation().add(0.5, 0.0, 0.5);

		final int period = 2;
		final int totalTicks = 20 * 2;

		new BukkitRunnable() {
			int tick = 0;

			@Override
			public void run() {
				if (tick++ >= totalTicks) {
					cancel();
					return;
				}

				double prog = (double) tick / totalTicks;

				for (double r = 0; r <= radius; r += 0.35) {
					int points = Math.max(8, (int) (72 * (r / radius)));

					for (int i = 0; i < points; i++) {
						double theta = (2 * Math.PI) * i / points;

						double x = center.getX() + Math.cos(theta) * r;
						double z = center.getZ() + Math.sin(theta) * r;

						double wave = 0.15 * Math.sin(theta * 2.0 + prog * Math.PI * 2);

						Location p = new Location(world, x, center.getY() + 1 + wave, z);

						world.spawnParticle(particle, p, 1, 0.02, 0.02, 0.02, 0.0);
					}
				}

				if (tick % 10 == 0) {
					world.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.35f, 1.05f);
				}
			}
		}.runTaskTimer(plugin, 0L, period);
	}

	public void spawnPurpleBlackSwirl(Main plugin, Block centerBlock, double radius, Particle blackParticle,
			Particle purpleParticle, SpleefPlayer sp) {

		final World world = centerBlock.getWorld();
		final Location center = centerBlock.getLocation().add(0.5, 1.0, 0.5);

		final int period = 2;
		final int totalTicks = 30;

		new BukkitRunnable() {
			int tick = 0;

			@Override
			public void run() {
				if (sp.isRoundStarting()) {
					cancel();
					return;
				}

				if (tick++ >= totalTicks) {
					cancel();
					return;
				}

				double prog = (double) tick / totalTicks;
				double spin = prog * Math.PI * 6;

				// Bottom black filled circle - less dense
				for (double r = 0; r <= radius; r += 0.55) { // was 0.35
					int points = Math.max(6, (int) (36 * (r / radius))); // was 72

					for (int i = 0; i < points; i++) {
						double theta = (2 * Math.PI) * i / points;

						double x = center.getX() + Math.cos(theta) * r;
						double z = center.getZ() + Math.sin(theta) * r;

						Location p = new Location(world, x, center.getY() + 0.05, z);

						world.spawnParticle(blackParticle, p, 1, 0.01, 0.01, 0.01, 0.0);
					}
				}

				// Purple swirling layer - same outline, fewer points
				for (double r = 0.35; r <= radius; r += 0.45) { // was 0.25
					int arms = 3; // was 4

					for (int arm = 0; arm < arms; arm++) {
						double theta = spin + (arm * ((2 * Math.PI) / arms)) + r * 2.2;

						double x = center.getX() + Math.cos(theta) * r;
						double z = center.getZ() + Math.sin(theta) * r;

						double yWave = 0.12 * Math.sin(spin + r * 3.0);

						Location p = new Location(world, x, center.getY() + 0.35 + yWave, z);

						world.spawnParticle(purpleParticle, p, 1, 0.02, 0.02, 0.02, 0.0);
					}
				}

				if (tick % 10 == 0) {
					world.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.35f, 1.2f);
				}
			}
		}.runTaskTimer(plugin, 0L, period);
	}

	public void spawnFireVeil(Main main, Block center, double radius, Particle particleType) {
		new BukkitRunnable() {
			int tick = 0;

			@Override
			public void run() {
				if (tick++ >= (20 * 3)) {
					cancel();
					return;
				}

				double prog = (double) tick / (20 * 3);

				for (int i = 0; i < 72; i++) {
					Location particle = new Location(center.getWorld(), center.getLocation().getX(),
							center.getLocation().getY() + 1, center.getLocation().getZ());
					center.getWorld().spawnParticle(particleType, particle, 1, 0.02, 0.02, 0.02, 0.0);

				}

				if (tick % 10 == 0) {
					center.getWorld().playSound(center.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.35f, 1.05f);
				}
			}
		}.runTaskTimer(main, 0L, 2);
	}

	private Attribute resolveBlockReachAttr() {
		try {
			return Attribute.valueOf("BLOCK_INTERACTION_RANGE");
		} catch (IllegalArgumentException ignored) {
			/* old name */ }
		return Attribute.valueOf("PLAYER_BLOCK_INTERACTION_RANGE");
	}

	private void spawnRecallSpiral(Location loc, World world, int ticks) {
		double baseAngle = ticks * 0.18;
		double radius = 0.65;

		for (double y = 0; y <= 1.5; y += 0.15) {
			double angle = baseAngle + (y * 2.4);
			double x = loc.getX() + Math.cos(angle) * radius;
			double z = loc.getZ() + Math.sin(angle) * radius;

			Location spiralLoc = new Location(world, x, loc.getY() + y, z);

			world.spawnParticle(Particle.DUST, spiralLoc, 1, new Particle.DustOptions(Color.GREEN, 1.1F));
		}
	}

	public void addEggsFired(Connection connection, UUID uuid, int amount) {
		String sql = "UPDATE splegg_ffa_eggs_fired SET eggs_fired = eggs_fired + ? WHERE uuid = ?";

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, amount);
			ps.setString(2, uuid.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityInteract(PlayerInteractEntityEvent event) {
		Entity clicked = event.getRightClicked();

		// Cancel right-click interactions with paintings
		if (clicked instanceof Painting) {
			event.setCancelled(true);
			return;
		}

		// Cancel right-click interactions with boats
		if (clicked instanceof Boat) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPaintingBreak(HangingBreakByEntityEvent event) {
		// Prevent players from breaking paintings by hitting them
		if (event.getEntity() instanceof Painting && event.getRemover() instanceof Player
				&& ((Player) event.getRemover()).getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onLilyPadBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.LILY_PAD && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
		}
	}

	private boolean isPlant(Block block) {
		Material type = block.getType();

		// Potted plants
		BlockState state = block.getState();
		if (state instanceof FlowerPot) {
			return true;
		}

		// Common plant tags
		if (Tag.FLOWERS.isTagged(type))
			return true;
		if (Tag.SAPLINGS.isTagged(type))
			return true;
		if (Tag.CROPS.isTagged(type))
			return true;

		// Extra plant-like blocks not always covered the way you want
		switch (type) {
		case SHORT_GRASS:
		case FERN:
		case DEAD_BUSH:
		case TALL_GRASS:
		case LARGE_FERN:
		case SUGAR_CANE:
		case CACTUS:
		case BAMBOO:
		case BAMBOO_SAPLING:
		case KELP:
		case KELP_PLANT:
		case SEAGRASS:
		case TALL_SEAGRASS:
		case VINE:
		case GLOW_LICHEN:
		case SWEET_BERRY_BUSH:
		case NETHER_WART:
		case CRIMSON_FUNGUS:
		case WARPED_FUNGUS:
		case CRIMSON_ROOTS:
		case WARPED_ROOTS:
		case HANGING_ROOTS:
		case SPORE_BLOSSOM:
		case BIG_DRIPLEAF:
		case BIG_DRIPLEAF_STEM:
		case SMALL_DRIPLEAF:
		case LILY_PAD:
			return true;
		default:
			return false;
		}
	}

	public void createPlayerStatsIfNotExists(Connection connection, UUID uuid) {
		String uuidString = uuid.toString();

		String insertBlocks = "INSERT IGNORE INTO splegg_ffa_blocks_broken (uuid, blocks_broken) VALUES (?, 0)";
		String insertEggs = "INSERT IGNORE INTO splegg_ffa_eggs_fired (uuid, eggs_fired) VALUES (?, 0)";
		String insertWins = "INSERT IGNORE INTO splegg_ffa_wins (uuid, wins) VALUES (?, 0)";
		String insertPoints = "INSERT IGNORE INTO splegg_ffa_points (uuid, points) VALUES (?, 0)";

		try (PreparedStatement ps1 = connection.prepareStatement(insertBlocks);
				PreparedStatement ps2 = connection.prepareStatement(insertEggs);
				PreparedStatement ps3 = connection.prepareStatement(insertWins);
				PreparedStatement ps4 = connection.prepareStatement(insertPoints)) {
			ps1.setString(1, uuidString);
			ps2.setString(1, uuidString);
			ps3.setString(1, uuidString);
			ps4.setString(1, uuidString);

			ps1.executeUpdate();
			ps2.executeUpdate();
			ps3.executeUpdate();
			ps4.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		lastShot.remove(e.getPlayer().getUniqueId());
		main.getSpleggQueueManager().removePlayer(e.getPlayer().getUniqueId());
	}

}
