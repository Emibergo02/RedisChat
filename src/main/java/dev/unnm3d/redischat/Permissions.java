package dev.unnm3d.redischat;

import lombok.Getter;

@Getter
public enum Permissions {
    MESSAGE("redischat.message"),
    MAIL_WRITE("redischat.mail.write"),
    MAIL_WRITE_PUBLIC("redischat.mail.writepublic"),
    MAIL_READ("redischat.mail.read"),
    MAIL_DELETE("redischat.mail.delete"),
    MAIL_PUBLIC_DELETE("redischat.mail.deletepublic"),
    MAIL_UNREAD("redischat.mail.unread"),
    IGNORE("redischat.ignore"),
    IGNORE_WHITELIST("redischat.ignore_whitelist"),
    BYPASS_FILTER_PREFIX("redischat.bypassfilter."),
    USE_FORMATTING("redischat.useformatting"),
    USE_DANGEROUS("redischat.usedangeroustags"),
    USE_ITEM("redischat.showitem"),
    USE_INVENTORY("redischat.showinv"),
    USE_ENDERCHEST("redischat.showenderchest"),
    USE_CUSTOM_PLACEHOLDERS("redischat.usecustomplaceholders"),
    BROADCAST("redischat.broadcast"),
    ANNOUNCER("redischat.announcer"),
    CLEARCHAT("redischat.clearchat"),
    ADMIN("redischat.admin"),
    ADMIN_EDIT("redischat.editmessage"),
    ADMIN_STAFF_CHAT("redischat.staffchat"),
    BYPASS_RATE_LIMIT("redischat.bypass_rate_limit"),
    CHANNEL_PREFIX("redischat.channel."),
    CHANNEL_PUBLIC("redischat.channel.public"),
    CHANNEL_CREATE("redischat.createchannel"),
    CHANNEL_INFO("redischat.infochannel"),
    CHANNEL_GUI("redischat.channelgui"),
    CHANNEL_CHANGE_DISPLAYNAME("redischat.changedisplayname"),
    CHANNEL_DELETE("redischat.deletechannel"),
    CHANNEL_TOGGLE_PLAYER("redischat.playerchannel"),
    CHANNEL_LIST("redischat.listchannel"),
    CHANNEL_MUTE("redischat.mutechannel"),
    CHANNEL_HIDE_PREFIX("redischat.hidechannel."),
    CHANNEL_SHOW_PREFIX("redischat.showchannel."),
    JOIN_QUIT("redischat.joinquit"),
    CHAT_COLOR("redischat.chatcolorcommand"),
    SET_PLACEHOLDER("redischat.setchatplaceholder"),
    SPY_OTHERS("redischat.spycommand.other"),
    ;

    private final String permission;

    Permissions(String permission) {
        this.permission = permission;
    }

}