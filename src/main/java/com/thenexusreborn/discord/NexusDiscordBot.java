package com.thenexusreborn.discord;

import com.stardevllc.config.file.FileConfig;
import com.stardevllc.config.file.yaml.YamlConfig;
import com.stardevllc.starsql.model.*;
import com.stardevllc.starsql.statements.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;

public class NexusDiscordBot {
    
    private static FileConfig config;
    private static JDA jda;
    private static Database database;
    private static Table messagesTable;
    private static Table messageChangesTable;
    
    public static void main(String[] args) {
        File file = new File("nexusdiscordbot.yml");
        
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        
        System.out.println(file.toPath().toAbsolutePath());
        
        config = YamlConfig.loadConfiguration(file);
        
        if (!config.contains("token")) {
            config.set("token", "{TOKEN}");
            config.save();
            
            System.out.println("No token setting found, set a token and restart.");
            return;
        }
        
        if (!config.contains("mysql")) {
            config.set("mysql.host", "host");
            config.set("mysql.port", 3306);
            config.set("mysql.database", "database");
            config.set("mysql.user", "user");
            config.set("mysql.password", "password");
            config.save();
            
            System.out.println("MySQL Database Connection Details not found, please configure and restart.");
            return;
        }
        
        database = new Database(config.getString("mysql.database"), "jdbc:mysql://" + config.getString("mysql.host") + ":" + config.getInt("mysql.port") + "/" + config.getString("mysql.database"), config.getString("mysql.user"), config.getString("mysql.password"));
        try {
            database.retrieveDatabaseInformation();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to database", e);
        }
        
        messagesTable = database.getOrCreateTable("messages", Table.builder()
                .addColumn(Column.builder().type(new Column.Type("bigint")).name("id").position(1).primaryKey().autoIncrement())
                .addColumn(Column.builder().type(new Column.Type("timestamp")).name("timestamp").position(2))
                .addColumn(Column.builder().type(new Column.Type("varchar(19)")).name("guild").position(3))
                .addColumn(Column.builder().type(new Column.Type("varchar(19)")).name("user").position(4))
                .addColumn(Column.builder().type(new Column.Type("varchar(19)")).name("channel").position(5))
                .addColumn(Column.builder().type(new Column.Type("varchar(19)")).name("messageid").position(6))
                .addColumn(Column.builder().type(new Column.Type("varchar(5000)")).name("content").position(7))
        );
        
        database.execute(new CreateTable(messagesTable.getName(), new HashSet<>(messagesTable.getColumns().values())).build());
        
        messageChangesTable = database.getOrCreateTable("messageChanges", Table.builder()
                .addColumn(Column.builder().type(new Column.Type("bigint")).name("id").position(1).primaryKey().autoIncrement())
                .addColumn(Column.builder().type(new Column.Type("timestamp")).name("timestamp").position(2))
                .addColumn(Column.builder().type(new Column.Type("varchar(19)")).name("guild").position(3))
                .addColumn(Column.builder().type(new Column.Type("varchar(19)")).name("user").position(4))
                .addColumn(Column.builder().type(new Column.Type("varchar(19)")).name("messageid").position(5))
                .addColumn(Column.builder().type(new Column.Type("varchar(5000)")).name("content").position(6))
        );
        
        database.execute(new CreateTable(messageChangesTable.getName(), new HashSet<>(messagesTable.getColumns().values())).build());
        
        JDABuilder builder = JDABuilder.createDefault(config.getString("token"))
                .enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES)
                .enableCache(CacheFlag.ACTIVITY, CacheFlag.MEMBER_OVERRIDES, CacheFlag.ROLE_TAGS, CacheFlag.VOICE_STATE)
                .addEventListeners(new DiscordListener())
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setBulkDeleteSplittingEnabled(false);
        
        jda = builder.build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            jda = null;
            return;
        }
        
        jda.getPresence().setStatus(OnlineStatus.ONLINE);
    }
    
    public static void messageReceive(Guild guild, User user, Channel channel, Message message) {
        long id = database.executeUpdate(new SqlInsert(messagesTable).columns("timestamp", "guild", "user", "channel", "messageid", "content").row(new Timestamp(message.getTimeCreated().toEpochSecond() * 1000), guild.getId(), user.getId(), channel.getId(), message.getId(), message.getContentRaw()).build());
    }
    
    public static void messageUpdate(Guild guild, User user, Message message) {
        long id = database.executeUpdate(new SqlInsert(messageChangesTable).columns("timestamp", "guild", "user", "messageid", "content").row(new Timestamp(message.getTimeEdited().toEpochSecond() * 1000), guild.getId(), user.getId(), message.getId(), message.getContentRaw()).build());
    }
    
    public static void setServerType(Guild guild, String type) {
        config.set("servers." + guild.getIdLong() + ".type", type.toLowerCase().replace(" ", "_"));
        config.save();
    }
}