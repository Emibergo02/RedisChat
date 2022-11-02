package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.TextParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

public class IgnoreCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player))return false;
        if(args.length==0)return false;
        new BukkitRunnable(){
            @Override
            public void run() {

                if(args[0].equalsIgnoreCase("list")){
                        final StringJoiner ignoreList = new StringJoiner(", ");
                        for (String username : RedisChat.getInstance().getRedisDataManager().ignoringList(sender.getName())) {
                            ignoreList.add(username);
                        }
                        sender.sendMessage(TextParser.parse(RedisChat.config.ignoring_list.replace("%list%", ignoreList.toString())));
                        return;

                }
                RedisChat.getInstance().getRedisDataManager().toggleIgnoring(sender.getName(), args[0]);
                sender.sendMessage(TextParser.parse(RedisChat.config.ignoring_player.replace("%player%", args[0])));


            }
        }.runTaskAsynchronously(RedisChat.getInstance());


        return false;
    }
}
