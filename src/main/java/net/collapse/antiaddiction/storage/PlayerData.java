package net.collapse.antiaddiction.storage;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String name;
    private long playtimeTicks;
    private long cooldownUntil;
    private boolean whitelisted;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.playtimeTicks = 0;
        this.cooldownUntil = 0;
        this.whitelisted = false;
    }

    public PlayerData(UUID uuid, String name, long playtimeTicks, long cooldownUntil, boolean whitelisted) {
        this.uuid = uuid;
        this.name = name;
        this.playtimeTicks = playtimeTicks;
        this.cooldownUntil = cooldownUntil;
        this.whitelisted = whitelisted;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getPlaytimeTicks() {
        return playtimeTicks;
    }

    public void setPlaytimeTicks(long playtimeTicks) {
        this.playtimeTicks = playtimeTicks;
    }

    public void addPlaytimeTicks(long ticks) {
        this.playtimeTicks += ticks;
    }

    public long getCooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(long cooldownUntil) {
        this.cooldownUntil = cooldownUntil;
    }

    public boolean isWhitelisted() {
        return whitelisted;
    }

    public void setWhitelisted(boolean whitelisted) {
        this.whitelisted = whitelisted;
    }
}
