package net.collapse.antiaddiction.scoreboard;

import net.collapse.antiaddiction.AntiaddictionMod;
import net.collapse.antiaddiction.config.PluginConfig;
import net.collapse.antiaddiction.storage.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScoreboardManager {
    private final AntiaddictionMod plugin;
    private final PluginConfig config;

    public ScoreboardManager(AntiaddictionMod plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void update(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getSessionMap().get(uuid);
        if (data == null) return;

        Scoreboard board = player.getScoreboard();
        
        // Ensure the player has their own custom scoreboard instance, not the main server board
        if (board == Bukkit.getScoreboardManager().getMainScoreboard() || board.getObjective("antiaddiction") == null) {
            createBoard(player);
            board = player.getScoreboard();
        }

        Objective objective = board.getObjective("antiaddiction");
        if (objective == null) return;

        // Update Title
        String title = ChatColor.translateAlternateColorCodes('&', config.getScoreboardTitle());
        if (!objective.getDisplayName().equals(title)) {
            objective.setDisplayName(title);
        }

        // Collect lines to display
        List<String> lines = new ArrayList<>();

        long limitTicks = config.getPlaytimeLimitTicks();
        long currentTicks = data.getPlaytimeTicks();
        double limitSeconds = limitTicks / 20.0;
        double currentSeconds = currentTicks / 20.0;
        lines.add(ChatColor.GREEN + "Playtime: " + ChatColor.WHITE + String.format("%.1fs / %.1fs", currentSeconds, limitSeconds));

        long now = System.currentTimeMillis();
        if (data.getCooldownUntil() > 0 && data.getCooldownUntil() > now) {
            long remaining = data.getCooldownUntil() - now;
            lines.add(ChatColor.RED + "Cooldown: " + ChatColor.WHITE + formatTime(remaining));
            lines.add(ChatColor.RED + "Rest: " + ChatColor.WHITE + formatTime(remaining));
        } else if (data.getCooldownUntil() > 0 && data.getCooldownUntil() <= now) {
            lines.add(ChatColor.YELLOW + "Cooldown: " + ChatColor.GREEN + "Expired");
            lines.add(ChatColor.GREEN + "Rest: " + ChatColor.WHITE + "Ready");
        } else {
            lines.add(ChatColor.YELLOW + "Cooldown: " + ChatColor.GREEN + "None");
            lines.add(ChatColor.GREEN + "Rest: " + ChatColor.WHITE + "Ready");
        }

        String statusLine = ChatColor.AQUA + "Status: " + (config.isAntiaddictionEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled");
        lines.add(statusLine);

        if (data.isWhitelisted()) {
            lines.add(ChatColor.GOLD + "Whitelisted: " + ChatColor.GREEN + "Yes");
        }

        // Clear existing scores to update lines safely
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        // Apply lines with descending score values (top to bottom)
        int scoreIndex = lines.size();
        for (String line : lines) {
            Score score = objective.getScore(line);
            score.setScore(scoreIndex--);
        }
    }

    public void remove(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board != null && board.getObjective("antiaddiction") != null) {
            board.getObjective("antiaddiction").unregister();
        }
    }

    private void createBoard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective(
            "antiaddiction", 
            Criteria.DUMMY, 
            ChatColor.translateAlternateColorCodes('&', config.getScoreboardTitle())
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}