package net.llamaslayers.minecraft.longsummerdays;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.ConfigurationNode;

public class LongSummerDays extends JavaPlugin implements Runnable {
	private static final long DAYTIME = 0;
	private static final long SUNSET = 12000;
	private static final long NIGHTTIME = 14000;
	private static final long SUNRISE = 22000;
	private static final long TOMORROW = 24000;
	// If time changes by more than an hour in a single tick, ignore it as someone
	// probably used the /time command.
	private static final long THRESHOLD = 1000;

	@Override
	public void onDisable() {
	}

	protected HashMap<String, Long> worlds;

	@Override
	public void onEnable() {
		checkConfig();

		worlds = new HashMap<String, Long>();
		for (World world : getServer().getWorlds()) {
			worlds.put(world.getName(), world.getFullTime());

			getServer().getLogger().info(
					"[Long Summer Days] Sunrise for "
							+ world.getName()
							+ " will last about "
							+ (int) Math.max(0,
									getMultiplier(world, SUNRISE) * 1.5)
							+ " minutes.");
			getServer().getLogger().info(
					"[Long Summer Days] Daytime for "
							+ world.getName()
							+ " will last about "
							+ (int) Math.max(0,
									getMultiplier(world, DAYTIME) * 10)
							+ " minutes.");
			getServer().getLogger().info(
					"[Long Summer Days] Sunset for "
							+ world.getName()
							+ " will last about "
							+ (int) Math.max(0,
									getMultiplier(world, SUNSET) * 1.5)
							+ " minutes.");
			getServer().getLogger().info(
					"[Long Summer Days] Nighttime for "
							+ world.getName()
							+ " will last about "
							+ (int) Math.max(0,
									getMultiplier(world, NIGHTTIME) * 7)
							+ " minutes.");
		}
		getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 1, 1);
	}

	private void checkConfig() {
		getConfiguration()
				.setHeader(
						"# Multipliers change how long each part of a day is. A higher",
						"# multiplier means that that piece of the day will be longer.",
						"# A multiplier of 1 makes time pass at a normal rate. Setting",
						"# all multipliers to 72 makes Minecraft days take almost",
						"# exactly the same amount of time as real days.",
						"#",
						"# The multipliers section is defaults. If you have multiple",
						"# worlds that should have the same multipliers, use that instead",
						"# of setting each of them in the worlds section.", "");
		getConfiguration().setProperty("multipliers.sunrise",
				getConfiguration().getDouble("multipliers.sunrise", 2.0));
		getConfiguration().setProperty("multipliers.daytime",
				getConfiguration().getDouble("multipliers.daytime", 1.0));
		getConfiguration().setProperty("multipliers.sunset",
				getConfiguration().getDouble("multipliers.sunset", 2.0));
		getConfiguration().setProperty("multipliers.nighttime",
				getConfiguration().getDouble("multipliers.nighttime", 0.5));
		Map<String, ConfigurationNode> worldConfig = getConfiguration()
				.getNodes("worlds");
		if (worldConfig == null || worldConfig.size() == 0) {
			getConfiguration()
					.setProperty("worlds.always_daytime.sunrise", 2.5);
			getConfiguration()
					.setProperty("worlds.always_daytime.daytime", 5.0);
			getConfiguration().setProperty("worlds.always_daytime.sunset", 2.5);
			getConfiguration().setProperty("worlds.always_daytime.nighttime",
					0.0);
		}
		getConfiguration().save();
	}

	@Override
	public void run() {
		for (World world : getServer().getWorlds()) {
			if (!worlds.containsKey(world.getName())) {
				worlds.put(world.getName(), world.getFullTime());
				continue;
			}
			long oldTime = worlds.get(world.getName());
			long timeDiff = world.getFullTime() - oldTime;

			if (timeDiff == 0) {
				continue;
			}

			if (timeDiff > THRESHOLD) {
				continue;
			}

			double multiplier;
			long end;
			long date = oldTime / 24000 * 24000;
			if (isSunrise(oldTime)) {
				multiplier = getMultiplier(world, SUNRISE);
				end = date + DAYTIME;
			} else if (isSunset(oldTime)) {
				multiplier = getMultiplier(world, SUNSET);
				end = date + NIGHTTIME;
			} else if (isDaytime(oldTime)) {
				multiplier = getMultiplier(world, DAYTIME);
				end = date + SUNSET;
			} else {
				multiplier = getMultiplier(world, NIGHTTIME);
				end = date + SUNRISE;
			}
			if (multiplier <= 0) {
				world.setFullTime(end);
				worlds.put(world.getName(), end);
			} else {
				long newTime = Math.min(end, oldTime
						+ (long) (timeDiff / multiplier));
				world.setFullTime(newTime);
				worlds.put(world.getName(), newTime);
			}
		}
	}

	private boolean isDaytime(long time) {
		long today = time % 24000;
		return today >= DAYTIME && today < SUNSET;
	}

	private boolean isSunrise(long time) {
		long today = time % 24000;
		return today >= SUNRISE && today < TOMORROW;
	}

	private boolean isSunset(long time) {
		long today = time % 24000;
		return today >= SUNSET && today < NIGHTTIME;
	}

	private double getMultiplier(World world, long type) {
		String worldName = world == null ? "" : world.getName();
		switch ((int) type) {
		case (int) SUNRISE:
			return getConfiguration().getDouble(
					"worlds." + worldName + ".sunrise",
					getConfiguration().getDouble("multipliers.sunrise", 2.0));
		case (int) DAYTIME:
			return getConfiguration().getDouble(
					"worlds." + worldName + ".daytime",
					getConfiguration().getDouble("multipliers.daytime", 1.0));
		case (int) SUNSET:
			return getConfiguration().getDouble(
					"worlds." + worldName + ".sunset",
					getConfiguration().getDouble("multipliers.sunset", 2.0));
		case (int) NIGHTTIME:
			return getConfiguration().getDouble(
					"worlds." + worldName + ".nighttime",
					getConfiguration().getDouble("multipliers.nighttime", 0.5));
		}
		return Double.NaN;
	}
}
