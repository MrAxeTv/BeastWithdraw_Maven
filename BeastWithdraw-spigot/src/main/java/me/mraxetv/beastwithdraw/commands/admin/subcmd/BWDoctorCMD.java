package me.mraxetv.beastwithdraw.commands.admin.subcmd;

import me.mraxetv.beastlib.commands.builder.CommandBuilder;
import me.mraxetv.beastlib.commands.builder.SubCommand;
import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import me.mraxetv.beastlib.lib.boostedyaml.block.implementation.Section;
import me.mraxetv.beastlib.lib.xmaterials.XMaterial;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BWDoctorCMD extends SubCommand {
    public BWDoctorCMD(CommandBuilder handle, String name) {
        super(handle, name);
    }

    @Override
    public String getPermission() {
        return "BeastWithdraw.admin.doctor";
    }

    @Override
    public String getDescription() {
        return "Checks BeastWithdraw configs for common setup problems.";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("check");
    }

    @Override
    public boolean allowConsole() {
        return true;
    }

    @Override
    public void playerExecute(CommandSender sender, String[] strings) {
        runDoctor(sender);
    }

    @Override
    public void consoleExecute(CommandSender sender, String[] strings) {
        runDoctor(sender);
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] strings) {
        return Collections.emptyList();
    }

    private void runDoctor(CommandSender sender) {
        BeastWithdrawPlugin plugin = BeastWithdrawPlugin.getInstance();
        List<String> issues = new ArrayList<>();

        checkGuiSettings(plugin.getWithdrawSettings(), "withdraw-settings.yml", issues);
        checkGuiSettings(plugin.getDepositSettings(), "deposit-settings.yml", issues);
        checkAssets(plugin, issues);

        plugin.getUtils().sendMessage(sender, "%prefix% &eDoctor checked BeastWithdraw configuration.");
        if (issues.isEmpty()) {
            plugin.getUtils().sendMessage(sender, "%prefix% &aNo common config issues were found.");
            return;
        }

        plugin.getUtils().sendMessage(sender, "%prefix% &cFound " + issues.size() + " issue(s):");
        int shown = 0;
        for (String issue : issues) {
            if (++shown > 20) {
                plugin.getUtils().sendMessage(sender, "%prefix% &7...and " + (issues.size() - 20) + " more.");
                break;
            }
            plugin.getUtils().sendMessage(sender, "%prefix% &c- &7" + issue);
        }
    }

    private void checkAssets(BeastWithdrawPlugin plugin, List<String> issues) {
        if (plugin.getWithdrawManager() == null) {
            issues.add("Withdraw manager is not loaded.");
            return;
        }

        for (String handlerId : plugin.getWithdrawManager().getAssetHandlerList()) {
            AssetHandler handler = plugin.getWithdrawManager().getAssetHandler(handlerId);
            if (handler == null) {
                issues.add("Asset handler " + handlerId + " is listed but cannot be resolved.");
                continue;
            }

            YamlDocument config = handler.getConfig();
            String label = "Withdraws/" + handler.getConfigFolderName() + "/" + handler.getConfigFileName();
            if (!"Withdraw.yml".equalsIgnoreCase(handler.getConfigFileName())) {
                issues.add(label + " should use Withdraw.yml as the withdraw config file name.");
            }

            checkMaterial(config.getString("Settings.Item", "PAPER"), label + " -> Settings.Item", issues);
            checkMinMax(config, label, issues);
            checkRequiredItem(config, "Settings.RequiredItem", label, issues);
            checkTypeWithdrawGui(config, label, issues);
        }
    }

    private void checkTypeWithdrawGui(YamlDocument config, String label, List<String> issues) {
        if (!config.getBoolean("WithdrawGUI.Enabled", false)) {
            return;
        }

        checkGuiSettings(config, label + " -> WithdrawGUI", issues, "WithdrawGUI.");
        if (config.getBoolean("WithdrawGUI.Command.Enabled", false)) {
            String command = config.getString("WithdrawGUI.Command.Name", "");
            if (command == null || command.trim().isEmpty()) {
                issues.add(label + " -> WithdrawGUI.Command.Name is empty while the command is enabled.");
            }
        }
    }

    private void checkGuiSettings(YamlDocument config, String label, List<String> issues) {
        checkGuiSettings(config, label, issues, "");
    }

    private void checkGuiSettings(YamlDocument config, String label, List<String> issues, String prefix) {
        if (config == null) {
            issues.add(label + " is not loaded.");
            return;
        }

        checkSize(config, prefix + "GUI.Size", label, issues);
        checkSlots(config, prefix + "GUI.RequiredItemSlots", prefix + "GUI.Size", label, issues);
        checkSlots(config, prefix + "GUI.DepositSlots", prefix + "GUI.Size", label, issues);
        checkButtonSlots(config, prefix + "GUI.Items", prefix + "GUI.Size", label, issues);
        checkButtonSlots(config, prefix + "Menu.Items", prefix + "Menu.Size", label, issues);
        checkSlots(config, prefix + "Menu.TypeSlots", prefix + "Menu.Size", label, issues);
        checkRequiredItem(config, prefix + "RequiredItem", label, issues);
    }

    private void checkSize(YamlDocument config, String path, String label, List<String> issues) {
        if (!config.contains(path)) {
            return;
        }

        int size = config.getInt(path, 54);
        if (size < 9 || size > 54 || size % 9 != 0) {
            issues.add(label + " -> " + path + " should be 9, 18, 27, 36, 45, or 54.");
        }
    }

    private void checkSlots(YamlDocument config, String path, String sizePath, String label, List<String> issues) {
        if (!config.contains(path)) {
            return;
        }

        int size = normalizeSize(config.getInt(sizePath, 54));
        for (Integer slot : config.getIntList(path)) {
            if (slot == null || slot < 1 || slot > size) {
                issues.add(label + " -> " + path + " contains invalid slot " + slot + " for GUI size " + size + ".");
            }
        }
    }

    private void checkButtonSlots(YamlDocument config, String path, String sizePath, String label, List<String> issues) {
        if (!config.isSection(path)) {
            return;
        }

        int size = normalizeSize(config.getInt(sizePath, 54));
        Section section = config.getSection(path);
        for (Object rawKey : section.getKeys()) {
            String key = String.valueOf(rawKey);
            String slotPath = path + "." + key + ".Slot";
            if (config.contains(slotPath)) {
                int slot = config.getInt(slotPath, -1);
                if (slot < 1 || slot > size) {
                    issues.add(label + " -> " + slotPath + " is invalid for GUI size " + size + ".");
                }
            }

            String slotsPath = path + "." + key + ".Slots";
            if (config.contains(slotsPath)) {
                for (Integer listedSlot : config.getIntList(slotsPath)) {
                    if (listedSlot == null || listedSlot < 1 || listedSlot > size) {
                        issues.add(label + " -> " + slotsPath + " contains invalid slot " + listedSlot + " for GUI size " + size + ".");
                    }
                }
            }
        }
    }

    private void checkMinMax(YamlDocument config, String label, List<String> issues) {
        double min = config.getDouble("Settings.Min", 0D);
        double max = config.getDouble("Settings.Max", 0D);
        if (max > 0 && min > max) {
            issues.add(label + " -> Settings.Min is greater than Settings.Max.");
        }
    }

    private void checkRequiredItem(YamlDocument config, String basePath, String label, List<String> issues) {
        if (!config.getBoolean(basePath + ".Enabled", false)) {
            return;
        }

        checkMaterial(config.getString(basePath + ".Match.Material", "PAPER"), label + " -> " + basePath + ".Match.Material", issues);
        int amount = config.getInt(basePath + ".Amount", 1);
        if (amount < 1) {
            issues.add(label + " -> " + basePath + ".Amount should be at least 1.");
        }
    }

    private void checkMaterial(String materialName, String path, List<String> issues) {
        Optional<XMaterial> material = XMaterial.matchXMaterial(materialName == null ? "" : materialName);
        if (!material.isPresent() || material.get().parseMaterial() == null) {
            issues.add(path + " uses unknown material '" + materialName + "'.");
        }
    }

    private int normalizeSize(int configuredSize) {
        if (configuredSize < 9) {
            return 9;
        }
        if (configuredSize > 54) {
            return 54;
        }
        return (configuredSize / 9) * 9;
    }
}
