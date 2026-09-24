package io.paper.permafrost.core.game;

import java.util.ArrayList;

import java.util.List;
import java.util.UUID;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.gui.AcceptDuelMenu;
import io.paper.permafrost.utilities.Utils;

public class SpleefDuel {
	private SpleefPlayer challenger;
	private List<SpleefPlayer> dueledPlayers;
	private List<SpleefPlayer> acceptedPlayers;
	private SpleefType type;
	private UUID uuid;
	private Main main;

	public SpleefDuel(Main main, SpleefPlayer challenger, List<SpleefPlayer> sp2, SpleefType type) {
		this.main = main;
		this.uuid = UUID.randomUUID();
		this.challenger = challenger;
		this.dueledPlayers = sp2;
		this.type = type;
		this.acceptedPlayers = new ArrayList<SpleefPlayer>();
		sendMessage();
	}

	public UUID getUUID() {
		return this.uuid;
	}

	public SpleefPlayer getChallenger() {
		return this.challenger;
	}

	public List<SpleefPlayer> getDueledPlayers() {
		return this.dueledPlayers;
	}

	public List<SpleefPlayer> getAcceptedPlayers() {
		return this.acceptedPlayers;
	}

	public List<SpleefPlayer> getAllPlayers() {
		List<SpleefPlayer> list = new ArrayList<SpleefPlayer>();
		list.addAll(this.dueledPlayers);
		list.add(this.challenger);
		return list;
	}

	public SpleefType getType() {
		return this.type;
	}

	@SuppressWarnings("deprecation")
	private void sendMessage() {
		if (dueledPlayers.contains(challenger))
			dueledPlayers.remove(challenger);

		if (dueledPlayers.size() == 1) {

			challenger.getPlayer().sendMessage("§bSent a duel request to §3§l" + dueledPlayers.get(0).getName()
					+ "§b for §3§l" + type.toString() + "!");
			dueledPlayers.get(0).getPlayer().sendMessage("§bThe player §3§l" + challenger.getName()
					+ " §bhas sent you a duel request for §3§l" + type.toString() + "!");

			dueledPlayers.get(0).getPlayer().spigot().sendMessage(getInvitation(challenger));
			new AcceptDuelMenu(main, dueledPlayers.get(0), this).o(dueledPlayers.get(0).getPlayer());
		} else {
			int size = (dueledPlayers.size() + 1) / 2;
			String mode = size + "VS" + size;
			challenger.getPlayer().sendMessage("§bYou have sent duel requests to multiple players for §3§l" + mode + "!");

			for (SpleefPlayer sp_2 : dueledPlayers) {
				sp_2.getPlayer().sendMessage("§bThe player §3§l" + challenger.getName()
						+ " §bhas sent you a duel request for §3§l" + mode + " §3§l" + type.toString() + "! §7");
				sp_2.getPlayer().spigot().sendMessage(getInvitation(challenger));
				new AcceptDuelMenu(main, sp_2, this).o(sp_2.getPlayer());
			}
		}

		TextComponent msg1 = new TextComponent("[CANCEL]");
		msg1.setColor(net.md_5.bungee.api.ChatColor.RED);
		msg1.setBold(true);
		msg1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hover duelcancel " + getUUID().toString()));
		msg1.setHoverEvent(
				new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§cCancel duel request").create()));
		challenger.getPlayer().spigot().sendMessage(msg1);
		challenger.getPlayer().closeInventory();
	}

	@SuppressWarnings("deprecation")
	private BaseComponent[] getInvitation(SpleefPlayer dueler) {
		TextComponent msg1 = new TextComponent("[ACCEPT]");
		msg1.setColor(net.md_5.bungee.api.ChatColor.GREEN);
		msg1.setBold(true);
		msg1.setClickEvent(
				new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hover duelaccept " + dueler.getPlayer().getName()));
		msg1.setHoverEvent(
				new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aAccept duel request").create()));
		TextComponent msg2 = new TextComponent("[DENY]");
		msg2.setColor(net.md_5.bungee.api.ChatColor.RED);
		msg2.setBold(true);
		msg2.setClickEvent(
				new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hover dueldeny " + dueler.getPlayer().getName()));
		msg2.setHoverEvent(
				new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§cReject duel request").create()));
		ComponentBuilder cb = new ComponentBuilder(msg1);
		cb.append(" ");
		cb.append(msg2);
		return cb.create();
	}

	public void acceptDuel(final SpleefPlayer sp) {
		SpleefDuel duel = this;
		(new BukkitRunnable() {
			@SuppressWarnings("deprecation")
			public void run() {
				final Player p = sp.getPlayer();
				if (SpleefDuel.this.getAcceptedPlayers().contains(sp)) {
					(new BukkitRunnable() {
						public void run() {
							p.sendMessage("§cYou already accepted this request.");
						}
					}).runTask(main);
					return;
				}
				SpleefDuel.this.getAcceptedPlayers().add(sp);
				if (SpleefDuel.this.getAcceptedPlayers().size() >= SpleefDuel.this.getDueledPlayers().size()) {
					challenger.getDuels().remove(duel);
					(new BukkitRunnable() {
						public void run() {
						main.getGameManager().duelGame(challenger, getDueledPlayers(), getType());
						}
					}).runTask(main);
				} else {

					final List<SpleefPlayer> list = new ArrayList<SpleefPlayer>();
					final List<SpleefPlayer> leftToAccept = new ArrayList<SpleefPlayer>();
					list.add(SpleefDuel.this.getChallenger());
					list.addAll(SpleefDuel.this.getDueledPlayers());

					for (SpleefPlayer splayer : SpleefDuel.this.getDueledPlayers()) {
						if (!SpleefDuel.this.getAcceptedPlayers().contains(splayer)) {
							leftToAccept.add(splayer);
						}
					}
					TextComponent msg1 = new TextComponent("[ACCEPT]");
					msg1.setColor(ChatColor.GREEN);
					msg1.setBold(Boolean.valueOf(true));
					msg1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
							"/hover duelaccept " + SpleefDuel.this.challenger.getPlayer().getName()));
					msg1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							(new ComponentBuilder("§aAccept duel request")).create()));
					TextComponent msg2 = new TextComponent("[DENY]");
					msg2.setColor(ChatColor.RED);
					msg2.setBold(Boolean.valueOf(true));
					msg2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
							"/hover dueldeny " + SpleefDuel.this.challenger.getPlayer().getName()));
					msg2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							(new ComponentBuilder("§cDeny duel request")).create()));

					ComponentBuilder cb = new ComponentBuilder(msg1);
					cb.append(" ");
					cb.append(msg2);
					final BaseComponent[] bc = cb.create();
					(new BukkitRunnable() {
						public void run() {
							for (SpleefPlayer dueled : list) {
								dueled.getPlayer().sendMessage(
										"§3§l" + sp.getName() + " §bhas accepted the request! §3§l(Left to accept: "
												+ Utils.getPlayerNamesFromList(leftToAccept) + ")");
								if (dueled != getChallenger() && !getAcceptedPlayers().contains(dueled)) {
									dueled.getPlayer().spigot().sendMessage(bc);
								}
							}
						}
					}).runTask(main);
				}

			}
		}).runTaskAsynchronously(main);
	}
}
