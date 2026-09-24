package io.paper.permafrost.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.mozilla.javascript.ast.Block;

import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

@SuppressWarnings("deprecation")
public class FAWEAPI {

	private HashMap<UUID, EditSession> sessions = new HashMap<UUID, EditSession>();

	public void placeBlocks(Location location1, Location location2, Material material) {
		EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location1.getWorld()));
		CuboidRegion region = new CuboidRegion(BukkitAdapter.asBlockVector(location1),
				BukkitAdapter.asBlockVector(location2));
		editSession.setBlocks((Region) region, BukkitAdapter.adapt(material.createBlockData()));
		editSession.flushQueue();
	}

	public void createFlatSnowPlane(Location corner1, Location corner2, Material m) {
		org.bukkit.World bukkitWorld = corner1.getWorld();
		if (bukkitWorld == null)
			return;
		com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(bukkitWorld);
		EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(corner1.getWorld()));
		BlockVector3 startPos = BlockVector3.at(corner1.getBlockX(), corner1.getBlockY(), corner1.getBlockZ());
		BlockVector3 endPos = BlockVector3.at(corner2.getBlockX(), corner2.getBlockY(), corner2.getBlockZ());
		CuboidRegion region = new CuboidRegion(startPos, endPos);
		BlockPattern pattern = new BlockPattern(BukkitAdapter.adapt(m.createBlockData()));
		editSession.setBlocks((Region) region, pattern);
		editSession.flushQueue();
		
	}
	
	public void createFlatSnowCircle(Location center, int radius, Material m) {
	    org.bukkit.World bukkitWorld = center.getWorld();
	    if (bukkitWorld == null) return;

	    EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(bukkitWorld));

	    int centerX = center.getBlockX();
	    int centerY = center.getBlockY();
	    int centerZ = center.getBlockZ();

	    double r = radius + 0.5;
	    double radiusSquared = r * r;

	    try {
	        for (int x = centerX - radius; x <= centerX + radius; x++) {
	            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
	                double dx = x - centerX;
	                double dz = z - centerZ;

	                if ((dx * dx) + (dz * dz) <= radiusSquared) {
	                    BlockVector3 pos = BlockVector3.at(x, centerY, z);
	                    editSession.setBlock(pos, BukkitAdapter.adapt(m.createBlockData()));
	                }
	            }
	        }
	    } finally {
	        editSession.flushQueue();
	        editSession.close();
	    }
	}

	public void placeBlocks(Location l1, Location l2, HashMap<Material, Double> materials) {
		EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(l1.getWorld()));
		CuboidRegion region = new CuboidRegion(BukkitAdapter.asBlockVector(l1), BukkitAdapter.asBlockVector(l2));
		RandomPattern pattern = new RandomPattern();
		for (Map.Entry<Material, Double> entry : materials.entrySet()) {
			Material material = entry.getKey();
			double i = entry.getValue();
			BlockPattern p = new BlockPattern(BukkitAdapter.adapt(material.createBlockData()));
			pattern.add(p, i);
		}

		editSession.setBlocks((Region) region, pattern);
		editSession.flushQueue();
	}

	public void replace(Location l1, Location l2, Material mask, HashMap<Material, Double> materials) {

		EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(l1.getWorld()));
		CuboidRegion region = new CuboidRegion(BukkitAdapter.asBlockVector(l1), BukkitAdapter.asBlockVector(l2));

		RandomPattern pattern = new RandomPattern();

		for (Map.Entry<Material, Double> entry : materials.entrySet()) {
			Material material = entry.getKey();
			double i = entry.getValue();
			BlockPattern p = new BlockPattern(BukkitAdapter.adapt(material.createBlockData()));
			pattern.add(p, i);
		}

		Mask blockMask = new BlockMask(editSession.getExtent(), new BaseBlock(mask.getId(), 0));
		editSession.replaceBlocks(region, blockMask, pattern);
		editSession.flushQueue();
	}

	public void replace(Location l1, Location l2, Material filter, Material replacement) {
		EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(l1.getWorld()));
		CuboidRegion region = new CuboidRegion(BukkitAdapter.asBlockVector(l1), BukkitAdapter.asBlockVector(l2));

		BaseBlock r = new BaseBlock(filter.getId(), 0);
		BaseBlock ff = new BaseBlock(replacement.getId(), 0);
		Set<BaseBlock> f = new HashSet<BaseBlock>();
		f.add(ff);
		editSession.replaceBlocks(region, f, r);
		editSession.flushQueue();
	}

	public UUID pasteSchematic(File file, Location location, boolean pasteAir) {

		BlockVector3 v = BukkitAdapter.asBlockVector(location);
		BukkitWorld w = new BukkitWorld(location.getWorld());
		UUID uuid = UUID.randomUUID();
		EditSession session = null;
		try {
			session = ClipboardFormats.findByFile(file).load(file).paste(w, v, true, pasteAir, (Transform) null);
			session.flushQueue();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sessions.put(uuid, session);

		return uuid;
	}

	public void undoSchematic(UUID uuid) {
		EditSession session = this.sessions.get(uuid);
		session.undo(session);
		session.flushQueue();
	}

}
