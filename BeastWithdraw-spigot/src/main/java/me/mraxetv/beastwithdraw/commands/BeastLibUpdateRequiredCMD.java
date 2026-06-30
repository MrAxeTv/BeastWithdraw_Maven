package me.mraxetv.beastwithdraw.commands;

import me.mraxetv.beastlib.commands.builder.ShortCommand;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.compatibility.BeastLibCompatibilityGuard;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.util.List;

public class BeastLibUpdateRequiredCMD extends ShortCommand {

    private BeastLibCompatibilityGuard compatibilityGuard;
    private boolean readyToRegister;

    public BeastLibUpdateRequiredCMD(BeastWithdrawPlugin pl, BeastLibCompatibilityGuard compatibilityGuard, String name, List<String> aliases) {
        super(pl, name, aliases, null);
        this.compatibilityGuard = compatibilityGuard;
        this.readyToRegister = true;
        register();
    }

    @Override
    public boolean execute(CommandSender sender, String cmd, String[] args) {
        compatibilityGuard.sendWarning(sender);
        return true;
    }

    @Override
    public void register() {
        if (!readyToRegister) {
            return;
        }

        try {
            getCommandMap().register(pl.getDescription().getName().toLowerCase(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CommandMap getCommandMap() throws NoSuchFieldException, IllegalAccessException {
        Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        commandMapField.setAccessible(true);
        return (CommandMap) commandMapField.get(Bukkit.getServer());
    }
}
