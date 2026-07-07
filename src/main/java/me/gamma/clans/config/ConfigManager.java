package me.gamma.clans.config;

import me.gamma.clans.Clans;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

	private final Clans plugin;
	private FileConfiguration config;
	private FileConfiguration messages;
	private FileConfiguration help;
	private FileConfiguration adminHelp;
	private File configFile;
	private File messagesFile;
	private File helpFile;
	private File adminHelpFile;

	public ConfigManager(Clans plugin) {
		this.plugin = plugin;
	}

	public void loadAll() {
		loadConfig();
		loadMessages();
		loadHelp();
		loadAdminHelp();
	}

	public void reloadAll() {
		loadAll();
	}

	private void loadConfig() {
		configFile = new File(plugin.getDataFolder(), "config.yml");
		if (!configFile.exists())
			plugin.saveResource("config.yml", false);
		config = YamlConfiguration.loadConfiguration(configFile);
		mergeDefaults(config, "config.yml");
	}

	private void loadMessages() {
		messagesFile = new File(plugin.getDataFolder(), "messages.yml");
		if (!messagesFile.exists())
			plugin.saveResource("messages.yml", false);
		messages = YamlConfiguration.loadConfiguration(messagesFile);
		mergeDefaults(messages, "messages.yml");
	}

	private void loadHelp() {
		helpFile = new File(plugin.getDataFolder(), "help.yml");
		if (!helpFile.exists())
			plugin.saveResource("help.yml", false);
		help = YamlConfiguration.loadConfiguration(helpFile);
		mergeDefaults(help, "help.yml");
	}

	private void loadAdminHelp() {
		adminHelpFile = new File(plugin.getDataFolder(), "admin_help.yml");
		if (!adminHelpFile.exists())
			plugin.saveResource("admin_help.yml", false);
		adminHelp = YamlConfiguration.loadConfiguration(adminHelpFile);
		mergeDefaults(adminHelp, "admin_help.yml");
	}

	private void mergeDefaults(FileConfiguration cfg, String resource) {
		java.io.InputStream stream = plugin.getResource(resource);
		if (stream == null)
			return;
		cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
	}

	public String getDbType() {
		return config.getString("database.type", "sqlite").toLowerCase();
	}

	public String getSqliteFile() {
		return config.getString("database.sqlite.file-name", "gclans.db");
	}

	public String getMysqlHost() {
		return config.getString("database.mysql.host", "localhost");
	}

	public int getMysqlPort() {
		return config.getInt("database.mysql.port", 3306);
	}

	public String getMysqlDatabase() {
		return config.getString("database.mysql.database", "gclans");
	}

	public String getMysqlUser() {
		return config.getString("database.mysql.username", "root");
	}

	public String getMysqlPassword() {
		return config.getString("database.mysql.password", "");
	}

	public int getMysqlPoolSize() {
		return config.getInt("database.mysql.pool-size", 10);
	}

	public String getTableClans() {
		return config.getString("database.tables.clans", "gclans_clans");
	}

	public String getTableMembers() {
		return config.getString("database.tables.members", "gclans_members");
	}

	public String getTableAllies() {
		return config.getString("database.tables.allies", "gclans_allies");
	}

	public int getNameMin() {
		return config.getInt("clans.name-min-length", 3);
	}

	public int getNameMax() {
		return config.getInt("clans.name-max-length", 8);
	}

	public String getNameRegex() {
		return config.getString("clans.name-regex", "^[a-zA-Z0-9_]+$");
	}

	public java.util.List<String> getNameBlacklist() {
		return config.getStringList("clans.name-blacklist");
	}

	public int getPrefixMin() {
		return config.getInt("clans.prefix-min-length", 2);
	}

	public int getPrefixMax() {
		return config.getInt("clans.prefix-max-length", 6);
	}

	public java.util.List<String> getPrefixBlockedCodes() {
		return config.getStringList("clans.prefix-blocked-codes");
	}

	public boolean requirePrefixOnCreate() {
		return config.getBoolean("clans.require-prefix-on-creation", false);
	}

	public String getDefaultColor() {
		return config.getString("clans.default-color", "e");
	}

	public int getDefaultSlots() {
		return config.getInt("clans.default-slots", 8);
	}

	public int getMaxAllies() {
		return config.getInt("clans.max-allies", 4);
	}

	public double getKillPoints() {
		return config.getDouble("points.kill-points", 1.0);
	}

	public double getDeathPoints() {
		return config.getDouble("points.death-points", 0.5);
	}

	public int getXpPerKill() {
		return config.getInt("levels.xp-per-kill", 1);
	}

	public String getXpFormula() {
		return config.getString("levels.xp-formula", "%level% * 10");
	}

	public int getMaxLevel() {
		return config.getInt("levels.max-level", 50);
	}

	public int getSlotsForLevel(int level) {
		return config.getInt("levels.slots-per-level." + level, 0);
	}

	public int getMaxSlots() {
		return config.getInt("clans.max-slots", 30);
	}

	public long getCreateCooldown() {
		return config.getLong("cooldowns.clan-create", 86400L);
	}

	public int getConfirmTimeout() {
		return config.getInt("confirmation.timeout-seconds", 10);
	}

	public int getInviteTimeout() {
		return config.getInt("invitations.timeout-seconds", 60);
	}

	public String getClanChatFormat() {
		return config.getString("chat.clan-format", "&8[&6ClanChat&8] &7{player}&8: &f{message}");
	}

	public String getAllyChatFormat() {
		return config.getString("chat.ally-format", "&8[&bAllyChat&8] &7({clan}) &f{player}&8: &f{message}");
	}

	public String getSpyChatFormat() {
		return config.getString("chat.spy-format", "&8[&cSpyChat&8] &7({clan}) &e{player}&8: &f{message}");
	}

	public boolean isSpyEnabled() {
		return config.getBoolean("chat.spy-enabled", true);
	}

	public boolean isProtectMembers() {
		return config.getBoolean("combat.protect-members", true);
	}

	public boolean isProtectAllies() {
		return config.getBoolean("combat.protect-allies", true);
	}

	public int getTopCount() {
		return config.getInt("top.display-count", 10);
	}

	public int getListPageSize() {
		return config.getInt("list.clans-per-page", 10);
	}

	public String getNoClanText() {
		return config.getString("placeholders.no-clan-text", "Sin clan");
	}

	public String getNoPrefixText() {
		return config.getString("placeholders.no-prefix-text", "Sin prefijo");
	}

	public String getNamePlaceholderColor() {
		return config.getString("placeholders.name-color", "e");
	}

	public String getPrefixPlaceholderColor() {
		return config.getString("placeholders.prefix-color", "f");
	}

	public boolean getBoolean(String path, boolean def) {
		return config.getBoolean(path, def);
	}

	public String getString(String path, String def) {
		return config.getString(path, def);
	}

	public String getNoClanZero() {
		return config.getString("placeholders.no-clan-zero", "0");
	}

	public String getPrefix() {
		return color(messages.getString("prefix", "&8[&6gClans&8] "));
	}

	public String getMessage(String path, String... replacements) {
		String raw = messages.getString(path);
		if (raw == null) {
			plugin.getLogger().warning("Mensaje no encontrado: " + path);
			return color("&cMensaje faltante: " + path);
		}
		return color(applyReplacements(getPrefix() + raw, replacements));
	}

	public String getRaw(String path, String... replacements) {
		String raw = messages.getString(path, "&cMensaje no configurado: " + path);
		return color(applyReplacements(raw, replacements));
	}

	public FileConfiguration getHelpConfig() {
		return help;
	}

	public FileConfiguration getAdminHelpConfig() {
		return adminHelp;
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public FileConfiguration getMessages() {
		return messages;
	}

	public static String color(String text) {
		if (text == null)
			return "";
		return text.replace("&", "§");
	}

	private String applyReplacements(String text, String[] replacements) {
		if (replacements.length % 2 != 0)
			return text;
		for (int i = 0; i < replacements.length; i += 2) {
			text = text.replace(replacements[i], replacements[i + 1]);
		}
		return text;
	}
}