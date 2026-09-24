package io.paper.permafrost.core.game;

import org.bukkit.Material;

public enum PowerType {

    SPEED(Material.SUGAR, "Speed", "Move faster around the arena", 10),
    DOUBLE_JUMP(Material.ELYTRA, "Double Jump", "Jump again mid-air", 12),
    RECALL(Material.RESPAWN_ANCHOR, "Recall", "Return to a saved position", 13),

    SNARE(Material.COBWEB, "Snare", "Trap enemies in place", 14),
    TNT(Material.TNT, "TNTRun", "Break terrain under enemies", 15),
    SWAP(Material.ENDER_PEARL, "Swap", "Swap positions with enemy", 16),
    BLIND(Material.BLACK_CONCRETE, "BlackGoo", "Disable your opponent with darkness", 17);

    private final Material material;
    private final String displayName;
    private final String description;
    private final int slot;

    PowerType(Material material, String displayName, String description, int slot) {
        this.material = material;
        this.displayName = displayName;
        this.description = description;
        this.slot = slot;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getSlot() {
        return slot;
    }

    public static PowerType getBySlot(int slot) {
        for (PowerType p : values()) {
            if (p.slot == slot) return p;
        }
        return null;
    }
}
