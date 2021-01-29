package com.programmerdan.minecraft.addgun.guns;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.programmerdan.minecraft.addgun.ArmorType;
import com.programmerdan.minecraft.addgun.ammo.AmmoType;

import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.MovingObjectPosition;
import net.minecraft.server.v1_16_R3.NBTBase;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.Vec3D;

/**
 * Collection of static utilities to help navigate the world of guns.
 * 
 * @author ProgrammerDan
 *
 */
public class Utilities {

	
	/**
	 * Using a Minecraft core utility, this determines where against a hitbox impact occurred, if at all.
	 * 
	 * See hitInformation() for more details.
	 * 
	 * @param origin The starting point of travel
	 * @param velocity The travel velocity
	 * @param entity The entity to test hit box against
	 * @return Location of impact, or the _origin_ if no hit detected.
	 */
	public static Location approximateHitBoxLocation(Location origin, Vector velocity, Entity entity) {
		
		Vec3D hit = hitInformation(origin, velocity, entity);

		if (hit == null) {
			return origin;
		} else {
			return new Location(origin.getWorld(), hit.getX(), hit.getY(), hit.getZ(), origin.getYaw(), origin.getPitch());
		}
	}
	
	public static HitDigest detailedHitBoxLocation(Location origin, Vector velocity, Entity entity) {
		Vec3D hit = hitInformation(origin, velocity, entity);
		
		if (hit == null) {
			return new HitDigest(HitPart.MISS, origin);
		} else {
			HitPart part = HitPart.MISS;
			net.minecraft.server.v1_16_R3.Entity hitentity = ((CraftEntity) entity).getHandle();
			//double locX = hitentity.locX;
			double locY = hitentity.locY();
			//double locZ = hitentity.locZ;
			//double hitX = hit.pos.x;
			double hitY = hit.getY();
			//double hitZ = hit.pos.z;
			double height = entity.getHeight();
			double head = hitentity.getHeadHeight();
			double midsection = (height - head > 0) ? (height - head) / 2 : height / 2;
			double legs = (height - head > 0) ? (height - head) / 5 : height / 5;

			if (hitY < locY + height && hitY >= locY + head) {
				part = HitPart.HEAD;
			} else if (hitY < locY + head && hitY >= locY + midsection) {
				part = HitPart.BODY;
			} else if (hitY < locY + midsection && hitY >= locY + legs) {
				part = HitPart.LEGS;
			} else if (hitY < locY + legs && hitY >= locY) {
				part = HitPart.FEET;
			}
			
			return new HitDigest(part, new Location(origin.getWorld(), hit.getX(), hit.getY(), hit.getZ(), origin.getYaw(), origin.getPitch()));
		}
	}
	
	/**
	 * Soft wrapper to convert Bukkit objects on vector and impact into a MovingObjectPosition representing
	 * if the passed entity was intersected by a vector "velocity" with origin of "origin". This assumes a 
	 * no-size projectile. Use other techniques for entity on entity boxtests.
	 * 
	 * @param origin Starting point of travel
	 * @param velocity Travel path
	 * @param entity The entity to test intersection against.
	 * @return a MovingObjectPosition with intersection details, or null.
	 */
	public static Vec3D hitInformation(Location origin, Vector velocity, Entity entity) {
		
		// we have the bullet's last tick location, its velocity, and the barycenter of the object it hit, and that
		// object's hitbox. We also know for sure that the object was intersected with.
		AxisAlignedBB boundingBox = ((CraftEntity) entity).getHandle().getBoundingBox();
		Vec3D origLocation = new Vec3D(origin.getX(), origin.getY(), origin.getZ());
		Vec3D origVector = new Vec3D(origin.getX() + velocity.getX(), origin.getY() + velocity.getY(), origin.getZ() + velocity.getZ());
		
		Optional<Vec3D> intersectLoc = boundingBox.b(origLocation, origVector);
		return intersectLoc.isPresent() ? intersectLoc.get() : null;
	}


	/**
	 * Originally adapted from 1.8 computation TODO: doublecheck still valid.
	 * 
	 * @param e The entity  whose current XP to compute
	 * @return the number of XP in hotbar right now.
	 */
	public static int computeTotalXP(LivingEntity e) {
		if (e instanceof Player) {
			Player p = (Player) e;
	        float cLevel = (float) p.getLevel();
	        float progress = p.getExp();
	        float a = 1f, b = 6f, c = 0f, x = 2f, y = 7f;
	        if (cLevel > 16 && cLevel <= 31) {
	                a = 2.5f; b = -40.5f; c = 360f; x = 5f; y = -38f;
	        } else if (cLevel >= 32) {
	                a = 4.5f; b = -162.5f; c = 2220f; x = 9f; y = -158f;
	        }
	        return (int) Math.floor(a * cLevel * cLevel + b * cLevel + c + progress * (x * cLevel + y));
		} else { 
			return 0; //TODO perhaps some fixed amount?
		}
	}
	
	/**
	 * Estimates the XP this entity has in inventory.
	 * 
	 * @param entity the entity to check
	 * @return how much XP is held
	 */
	public static int getInvXp(LivingEntity entity) {
		if (entity == null)
			return 0;

		ItemStack[] inv;
		if (entity instanceof InventoryHolder) {
			// complex inventory
			InventoryHolder holder = (InventoryHolder) entity;
			inv = holder.getInventory().getContents();
		} else {
			// simple inventory
			inv = entity.getEquipment().getArmorContents();
		}

		int total = 0;
		if (inv != null) {
			for (ItemStack item : inv) {
				if (Material.EXPERIENCE_BOTTLE.equals(item)) {
					total += item.getAmount();
				}
			}
		}
		return total;
	}


	/**
	 * Attempts to reduce the complexity of all materials to a more
	 * manageable pile of enumeration
	 * 
	 * @param material any material
	 * @return the rough armor grade, if any
	 */
	public static ArmorType getArmorType(Material material) {
		switch(material) {
		case IRON_HORSE_ARMOR:
			return ArmorType.IRON_BARDING;
		case GOLDEN_HORSE_ARMOR:
			return ArmorType.GOLD_BARDING;
		case DIAMOND_HORSE_ARMOR:
			return ArmorType.DIAMOND_BARDING;
		case LEATHER_BOOTS:
		case LEATHER_HELMET:
		case LEATHER_CHESTPLATE:
		case LEATHER_LEGGINGS:
			return ArmorType.LEATHER;
		case IRON_BOOTS:
		case IRON_HELMET:
		case IRON_CHESTPLATE:
		case IRON_LEGGINGS:
			return ArmorType.IRON;
		case GOLDEN_BOOTS:
		case GOLDEN_HELMET:
		case GOLDEN_CHESTPLATE:
		case GOLDEN_LEGGINGS:
			return ArmorType.GOLD;
		case DIAMOND_BOOTS:
		case DIAMOND_HELMET:
		case DIAMOND_CHESTPLATE:
		case DIAMOND_LEGGINGS:
			return ArmorType.DIAMOND;
		case CHAINMAIL_BOOTS:
		case CHAINMAIL_CHESTPLATE:
		case CHAINMAIL_HELMET:
		case CHAINMAIL_LEGGINGS:
			return ArmorType.CHAIN;
		case SHIELD:
			return ArmorType.SHIELD;
		case ELYTRA:
			return ArmorType.WINGS;
		default:
			return ArmorType.NONE;
		}
	}
	
	/**
	 * Supported data right now:
	 * 
	 * "ammo": String with unique Bullet name of the ammo loaded
	 * "magazine": String with unique magazine name of the magazine used, if applicable
	 * "rounds": Integer with # of bullets or size of magazine
	 * "type": AmmoType; stored as a string, but we convert for you
	 * "lifetimeShots": total shots fired over whole life, a Long
	 * "health": remaining shots until 0 health, basically a hidden durability.
	 * "owner": UUID of last user.
	 * "group": String value describing the Citadel group this gun is locked to, if supported. (TODO)
	 * 
	 * @param gun
	 * @return
	 */
	public static Map<String, Object> getGunData(ItemStack gun) {
		net.minecraft.server.v1_16_R3.ItemStack nmsGun = CraftItemStack.asNMSCopy(gun);
		Map<String, Object> gunMap = new HashMap<String, Object>();
		if (nmsGun.hasTag()) {
			NBTTagCompound compound = nmsGun.getTag();
			NBTBase gunDataStub = compound.get("GunData");
			if (gunDataStub != null) {
				NBTTagCompound gunData = (NBTTagCompound) gunDataStub;
				
				if (gunData.hasKey("ammo")) {
					gunMap.put("ammo", gunData.getString("ammo"));
				}
				
				if (gunData.hasKey("magazine")) {
					gunMap.put("magazine", gunData.getString("magazine"));
				}
				
				if (gunData.hasKey("rounds")) {
					gunMap.put("rounds", gunData.getInt("rounds"));
				}
				
				if (gunData.hasKey("type")) {
					gunMap.put("type", AmmoType.valueOf(gunData.getString("type")));
				}
				
				if (gunData.hasKey("lifetimeShots")) {
					gunMap.put("lifetimeShots", gunData.getLong("lifetimeShots"));
				}
				
				if (gunData.hasKey("health")) {
					gunMap.put("health", gunData.getInt("health"));
				}
				
				if (gunData.b("owner")) {
					gunMap.put("owner", gunData.a("owner"));
				}
				
				if (gunData.hasKey("group")) {
					gunMap.put("group", gunData.getString("group"));
				}
				
				if (gunData.hasKey("unid")) {
					gunMap.put("unid", gunData.getString("unid"));
				}
			}
		}
		return gunMap;
	}
	
	/**
	 * This will update the fields passed in via the map, leaving other data unmodified.
	 * 
	 * @param gun the gun item to update
	 * @param update the limited set of data to update, fields not in the map are unchanged. A field in the map with NULL as value is removed.
	 * @return the ItemStack, augmented
	 */
	public static ItemStack updateGunData(ItemStack gun, Map<String, Object> update) {
		net.minecraft.server.v1_16_R3.ItemStack nmsGun = CraftItemStack.asNMSCopy(gun);
		NBTTagCompound compound = nmsGun.hasTag() ? nmsGun.getTag() : new NBTTagCompound();
		NBTBase gunDataStub = compound.get("GunData");
		NBTTagCompound gunData = null;
		if (gunDataStub != null) {
			gunData = (NBTTagCompound) gunDataStub;
		} else {
			gunData = new NBTTagCompound();
		}
		
		if (update.containsKey("ammo")) {
			Object value = update.get("ammo");
			if (value == null) {
				gunData.remove("ammo");
			} else {
				gunData.setString("ammo", (String) value);
			}
		}
		
		if (update.containsKey("magazine")) {
			Object value = update.get("magazine");
			if (value == null) {
				gunData.remove("magazine");
			} else {
				gunData.setString("magazine", (String) value);
			}
		}
		
		if (update.containsKey("rounds")) {
			Object value = update.get("rounds");
			if (value == null) {
				gunData.remove("rounds");
			} else {
				gunData.setInt("rounds", (Integer) value);
			}
		}
		
		if (update.containsKey("type")) {
			Object value = update.get("type");
			if (value == null) {
				gunData.remove("type");
			} else {
				gunData.setString("type", ((AmmoType) value).toString());
			}
		}
		
		if (update.containsKey("lifetimeShots")) {
			Object value = update.get("lifetimeShots");
			if (value == null) {
				gunData.remove("lifetimeShots");
			} else {
				gunData.setLong("lifetimeShots", (Long) value);
			}
		}
		
		if (update.containsKey("health")) {
			Object value = update.get("health");
			if (value == null) {
				gunData.remove("health");
			} else {
				gunData.setInt("health", (Integer) value);
			}
		}
		
		if (update.containsKey("owner")) {
			Object value = update.get("owner");
			if (value == null) {
				gunData.remove("ownerMost"); // s + "Most" / "Least"
				gunData.remove("ownerLeast");
			} else {
				gunData.a("owner", (UUID) value);
			}
		}
		
		if (update.containsKey("group")) {
			Object value = update.get("group");
			if (value == null) {
				gunData.remove("group");
			} else {
				gunData.setString("group", (String) value);
			}
		}
		
		if (update.containsKey("unid")) {
			Object value = update.get("unid");
			if (value == null) {
				gunData.remove("unid");
			} else {
				gunData.setString("unid", (String) value);
			}
		}
		compound.set("GunData", gunData);
		nmsGun.setTag(compound);
		
		return CraftItemStack.asBukkitCopy(nmsGun);
	}
	
	
	
	
	/**
	 * Supported data right now:
	 * 
	 * "ammo": String with unique Bullet name of the ammo loaded
	 * "rounds": Integer with # of bullets in magazine
	 * 
	 * @param magazine
	 * @return
	 */
	public static Map<String, Object> getMagazineData(ItemStack magazine) {
		net.minecraft.server.v1_16_R3.ItemStack nmsMag = CraftItemStack.asNMSCopy(magazine);
		Map<String, Object> magazineMap = new HashMap<String, Object>();
		if (nmsMag.hasTag()) {
			NBTTagCompound compound = nmsMag.getTag();
			NBTBase magDataStub = compound.get("MagazineData");
			if (magDataStub != null) {
				NBTTagCompound gunData = (NBTTagCompound) magDataStub;
				
				if (gunData.hasKey("ammo")) {
					magazineMap.put("ammo", gunData.getString("ammo"));
				}
				
				if (gunData.hasKey("rounds")) {
					magazineMap.put("rounds", gunData.getInt("rounds"));
				}
				
				if (gunData.hasKey("unid")) {
					magazineMap.put("unid", gunData.getString("unid"));
				}
			}
		}
		return magazineMap;
	}
	
	/**
	 * This will update the fields passed in via the map, leaving other data unmodified.
	 * 
	 * @param magazine the magazine item to update
	 * @param update the limited set of data to update, fields not in the map are unchanged. A field in the map with NULL as value is removed.
	 * @return the ItemStack, augmented
	 */
	public static ItemStack updateMagazineData(ItemStack magazine, Map<String, Object> update) {
		net.minecraft.server.v1_16_R3.ItemStack nmsMagazine = CraftItemStack.asNMSCopy(magazine);
		NBTTagCompound compound = nmsMagazine.hasTag() ? nmsMagazine.getTag() : new NBTTagCompound();
		NBTBase magDataStub = compound.get("MagazineData");
		NBTTagCompound magData = null;
		if (magDataStub != null) {
			magData = (NBTTagCompound) magDataStub;
		} else {
			magData = new NBTTagCompound();
		}
		
		if (update.containsKey("ammo")) {
			Object value = update.get("ammo");
			if (value == null) {
				magData.remove("ammo");
			} else {
				magData.setString("ammo", (String) value);
			}
		}
		
		if (update.containsKey("rounds")) {
			Object value = update.get("rounds");
			if (value == null) {
				magData.remove("rounds");
			} else {
				magData.setInt("rounds", (Integer) value);
			}
		}
		
		if (update.containsKey("unid")) {
			Object value = update.get("unid");
			if (value == null) {
				magData.remove("unid");
			} else {
				magData.setString("unid", (String) value);
			}
		}

		compound.set("MagazineData", magData);
		nmsMagazine.setTag(compound);
		
		return CraftItemStack.asBukkitCopy(nmsMagazine);
	}
	
	/**
	 * private function to compute a soft sigmoid. Originally designed for time functions, can be used elsewhere
	 * 
	 * @param elapsed in fractions of a second
	 * @param asymptote fraction of a second of inflection
	 * @param y expansion factor, 0.25 for [0,.5] 0.5 for [0,1]
	 * @param spread smoothness of sigmoid, larger is a smoother curve (but high minimum) (2.5 - 5 is a good range)
	 * @return
	 */
	public static double sigmoid(double elapsed, double asymptote, double y, double spread) {
		double term = (elapsed - asymptote) / spread;
		return y + y * (term / Math.sqrt(1.0 + term * term));
	}
}
