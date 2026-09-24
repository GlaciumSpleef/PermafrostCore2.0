package io.paper.permafrost.core.game;
import org.bukkit.Material;

public enum SpleefType {
	SPLEEF, SPLEGG, TNTRUN;
	
	public String getName() {
	    switch (this) {
	      default:
	        return null;
	      case SPLEEF:
	        return "§b§lSpleef";
	      case SPLEGG:
	        return "§e§lSplegg";
	      case TNTRUN:
	    	  return "§c§lTNT Run";
	    } 

	  }
	  
	  public int getId() {
	    switch (this) {
	      case SPLEEF:
	        return 178;
	      case SPLEGG:
	        return 174;
	      case TNTRUN:
	        return 173;
	    } 
	    return -1;
	  }
	  
	  public Material getMaterial(boolean ffa) {
	    Material material = Material.STONE;
	    switch (this) {
	      case SPLEEF:
	        material = ffa ? Material.SNOW_BLOCK : Material.DIAMOND_SHOVEL;
	        break;
	      case SPLEGG:
	        material = ffa ? Material.CAKE : Material.EGG;
	        break;
	      case TNTRUN:
	        material = ffa ? Material.TNT : Material.GUNPOWDER;
	        break;
	    } 
	    return material;
	  }
	}
