package net.llamaslayers.minecraft.longsummerdays;

import java.util.HashMap;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class LongSummerDays extends JavaPlugin implements Runnable {
	private static final long SUNRISE = 4200;
	private static final long DAYTIME = 6000;
	private static final long SUNSET = 18000;
	private static final long NIGHTTIME = 19800;

	@Override
	public void onDisable() {
	}

	protected HashMap<World, Long> worlds;

	@Override
	public void onEnable() {
		checkConfig();

		getServer().getLogger().info(
				"[Long Summer Days] Sunrise will last about "
						+ Math.max(
								0,
								(int) (getConfiguration().getDouble(
										"multipliers.sunrise", 2.0) * 1.5))
						+ " minutes.");
		getServer().getLogger().info(
				"[Long Summer Days] Daytime will last about "
						+ Math.max(
								0,
								(int) (getConfiguration().getDouble(
										"multipliers.daytime", 1.0) * 10))
						+ " minutes.");
		getServer().getLogger().info(
				"[Long Summer Days] Sunset will last about "
						+ Math.max(
								0,
								(int) (getConfiguration().getDouble(
										"multipliers.sunset", 2.0) * 1.5))
						+ " minutes.");
		getServer().getLogger().info(
				"[Long Summer Days] Nighttime will last about "
						+ Math.max(
								0,
								(int) (getConfiguration().getDouble(
										"multipliers.nighttime", 0.5) * 7))
						+ " minutes.");

		worlds = new HashMap<World, Long>();
		for (World world : getServer().getWorlds()) {
			worlds.put(world, world.getFullTime());
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
						"# exactly the same amount of time as real days.", "");
		getConfiguration().setProperty("multipliers.sunrise",
				getConfiguration().getDouble("multipliers.sunrise", 2.0));
		getConfiguration().setProperty("multipliers.sunset",
				getConfiguration().getDouble("multipliers.sunset", 2.0));
		getConfiguration().setProperty("multipliers.daytime",
				getConfiguration().getDouble("multipliers.daytime", 1.0));
		getConfiguration().setProperty("multipliers.nighttime",
				getConfiguration().getDouble("multipliers.nighttime", 0.5));
		getConfiguration().save();
	}

	@Override
	public void run() {
		for (World world : getServer().getWorlds()) {
			if (!worlds.containsKey(world)) {
				worlds.put(world, world.getFullTime());
				continue;
			}
			long oldTime = worlds.get(world);
			worlds.put(world, world.getFullTime());
			long timeDiff = world.getFullTime() - oldTime;
			double multiplier;
			long end;
			if (isSunrise(oldTime)) {
				multiplier = getConfiguration().getDouble(
						"multipliers.sunrise", 2.0);
				end = DAYTIME;
			} else if (isSunset(oldTime)) {
				multiplier = getConfiguration().getDouble("multipliers.sunset",
						2.0);
				end = NIGHTTIME;
			} else if (isDaytime(oldTime)) {
				multiplier = getConfiguration().getDouble(
						"multipliers.daytime", 1.0);
				end = SUNSET;
			} else {
				multiplier = getConfiguration().getDouble(
						"multipliers.nighttime", 0.5);
				end = SUNRISE;
			}
			if (multiplier <= 0) {
				world.setTime(end);
			} else {
				world.setTime(oldTime + (long) (timeDiff / multiplier));
			}
		}
	}

	private boolean isDaytime(long time) {
		long today = time % 24000;
		return today >= DAYTIME && today < SUNSET;
	}

	private boolean isSunrise(long time) {
		long today = time % 24000;
		return today >= SUNRISE && today < DAYTIME;
	}

	private boolean isSunset(long time) {
		long today = time % 24000;
		return today >= SUNSET && today < NIGHTTIME;
	}
}
