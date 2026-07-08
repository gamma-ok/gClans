package me.gamma.clans.storage;

import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {

	void initialize() throws Exception;
	void shutdown();
	CompletableFuture<Void> saveClan(Clan clan);
	CompletableFuture<Void> updateClan(Clan clan);
	CompletableFuture<Void> deleteClan(String clanId);
	CompletableFuture<Optional<Clan>> findClanById(String clanId);
	CompletableFuture<Optional<Clan>> findClanByName(String name);
	CompletableFuture<List<Clan>> loadAllClans();
	CompletableFuture<Void> saveClanPlayer(ClanPlayer cp);
	CompletableFuture<Optional<ClanPlayer>> loadClanPlayer(UUID uuid);
	CompletableFuture<Void> updateMemberRank(String clanId, UUID uuid, Rank rank);
	CompletableFuture<Void> removeMember(UUID uuid);
	CompletableFuture<Void> addAlliance(String id1, String id2);
	CompletableFuture<Void> removeAlliance(String id1, String id2);
	CompletableFuture<List<String>> loadAllies(String clanId);
	CompletableFuture<List<Clan>> getTopByPoints(int limit);
	CompletableFuture<List<Clan>> getTopByKills(int limit);
	CompletableFuture<List<Clan>> getTopByLevel(int limit);
	CompletableFuture<Void> loadAllMembersIntoClans(Map<String, Clan> clanById);
}