package io.paper.permafrost.utilities.important;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import io.paper.permafrost.Main;

public class PoolManager {
  private final Main plugin;
  
  private String hostname;
  
  private String port;
  
  private String database;
  
  private String username;
  
  private String password;
  
  private int minimumConnections;
  
  private int maximumConnections;
  
  private long connectionTimeout;
  
  private String testQuery;
  
  public PoolManager(Main plugin) {
    this.plugin = plugin;
    init();
  }
  
  private void init() {
    this.hostname = this.plugin.getConfig().getString("database.hostname");
    this.port = this.plugin.getConfig().getString("database.port");
    this.database = this.plugin.getConfig().getString("database.database");
    this.username = this.plugin.getConfig().getString("database.username");
    this.password = this.plugin.getConfig().getString("database.password");
    this.minimumConnections = this.plugin.getConfig().getInt("database.min-connections");
    this.maximumConnections = this.plugin.getConfig().getInt("database.max-connections");
    this.connectionTimeout = this.plugin.getConfig().getLong("database.timeout");
    this.testQuery = "";
  }
  public void close(Connection conn, PreparedStatement ps, ResultSet res) {
    if (conn != null)
      try {
        conn.close();
      } catch (SQLException sQLException) {} 
    if (ps != null)
      try {
        ps.close();
      } catch (SQLException sQLException) {} 
    if (res != null)
      try {
        res.close();
      } catch (SQLException sQLException) {} 
  }
 
}

