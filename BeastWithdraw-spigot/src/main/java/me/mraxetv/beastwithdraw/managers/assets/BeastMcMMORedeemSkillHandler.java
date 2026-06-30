package me.mraxetv.beastwithdraw.managers.assets;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import org.bukkit.entity.Player;

public class BeastMcMMORedeemSkillHandler extends BeastMcMMORedeemHandler {
    private static final String REDEEM_PERMISSION = "BeastWithdraw.McMMORedeemCredits.Redeem";
    private static final String STACKED_REDEEM_PERMISSION = "BeastWithdraw.McMMORedeemCredits.Redeem.Stacked";
    private static final String WITHDRAW_ALL_PERMISSION = "BeastWithdraw.McMMORedeemCredits.Withdraw.All";
    private static final String BYPASS_FEE_PERMISSION = "BeastWithdraw.McMMORedeemCredits.ByPass.Fee";
    private static final String BYPASS_TAX_PERMISSION = "BeastWithdraw.McMMORedeemCredits.ByPass.Tax";
    private static final String PERMISSION_NOTES_PREFIX = "BeastWithdraw.McMMORedeemCredits.PermissionNotes.";

    public BeastMcMMORedeemSkillHandler(BeastWithdrawPlugin plugin, String id) {
        super(plugin, id, "Withdraw-Skill.yml", "Depositer-Skills.yml");
        removeLegacyPermissionConfig();
    }

    @Override
    public String getConfigName() {
        return "McMMORedeemSkillCredits";
    }

    @Override
    public WithdrawCMD getWithdrawCMD() {
        return null;
    }

    @Override
    public String applyPlaceholders(String value, Player player) {
        if (value == null) {
            return null;
        }

        String skillName = getSkillPlaceholderContext();
        if (skillName == null || skillName.trim().isEmpty()) {
            skillName = "mcMMO Skill";
        }

        String text = value
                .replace("%type%", skillName)
                .replace("%skill%", skillName)
                .replace("%TYPE%", skillName)
                .replace("%SKILL%", skillName);
        return super.applyPlaceholders(text, player);
    }

    @Override
    public boolean hasRedeemPermission(Player player) {
        return player != null && player.hasPermission(REDEEM_PERMISSION);
    }

    @Override
    public boolean hasStackedRedeemPermission(Player player) {
        return player != null && player.hasPermission(STACKED_REDEEM_PERMISSION);
    }

    @Override
    public boolean hasWithdrawAllPermission(Player player) {
        return player != null && player.hasPermission(WITHDRAW_ALL_PERMISSION);
    }

    @Override
    public boolean hasBypassFeePermission(Player player) {
        return player != null && player.hasPermission(BYPASS_FEE_PERMISSION);
    }

    @Override
    public boolean hasBypassTaxPermission(Player player) {
        return player != null && player.hasPermission(BYPASS_TAX_PERMISSION);
    }

    @Override
    public boolean hasPermissionNote(Player player, String permissionName) {
        if (permissionName == null || permissionName.trim().isEmpty()) {
            return false;
        }
        if (player == null) {
            return false;
        }
        return player.hasPermission(PERMISSION_NOTES_PREFIX + permissionName.trim());
    }

    private void removeLegacyPermissionConfig() {
        boolean changed = false;
        if (getConfig().contains("Settings.Permission")) {
            getConfig().remove("Settings.Permission");
            changed = true;
        }
        if (getConfig().contains("Settings.Permissions")) {
            getConfig().remove("Settings.Permissions");
            changed = true;
        }
        if (changed) {
            saveConfig();
        }
    }
}
