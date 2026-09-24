package io.paper.permafrost.commands;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.DataLoader;
import io.paper.permafrost.DataLoader.PowerStat;
import io.paper.permafrost.DataLoader.WinStat;
import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.ShovelPowers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class StatsCommand implements CommandExecutor {

	private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0");
	private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");

	private final Main main;
	private final DataLoader dataLoader;

	public StatsCommand(Main main) {
		this.main = main;
		this.dataLoader = main.getDataLoader();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("This command can only be used by players.");
			return true;
		}

		if (args.length == 0) {
			sendMainMenu(player);
			return true;
		}

		String category = args[0].toLowerCase(Locale.ROOT);

		switch (category) {
		case "powerspleef" -> {
			if (args.length >= 2 && args[1].equalsIgnoreCase("global")) {
				sendGlobalPowerspleefStats(player);
			} else {
				sendPersonalPowerspleefStats(player);
			}
		}

		case "rankedspleef" -> sendRankedSpleefStats(player);
		case "rankedsplegg" -> sendRankedSpleggStats(player);
		case "spleggffa" -> sendSpleggFFAStats(player);
		case "spleeffa" -> sendSpleefFFAStats(player);
		case "currencies" -> sendCurrencyStats(player);
		default -> sendMainMenu(player);
		}

		return true;
	}

	private void sendMainMenu(Player player) {
		sendSeparator(player);

		player.sendMessage(
				Component.text("Your Statistics")
						.color(NamedTextColor.AQUA)
						.decorate(TextDecoration.BOLD));

		player.sendMessage(Component.empty());

		player.sendMessage(createMenuButton(
				"Powerspleef",
				"/stats powerspleef",
				"View personal and global ranked power statistics."));

		player.sendMessage(createMenuButton(
				"Ranked Spleef",
				"/stats rankedspleef",
				"View your ranked Spleef ratings and record."));

		player.sendMessage(createMenuButton(
				"Ranked Splegg",
				"/stats rankedsplegg",
				"View your ranked Splegg rating and record."));

		player.sendMessage(createMenuButton(
				"Splegg FFA",
				"/stats spleggffa",
				"View your Splegg FFA statistics."));

		player.sendMessage(createMenuButton(
				"Spleef FFA",
				"/stats spleeffa",
				"View your Spleef FFA statistics."));

		player.sendMessage(createMenuButton(
				"Currencies",
				"/stats currencies",
				"View all your currency balances."));

		player.sendMessage(Component.empty());

		player.sendMessage(
				Component.text("Click a category to view it.")
						.color(NamedTextColor.GRAY));

		sendSeparator(player);
	}

	private void sendPersonalPowerspleefStats(Player player) {
		Map<ShovelPowers, PowerStat> stats =
				dataLoader.getPersonalPowerspleefPowerStats(player.getUniqueId());

		long totalPicks = stats.values().stream()
				.mapToLong(PowerStat::picks)
				.sum();

		long totalBans = stats.values().stream()
				.mapToLong(PowerStat::bans)
				.sum();

		sendSeparator(player);
		sendTitle(player, "Your Powerspleef Statistics");

		player.sendMessage(
				createTabButton(
						"Personal",
						"/stats powerspleef",
						true)
						.append(Component.space())
						.append(createTabButton(
								"Global",
								"/stats powerspleef global",
								false)));

		player.sendMessage(Component.empty());

		boolean hasStats = false;

		for (ShovelPowers power : ShovelPowers.values()) {
			if (power == ShovelPowers.NONE) {
				continue;
			}

			PowerStat stat = stats.getOrDefault(power, new PowerStat(0, 0));

			if (stat.picks() == 0 && stat.bans() == 0) {
				continue;
			}

			hasStats = true;

			double pickRate = calculateRate(stat.picks(), totalPicks);
			double banRate = calculateRate(stat.bans(), totalBans);

			sendPowerLine(
					player,
					formatPowerName(power),
					stat.picks(),
					pickRate,
					stat.bans(),
					banRate);
		}

		if (!hasStats) {
			player.sendMessage(
					Component.text("You do not have any ranked power statistics yet.")
							.color(NamedTextColor.GRAY));
		}

		player.sendMessage(Component.empty());
		sendBackButton(player);
		sendSeparator(player);
	}

	private void sendGlobalPowerspleefStats(Player player) {
		Map<ShovelPowers, PowerStat> stats =
				dataLoader.getGlobalPowerspleefPowerStats();

		long totalPicks = stats.values().stream()
				.mapToLong(PowerStat::picks)
				.sum();

		long totalBans = stats.values().stream()
				.mapToLong(PowerStat::bans)
				.sum();

		sendSeparator(player);
		sendTitle(player, "Global Powerspleef Statistics");

		player.sendMessage(
				createTabButton(
						"Personal",
						"/stats powerspleef",
						false)
						.append(Component.space())
						.append(createTabButton(
								"Global",
								"/stats powerspleef global",
								true)));

		player.sendMessage(Component.empty());

		boolean hasStats = false;

		for (ShovelPowers power : ShovelPowers.values()) {
			if (power == ShovelPowers.NONE) {
				continue;
			}

			PowerStat stat = stats.getOrDefault(power, new PowerStat(0, 0));

			if (stat.picks() == 0 && stat.bans() == 0) {
				continue;
			}

			hasStats = true;

			double pickRate = calculateRate(stat.picks(), totalPicks);
			double banRate = calculateRate(stat.bans(), totalBans);

			sendPowerLine(
					player,
					formatPowerName(power),
					stat.picks(),
					pickRate,
					stat.bans(),
					banRate);
		}

		if (!hasStats) {
			player.sendMessage(
					Component.text("No global ranked power statistics have been recorded yet.")
							.color(NamedTextColor.GRAY));
		}

		player.sendMessage(Component.empty());
		sendBackButton(player);
		sendSeparator(player);
	}

	private void sendRankedSpleefStats(Player player) {
		UUID uuid = player.getUniqueId();

		int airElo = dataLoader.getRankedAirSpleefElo(uuid);
		int miniElo = dataLoader.getRankedMiniSpleefElo(uuid);
		int powerElo = dataLoader.getRankedPowerSpleefElo(uuid);

		WinStat winStats = dataLoader.getRankedSpleefWinStats(uuid);

		sendSeparator(player);
		sendTitle(player, "Ranked Spleef Statistics");
		player.sendMessage(Component.empty());

		sendStatistic(player, "Air ELO", NUMBER_FORMAT.format(airElo));
		sendStatistic(player, "Mini ELO", NUMBER_FORMAT.format(miniElo));
		sendStatistic(player, "Power ELO", NUMBER_FORMAT.format(powerElo));

		player.sendMessage(Component.empty());

		sendStatistic(player, "Wins", NUMBER_FORMAT.format(winStats.wins()));
		sendStatistic(player, "Games Played", NUMBER_FORMAT.format(winStats.gamesPlayed()));
		sendStatistic(player, "Win Rate", formatPercent(winStats.getWinRate()));

		player.sendMessage(Component.empty());
		sendBackButton(player);
		sendSeparator(player);
	}

	private void sendRankedSpleggStats(Player player) {
		UUID uuid = player.getUniqueId();

		int elo = dataLoader.getRankedSpleggElo(uuid);
		WinStat winStats = dataLoader.getRankedSpleggWinStats(uuid);

		sendSeparator(player);
		sendTitle(player, "Ranked Splegg Statistics");
		player.sendMessage(Component.empty());

		sendStatistic(player, "Splegg ELO", NUMBER_FORMAT.format(elo));
		sendStatistic(player, "Wins", NUMBER_FORMAT.format(winStats.wins()));
		sendStatistic(player, "Games Played", NUMBER_FORMAT.format(winStats.gamesPlayed()));
		sendStatistic(player, "Win Rate", formatPercent(winStats.getWinRate()));

		player.sendMessage(Component.empty());
		sendBackButton(player);
		sendSeparator(player);
	}

	private void sendSpleggFFAStats(Player player) {
		UUID uuid = player.getUniqueId();

		int wins = dataLoader.getSpleggFFAWins(uuid);
		int blocksBroken = dataLoader.getSpleggFFABlocksBroken(uuid);
		int eggsFired = dataLoader.getSpleggFFAEggsFired(uuid);

		sendSeparator(player);
		sendTitle(player, "Splegg FFA Statistics");
		player.sendMessage(Component.empty());

		sendStatistic(player, "Wins", NUMBER_FORMAT.format(wins));
		sendStatistic(player, "Blocks Broken", NUMBER_FORMAT.format(blocksBroken));
		sendStatistic(player, "Eggs Fired", NUMBER_FORMAT.format(eggsFired));

		player.sendMessage(Component.empty());
		sendBackButton(player);
		sendSeparator(player);
	}

	private void sendSpleefFFAStats(Player player) {
		UUID uuid = player.getUniqueId();

		int wins = dataLoader.getSpleefFFAWins(uuid);
		int blocksBroken = dataLoader.getSpleefFFABlocksBroken(uuid);

		sendSeparator(player);
		sendTitle(player, "Spleef FFA Statistics");
		player.sendMessage(Component.empty());

		sendStatistic(player, "Wins", NUMBER_FORMAT.format(wins));
		sendStatistic(player, "Blocks Broken", NUMBER_FORMAT.format(blocksBroken));

		player.sendMessage(Component.empty());
		sendBackButton(player);
		sendSeparator(player);
	}

	private void sendCurrencyStats(Player player) {
		UUID uuid = player.getUniqueId();

		long shards = dataLoader.getShards(uuid);
		long shells = dataLoader.getPoints(uuid);
		long currency = dataLoader.getCurrency(uuid);

		sendSeparator(player);
		sendTitle(player, "Currencies");
		player.sendMessage(Component.empty());

		sendStatistic(player, "Shards", NUMBER_FORMAT.format(shards));
		sendStatistic(player, "Shells", NUMBER_FORMAT.format(shells));
		sendStatistic(player, "Currency", NUMBER_FORMAT.format(currency));

		player.sendMessage(Component.empty());
		sendBackButton(player);
		sendSeparator(player);
	}

	private void sendPowerLine(
			Player player,
			String powerName,
			long picks,
			double pickRate,
			long bans,
			double banRate) {

		Component line = Component.text(powerName)
				.color(NamedTextColor.AQUA)
				.append(Component.text("  "))
				.append(Component.text("Picks: ")
						.color(NamedTextColor.GRAY))
				.append(Component.text(NUMBER_FORMAT.format(picks))
						.color(NamedTextColor.WHITE))
				.append(Component.text(" (")
						.color(NamedTextColor.DARK_GRAY))
				.append(Component.text(formatPercent(pickRate))
						.color(NamedTextColor.WHITE))
				.append(Component.text(")")
						.color(NamedTextColor.DARK_GRAY))
				.append(Component.text("  "))
				.append(Component.text("Bans: ")
						.color(NamedTextColor.GRAY))
				.append(Component.text(NUMBER_FORMAT.format(bans))
						.color(NamedTextColor.WHITE))
				.append(Component.text(" (")
						.color(NamedTextColor.DARK_GRAY))
				.append(Component.text(formatPercent(banRate))
						.color(NamedTextColor.WHITE))
				.append(Component.text(")")
						.color(NamedTextColor.DARK_GRAY));

		player.sendMessage(line);
	}

	private void sendStatistic(Player player, String name, String value) {
		player.sendMessage(
				Component.text(name + ": ")
						.color(NamedTextColor.GRAY)
						.append(Component.text(value)
								.color(NamedTextColor.AQUA)));
	}

	private Component createMenuButton(String text, String command, String hoverText) {
		return Component.text("[" + text + "]")
				.color(NamedTextColor.AQUA)
				.decorate(TextDecoration.BOLD)
				.clickEvent(ClickEvent.runCommand(command))
				.hoverEvent(HoverEvent.showText(
						Component.text(hoverText)
								.color(NamedTextColor.GRAY)));
	}

	private Component createTabButton(String text, String command, boolean selected) {
		NamedTextColor color = selected
				? NamedTextColor.WHITE
				: NamedTextColor.AQUA;

		Component component = Component.text("[" + text + "]")
				.color(color)
				.decorate(TextDecoration.BOLD);

		if (!selected) {
			component = component
					.clickEvent(ClickEvent.runCommand(command))
					.hoverEvent(HoverEvent.showText(
							Component.text("Click to view " + text.toLowerCase(Locale.ROOT) + " statistics.")
									.color(NamedTextColor.GRAY)));
		}

		return component;
	}

	private void sendBackButton(Player player) {
		player.sendMessage(
				Component.text("[Back]")
						.color(NamedTextColor.AQUA)
						.decorate(TextDecoration.BOLD)
						.clickEvent(ClickEvent.runCommand("/stats"))
						.hoverEvent(HoverEvent.showText(
								Component.text("Return to the statistics menu.")
										.color(NamedTextColor.GRAY))));
	}

	private void sendTitle(Player player, String title) {
		player.sendMessage(
				Component.text(title)
						.color(NamedTextColor.AQUA)
						.decorate(TextDecoration.BOLD));
	}

	private void sendSeparator(Player player) {
		player.sendMessage(
				Component.text("--------------------------------")
						.color(NamedTextColor.DARK_GRAY));
	}

	private double calculateRate(long amount, long total) {
		if (total <= 0) {
			return 0.0;
		}

		return (amount * 100.0) / total;
	}

	private String formatPercent(double percentage) {
		return PERCENT_FORMAT.format(percentage) + "%";
	}

	private String formatPowerName(ShovelPowers power) {
		return switch (power) {
		case SPEED -> "Speed";
		case WINDCHARGE -> "Wind Charge";
		case BLACKGOO -> "Black Goo";
		case LEVITATION -> "Levitation";
		case ICEGRENADE -> "Ice Grenade";
		case BUILDER -> "Builder";
		case TNTRUN -> "TNT Run";
		case DECAYGRENADE -> "Decay Grenade";
		case SWAP -> "Swap";
		case SILENCER -> "Silencer";
		case DOUBLEJUMP -> "Double Jump";
		case SNARE -> "Snare";
		case RECALL -> "Recall";
		case SMOKEGRENADE -> "Smoke Grenade";
		case ROULETTE -> "Roulette";
		case ROULETTE2 -> "Roulette2";
		case VORTEXGRENADE -> "Vortex Grenade";
		case PURGINGSHIELD -> "Purging Shield";
		case ENDERPEARL -> "Ender Pearl";
		case JUMPPAD -> "Jump Pad";
		case NONE -> "None";
		default -> "None";
		};
	}
}
