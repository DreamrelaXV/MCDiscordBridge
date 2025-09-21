package com.dreamrela.mcdiscordbridge.utils;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class AsyncTaskRunner {
    
    private final MCDiscordBridge plugin;
    private final ExecutorService executorService;
    
    public AsyncTaskRunner(MCDiscordBridge plugin) {
        this.plugin = plugin;
        this.executorService = Executors.newFixedThreadPool(
            plugin.getConfigManager().getThreadPoolSize()
        );
    }
    
    public void runAsync(Runnable task) {
        executorService.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in async task", e);
            }
        });
    }
    
    public <T> CompletableFuture<T> runAsync(java.util.concurrent.Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executorService.submit(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in async task", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }
    
    public void runSync(Runnable task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in sync task", e);
                }
            }
        }.runTask(plugin);
    }
    
    public void runSyncDelayed(Runnable task, long delayTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in delayed sync task", e);
                }
            }
        }.runTaskLater(plugin, delayTicks);
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("ExecutorService did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}