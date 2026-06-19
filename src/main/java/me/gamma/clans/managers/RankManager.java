package me.gamma.clans.managers;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Rank;
import me.gamma.clans.models.RankPermission;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RankManager {

	private final Clans plugin;
	private FileConfiguration ranksConfig;

	private final Map<Rank, Set<RankPermission>> permissionCache = new EnumMap<>(Rank.class);

	private final Map<Rank, String> displayNameCache = new EnumMap<>(Rank.class);

	private static final Map<String, Rank> KEY_TO_RANK = new LinkedHashMap<>();
	private static final Map<Rank, String> RANK_TO_KEY = new EnumMap<>(Rank.class);

	static {
		KEY_TO_RANK.put("leader", Rank.LEADER);
		KEY_TO_RANK.put("coleader", Rank.CO_LEADER);
		KEY_TO_RANK.put("captain", Rank.CAPTAIN);
		KEY_TO_RANK.put("member", Rank.MEMBER);

		RANK_TO_KEY.put(Rank.LEADER, "leader");
		RANK_TO_KEY.put(Rank.CO_LEADER, "coleader");
		RANK_TO_KEY.put(Rank.CAPTAIN, "captain");
		RANK_TO_KEY.put(Rank.MEMBER, "member");
	}

	public RankManager(Clans plugin) {
		this.plugin = plugin;
		load();
	}

	public void load() {
		File file = new File(plugin.getDataFolder(), "ranks.yml");
		if (!file.exists())
			plugin.saveResource("ranks.yml", false);

		ranksConfig = YamlConfiguration.loadConfiguration(file);

		java.io.InputStream stream = plugin.getResource("ranks.yml");
		if (stream != null) {
			ranksConfig.setDefaults(
					YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
		}

		rebuildCaches();
		plugin.getLogger().info("[gClans] ranks.yml cargado correctamente.");
	}

	public void reload() {
		load();
	}

	public boolean hasPermission(Rank rank, RankPermission permission) {
		if (rank == null || permission == null)
			return false;
		Set<RankPermission> perms = permissionCache.get(rank);
		if (perms == null)
			return false;
		return perms.contains(RankPermission.ALL) || perms.contains(permission);
	}

	public boolean hasPermission(Rank rank, String permissionName) {
		RankPermission rp = RankPermission.fromString(permissionName);
		return rp != null && hasPermission(rank, rp);
	}

	public String getDisplayName(Rank rank) {
		return displayNameCache.getOrDefault(rank, rank.getColoredName());
	}

	public String getPrefix(Rank rank) {
		String key = RANK_TO_KEY.get(rank);
		if (key == null)
			return "";
		String prefix = ranksConfig.getString("ranks." + key + ".prefix", "");
		return colorize(prefix);
	}

	private void rebuildCaches() {
		permissionCache.clear();
		displayNameCache.clear();

		for (Map.Entry<String, Rank> entry : KEY_TO_RANK.entrySet()) {
			String yamlKey = entry.getKey();
			Rank rank = entry.getValue();
			String path = "ranks." + yamlKey;

			String rawName = ranksConfig.getString(path + ".display-name", rank.getDisplayName());
			displayNameCache.put(rank, colorize(rawName));

			List<String> rawPerms = ranksConfig.getStringList(path + ".permissions");
			Set<RankPermission> perms = EnumSet.noneOf(RankPermission.class);

			for (String raw : rawPerms) {
				RankPermission rp = RankPermission.fromString(raw.trim());
				if (rp != null) {
					perms.add(rp);
				} else {
					plugin.getLogger()
							.warning("[gClans] Permiso desconocido en ranks.yml" + " para '" + yamlKey + "': " + raw);
				}
			}

			permissionCache.put(rank, perms);
		}
	}

	private static String colorize(String text) {
		return text == null ? "" : text.replace("&", "§");
	}
}