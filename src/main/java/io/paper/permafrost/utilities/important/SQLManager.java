package io.paper.permafrost.utilities.important;

import com.google.gson.JsonObject;

import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

public class SQLManager {
	private final PoolManager pool;

	private Main main;

	public SQLManager(Main plugin) {
		main = plugin;
		this.pool = new PoolManager(plugin);

	}

	public SpleefPlayer loadData(UUID uuid) {
		Connection conn = null;
		PreparedStatement ps = null;
		String ip = null;
		Date register_date = null;
		Date lastlogin = null;
		int onlinetime = 0;
		int spleef_elo = 1000;
		int splegg_elo = 1000;
		int tntrun_elo = 1000;
		String country = null;

		ResultSet resultSet = null;
		try {
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder = new StringBuilder();
			queryBuilder.append(
					"SELECT ip, register_date, onlinetime, language, country, last_login, spleef_elo, splegg_elo, tntrun_elo ");
			queryBuilder.append("FROM `player_data` ");
			queryBuilder.append("WHERE `uuid` = ? ");
			queryBuilder.append("LIMIT 1;");
			ps = conn.prepareStatement(queryBuilder.toString());
			ps.setString(1, uuid.toString());
			resultSet = ps.executeQuery();
			if (resultSet != null && resultSet.next()) {
				register_date = resultSet.getDate("register_date");
				lastlogin = resultSet.getDate("last_login");
				ip = resultSet.getString("ip");
				onlinetime = resultSet.getInt("onlinetime");
				country = resultSet.getString("country");
				spleef_elo = resultSet.getInt("spleef_elo");
				splegg_elo = resultSet.getInt("splegg_elo");
				tntrun_elo = resultSet.getInt("tntrun_elo");

			}
		} catch (SQLException sqlException) {
			sqlException.printStackTrace();
		} finally {
			this.pool.close(conn, ps, null);
		}
		final SpleefPlayer sp = new SpleefPlayer(main, uuid);
		sp.setTotalOnlineTime(onlinetime);
		sp.setIP(ip);
		sp.setRegisterDate(register_date);
		sp.setTotalOnlineTime(onlinetime);
		sp.setIP(ip);
		sp.setRegisterDate(register_date);
		sp.setELO(SpleefType.SPLEEF, spleef_elo);
		sp.setELO(SpleefType.SPLEGG, splegg_elo);
		sp.setELO(SpleefType.TNTRUN, tntrun_elo);

		if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
			Date now = new Date();
			sp.setLastLogin(now);
		} else {
			sp.setLastLogin(lastlogin);
		}
		sp.setCountry(country);

		return sp;
	}

	public void saveData(SpleefPlayer sp) {
		savePlayerData(sp);
	}

	private void savePlayerData(SpleefPlayer sp) {
		Connection conn = null;
		PreparedStatement ps = null;
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("UPDATE `player_data` SET ");
		queryBuilder.append(" name  = ?,  ip  = ?, ");
		queryBuilder.append(
				" onlinetime  = ?,  last_login  = ?, language  = ?,country  = ?, spleef_elo = ?, splegg_elo = ?, tntrun_elo = ?");
		queryBuilder.append(" WHERE uuid = ?;");

		try {
			ps = conn.prepareStatement(queryBuilder.toString());
			ps.setString(1, sp.getName());
			ps.setString(2, sp.getIP());
			ps.setInt(3, sp.getOnlineTime());
			ps.setDate(4, new java.sql.Date(sp.getLastLogin().getTime()));

			ps.setString(6, sp.getCountry());

			ps.setString(10, sp.getUUID().toString());
			ps.executeUpdate();
		} catch (SQLException sqlException) {
			sqlException.printStackTrace();
		} finally {
			this.pool.close(conn, ps, null);
		}
	}

	public LinkedHashMap<UUID, Integer> getRanking(SpleefType type) {
		Connection conn = null;
		PreparedStatement ps = null;
		LinkedHashMap<UUID, Integer> hashmap = new LinkedHashMap<UUID, Integer>();

		ResultSet resultSet = null;
		try {
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder = new StringBuilder();
			String column = type.toString().toLowerCase() + "_elo";
			queryBuilder.append(
					"select uuid," + column + " from player_data order by " + column + " DESC, last_login DESC;");
			ps = conn.prepareStatement(queryBuilder.toString());
			resultSet = ps.executeQuery();
			while (resultSet.next()) {
				UUID uuid = UUID.fromString(resultSet.getString("uuid"));
				int values = resultSet.getInt(column);
				hashmap.put(uuid, Integer.valueOf(values));
			}
		} catch (SQLException sqlException) {
			sqlException.printStackTrace();
		} finally {
			this.pool.close(conn, ps, null);
		}
		return hashmap;
	}

	public String languageFromCountry(String s) {
		if (s == null)
			return "ENGLISH";
		if (s.equalsIgnoreCase("AR") || s.equalsIgnoreCase("ES") || s.equalsIgnoreCase("BO") || s.equalsIgnoreCase("BR")
				|| s.equalsIgnoreCase("CL") || s.equalsIgnoreCase("UY") || s.equalsIgnoreCase("PY")
				|| s.equalsIgnoreCase("CO") || s.equalsIgnoreCase("PE") || s.equalsIgnoreCase("VE")
				|| s.equalsIgnoreCase("MX") || s.equalsIgnoreCase("CU") || s.equalsIgnoreCase("EC")
				|| s.equalsIgnoreCase("GT") || s.equalsIgnoreCase("HN"))
			return "SPANISH";
		if (s.equalsIgnoreCase("RU") || s.equalsIgnoreCase("BY") || s.equalsIgnoreCase("KZ") || s.equalsIgnoreCase("KG")
				|| s.equalsIgnoreCase("UZ"))
			return "RUSSIAN";
		return "ENGLISH";
	}

	@SuppressWarnings("deprecation")
	public String getCountry(String ip) throws IOException {
		if (ip == null)
			return null;
		String jsonS = "";
		URL url = null;
		try {
			ip = ip.replace("/", "");
			if (ip.equalsIgnoreCase("127.0.0.1") || ip.contains("192.168.0"))
				return "AR";
			url = new URL("http://ip-api.com/json/" + ip + "?fields=message,countryCode");
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return null;
		}
		URLConnection conn = null;
		try {
			conn = url.openConnection();
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		try {
			conn.connect();
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			jsonS = String.valueOf(jsonS) + inputLine;
		}
		JsonObject jsonObject = new JsonParser().parse(jsonS).getAsJsonObject();
		return jsonObject.get("countryCode").getAsString();
	}
}
