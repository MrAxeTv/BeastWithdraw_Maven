package me.mraxetv.beastwithdraw.commands.admin;
import me.mraxetv.beastlib.commands.builder.CommandBuilder;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.admin.subcmd.BWGiveAllCMD;
import me.mraxetv.beastwithdraw.commands.admin.subcmd.BWGiveCMD;
import me.mraxetv.beastwithdraw.commands.admin.subcmd.BWReloadCMD;
import me.mraxetv.beastwithdraw.commands.admin.subcmd.BWVersionCMD;
import me.mraxetv.beastwithdraw.utils.Utils;

import java.util.List;

public class BeastWithdrawCMD extends CommandBuilder {
    private BeastWithdrawPlugin pl;


    public BeastWithdrawCMD(BeastWithdrawPlugin pl, String name, String description, String usageMessage, List<String> aliases) {
        super(pl, name, description, usageMessage, aliases);
        this.pl = pl;
        setHelpHeader(Utils.setColor(pl.getMessages().getString("Help.Header")));
        setPermission("BeastTokens.admin");
        setNoPermissionsMessage(pl.getMessagesLang().NO_PERMISSIONS);
        setNoConsoleAllowMessage("%prefix% This command is not allowed in console.");
        addSubCommand( new BWGiveCMD(this, "give"));
        addSubCommand( new BWGiveAllCMD(this, "giveall"));
        addSubCommand( new BWReloadCMD(this, "reload"));
        addSubCommand(new BWVersionCMD(this, "version"));
        setHelpSuggestions(getSubCommands().size()); 
        setHelpFooter(Utils.setColor(pl.getMessages().getString("Help.Footer")));
        register();
    }

}
