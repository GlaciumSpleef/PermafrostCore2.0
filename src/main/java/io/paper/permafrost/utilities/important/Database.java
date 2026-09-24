package io.paper.permafrost.utilities.important;

import java.sql.Connection;
import java.sql.DriverManager;

import com.google.gson.Gson;

public final class Database {
	private static final Database instance = new Database();
	
	String url = "jdbc:mysql://localhost:3306/minecraft_plugin";
	String user = "pancircus";
	String password = "Gmail120133_";

	
	private final Gson gson = new Gson();
	private Connection connection;
	private final CoreSettings settings;
	
	private Database() {
		this.settings = CoreSettings.getInstance();
	}
	
	public void load() {
		this.connect();
	}
	
	private void connect() {
	}
}
