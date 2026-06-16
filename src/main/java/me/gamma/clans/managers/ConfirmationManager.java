package me.gamma.clans.managers;

import me.gamma.clans.Clans;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Gestiona confirmaciones temporales para acciones críticas (disband,
 * setleader).
 */
public class ConfirmationManager {

	public enum Type {
		DISBAND, SET_LEADER
	}

	private static class Pending {
		final Type type;
		final Object payload;
		final Consumer<UUID> onConfirm;
		final long expiresAt;
		int taskId = -1;

		Pending(Type type, Object payload, Consumer<UUID> onConfirm, long expiresAt) {
			this.type = type;
			this.payload = payload;
			this.onConfirm = onConfirm;
			this.expiresAt = expiresAt;
		}
	}

	private final Clans plugin;
	private final Map<UUID, Pending> pending = new HashMap<>();
	private final int timeoutSeconds;

	public ConfirmationManager(Clans plugin) {
		this.plugin = plugin;
		this.timeoutSeconds = plugin.getConfigManager().getConfirmTimeout();
	}

	/**
	 * Registra una solicitud de confirmación.
	 * 
	 * @return true si fue registrada correctamente.
	 */
	public boolean request(UUID uuid, Type type, Object payload, Consumer<UUID> onConfirm) {
		cancelTask(uuid);

		long expiresAt = System.currentTimeMillis() + timeoutSeconds * 1000L;
		Pending p = new Pending(type, payload, onConfirm, expiresAt);

		int taskId = plugin.getServer().getScheduler()
				.runTaskLater(plugin, () -> pending.remove(uuid), timeoutSeconds * 20L).getTaskId();
		p.taskId = taskId;
		pending.put(uuid, p);
		return true;
	}

	/**
	 * Intenta confirmar la acción. Retorna true si se ejecutó.
	 */
	public boolean confirm(UUID uuid, Type type) {
		Pending p = pending.get(uuid);
		if (p == null || p.type != type)
			return false;
		if (System.currentTimeMillis() > p.expiresAt) {
			pending.remove(uuid);
			return false;
		}

		cancelTask(uuid);
		pending.remove(uuid);
		p.onConfirm.accept(uuid);
		return true;
	}

	public boolean hasPending(UUID uuid, Type type) {
		Pending p = pending.get(uuid);
		return p != null && p.type == type && System.currentTimeMillis() <= p.expiresAt;
	}

	public Object getPayload(UUID uuid, Type type) {
		Pending p = pending.get(uuid);
		return (p != null && p.type == type) ? p.payload : null;
	}

	public void cancel(UUID uuid) {
		cancelTask(uuid);
		pending.remove(uuid);
	}

	public void clearAll() {
		pending.keySet().forEach(this::cancelTask);
		pending.clear();
	}

	private void cancelTask(UUID uuid) {
		Pending p = pending.get(uuid);
		if (p != null && p.taskId != -1) {
			plugin.getServer().getScheduler().cancelTask(p.taskId);
		}
	}
}