package me.mraxetv.beastwithdraw.commands.xpbottle;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.managers.assets.XpBottleHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class XpBottleCMD extends WithdrawCMD {

    private final BeastWithdrawPlugin pl;
    private final XpBottleHandler assetHandler;

    public XpBottleCMD(BeastWithdrawPlugin plugin, XpBottleHandler assetHandler) {
        super(plugin, assetHandler);
        this.pl = plugin;
        this.assetHandler = assetHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String cmd, String[] args) {
        if (!(sender instanceof Player)) {
            pl.getUtils().sendMessage(sender, "%prefix% Console can't use this command! /BeastWithdraw help");
            return true;
        }
        handleWithdraw((Player) sender, args);
        return true;
    }

    private void handleWithdraw(Player player, String[] args) {
        if (!player.hasPermission(getPermission())) {
            pl.getUtils().noPermission(player);
            return;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return;
        }

        int stackSize = parseStackSize(player, args);
        if (stackSize == -1) return;

        double balance = assetHandler.getBalance(player);
        double takenAmount = parseWithdrawAmount(player, args[0], balance);
        if (takenAmount == -1) return;
        if (!validateInventorySpace(player)) return;
        if (!validateWithdrawLimits(player, takenAmount)) return;
        if (!validateBigAmount(player, takenAmount, stackSize)) return;
        if (!validateBalance(player, balance, takenAmount, stackSize)) return;

        if (!chargeFee(player, stackSize)) return;

        performWithdraw(player, takenAmount, stackSize);
    }

    protected double parseWithdrawAmount(Player p, String arg, double balance) {
        if (arg.equalsIgnoreCase("all")) {
            if (!p.hasPermission(getPermission() + ".All")) {
                pl.getUtils().noPermission(p);
                return -1;
            }
            return balance;
        }
        else if(arg.toLowerCase().endsWith("l") && arg.toLowerCase().split("l").length == 1 && Utils.isInt(arg.toLowerCase().split("l")[0])){
            int lv = Math.abs(Integer.parseInt(arg.toLowerCase().split("l")[0]));
            if(lv > p.getLevel()){
                String s = assetHandler.getMessageSection().getString("NotEnoughLevels");
                s = s.replaceAll("%amount%", ""+lv);
                pl.getUtils().sendMessage(p, s);
                return -1;
            }
            /**Calculating off set**/
            int ofSetLevel = p.getLevel() - lv;
            int offSetXp = XpManager.getExpToLevel(ofSetLevel);
            return  (double) XpManager.getExpToLevel(lv+ofSetLevel) - offSetXp;
        }
        if (!Utils.isInt(arg)) {
            String s = pl.getMessages().getString("Withdraws.InvalidNumber");
            s = s.replace("%amount%",arg);
            pl.getUtils().sendMessage(p,s);
            return -1;
        }
        return Math.abs(Double.parseDouble(arg));
    }
}
