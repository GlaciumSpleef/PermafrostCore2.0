package io.paper.permafrost.utilities;

public enum ShardShovel {
	GLORY_RED("Glory Red", 1004),
	GLORY_BLUE("Glory Blue", 1006),
	GLORY_GREEN("Glory Green", 1009),
	GLORY_PURPLE("Glory Purple", 1008),
	GLORY_LIGHTBLUE("Glory Light blue", 1011),
	GLORY_PINK("Glory Pink", 1007),
	GLORY_BLACK("Glory Black", 1010),
	POLAR_SHOVEL("Polar Shovel", 1012);
	
	 private final String displayName;
	 private final int customModelData;

	    ShardShovel(String displayName, int customModelData) {
	        this.displayName = displayName;
	        this.customModelData = customModelData;
	    }

	    public String getDisplayName() {
	        return displayName;
	    }
	    
	    public static ShardShovel fromDisplayName(String name) {
	        for (ShardShovel rank : values()) {
	            if (rank.getDisplayName().equalsIgnoreCase(name)) {
	                return rank;
	            }
	        }
	        return null;
	    }
	    
	    public int getCustomModelData()  {
	    	return customModelData;
	    }
}
