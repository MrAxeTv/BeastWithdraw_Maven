package me.mraxetv.beastwithdraw.commands.tokenwithdraw;


import me.mraxetv.beastlib.commands.builder.ShortCommand;
import me.mraxetv.beasttokens.api.BeastTokensAPI;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.assets.BeastTokensHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BeastTokenNoteCMD extends WithdrawCMD {

    private BeastWithdrawPlugin pl;
    BeastTokensHandler assetHandler;

    public BeastTokenNoteCMD(BeastWithdrawPlugin pl, BeastTokensHandler assetHandler) {
        super(pl, assetHandler);
        this.pl = pl;
        this.assetHandler = assetHandler;

    }



}
