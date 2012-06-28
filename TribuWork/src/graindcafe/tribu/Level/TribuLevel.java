package graindcafe.tribu.Level;

import graindcafe.tribu.Package;
import graindcafe.tribu.Tribu;
import graindcafe.tribu.Configuration.Constants;
import graindcafe.tribu.Signs.TribuSign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class TribuLevel {
	private ArrayList<Location> activeZombieSpawns;
	private boolean changed; // For deciding whether the level needs saving
								// again
	private Location deathSpawn;
	private Location initialSpawn;
	private String name;
	private Random rnd = new Random();
	private HashMap<Location, TribuSign> Signs;
	private HashMap<String, Location> zombieSpawns;
	private LinkedList<Package> Packages;

	public TribuLevel(String name, Location spawn) {
		this.zombieSpawns = new HashMap<String, Location>();
		this.activeZombieSpawns = new ArrayList<Location>();
		this.name = name;
		this.initialSpawn = spawn;
		this.deathSpawn = spawn;
		this.changed = false;
		this.Signs = new HashMap<Location, TribuSign>();
		this.Packages = new LinkedList<Package>();
	}
	
	public void activateZombieSpawn(String name) {
		for (String sname : zombieSpawns.keySet()) {
			if (sname.equalsIgnoreCase(name)) {
				Location spawn = zombieSpawns.get(sname);
				if (!this.activeZombieSpawns.contains(spawn))
					this.activeZombieSpawns.add(spawn);
				return;
			}
		}
	}

	public boolean addSign(TribuSign sign) {
		if (sign == null)
			return false;
		Signs.put(sign.getLocation(), sign);
		return true;

	}

	public void addZombieSpawn(Location loc, String name) {
		zombieSpawns.put(name, loc);
		activeZombieSpawns.add(loc);
		changed = true;
	}

	public void deactivateZombieSpawn(String name) {
		for (String sname : zombieSpawns.keySet()) {
			if (sname.equalsIgnoreCase(name)) {
				Location spawn = zombieSpawns.get(sname);
				if (this.activeZombieSpawns.contains(spawn))
					this.activeZombieSpawns.remove(spawn);
				return;
			}
		}
	}

	public Location getDeathSpawn() {
		return deathSpawn;
	}

	public Location getInitialSpawn() {
		return initialSpawn;
	}

	public String getName() {
		return name;
	}

	public Location getRandomZombieSpawn() {
		if (activeZombieSpawns.size() == 0) {
			return null;
		}
		return activeZombieSpawns.get(rnd.nextInt(activeZombieSpawns.size()));
	}

	public void addPackage(Package newPck) {
		if(newPck==null)
			return;
		String name=newPck.getName();
		for (Package cur : Packages)
			if (cur.getName().equalsIgnoreCase(name))
			{
				Packages.remove(cur);
				break;
			}
		this.Packages.add(newPck);
	}

	public Package getPackage(String name) {
		for (Package n : Packages)
			if (n.getName().equalsIgnoreCase(name))
				return n;
		return null;
	}

	public LinkedList<Package> getPackages() {
		return this.Packages;
	}

	public boolean removePackage(Package n) {
		return this.Packages.remove(n);
	}

	public boolean removePackage(String name) {
		return this.Packages.add(this.getPackage(name));
	}

	public String listPackages() {
		String str = "";
		if (!Packages.isEmpty()) {
			for (Package n : Packages)
				str += n.getName() + ", ";

			str = str.substring(0, str.length() - 2);
		}
		return str;
	}

	public TribuSign[] getSigns() {
		return Signs.values().toArray(new TribuSign[] {});
	}

	public HashMap<String, Location> getSpawns() {
		return zombieSpawns;
	}

	public Location getZombieSpawn(String name) {
		return zombieSpawns.get(name);
	}

	public void setChanged() {
		changed = true;
	}

	/**
	 * @return
	 */
	public boolean hasChanged() {
		return changed;
	}

	/**
	 * Initialize all signs of the level
	 */
	public void initSigns() {
		for (TribuSign s : Signs.values())
			s.init();
	}

	/**
	 * Is this sign part of this level ?
	 * 
	 * @param pos
	 *            Position of the sign
	 * @return
	 */
	public boolean isSpecialSign(Location pos) {
		return Signs.containsKey(pos);
	}

	/**
	 * List the spawns to a player
	 * 
	 * @param player
	 */
	public void listZombieSpawns(Player player) {
		Set<String> names = zombieSpawns.keySet();
		String nameList = "";
		String separator = "";
		for (String name : names) {
			nameList += separator + name;
			separator = ", ";
		}
		Tribu.messagePlayer(player,String.format(Constants.MessageZombieSpawnList, nameList),ChatColor.YELLOW);
	}

	/**
	 * On click
	 * 
	 * @param e
	 *            The player event that occurs
	 */
	public void onClick(PlayerInteractEvent e) {
		for (TribuSign s : Signs.values())
			if (s.isUsedEvent(e))
				s.raiseEvent(e);
	}

	/**
	 * When a redstone change
	 * 
	 * @param e
	 *            The redstone event that occurs
	 */
	public void onRedstoneChange(BlockRedstoneEvent e) {
		for (TribuSign s : Signs.values())
			if (s.isUsedEvent(e))
				s.raiseEvent(e);
	}

	/**
	 * When a sign is clicked on
	 * 
	 * @param e
	 *            The player event that occurs
	 */
	public void onSignClicked(PlayerInteractEvent e) {
		if (Signs.containsKey(e.getClickedBlock().getLocation())) {
			TribuSign ss = Signs.get(e.getClickedBlock().getLocation());
			if (ss.isUsedEvent(e))
				ss.raiseEvent(e);
		}
	}

	/**
	 * Actions to run at begining of a wave
	 */
	public void onWaveStart() {
		for (TribuSign s : Signs.values())
			if (s.isUsedEvent(null))
				s.raiseEvent(null);
	}

	/**
	 * Remove a sign
	 * 
	 * @param pos
	 *            Location of the sign to delete
	 * @return
	 */
	public boolean removeSign(Location pos) {
		if (Signs.containsKey(pos)) {
			removeSign(Signs.get(pos));
			return true;
		}
		return false;
	}

	/**
	 * Remove a sign
	 * 
	 * @param sign
	 *            The sign to delete from the level
	 */
	public void removeSign(TribuSign sign) {
		Signs.remove(sign.getLocation());
	}

	/**
	 * Remove a zombie spawn
	 * 
	 * @param name
	 *            Zombie spawn to be deleted
	 */
	public void removeZombieSpawn(String name) {
		zombieSpawns.remove(name);
		changed = true;
	}

	/**
	 * Set the death spawn of players (where they go after reviving)
	 * 
	 * @param loc
	 *            Location of the death spawn
	 * @return success or fail
	 */
	public boolean setDeathSpawn(Location loc) {
		if (loc.getWorld() == initialSpawn.getWorld()) {
			deathSpawn = loc;
			changed = true;
			return true;
		}
		return false;
	}

	/**
	 * Set the initial spawn of players
	 * 
	 * @param loc
	 *            Location of the initial spawn
	 * @return success or fail
	 */
	public boolean setInitialSpawn(Location loc) {
		if (loc.getWorld() == initialSpawn.getWorld()) {
			initialSpawn = loc;
			changed = true;
			return true;
		}
		return false;
	}

	/**
	 * Mark the level as just saved and to be save in a file
	 */
	public void setSaved() {
		changed = false;
	}

}
