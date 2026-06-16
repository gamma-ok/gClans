package me.gamma.clans;

import me.gamma.clans.api.ClansExpansion;
import me.gamma.clans.commands.ClanCommandExecutor;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.listeners.ClanChatListener;
import me.gamma.clans.listeners.FriendlyFireListener;
import me.gamma.clans.listeners.PlayerConnectionListener;
import me.gamma.clans.listeners.PlayerDeathListener;
import me.gamma.clans.managers.ClanManager;
import me.gamma.clans.managers.ConfirmationManager;
import me.gamma.clans.managers.InvitationManager;
import me.gamma.clans.storage.SQLiteProvider;
import me.gamma.clans.storage.StorageProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Clase principal de gClans v2.0.0 — KitPvP Edition.
 *
 * Ciclo de vida: onEnable → config → BD → managers → caché → listeners →
 * comandos → PAPI onDisable → limpiar estados → guardar datos → cerrar BD
 */
public class Clans extends JavaPlugin {

	// -------------------------------------------------------
	// Componentes
	// -------------------------------------------------------
	private ConfigManager configManager;
	private StorageProvider storageProvider;
	private ClanManager clanManager;
	private ConfirmationManager confirmationManager;
	private InvitationManager invitationManager;
	private ClansExpansion papiExpansion;

	// -------------------------------------------------------
	// onEnable
	// -------------------------------------------------------

	@Override
	public void onEnable() {
		long start = System.currentTimeMillis();
		getLogger().info("╔═══════════════════════════╗");
		getLogger().info("║   gClans v2.0.0  KitPvP  ║");
		getLogger().info("╚═══════════════════════════╝");

		if (!initConfig()) {
			disable("Error cargando configuración.");
			return;
		}
		if (!initStorage()) {
			disable("Error inicializando base de datos.");
			return;
		}

		initManagers();
		loadDataAsync();
		registerListeners();
		registerCommands();
		registerPAPI();

		long ms = System.currentTimeMillis() - start;
		getLogger().info("Plugin activado en " + ms + "ms.");
	}

	// -------------------------------------------------------
	// onDisable
	// -------------------------------------------------------

	@Override
	public void onDisable() {
		getLogger().info("Deteniendo gClans...");

		if (confirmationManager != null)
			confirmationManager.clearAll();
		if (invitationManager != null)
			invitationManager.clearAll();

		getServer().getScheduler().cancelTasks(this);

		// Guardar jugadores online antes de cerrar
		if (clanManager != null) {
			getServer().getOnlinePlayers().forEach(p -> clanManager.unloadPlayer(p.getUniqueId()));
		}

		if (storageProvider != null)
			storageProvider.shutdown();
		getLogger().info("gClans desactivado correctamente.");
	}

	// -------------------------------------------------------
	// Inicialización modular
	// -------------------------------------------------------

	private boolean initConfig() {
		try {
			if (!getDataFolder().exists())
				getDataFolder().mkdirs();
			configManager = new ConfigManager(this);
			configManager.loadAll();
			getLogger().info("Configuración cargada.");
			return true;
		} catch (Exception e) {
			getLogger().severe("Error en initConfig: " + e.getMessage());
			return false;
		}
	}

	private boolean initStorage() {
		try {
			String type = configManager.getDbType();
			if ("mysql".equals(type)) {
				// MySQLProvider implementable en el futuro con HikariCP
				getLogger().warning("MySQL no disponible aún. Usando SQLite.");
			}
			storageProvider = new SQLiteProvider(this);
			storageProvider.initialize();
			getLogger().info("Base de datos inicializada (SQLite).");
			return true;
		} catch (Exception e) {
			getLogger().severe("Error en initStorage: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	private void initManagers() {
		clanManager = new ClanManager(this, storageProvider);
		confirmationManager = new ConfirmationManager(this);
		invitationManager = new InvitationManager(this);
		getLogger().info("Managers inicializados.");
	}

	/**
	 * Carga clanes desde BD de forma asíncrona. Los jugadores que se conecten antes
	 * de que termine verán su clan en cuanto cargue.
	 */
	private void loadDataAsync() {
		clanManager.loadAll().thenRun(() -> getLogger().info("Caché de clanes lista.")).exceptionally(ex -> {
			getLogger().severe("Error cargando caché: " + ex.getMessage());
			return null;
		});
	}

	private void registerListeners() {
		org.bukkit.plugin.PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new PlayerConnectionListener(this), this);
		pm.registerEvents(new FriendlyFireListener(this), this);
		pm.registerEvents(new PlayerDeathListener(this), this);
		pm.registerEvents(new ClanChatListener(this), this);
		getLogger().info("Listeners registrados.");
	}

	private void registerCommands() {
		ClanCommandExecutor executor = new ClanCommandExecutor(this);
		getCommand("clan").setExecutor(executor);
		getCommand("clan").setTabCompleter(executor);
		getLogger().info("Comandos registrados.");
	}

	private void registerPAPI() {
		if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
			papiExpansion = new ClansExpansion(this);
			papiExpansion.register();
			getLogger().info("PlaceholderAPI: expansión %gclan_...% registrada.");
		} else {
			getLogger().info("PlaceholderAPI no encontrado. Placeholders no disponibles.");
		}
	}

	private void disable(String reason) {
		getLogger().severe(reason);
		getServer().getPluginManager().disablePlugin(this);
	}

	// -------------------------------------------------------
	// Getters (Service Locator)
	// -------------------------------------------------------

	public ConfigManager getConfigManager() {
		return configManager;
	}

	public StorageProvider getStorageProvider() {
		return storageProvider;
	}

	public ClanManager getClanManager() {
		return clanManager;
	}

	public ConfirmationManager getConfirmationManager() {
		return confirmationManager;
	}

	public InvitationManager getInvitationManager() {
		return invitationManager;
	}

	public ClansExpansion getPapiExpansion() {
		return papiExpansion;
	}
}