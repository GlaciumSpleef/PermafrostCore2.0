package io.paper.permafrost.utilities;

public enum ShellRank {
    FARMER("Farmer"),
    COOK("Cook"),
    CHEF("Chef"),
    MASTER("Master"),
    HUMPTY_DUMPTY("Humpty Dumpty"),
    RAMSAY("Ramsay"),
    HOT_N_SPICY("Hot'n'Spicy"),
    SPLEGG_HEAD("Splegg Head"),
    BACON("Bacon"),
    SUNNYSIDE("Sunnyside"),
    EASTER_BUNNY("Easter Bunny"),
    EGGSPERT("The Eggspert");

    private final String displayName;

    ShellRank(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public static ShellRank fromDisplayName(String name) {
        for (ShellRank rank : values()) {
            if (rank.getDisplayName().equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return null;
    }
}