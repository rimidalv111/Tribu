package graindcafe.tribu.Listeners;

import java.util.HashMap;
import java.util.Map.Entry;

import graindcafe.tribu.PlayerStats;
import graindcafe.tribu.Tribu;
import graindcafe.tribu.TribuZombie.CraftTribuZombie;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

public class TribuEntityListener implements Listener {
	private Tribu plugin;

	public TribuEntityListener(Tribu instance) {
		plugin = instance;
	}


	@EventHandler(priority = EventPriority.NORMAL)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if(!plugin.isCorrectWorld(event.getEntity().getWorld())) return; //world check
		if (plugin.isInsideLevel(event.getLocation()) && !plugin.getSpawner().justSpawned()) {
			event.setCancelled(true);
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent dam) {
		if (dam.isCancelled()) {
			return;
		}
		if(!plugin.isCorrectWorld(dam.getEntity().getWorld())) return; //world check
		if (dam.getEntity() instanceof Player) {
			Player p = (Player) dam.getEntity();
			if(plugin.isRunning())
			{
				if (plugin.isPlaying(p)) {
					if (p.getHealth() - dam.getDamage() <= 0) {
						dam.setCancelled(true);
						p.teleport(plugin.getLevel().getDeathSpawn());
						p.setHealth(1);
						if (!plugin.config().PlayersDontLooseItem)
						{
							for(ItemStack is : p.getInventory())
							{
								p.getLocation().getWorld().dropItemNaturally(p.getLocation(), is);
							}
							p.getInventory().clear();
						}
						
						plugin.setDead(p);
					}
				} else
					plugin.restoreInventory(p);
			}
		}
		if (dam.getEntity() instanceof CraftTribuZombie) {
			if (dam.getCause().equals(DamageCause.FIRE_TICK) && plugin.config().ZombiesFireResistant) {
				dam.setCancelled(true);
				dam.getEntity().setFireTicks(0);
				return;
			}

			if (plugin.isRunning()
					&& (dam.getCause() == DamageCause.ENTITY_ATTACK || dam.getCause() == DamageCause.PROJECTILE || dam.getCause() == DamageCause.POISON)) {
				EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) dam;

				CraftTribuZombie zomb = (CraftTribuZombie) event.getEntity();
				Player p = null;
				if (event.getDamager() instanceof Projectile) {
					Projectile pj = (Projectile) event.getDamager();
					if (pj.getShooter() instanceof Player)
						p = (Player) pj.getShooter();
				} else if (event.getDamager() instanceof Player) {
					p = (Player) event.getDamager();
				} else if (zomb.getTarget() instanceof Player)
					p = (Player) zomb.getTarget();
				if (p != null)
					zomb.addAttack(p, event.getDamage());
			}

		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if(!plugin.isCorrectWorld(event.getEntity().getWorld())) return; //world check
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			plugin.setDead(player);

			if (plugin.config().PlayersDontLooseItem)
			{
				plugin.keepTempInv((Player) event.getEntity(), event.getDrops().toArray(new ItemStack[] {}));
				event.getDrops().clear();
			}

		} else if (event.getEntity() instanceof CraftTribuZombie) {
			CraftTribuZombie zombie = (CraftTribuZombie) event.getEntity();

			HashMap<Player,Float> rewards=new HashMap<Player,Float>();
			String rewardMethod=plugin.config().StatsRewardMethod;
			boolean onlyForAlive=plugin.config().StatsRewardOnlyAlive;
			float baseMoney=(float)plugin.config().StatsOnZombieKillMoney;
			float basePoint=(float)plugin.config().StatsOnZombieKillPoints;
			if(rewardMethod.equalsIgnoreCase("Last"))
				rewards.put(zombie.getLastAttacker(),1f);
			else if(rewardMethod.equalsIgnoreCase("First"))
				rewards.put(zombie.getFirstAttacker(),1f);
			else if(rewardMethod.equalsIgnoreCase("Best"))
				rewards.put(zombie.getBestAttacker(),1f);
			else if(rewardMethod.equalsIgnoreCase("Percentage"))
				rewards.putAll(zombie.getAttackersPercentage());
			else if(rewardMethod.equalsIgnoreCase("All"))
				for(Player p : plugin.getPlayers())
				{
					rewards.put(p, 1f);
				}
			
			Player player;
			Float percentage;
			for(Entry<Player, Float> entry : rewards.entrySet())
			{
				player=entry.getKey();
				percentage=entry.getValue();
				if (player == null && zombie.getTarget() instanceof Player)
					player = (Player) zombie.getTarget();
				if (player != null && player.isOnline() && !(onlyForAlive && player.isDead())) {
					PlayerStats stats = plugin.getStats(player);
					if (stats != null) {
						stats.addMoney(Math.round(baseMoney*percentage));
						stats.addPoints(Math.round(basePoint*percentage));
						stats.msgStats();
						// Removed 24/06 : why is it here ?
						// plugin.getLevel().onWaveStart();
					}
				}
			}

			plugin.getSpawner().despawnZombie(zombie, event.getDrops());
		}
	}

	public void registerEvents(PluginManager pm) {
		pm.registerEvents(this, plugin);
	}
}