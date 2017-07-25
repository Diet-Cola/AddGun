package com.programmerdan.minecraft.addgun;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.reflect.ClassPath;
import com.programmerdan.minecraft.addgun.commands.CommandHandler;
import com.programmerdan.minecraft.addgun.guns.BasicGun;
import com.programmerdan.minecraft.addgun.listeners.PlayerListener;

public class AddGun  extends JavaPlugin {
	private static AddGun instance;
	private CommandHandler commandHandler;
	private PlayerListener playerListener;
	
	private Map<String, BasicGun> guns;
	
	@Override
	public void onEnable() {
		super.onEnable();

		saveDefaultConfig();
		reloadConfig();
		
		AddGun.instance = this;
		guns = new ConcurrentHashMap<String, BasicGun>();

		addGuns(getConfig());

		registerPlayerListener();
		registerCommandHandler();
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		
		if (this.playerListener != null) this.playerListener.shutdown();
	}
	
	public PlayerListener getPlayerListener() {
		return this.playerListener;
	}
	
	public CommandHandler getCommandHandler() {
		return this.commandHandler;
	}
	
	
	private void registerCommandHandler() {
		if (!this.isEnabled()) return;
		try {
			this.commandHandler = new CommandHandler(getConfig());
		} catch (Exception e) {
			this.severe("Failed to set up command handling", e);
			this.setEnabled(false);
		}
	}

	private void registerPlayerListener() {
		if (!this.isEnabled()) return;
		try {
			this.playerListener = new PlayerListener(getConfig());
		} catch (Exception e) {
			this.severe("Failed to set up player event capture / handling", e);
			this.setEnabled(false);
		}	
	}
	
	private void addGuns(FileConfiguration config) {
		ConfigurationSection guns = config.getConfigurationSection("guns");
		if (guns == null || guns.getKeys(false) == null) {
			this.warning("No guns enabled!");
			return;
		}
		
		// load all possible guns
		Map<String, BasicGun> possibleGuns = new HashMap<String, BasicGun>();
		
		try {
			ClassPath getSamplersPath = ClassPath.from(this.getClassLoader());

			for (ClassPath.ClassInfo clsInfo : getSamplersPath.getTopLevelClasses("com.programmerdan.minecraft.addgun.guns.impl")) {
				Class<?> clazz = clsInfo.load();
				info("Found gun {0}, attempting to find a suitable constructor", clazz.getName());
				if (clazz != null && BasicGun.class.isAssignableFrom(clazz)) {
					BasicGun basicGun = null;
					try {
						Constructor<?> constructBasic = clazz.getConstructor();
						basicGun = (BasicGun) constructBasic.newInstance();
						possibleGuns.put(basicGun.getName(), basicGun);
						info("Created a new Gun Manager for guns of type {0}", clazz.getName());
					} catch (Exception e) {}
				}
			}
		} catch (IOException ioe) {
			warning("Failed to load any guns, due to IO error", ioe);
		}
		
		// configure guns desired (which enables them)
		for (String gun : guns.getKeys(false)) {
			try {
				BasicGun possibleGun = possibleGuns.get(gun);
				if (possibleGun == null) {
					warning("Gun {0} configured, but not found to be available.", gun);
				} else {
					possibleGun.configure(guns.getConfigurationSection(gun));
					if (possibleGun.isListener()) {
						this.getServer().getPluginManager().registerEvents((Listener) possibleGun, this);
					}
					this.guns.put(possibleGun.getName(), possibleGun);
					info("Configured gun {0} for use", gun);
				}
			} catch (Exception e) {
				warning("Gun {0} failed during setup", gun);
				warning("Exception trapped: ", e);
			}
		}
	}

	public Set<String> getGuns() {
		return guns.keySet();
	}
	
	public BasicGun getGun(String name) {
		return guns.get(name);
	}
	
	/**
	 * 
	 * @return the static global instance. Not my fav pattern, but whatever.
	 */
	public static AddGun getPlugin() {
		return AddGun.instance;
	}

	/**
	 * Simple SEVERE level logging.
	 */
	public void severe(String message) {
		getLogger().log(Level.SEVERE, message);
	}

	/**
	 * Simple SEVERE level logging with Throwable record.
	 */
	public void severe(String message, Throwable error) {
		getLogger().log(Level.SEVERE, message, error);
	}

	/**
	 * Simple WARNING level logging.
	 */
	public void warning(String message) {
		getLogger().log(Level.WARNING, message);
	}

	/**
	 * Simple WARNING level logging with Throwable record.
	 */
	public void warning(String message, Throwable error) {
		getLogger().log(Level.WARNING, message, error);
	}

	/**
	 * Simple WARNING level logging with ellipsis notation shortcut for deferred injection argument array.
	 */
	public void warning(String message, Object... vars) {
		getLogger().log(Level.WARNING, message, vars);
	}

	/**
	 * Simple INFO level logging
	 */
	public void info(String message) {
		getLogger().log(Level.INFO, message);
	}

	/**
	 * Simple INFO level logging with ellipsis notation shortcut for deferred injection argument array.
	 */
	public void info(String message, Object... vars) {
		getLogger().log(Level.INFO, message, vars);
	}
	
	/**
	 * Toggle debug live
	 * 
	 * @param state true for on, false for off.
	 */
	public void setDebug(boolean state) {
		if (state) {
			getConfig().set("debug", true);
		} else {
			getConfig().set("debug", false);
		}
	}

	/**
	 * Live on/off debug message at INFO level.
	 *
	 * Skipped if `debug` in root config is false.
	 */
	public void debug(String message) {
		if (getConfig() != null && getConfig().getBoolean("debug", false)) {
			getLogger().log(Level.INFO, message);
		}
	}

	/**
	 * Live on/off debug message  at INFO level with ellipsis notation
	 * shortcut for deferred injection argument array.
	 *
	 * Skipped if `debug` in root config is false.
	 */
	public void debug(String message, Object... vars) {
		if (getConfig() != null && getConfig().getBoolean("debug", false)) {
			getLogger().log(Level.INFO, message, vars);
		}
	}

}
