package net.llamaslayers.minecraft.longsummerdays;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
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
	private static final Random random = new Random();

	@Override
	public void onDisable() {
	}

	protected HashMap<String, Long> worlds;
	protected HashMap<String, Long> leftover;
	protected HashMap<String, Long> real;
	protected HashMap<String, Long> fullTicks;
	protected HashMap<String, Long> specialTicks;
	protected HashMap<String, Integer> specialMode;

	@Override
	public void onEnable() {
		checkConfig();

		worlds = new HashMap<String, Long>();
		leftover = new HashMap<String, Long>();
		real = new HashMap<String, Long>();
		specialTicks = new HashMap<String, Long>();
		specialMode = new HashMap<String, Integer>();
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
						"# \"always_daytime\" is an example world. You may either input",
						"# your own worlds instead of \"always_daytime,\" or use the",
						"# multipliers, which affect every world not listed under",
						"# \"worlds:\". Any world listed under \"worlds:\" will not be",
						"# affected by the multipliers.", "");
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
				real.put(world.getName(), world.getFullTime());
				continue;
			}
			long oldTime = worlds.get(world.getName());
			long timeDiff = world.getFullTime() - oldTime;
			if (leftover.containsKey(world.getName())) {
				timeDiff += leftover.get(world.getName());
			}

			if (timeDiff == 0) {
				continue;
			}

			if (getConfiguration().getBoolean("redstonefix", true)) {
				if (real.containsKey(world.getName())) {
					long realTime = real.get(world.getName());
					long old = world.getFullTime();
					world.setFullTime(realTime + timeDiff);
					((CraftWorld) world).getHandle().a(false);
					real.put(world.getName(), realTime + timeDiff);
					world.setFullTime(old);
				} else {
					real.put(world.getName(), world.getFullTime());
				}
			}

			if (timeDiff > THRESHOLD) {
				worlds.put(world.getName(), world.getFullTime());
				continue;
			}

			double multiplier;
			long end;
			long date = oldTime / 24000 * 24000;
			if (date + 24000 < oldTime + timeDiff) {
				checkForRandomOccurance(world);
			}
			if (specialMode.containsKey(world.getName())) {
				if (!specialTicks.containsKey(world.getName())
						|| specialTicks.get(world.getName()) <= 1) {
					specialTicks.remove(world.getName());
					specialMode.remove(world.getName());
				} else {
					specialTicks.put(world.getName(),
							specialTicks.get(world.getName()) - 1);
					long newTime = 0;
					switch (specialMode.get(world.getName())) {
					case 1: // Blood moon - Only nighttime for 24h
						newTime = date
								+ (long) (NIGHTTIME - (SUNRISE - NIGHTTIME)
										* -specialTicks.get(world.getName())
												.doubleValue()
										/ fullTicks.get(world.getName())
												.doubleValue());
						break;
					case 2: // All day - Only daytime for 24h - NEEDS A BETTER COMMANDO NAME
						newTime = date
								+ (long) (DAYTIME - (SUNSET - DAYTIME)
										* -specialTicks.get(world.getName())
												.doubleValue()
										/ fullTicks.get(world.getName())
												.doubleValue());
						break;
					default:
						throw (RuntimeException) new RuntimeException(
								"Unknown special mode "
										+ specialMode.get(world.getName()))
								.fillInStackTrace();
					}
					world.setFullTime(newTime);
					worlds.put(world.getName(), newTime);
					return;
				}
			}
			if (isSunrise(oldTime)) {
				multiplier = getMultiplier(world, SUNRISE);
				end = date + TOMORROW;
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
				leftover.put(world.getName(), 0L);
			} else {
				if (timeDiff > 1 / multiplier) {
					long newTime = Math.min(end, oldTime
							+ (long) (timeDiff / multiplier));
					leftover.put(world.getName(), newTime == end ? 0
							: (timeDiff % Math.max(1, (long) (1 / multiplier))));
					world.setFullTime(newTime);
					worlds.put(world.getName(), newTime);
				} else {
					world.setFullTime(oldTime);
					worlds.put(world.getName(), oldTime);
					leftover.put(world.getName(), timeDiff);
				}
			}
		}
	}

	private void checkForRandomOccurance(World world) {
		String worldName = world == null ? "" : world.getName();
		int chances[] = new int[2];
		int totalChance = 0;
		int bloodMoonChance = getConfiguration().getInt(
				"worlds." + worldName + ".bloodmoonchance", 0);
		if (bloodMoonChance > 0) {
			chances[0] = bloodMoonChance;
			totalChance += bloodMoonChance;
		}
		int allDayChance = getConfiguration().getInt(
				"worlds." + worldName + ".alldaychance", 0);
		if (allDayChance > 0) {
			chances[1] = allDayChance;
			totalChance += allDayChance;
		}

		if (totalChance > 0) {
			int chosenChance = random.nextInt(totalChance);
			for (int i = 0; i < chances.length; i++) {
				if (chances[i] == 0) {
					continue;
				}
				if (chosenChance == 0) {
					specialTicks
							.put(worldName,
									(long) (1800
											* (getMultiplier(world, SUNRISE) + getMultiplier(
													world, SUNSET)) + 12000
											* getMultiplier(world, DAYTIME) + 8400 * getMultiplier(
											world, NIGHTTIME)));
					fullTicks
							.put(worldName,
									(long) (1800
											* (getMultiplier(world, SUNRISE) + getMultiplier(
													world, SUNSET)) + 12000
											* getMultiplier(world, DAYTIME) + 8400 * getMultiplier(
											world, NIGHTTIME)));
					specialMode.put(worldName, i + 1);
					break;
				}
				chosenChance -= chances[i];
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
		if (getConfiguration().getBoolean("worlds." + worldName + ".realistic",
				false))
			return getRealisticMultiplier(
					type,
					getConfiguration().getDouble(
							"worlds." + worldName + ".latitude", 0.0),
					getConfiguration().getDouble(
							"worlds." + worldName + ".longitude", 0.0));
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

	/**
	 * @see {@link http://en.wikipedia.org/wiki/Sunrise_equation}
	 */
	private double getRealisticMultiplier(long type, double latitude,
			double longitude) {
		long julian = (long) (System.currentTimeMillis() - 946684800000L - longitude / 360);
		double solarNoon = 0.0009 + longitude / 360 + julian;
		double solarMeanAnomaly = (357.5291 + 0.98560028 * solarNoon) % 360;
		double center = 1.9148 * Math.sin(solarMeanAnomaly / 180 * Math.PI)
				+ 0.02 * Math.sin(solarMeanAnomaly / 90 * Math.PI) + 0.0003
				* Math.sin(solarMeanAnomaly / 60 * Math.PI);
		double eclipticLongitude = (solarMeanAnomaly + 282.9372 + center) % 360;
		double transit = solarNoon + 0.0053
				* Math.sin(solarMeanAnomaly / 180 * Math.PI) - 0.0069
				* Math.sin(eclipticLongitude / 90 * Math.PI);
		double declination = Math.asin(Math.sin(eclipticLongitude / 180
				* Math.PI)
				* Math.sin(23.45 / 180 * Math.PI));
		double hourAngle = Math.acos((Math.sin(-0.83 / 180 * Math.PI) - Math
				.sin(latitude / 180 * Math.PI) * Math.sin(declination))
				/ (Math.cos(latitude / 180 * Math.PI) * Math.cos(declination)));
		double set = (0.0009 + (hourAngle / Math.PI / 2 + longitude / 360) - 0.0069 * Math
				.sin(eclipticLongitude / 90 * Math.PI));
		double rise = transit - (set - transit);
		set = set % 1;
		rise = rise % 1;

		// Whew!
		double dayMultiplier = (set - rise) * 141; // 10 minutes -> (24 hours - 30 minutes for sunrise and sunset)
		double sunMultiplier = 10; // Keep this constant at 15 minutes each for now
		double nightMultiplier = (1 - (set - rise)) * 1410 / 7; // 7 minutes -> (24 hours - 30 minutes for sunrise and sunset)

		return type == SUNRISE || type == SUNSET ? sunMultiplier
				: (type == DAYTIME ? dayMultiplier : nightMultiplier);
	}
}
