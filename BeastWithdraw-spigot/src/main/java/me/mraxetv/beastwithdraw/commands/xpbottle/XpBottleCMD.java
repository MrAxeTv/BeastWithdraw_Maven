package me.mraxetv.beastwithdraw.commands.xpbottle;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.managers.assets.XpBottleHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

        withdraw(player, args[0], stackSize, null);
    }

    protected void performWithdraw(Player p, double takenAmount, int stackSize) {

        assetHandler.withdrawAmount(p, takenAmount * stackSize);

        String s = assetHandler.getMessageSection().getString("Withdraw");
        s = s.replace("%amount%", assetHandler.formatWithPreSuffix(takenAmount));
        s = s.replace("%stacked-amount%", assetHandler.formatWithPreSuffix(takenAmount* stackSize));
        s = s.replace("%level-amount%", assetHandler.formatNumber(XpManager.getLevelFromExp((int)takenAmount)));
        s = Utils.formatStackSize(s,stackSize);
        s = s.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalance(p)));
        pl.getUtils().sendMessage(p, s);
        double tax = calculateTax(p);
        ItemStack item = assetHandler.getItem(p.getName(), takenAmount, stackSize, true,tax);
        if (p.getInventory().firstEmpty() != -1) {
            Utils.addItem(p, item);
        } else {
            p.getWorld().dropItem(p.getLocation(), item);
        }

        pl.getWithdrawLogger().logWithdraw(assetHandler, p, takenAmount, stackSize, takenAmount * stackSize, assetHandler.getBalance(p));

        playWithdrawSound(p, takenAmount);
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
                s = s.replace("%amount%", ""+lv);
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
