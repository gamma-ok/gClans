package me.gamma.clans.storage;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SQLiteProvider implements StorageProvider {

	private final Clans plugin;
	private Connection connection;
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "gClans-DB"));

	private final String TC, TM, TA;

	public SQLiteProvider(Clans plugin) {
		this.plugin = plugin;
		this.TC = plugin.getConfigManager().getTableClans();
		this.TM = plugin.getConfigManager().getTableMembers();
		this.TA = plugin.getConfigManager().getTableAllies();
	}

	@Override
	public void initialize() throws Exception {
		File f = new File(plugin.getDataFolder(), plugin.getConfigManager().getSqliteFile());
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + f.getAbsolutePath());
		try (Statement s = connection.createStatement()) {
			s.execute("PRAGMA journal_mode=WAL;");
		}
		createTables();
		plugin.getLogger().info("[gClans] SQLite iniciado: " + f.getName());
	}

	@Override
	public void shutdown() {
		executor.shutdown();
		try {
			if (connection != null && !connection.isClosed())
				connection.close();
		} catch (SQLException e) {
			plugin.getLogger().severe("Error cerrando BD: " + e.getMessage());
		}
	}

	private void createTables() throws SQLException {
		try (Statement s = connection.createStatement()) {
			s.executeUpdate("CREATE TABLE IF NOT EXISTS " + TC + " (" + "  id                    TEXT PRIMARY KEY,"
					+ "  name                  TEXT NOT NULL UNIQUE," + "  prefix                TEXT NOT NULL,"
					+ "  color                 TEXT NOT NULL DEFAULT 'e'," + "  leader_uuid           TEXT NOT NULL,"
					+ "  created_at            INTEGER NOT NULL," + "  level                 INTEGER DEFAULT 1,"
					+ "  xp                    INTEGER DEFAULT 0," + "  slots                 INTEGER DEFAULT 8,"
					+ "  total_kills           INTEGER DEFAULT 0," + "  total_deaths          INTEGER DEFAULT 0,"
					+ "  total_points          REAL    DEFAULT 0.0," + "  best_killstreak       INTEGER DEFAULT 0,"
					+ "  best_killstreak_player TEXT DEFAULT ''," + "  pvp_enabled           INTEGER DEFAULT 0" + ");");

			s.executeUpdate("CREATE TABLE IF NOT EXISTS " + TM + " (" + "  uuid                  TEXT PRIMARY KEY,"
					+ "  name                  TEXT NOT NULL," + "  clan_id               TEXT,"
					+ "  rank                  TEXT," + "  kills                 INTEGER DEFAULT 0,"
					+ "  deaths                INTEGER DEFAULT 0," + "  points                REAL    DEFAULT 0.0,"
					+ "  best_killstreak       INTEGER DEFAULT 0," + "  create_cooldown_until INTEGER DEFAULT 0,"
					+ "  clan_kills            INTEGER DEFAULT 0" + ");");

			s.executeUpdate("CREATE TABLE IF NOT EXISTS " + TA + " (" + "  clan_id_1 TEXT NOT NULL,"
					+ "  clan_id_2 TEXT NOT NULL," + "  PRIMARY KEY (clan_id_1, clan_id_2)" + ");");
		}
	}

	@Override
	public CompletableFuture<Void> saveClan(Clan c) {
		return run(() -> {
			String sql = "INSERT OR REPLACE INTO " + TC
					+ " (id,name,prefix,color,leader_uuid,created_at,level,xp,slots,"
					+ "  total_kills,total_deaths,total_points,best_killstreak,best_killstreak_player,pvp_enabled)"
					+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, c.getId());
				ps.setString(2, c.getName());
				ps.setString(3, c.getPrefix());
				ps.setString(4, c.getColor());
				ps.setString(5, c.getLeaderUuid().toString());
				ps.setLong(6, c.getCreatedAt());
				ps.setInt(7, c.getLevel());
				ps.setInt(8, c.getXp());
				ps.setInt(9, c.getSlots());
				ps.setInt(10, c.getTotalKills());
				ps.setInt(11, c.getTotalDeaths());
				ps.setDouble(12, c.getTotalPoints());
				ps.setInt(13, c.getBestKillstreak());
				ps.setString(14, c.getBestKillstreakPlayer());
				ps.setInt(15, c.isPvpEnabled() ? 1 : 0);
				ps.executeUpdate();
			}
			for (Map.Entry<UUID, Rank> e : c.getMembers().entrySet()) {
				String ms = "UPDATE " + TM + " SET clan_id=?, rank=? WHERE uuid=?";
				try (PreparedStatement ps = connection.prepareStatement(ms)) {
					ps.setString(1, c.getId());
					ps.setString(2, e.getValue().name());
					ps.setString(3, e.getKey().toString());
					ps.executeUpdate();
				}
			}
		});
	}

	@Override
	public CompletableFuture<Void> updateClan(Clan c) {
		return run(() -> {
			String sql = "UPDATE " + TC + " SET name=?,prefix=?,color=?,leader_uuid=?,level=?,xp=?,slots=?,"
					+ "     total_kills=?,total_deaths=?,total_points=?,"
					+ "     best_killstreak=?,best_killstreak_player=?,pvp_enabled=?" + " WHERE id=?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, c.getName());
				ps.setString(2, c.getPrefix());
				ps.setString(3, c.getColor());
				ps.setString(4, c.getLeaderUuid().toString());
				ps.setInt(5, c.getLevel());
				ps.setInt(6, c.getXp());
				ps.setInt(7, c.getSlots());
				ps.setInt(8, c.getTotalKills());
				ps.setInt(9, c.getTotalDeaths());
				ps.setDouble(10, c.getTotalPoints());
				ps.setInt(11, c.getBestKillstreak());
				ps.setString(12, c.getBestKillstreakPlayer());
				ps.setInt(13, c.isPvpEnabled() ? 1 : 0);
				ps.setString(14, c.getId());
				ps.executeUpdate();
			}
		});
	}

	@Override
	public CompletableFuture<Void> deleteClan(String id) {
		return run(() -> {
			try (PreparedStatement ps = connection
					.prepareStatement("UPDATE " + TM + " SET clan_id=NULL, rank=NULL, clan_kills=0 WHERE clan_id=?")) {
				ps.setString(1, id);
				ps.executeUpdate();
			}
			try (PreparedStatement ps = connection
					.prepareStatement("DELETE FROM " + TA + " WHERE clan_id_1=? OR clan_id_2=?")) {
				ps.setString(1, id);
				ps.setString(2, id);
				ps.executeUpdate();
			}
			try (PreparedStatement ps = connection.prepareStatement("DELETE FROM " + TC + " WHERE id=?")) {
				ps.setString(1, id);
				ps.executeUpdate();
			}
		});
	}

	@Override
	public CompletableFuture<Optional<Clan>> findClanById(String id) {
		return supply(() -> {
			try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + TC + " WHERE id=?")) {
				ps.setString(1, id);
				ResultSet rs = ps.executeQuery();
				return rs.next() ? Optional.of(mapClan(rs)) : Optional.empty();
			}
		});
	}

	@Override
	public CompletableFuture<Optional<Clan>> findClanByName(String name) {
		return supply(() -> {
			try (PreparedStatement ps = connection
					.prepareStatement("SELECT * FROM " + TC + " WHERE name=? COLLATE NOCASE")) {
				ps.setString(1, name);
				ResultSet rs = ps.executeQuery();
				return rs.next() ? Optional.of(mapClan(rs)) : Optional.empty();
			}
		});
	}

	@Override
	public CompletableFuture<List<Clan>> loadAllClans() {
		return supply(() -> {
			List<Clan> list = new ArrayList<>();
			try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM " + TC)) {
				while (rs.next())
					list.add(mapClan(rs));
			}
			return list;
		});
	}

	@Override
	public CompletableFuture<Void> saveClanPlayer(ClanPlayer cp) {
		return run(() -> {
			String sql = "INSERT OR REPLACE INTO " + TM
					+ " (uuid,name,clan_id,rank,kills,deaths,points,best_killstreak,create_cooldown_until,clan_kills)"
					+ " VALUES (?,?,?,?,?,?,?,?,?,?)";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, cp.getUuid().toString());
				ps.setString(2, cp.getName());
				ps.setString(3, cp.getClanId());
				ps.setString(4, cp.getRank() != null ? cp.getRank().name() : null);
				ps.setInt(5, cp.getKills());
				ps.setInt(6, cp.getDeaths());
				ps.setDouble(7, cp.getPoints());
				ps.setInt(8, cp.getBestKillstreak());
				ps.setLong(9, cp.getCreateCooldownUntil());
				ps.setInt(10, cp.getClanKills());
				ps.executeUpdate();
			}
		});
	}

	@Override
	public CompletableFuture<Optional<ClanPlayer>> loadClanPlayer(UUID uuid) {
		return supply(() -> {
			try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + TM + " WHERE uuid=?")) {
				ps.setString(1, uuid.toString());
				ResultSet rs = ps.executeQuery();
				return rs.next() ? Optional.of(mapPlayer(rs)) : Optional.empty();
			}
		});
	}

	@Override
	public CompletableFuture<Void> updateMemberRank(String clanId, UUID uuid, Rank rank) {
		return run(() -> {
			try (PreparedStatement ps = connection
					.prepareStatement("UPDATE " + TM + " SET rank=? WHERE uuid=? AND clan_id=?")) {
				ps.setString(1, rank.name());
				ps.setString(2, uuid.toString());
				ps.setString(3, clanId);
				ps.executeUpdate();
			}
		});
	}

	@Override
	public CompletableFuture<Void> removeMember(UUID uuid) {
		return run(() -> {
			try (PreparedStatement ps = connection
					.prepareStatement("UPDATE " + TM + " SET clan_id=NULL, rank=NULL, clan_kills=0 WHERE uuid=?")) {
				ps.setString(1, uuid.toString());
				ps.executeUpdate();
			}
		});
	}

	@Override
	public CompletableFuture<Void> addAlliance(String a, String b) {
		return run(() -> {
			String sql = "INSERT OR IGNORE INTO " + TA + " (clan_id_1,clan_id_2) VALUES (?,?)";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, a);
				ps.setString(2, b);
				ps.executeUpdate();
				ps.setString(1, b);
				ps.setString(2, a);
				ps.executeUpdate();
			}
		});
	}

	@Override
	public CompletableFuture<Void> removeAlliance(String a, String b) {
		return run(() -> {
			String sql = "DELETE FROM " + TA + " WHERE (clan_id_1=? AND clan_id_2=?) OR (clan_id_1=? AND clan_id_2=?)";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, a);
				ps.setString(2, b);
				ps.setString(3, b);
				ps.setString(4, a);
				ps.executeUpdate();
			}
		});
	}

	@Override
	public CompletableFuture<List<String>> loadAllies(String clanId) {
		return supply(() -> {
			List<String> list = new ArrayList<>();
			try (PreparedStatement ps = connection
					.prepareStatement("SELECT clan_id_2 FROM " + TA + " WHERE clan_id_1=?")) {
				ps.setString(1, clanId);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					list.add(rs.getString(1));
			}
			return list;
		});
	}

	@Override
	public CompletableFuture<List<Clan>> getTopByPoints(int limit) {
		return queryTop("ORDER BY total_points DESC", limit);
	}

	@Override
	public CompletableFuture<List<Clan>> getTopByKills(int limit) {
		return queryTop("ORDER BY total_kills DESC", limit);
	}

	@Override
	public CompletableFuture<List<Clan>> getTopByLevel(int limit) {
		return queryTop("ORDER BY level DESC, xp DESC", limit);
	}

	private CompletableFuture<List<Clan>> queryTop(String order, int limit) {
		return supply(() -> {
			List<Clan> list = new ArrayList<>();
			try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + TC + " " + order + " LIMIT ?")) {
				ps.setInt(1, limit);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					list.add(mapClan(rs));
			}
			return list;
		});
	}

	private Clan mapClan(ResultSet rs) throws SQLException {
		return new Clan(rs.getString("id"), rs.getString("name"), rs.getString("prefix"), rs.getString("color"),
				UUID.fromString(rs.getString("leader_uuid")), rs.getLong("created_at"), rs.getInt("level"),
				rs.getInt("xp"), rs.getInt("slots"), rs.getInt("total_kills"), rs.getInt("total_deaths"),
				rs.getDouble("total_points"), rs.getInt("best_killstreak"), rs.getString("best_killstreak_player"),
				rs.getInt("pvp_enabled") == 1);
	}

	private ClanPlayer mapPlayer(ResultSet rs) throws SQLException {
		String rankStr = rs.getString("rank");
		String clanId = rs.getString("clan_id");
		return new ClanPlayer(UUID.fromString(rs.getString("uuid")), rs.getString("name"), clanId,
				rankStr != null ? Rank.fromString(rankStr) : null, rs.getInt("kills"), rs.getInt("deaths"),
				rs.getDouble("points"), rs.getInt("best_killstreak"), rs.getLong("create_cooldown_until"),
				rs.getInt("clan_kills"));
	}

	private CompletableFuture<Void> run(SqlRunnable r) {
		return CompletableFuture.runAsync(() -> {
			try {
				r.run();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}

	private <T> CompletableFuture<T> supply(SqlSupplier<T> s) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return s.get();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}

	@FunctionalInterface
	interface SqlRunnable {
		void run() throws SQLException;
	}

	@FunctionalInterface
	interface SqlSupplier<T> {
		T get() throws SQLException;
	}
}