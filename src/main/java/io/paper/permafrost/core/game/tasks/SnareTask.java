package io.paper.permafrost.core.game.tasks;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.utilities.SpleefFFAMode;

public class SnareTask {
	private Main main;
	private UUID uuid;
	private Location loc;
	private String snareTag;
	private boolean snareReady;
	private boolean snareActive;
	private double seconds;
	private double timeSnared;
	private Player snared;

	public SnareTask(Main main, UUID uuid, Location loc, String snareTag) {
		this.main = main;
		this.uuid = uuid;
		this.loc = loc;
		this.snareTag = snareTag;
		this.snareReady = false;
		this.snareActive = false;
		this.seconds = 0;
		this.timeSnared = 0;

		startSnare();
	}

	public void startSnare() {
		Player player = Bukkit.getPlayer(uuid);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (player == null || !player.isOnline()) {
					removeThisSnare();
					cancel();
					return;
				}

				if (timeSnared == 0) {
					seconds += 0.1;
				} else {
					timeSnared += 0.1;
				}

				if (!snareReady && seconds >= 0.5) {
					snareReady = true;
				}

				// Remove trap 5 seconds after being triggered but not fully escaped
				if (timeSnared != 0 && timeSnared >= (seconds + 5)) {
					removeThisSnare();
					cancel();
					return;
				}

				// Remove trap after 15 second lifetime
				if (seconds >= 9.5) {
					removeThisSnare();
					cancel();
					return;
				}

				SpleefPlayer ownerSp = main.getDataLoader().getSpleefPlayer(player);

				// Remove trap during reset
				if (ownerSp != null && ownerSp.isRoundStarting()) {
					removeThisSnare();
					cancel();
					return;
				}

				World world = loc.getWorld();

				if (world == null) {
					removeThisSnare();
					cancel();
					return;
				}

				for (Player p : world.getPlayers()) {
					if (p.equals(player)) {
						continue;
					}

					SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);

					if (sp == null) {
						continue;
					}

					boolean validPlayer = (sp.getInGame() || sp.getInTeamGame()
							|| (main.getSpleefQueueManager().isAliveInAnyFFA(p.getUniqueId())
									&& main.getSpleefQueueManager().getFFAMode(p.getUniqueId())
											.equals(SpleefFFAMode.POWERSPLEEF)))
							&& !sp.isSpectating();

					if (!validPlayer) {
						main.getLogger().info("[SNARE-SKIP-INVALID-PLAYER] snareOwner=" + player.getName()
								+ " checkedPlayer=" + p.getName()
								+ " inGame=" + sp.getInGame()
								+ " inTeamGame=" + sp.getInTeamGame()
								+ " aliveFFA=" + main.getSpleefQueueManager().isAliveInAnyFFA(p.getUniqueId())
								+ " ffaMode=" + main.getSpleefQueueManager().getFFAMode(p.getUniqueId())
								+ " spectating=" + sp.isSpectating()
								+ " distance=" + p.getLocation().distance(loc)
								+ " snareReady=" + snareReady
								+ " snareActive=" + snareActive);
						continue;
					}

					// Player steps into trap radius - doubled from 1 to 2
					if ((p.getLocation().distance(loc) < 2) && snareReady && !snareActive && !main.getDataLoader().getProtectedPlayers().contains(p.getUniqueId())) {
						snareActive = true;
						timeSnared = seconds;
						snared = p;

						p.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "SNARE! " + ChatColor.GRAY
								+ "You triggered a shadow snare. Move too far and you'll be slowed.");
						p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 1.0f, 0.7f);
						p.showTitle(net.kyori.adventure.title.Title.title(
								net.kyori.adventure.text.Component.text("SNARED",
										net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY),
								net.kyori.adventure.text.Component.text("Stay close or be rooted.",
										net.kyori.adventure.text.format.NamedTextColor.GRAY),
								net.kyori.adventure.title.Title.Times.times(
										java.time.Duration.ofMillis(150),
										java.time.Duration.ofMillis(900),
										java.time.Duration.ofMillis(250))));

						if (player.isOnline()) {
							player.sendMessage(ChatColor.BLACK + "" + ChatColor.BOLD + "[Snare] " + ChatColor.RED
									+ p.getName() + ChatColor.GRAY + " stepped into your trap.");
							player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
						}
					}

					// Victim moved too far away after triggering it - doubled from 2 to 4
					if (snareActive && p.getLocation().distance(loc) > 3 && p.equals(snared) && !main.getDataLoader().getProtectedPlayers().contains(p.getUniqueId())) {
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
						p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));

						p.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "SLOWED! " + ChatColor.GRAY
								+ "The snare tightens around you.");
						p.playSound(p.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.8f);

						if (player.isOnline()) {
							player.sendMessage(ChatColor.BLACK + "" + ChatColor.BOLD + "[Snare] "
									+ ChatColor.RED + p.getName() + ChatColor.GRAY + " is now trapped.");
							player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.4f);
						}

						removeThisSnareVisualOnly();
						startSnaredEffects(p);
						startChainEffect();

						cancel();
						return;
					}
				}

				if (snareActive) {
					spawnSnareCircle(loc);
					spawnSnareCenter(loc);
				}
			}
		}.runTaskTimer(main, 0L, 2L);
	}

	private void startChainEffect() {
		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				ticks++;

				if (ticks >= 100) {
					removeThisSnare();
					cancel();
					return;
				}

				// Chain beam effect
				if (ticks % 2 == 0 && snared != null && snared.isOnline()) {
					Location playerLoc = snared.getLocation().clone().add(0, 1, 0);
					Location start = loc.clone().add(0, 0.2, 0);

					Vector direction = playerLoc.toVector().subtract(start.toVector());
					double length = direction.length();

					if (length > 0) {
						direction.normalize();

						double spacing = 0.50;

						for (double i = 0; i < length; i += spacing) {
							Location point = start.clone().add(direction.clone().multiply(i));
							point.getWorld().spawnParticle(Particle.DUST, point, 1,
									new Particle.DustOptions(Color.BLACK, 1.2F));
						}
					}
				}

				// Small burst around trapped player
				if (ticks % 5 == 0 && snared != null && snared.isOnline()) {
					spawnTrapBurst(snared.getLocation());
				}
			}
		}.runTaskTimer(main, 0L, 1L);
	}

	private void startSnaredEffects(Player target) {
		new BukkitRunnable() {
			int ticksLeft = 100;

			@Override
			public void run() {
				if (target == null || !target.isOnline()) {
					cancel();
					return;
				}

				if (ticksLeft <= 0) {
					target.sendActionBar(net.kyori.adventure.text.Component.text("You are no longer slowed.",
							net.kyori.adventure.text.format.NamedTextColor.GRAY));
					target.sendMessage(ChatColor.GREEN + "You broke free from the snare.");
					target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
					cancel();
					return;
				}

				double secondsLeft = ticksLeft / 20.0;

				target.sendActionBar(net.kyori.adventure.text.Component.text(
						"Slowed for " + String.format("%.1f", secondsLeft) + "s",
						net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));

				if (ticksLeft == 100 || ticksLeft == 60 || ticksLeft == 20) {
					target.showTitle(net.kyori.adventure.title.Title.title(
							net.kyori.adventure.text.Component.text("SLOWED",
									net.kyori.adventure.text.format.NamedTextColor.DARK_RED),
							net.kyori.adventure.text.Component.text(
									String.format("%.1f seconds remaining", secondsLeft),
									net.kyori.adventure.text.format.NamedTextColor.GRAY),
							net.kyori.adventure.title.Title.Times.times(
									java.time.Duration.ofMillis(100),
									java.time.Duration.ofMillis(400),
									java.time.Duration.ofMillis(150))));
				}

				ticksLeft--;
			}
		}.runTaskTimer(main, 0L, 1L);
	}

	public void spawnSnareCircle(Location loc) {
		double radius = 3.0;
		int points = 60;

		World world = loc.getWorld();

		if (world == null) {
			return;
		}

		for (int i = 0; i < points; i++) {
			double angle = 2 * Math.PI * i / points;

			double x = loc.getX() + radius * Math.cos(angle);
			double z = loc.getZ() + radius * Math.sin(angle);

			Location particleLoc = new Location(world, x, loc.getY() + 0.05, z);

			world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(Color.BLACK, 1.5F));
		}
	}

	public void spawnSnareCenter(Location loc) {
		World world = loc.getWorld();

		if (world == null) {
			return;
		}

		world.spawnParticle(Particle.SMOKE, loc.clone().add(0, 0.15, 0), 2, 0.12, 0.02, 0.12, 0.0);

		world.spawnParticle(Particle.DUST, loc.clone().add(0, 0.1, 0), 2, 0.08, 0.01, 0.08,
				new Particle.DustOptions(Color.BLACK, 1.0F));
	}

	public void spawnTrapBurst(Location loc) {
		World world = loc.getWorld();

		if (world == null) {
			return;
		}

		world.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 12, 0.35, 0.45, 0.35,
				new Particle.DustOptions(Color.BLACK, 1.2F));

		world.spawnParticle(Particle.SMOKE, loc.clone().add(0, 1, 0), 8, 0.25, 0.35, 0.25, 0.0);
	}

	private void removeThisSnare() {
		removeThisSnareVisualOnly();
		main.getDataLoader().setSnareUsed(uuid, false);
	}

	private void removeThisSnareVisualOnly() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				"kill @e[type=block_display,tag=" + snareTag + "]");
	}
}
