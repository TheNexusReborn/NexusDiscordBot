package com.thenexusreborn.discord;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class DiscordListener extends ListenerAdapter {
    
    @Override
    public void onReady(ReadyEvent event) {
        for (Guild guild : event.getJDA().getGuilds()) {
            guild.updateCommands().addCommands(
                            Commands.slash("settype", "Sets the type of this guild for The Nexus Reborn.")
                                    .addOptions(new OptionData(OptionType.STRING, "type", "The type", true))
                                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    )
                    .queue();
        }
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        NexusDiscordBot.messageReceive(event.getGuild(), event.getAuthor(), event.getChannel(), event.getMessage());
    }
    
    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        NexusDiscordBot.messageUpdate(event.getGuild(), event.getAuthor(), event.getMessage());
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.getHook().setEphemeral(true);
        if (event.getName().equalsIgnoreCase("settype")) {
            String type = event.getOption("type").getAsString();
            NexusDiscordBot.setServerType(event.getGuild(), type);
            event.reply("You set the type to " + type).queue();
        }
    }
}