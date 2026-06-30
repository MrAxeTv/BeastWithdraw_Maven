package me.mraxetv.beastwithdraw.gui.withdraw;

import me.mraxetv.beastlib.lib.boostedyaml.block.implementation.Section;
import me.mraxetv.beastlib.lib.kyori.adventure.text.Component;
import me.mraxetv.beastlib.lib.tgui.gui.guis.Gui;
import me.mraxetv.beastlib.lib.tgui.gui.guis.GuiItem;
import me.mraxetv.beastlib.lib.tgui.gui.guis.StorageGui;
import me.mraxetv.beastlib.lib.xmaterials.XMaterial;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.WithdrawItemRequirement;
import me.mraxetv.beastwithdraw.managers.assets.BeastMcMMORedeemHandler;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class WithdrawGuiManager {
    private static final String MENU_BASE = "Menu";
    private static final String GUI_BASE = "GUI";
    private static final String EMPTY_SLOT_BASE = GUI_BASE + ".EmptySlot";
    private static final String MENU_EMPTY_SLOT_BASE = MENU_BASE + ".EmptySlot";
    private static final String MCMMO_REDEEM_MENU_BASE = MENU_BASE + ".McMMORedeemSkillMenu";
    private static final String AMOUNT_EDITOR_BASE = GUI_BASE + ".AmountEditor";

    private final BeastWithdrawPlugin plugin;
    private final WithdrawGuiItemFactory itemFactory;
    private final WithdrawGuiProfile mainProfile;
    private final WithdrawGuiSoundSettings soundSettings;
    private final WithdrawGuiAnimationSettings animationSettings;
    private final List<WithdrawGuiProfile> commandProfiles;
    private final Set<UUID> invalidMessageCooldown = new HashSet<>();
    private final Map<UUID, ClickStamp> guiActionCooldown = new HashMap<>();
    private final Set<UUID> withdrawActionCooldown = new HashSet<>();
    private static final long DUPLICATE_GUI_ACTION_WINDOW_NANOS = 75_000_000L;

    public WithdrawGuiManager(BeastWithdrawPlugin plugin) {
        this.plugin = plugin;
        this.itemFactory = new WithdrawGuiItemFactory(plugin);
        this.mainProfile = WithdrawGuiProfile.main(plugin);
        this.soundSettings = WithdrawGuiSoundSettings.from(plugin, mainProfile);
        this.animationSettings = WithdrawGuiAnimationSettings.from(plugin, mainProfile);
        this.commandProfiles = buildCommandProfiles();
    }

    public boolean isEnabled() {
        return mainProfile.isEnabled();
    }

    public List<WithdrawGuiProfile> getCommandProfiles() {
        return new ArrayList<>(commandProfiles);
    }

    public void open(Player player, WithdrawGuiProfile profile) {
        if (profile.isMainProfile()) {
            openMenu(player);
            return;
        }
        if (shouldOpenMcMMORedeemSelector(player, profile.getAssetHandler())) {
            openMcMMORedeemMenu(player, (BeastMcMMORedeemHandler) profile.getAssetHandler(), 0);
            return;
        }
        openWithdraw(player, profile);
    }

    public void openMenu(Player player) {
        int size = normalizeSize(mainProfile.getInt(MENU_BASE + ".Size", 54));
        int rows = size / 9;
        StorageGui gui = Gui.storage()
                .title(Component.text(mainProfile.applyPlaceholders(player, mainProfile.getString(MENU_BASE + ".Title", "&8Withdraw Menu"))))
                .rows(rows)
                .create();

        Map<Integer, GuiItem> backgroundItems = applyFillers(mainProfile, player, gui, rows, MENU_EMPTY_SLOT_BASE);
        applyTypeButtons(player, gui, size);
        applyConfiguredButton(mainProfile, player, gui, MENU_BASE + ".Items.CloseButton", event -> {
            event.setCancelled(true);
            if (!shouldHandleGuiAction(player, event, "menu-close")) {
                return;
            }
            gui.close(player);
        });

        gui.setDefaultTopClickAction(event -> event.setCancelled(true));
        gui.setPlayerInventoryAction(event -> event.setCancelled(true));
        gui.setDragAction(event -> event.setCancelled(true));
        openManagedGui(player, gui, rows, WithdrawGuiAnimationSettings.Menu.MAIN_MENU, backgroundItems);
        sendMessage(player, "WithdrawGui.MenuOpened", "%prefix% &eSelect which note type you want to withdraw.");
    }

    public void openWithdraw(Player player, WithdrawGuiProfile profile) {
        openWithdraw(player, WithdrawContext.normal(profile));
    }

    private void openWithdraw(Player player, WithdrawContext context) {
        WithdrawGuiProfile profile = context.getProfile();
        AssetHandler assetHandler = context.getAssetHandler();
        if (assetHandler == null) {
            openMenu(player);
            return;
        }

        int size = normalizeSize(profile.getInt(GUI_BASE + ".Size", 54));
        int rows = size / 9;
        List<Integer> inputSlots = getInputSlots(profile, size);
        Set<Integer> inputSlotSet = new LinkedHashSet<>(inputSlots);
        WithdrawItemRequirement requirement = assetHandler.getWithdrawItemRequirement();

        StorageGui gui = Gui.storage()
                .title(Component.text(applyContextPlaceholders(context, player,
                        profile.getString(GUI_BASE + ".Title", "&8Withdraw %type%"))))
                .rows(rows)
                .create();

        Map<Integer, GuiItem> backgroundItems = applyFillers(profile, player, gui, rows, EMPTY_SLOT_BASE);
        if (requirement.isEnabled()) {
            for (Integer slot : inputSlots) {
                gui.removeItem(slot);
                gui.getInventory().setItem(slot, null);
                backgroundItems.remove(slot);
            }
        }
        applyAmountEditor(context, player, gui, inputSlots);
        applyConfiguredButton(profile, player, gui, GUI_BASE + ".Items.BackButton", event -> {
            event.setCancelled(true);
            if (!shouldHandleGuiAction(player, event, "withdraw-back")) {
                return;
            }
            if (requirement.isEnabled()) {
                returnInputItems(player, gui.getInventory(), inputSlots);
            }
            openBackTarget(player, context);
        });
        applyConfiguredButton(profile, player, gui, GUI_BASE + ".Items.CloseButton", event -> {
            event.setCancelled(true);
            if (!shouldHandleGuiAction(player, event, "withdraw-close")) {
                return;
            }
            gui.close(player);
        });

        gui.setDefaultTopClickAction(event -> handleTopClick(profile, requirement, player, event, inputSlotSet, size));
        gui.setPlayerInventoryAction(event -> handlePlayerInventoryClick(requirement, player, event, gui.getInventory(), inputSlots));
        gui.setDragAction(event -> handleDrag(requirement, player, event, inputSlotSet, size));
        if (requirement.isEnabled()) {
            gui.setCloseGuiAction(event -> returnInputItems(player, event.getInventory(), inputSlots));
        }
        openManagedGui(player, gui, rows, WithdrawGuiAnimationSettings.Menu.AMOUNT_MENU, backgroundItems);
        sendMessage(player, "WithdrawGui.Opened", "%prefix% &eChoose an amount to withdraw.");
    }

    private List<WithdrawGuiProfile> buildCommandProfiles() {
        List<WithdrawGuiProfile> profiles = new ArrayList<>();
        if (mainProfile.isCommandEnabled()) {
            profiles.add(mainProfile);
        }

        if (plugin.getWithdrawManager() == null) {
            return profiles;
        }

        for (String handlerId : plugin.getWithdrawManager().getAssetHandlerList()) {
            AssetHandler assetHandler = plugin.getWithdrawManager().getAssetHandler(handlerId);
            if (assetHandler == null || assetHandler.getWithdrawCMD() == null) {
                continue;
            }

            WithdrawGuiProfile profile = WithdrawGuiProfile.asset(plugin, assetHandler);
            if (profile.isEnabled() && profile.isCommandEnabled()) {
                profiles.add(profile);
            }
        }
        return profiles;
    }

    private void applyTypeButtons(Player player, StorageGui gui, int size) {
        List<AssetHandler> handlers = getMenuHandlers();
        List<Integer> slots = getTypeSlots(size, handlers.size());

        int index = 0;
        for (AssetHandler assetHandler : handlers) {
            if (index >= slots.size()) {
                break;
            }

            WithdrawGuiProfile assetProfile = WithdrawGuiProfile.asset(plugin, assetHandler);
            int slot = slots.get(index++);
            Map<String, String> replacements = typeReplacements(player, assetHandler);
            ItemStack itemStack = itemFactory.createItem(assetProfile, player, MENU_BASE + ".Items.TypeButton", replacements, assetHandler.getMaterial());
            gui.setItem(slot, new GuiItem(itemStack, event -> {
                event.setCancelled(true);
                if (!shouldHandleGuiAction(player, event, "menu-type-" + assetHandler.getID())) {
                    return;
                }
                if (shouldOpenMcMMORedeemSelector(player, assetHandler)) {
                    openMcMMORedeemMenu(player, (BeastMcMMORedeemHandler) assetHandler, 0);
                    return;
                }
                openWithdraw(player, assetProfile);
            }));
        }
    }

    private boolean shouldOpenMcMMORedeemSelector(Player player, AssetHandler assetHandler) {
        if (!(assetHandler instanceof BeastMcMMORedeemHandler)) {
            return false;
        }

        BeastMcMMORedeemHandler handler = (BeastMcMMORedeemHandler) assetHandler;
        return handler.getSkillHandler() != null
                && handler.getWithdrawCMD() != null
                && mainProfile.getBoolean(MCMMO_REDEEM_MENU_BASE + ".Enabled", true)
                && handler.isSkillNotesEnabled()
                && handler.hasSkillWithdrawPermission(player)
                && !getMcMMOSkillOptions(handler).isEmpty();
    }

    private void openMcMMORedeemMenu(Player player, BeastMcMMORedeemHandler handler, int page) {
        List<String> skills = getMcMMOSkillOptions(handler);
        List<Integer> skillSlots = getMcMMOSkillSlots(normalizeSize(mainProfile.getInt(MCMMO_REDEEM_MENU_BASE + ".Size", 54)));
        if (skills.isEmpty() || skillSlots.isEmpty()) {
            openWithdraw(player, WithdrawContext.normal(WithdrawGuiProfile.asset(plugin, handler)));
            return;
        }

        int size = normalizeSize(mainProfile.getInt(MCMMO_REDEEM_MENU_BASE + ".Size", 54));
        int rows = size / 9;
        int maxPage = Math.max(1, (int) Math.ceil(skills.size() / (double) skillSlots.size()));
        int normalizedPage = clamp(page, 0, maxPage - 1);
        Map<String, String> replacements = mcMMORedeemMenuReplacements(player, handler, null, normalizedPage, maxPage, skills.size());

        StorageGui gui = Gui.storage()
                .title(Component.text(applyRawPlaceholders(player,
                        mainProfile.getString(MCMMO_REDEEM_MENU_BASE + ".Title", "&8mcMMO Redeem Withdraw"),
                        replacements)))
                .rows(rows)
                .create();

        Map<Integer, GuiItem> backgroundItems = applyMcMMORedeemFillers(player, gui, rows);
        applyLockedConfiguredItem(mainProfile, player, gui, MCMMO_REDEEM_MENU_BASE + ".Items.GuideButton",
                replacements, Material.BOOK);
        applyMcMMOCreditButton(player, gui, handler, normalizedPage, maxPage, skills.size(), size);
        applyMcMMOSkillButtons(player, gui, handler, skills, skillSlots, normalizedPage, maxPage);
        applyMcMMOPageButton(player, gui, handler, MCMMO_REDEEM_MENU_BASE + ".Items.PreviousPageButton",
                Material.ARROW, normalizedPage, maxPage, skills.size(), normalizedPage - 1,
                normalizedPage > 0, "mcmmo-prev-page");
        applyMcMMOPageButton(player, gui, handler, MCMMO_REDEEM_MENU_BASE + ".Items.NextPageButton",
                Material.ARROW, normalizedPage, maxPage, skills.size(), normalizedPage + 1,
                normalizedPage + 1 < maxPage, "mcmmo-next-page");
        applyConfiguredButton(mainProfile, player, gui, MCMMO_REDEEM_MENU_BASE + ".Items.BackButton", replacements,
                Material.ARROW, event -> {
                    event.setCancelled(true);
                    if (!shouldHandleGuiAction(player, event, "mcmmo-menu-back")) {
                        return;
                    }
                    openMenu(player);
                });
        applyConfiguredButton(mainProfile, player, gui, MCMMO_REDEEM_MENU_BASE + ".Items.CloseButton", replacements,
                Material.BARRIER, event -> {
                    event.setCancelled(true);
                    if (!shouldHandleGuiAction(player, event, "mcmmo-menu-close")) {
                        return;
                    }
                    gui.close(player);
                });

        gui.setDefaultTopClickAction(event -> event.setCancelled(true));
        gui.setPlayerInventoryAction(event -> event.setCancelled(true));
        gui.setDragAction(event -> event.setCancelled(true));
        openManagedGui(player, gui, rows, WithdrawGuiAnimationSettings.Menu.MCMMO_REDEEM_SKILL_MENU, backgroundItems);
        sendMessage(player, "WithdrawGui.McMMOSkillMenuOpened", "%prefix% &eChoose mcMMO credits or a skill to withdraw.");
    }

    private Map<Integer, GuiItem> applyMcMMORedeemFillers(Player player, StorageGui gui, int rows) {
        String basePath = MCMMO_REDEEM_MENU_BASE + ".EmptySlot";
        if (!mainProfile.getBoolean(basePath + ".Enabled", true) || !mainProfile.isSection(basePath + ".Items")) {
            return applyFillers(mainProfile, player, gui, rows, MENU_EMPTY_SLOT_BASE);
        }

        Section itemsSection = mainProfile.getSection(basePath + ".Items");
        boolean hasExplicitSlots = false;
        for (Object rawKey : itemsSection.getKeys()) {
            String key = String.valueOf(rawKey);
            if (!mainProfile.getIntList(basePath + ".Items." + key + ".Slots").isEmpty()) {
                hasExplicitSlots = true;
                break;
            }
        }

        if (!hasExplicitSlots) {
            return applyFillers(mainProfile, player, gui, rows, basePath);
        }

        Map<Integer, GuiItem> backgroundItems = new HashMap<>();
        for (Object rawKey : itemsSection.getKeys()) {
            String key = String.valueOf(rawKey);
            String path = basePath + ".Items." + key;
            GuiItem item = itemFactory.createLockedGuiItem(mainProfile, player, path);
            if (item == null) {
                continue;
            }
            for (Integer slot : getConfiguredSlotList(mainProfile, path + ".Slots", gui.getInventory().getSize(), null)) {
                gui.setItem(slot, item);
                backgroundItems.put(slot, item);
            }
        }
        return backgroundItems;
    }

    private void applyMcMMOCreditButton(Player player, StorageGui gui, BeastMcMMORedeemHandler handler,
                                        int page, int maxPage, int skillCount, int size) {
        String path = MCMMO_REDEEM_MENU_BASE + ".Items.CreditButton";
        if (!mainProfile.getBoolean(path + ".Enabled", true)) {
            return;
        }

        int slot = configSlotToIndex(mainProfile.getInt(path + ".Slot", 5), size);
        if (slot < 0) {
            return;
        }

        Map<String, String> replacements = mcMMORedeemMenuReplacements(player, handler, null, page, maxPage, skillCount);
        ItemStack itemStack = itemFactory.createItem(mainProfile, player, path, replacements, handler.getMaterial());
        gui.setItem(slot, new GuiItem(itemStack, event -> {
            event.setCancelled(true);
            if (!shouldHandleGuiAction(player, event, "mcmmo-credit-withdraw")) {
                return;
            }
            openWithdraw(player, WithdrawContext.mcMMOCredit(WithdrawGuiProfile.asset(plugin, handler), handler, page));
        }));
    }

    private void applyMcMMOSkillButtons(Player player, StorageGui gui, BeastMcMMORedeemHandler handler,
                                        List<String> skills, List<Integer> slots, int page, int maxPage) {
        String path = MCMMO_REDEEM_MENU_BASE + ".Items.SkillButton";
        if (!mainProfile.getBoolean(path + ".Enabled", true)) {
            return;
        }

        int start = page * slots.size();
        int end = Math.min(skills.size(), start + slots.size());
        BeastMcMMORedeemHandler skillHandler = handler.getSkillHandler();
        WithdrawGuiProfile skillProfile = WithdrawGuiProfile.asset(plugin, skillHandler);
        for (int index = start; index < end; index++) {
            String skillName = skills.get(index);
            int slot = slots.get(index - start);
            Map<String, String> replacements = mcMMORedeemMenuReplacements(player, handler, skillName, page, maxPage, skills.size());
            Material material = resolveSkillMaterial(skillName, Material.BOOK);
            ItemStack itemStack = itemFactory.createItem(mainProfile, player, path, replacements, material);
            gui.setItem(slot, new GuiItem(itemStack, event -> {
                event.setCancelled(true);
                if (!shouldHandleGuiAction(player, event, "mcmmo-skill-" + normalizeSkillKey(skillName))) {
                    return;
                }
                openWithdraw(player, WithdrawContext.mcMMOSkill(skillProfile, handler, skillName, page));
            }));
        }
    }

    private void applyMcMMOPageButton(Player player, StorageGui gui, BeastMcMMORedeemHandler handler, String path,
                                      Material materialFallback, int page, int maxPage, int skillCount, int targetPage,
                                      boolean visible, String actionKey) {
        if (!visible || !mainProfile.getBoolean(path + ".Enabled", true)) {
            return;
        }

        Map<String, String> replacements = mcMMORedeemMenuReplacements(player, handler, null, page, maxPage, skillCount);
        applyConfiguredButton(mainProfile, player, gui, path, replacements, materialFallback, event -> {
            event.setCancelled(true);
            if (!shouldHandleGuiAction(player, event, actionKey)) {
                return;
            }
            openMcMMORedeemMenu(player, handler, targetPage);
        });
    }

    private List<String> getMcMMOSkillOptions(BeastMcMMORedeemHandler handler) {
        List<String> skills = new ArrayList<>();
        for (String rawSkill : handler.getSkillSuggestions()) {
            String skillName = handler.normalizeSkillName(rawSkill);
            if (skillName == null || !handler.isValidSkill(skillName) || skills.contains(skillName)) {
                continue;
            }
            skills.add(skillName);
        }
        sortMcMMOSkillOptions(skills);
        return skills;
    }

    private List<Integer> getMcMMOSkillSlots(int size) {
        List<Integer> defaults = new ArrayList<>();
        int[] defaultSlots = {
                11, 12, 13, 14, 15, 16, 17,
                20, 21, 22, 23, 24, 25, 26,
                30, 34
        };
        for (int slot : defaultSlots) {
            defaults.add(slot);
        }
        return getConfiguredSlotList(mainProfile, MCMMO_REDEEM_MENU_BASE + ".SkillSlots", size, defaults);
    }

    private void sortMcMMOSkillOptions(List<String> skills) {
        List<String> configuredOrder = mainProfile.getStringList(MCMMO_REDEEM_MENU_BASE + ".SkillOrder");
        if (configuredOrder.isEmpty()) {
            configuredOrder = defaultMcMMOSkillOrder();
        }

        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < configuredOrder.size(); i++) {
            order.put(normalizeSkillKey(configuredOrder.get(i)), i);
        }

        skills.sort((left, right) -> {
            int leftIndex = order.getOrDefault(normalizeSkillKey(left), Integer.MAX_VALUE);
            int rightIndex = order.getOrDefault(normalizeSkillKey(right), Integer.MAX_VALUE);
            if (leftIndex != rightIndex) {
                return Integer.compare(leftIndex, rightIndex);
            }
            return left.compareToIgnoreCase(right);
        });
    }

    private List<String> defaultMcMMOSkillOrder() {
        List<String> order = new ArrayList<>();
        order.add("Acrobatics");
        order.add("Alchemy");
        order.add("Archery");
        order.add("Axes");
        order.add("Crossbows");
        order.add("Excavation");
        order.add("Fishing");
        order.add("Herbalism");
        order.add("Maces");
        order.add("Mining");
        order.add("Repair");
        order.add("Swords");
        order.add("Taming");
        order.add("Tridents");
        order.add("Unarmed");
        order.add("Woodcutting");
        return order;
    }

    private Map<String, String> mcMMORedeemMenuReplacements(Player player, BeastMcMMORedeemHandler handler,
                                                            String skillName, int page, int maxPage, int skillCount) {
        BeastMcMMORedeemHandler skillHandler = handler.getSkillHandler() == null ? handler : handler.getSkillHandler();
        double creditBalance = handler.getBalanceAsDouble(player);
        double displayedBalance = skillName == null ? creditBalance : handler.getSkillBalance(player, skillName);
        String formattedBalance = (skillName == null ? handler : skillHandler).formatWithPreSuffix(displayedBalance);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("%type%", skillName == null ? handler.getConfigName() : skillName);
        replacements.put("%type_id%", skillName == null ? handler.getID() : skillHandler.getID());
        replacements.put("%skill%", skillName == null ? "" : skillName);
        replacements.put("%withdraw_command%", handler.getCommandName());
        replacements.put("%balance%", formattedBalance);
        replacements.put("%credit_balance%", handler.formatWithPreSuffix(creditBalance));
        replacements.put("%skill_balance%", skillName == null ? "" : skillHandler.formatWithPreSuffix(displayedBalance));
        replacements.put("%skill_count%", String.valueOf(skillCount));
        replacements.put("%page%", String.valueOf(page + 1));
        replacements.put("%max_page%", String.valueOf(maxPage));
        return replacements;
    }

    private Material resolveSkillMaterial(String skillName, Material fallback) {
        String configured = getConfiguredSkillString(MCMMO_REDEEM_MENU_BASE + ".SkillIcons", skillName);
        return resolveMaterial(configured, fallback);
    }

    private String getConfiguredSkillString(String basePath, String skillName) {
        if (skillName == null) {
            return "";
        }

        List<String> keys = new ArrayList<>();
        keys.add(skillName);
        keys.add(skillName.toLowerCase(Locale.ENGLISH));
        keys.add(skillName.toUpperCase(Locale.ENGLISH));
        keys.add(normalizeSkillKey(skillName));
        for (String key : keys) {
            String path = basePath + "." + key;
            if (mainProfile.contains(path)) {
                return mainProfile.getString(path, "");
            }
        }
        return "";
    }

    private String normalizeSkillKey(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized.isEmpty() ? "unknown" : normalized;
    }

    private Material resolveMaterial(String materialName, Material fallback) {
        if (materialName != null && !materialName.trim().isEmpty()) {
            Optional<XMaterial> material = XMaterial.matchXMaterial(materialName.trim().toUpperCase(Locale.ENGLISH));
            if (material.isPresent() && material.get().parseMaterial() != null) {
                return material.get().parseMaterial();
            }
        }
        return fallback == null ? Material.STONE : fallback;
    }

    private List<AssetHandler> getMenuHandlers() {
        List<AssetHandler> handlers = new ArrayList<>();
        if (plugin.getWithdrawManager() == null) {
            return handlers;
        }

        for (String handlerId : plugin.getWithdrawManager().getAssetHandlerList()) {
            AssetHandler assetHandler = plugin.getWithdrawManager().getAssetHandler(handlerId);
            if (assetHandler != null && assetHandler.getWithdrawCMD() != null && mainProfile.accepts(assetHandler)) {
                handlers.add(assetHandler);
            }
        }
        return handlers;
    }

    private void applyAmountEditor(WithdrawContext context, Player player, StorageGui gui, List<Integer> inputSlots) {
        WithdrawGuiProfile profile = context.getProfile();
        AssetHandler assetHandler = context.getAssetHandler();
        if (assetHandler == null) {
            return;
        }

        double startingAmount = parseDouble(profile.getString(AMOUNT_EDITOR_BASE + ".StartingAmount", "1"), 1D);
        int startingStack = profile.getInt(AMOUNT_EDITOR_BASE + ".StartingStack", 1);
        WithdrawSelection selection = new WithdrawSelection(
                normalizeAmount(assetHandler, Math.max(0D, startingAmount)),
                clamp(startingStack, 1, getMaxStackSize(assetHandler))
        );

        renderAmountEditor(context, player, gui, inputSlots, selection);
    }

    private void renderAmountEditor(WithdrawContext context, Player player, StorageGui gui,
                                    List<Integer> inputSlots, WithdrawSelection selection) {
        WithdrawGuiProfile profile = context.getProfile();
        AssetHandler assetHandler = context.getAssetHandler();
        if (assetHandler == null) {
            return;
        }

        selection.stack = clamp(selection.stack, 1, getMaxStackSize(assetHandler));
        selection.amount = normalizeAmount(assetHandler, Math.max(0D, selection.amount));
        double maxAmount = getMaxSelectableAmount(context, player, selection.stack);
        if (maxAmount <= 0D) {
            selection.amount = 0D;
        } else if (selection.amount > maxAmount) {
            selection.amount = maxAmount;
        }
        selection.amount = normalizeAmount(assetHandler, selection.amount);

        boolean maxSelected = maxAmount > 0D && Math.abs(selection.amount - maxAmount) < 0.000001D;
        Map<String, String> replacements = editorReplacements(context, player, selection, maxAmount);

        applyLockedConfiguredItem(profile, player, gui, GUI_BASE + ".Items.InfoItem", replacements, Material.BOOK);
        applyAmountPreviewItem(context, player, gui, selection);
        applyLockedConfiguredItem(profile, player, gui, GUI_BASE + ".Items.StackItem", replacements,
                Material.CHEST, selection.amount, maxSelected);

        applyAmountAdjustmentButtons(context, player, gui, GUI_BASE + ".Items.AddAmountButton",
                getAmountAdjustmentValues(profile), true, selection, replacements, inputSlots);
        applyAmountAdjustmentButtons(context, player, gui, GUI_BASE + ".Items.RemoveAmountButton",
                getAmountAdjustmentValues(profile), false, selection, replacements, inputSlots);
        applyStackAdjustmentButtons(context, player, gui, GUI_BASE + ".Items.AddStackButton",
                getStackAdjustmentValues(profile), true, selection, replacements, inputSlots);
        applyStackAdjustmentButtons(context, player, gui, GUI_BASE + ".Items.RemoveStackButton",
                getStackAdjustmentValues(profile), false, selection, replacements, inputSlots);

        applyEditorButton(profile, player, gui, GUI_BASE + ".Items.ResetButton", replacements,
                Material.HOPPER, selection.amount, maxSelected, event -> {
                    event.setCancelled(true);
                    if (!shouldHandleGuiAction(player, event, "amount-reset")) {
                        return;
                    }
                    selection.amount = 0D;
                    renderAmountEditor(context, player, gui, inputSlots, selection);
                });
        applyEditorButton(profile, player, gui, GUI_BASE + ".Items.MaxButton", replacements,
                Material.NETHER_STAR, selection.amount, maxSelected, event -> {
                    event.setCancelled(true);
                    if (!shouldHandleGuiAction(player, event, "amount-max")) {
                        return;
                    }
                    selection.amount = getMaxSelectableAmount(context, player, selection.stack);
                    renderAmountEditor(context, player, gui, inputSlots, selection);
                });
        applyEditorButton(profile, player, gui, GUI_BASE + ".Items.ConfirmButton", replacements,
                Material.EMERALD, selection.amount, maxSelected, event -> {
                    event.setCancelled(true);
                    if (!shouldHandleGuiAction(player, event, "amount-confirm")) {
                        return;
                    }
                    if (selection.amount <= 0D) {
                        sendMessage(player, "WithdrawGui.AmountRequired", "%prefix% &cSelect an amount greater than 0.");
                        return;
                    }

                    if (!beginWithdrawAction(player)) {
                        return;
                    }

                    WithdrawItemRequirement requirement = assetHandler.getWithdrawItemRequirement();
                    WithdrawItemRequirement.ItemSource source = requirement.isEnabled()
                            ? WithdrawItemRequirement.inventorySlots(gui.getInventory(), inputSlots)
                            : null;
                    boolean success = context.withdraw(player, formatCommandAmount(selection.amount), selection.stack, source);
                    if (success && profile.getBoolean(GUI_BASE + ".CloseAfterWithdraw", false)) {
                        gui.close(player);
                        return;
                    }
                    renderAmountEditor(context, player, gui, inputSlots, selection);
                });

        gui.update();
    }

    private void applyAmountAdjustmentButtons(WithdrawContext context, Player player, StorageGui gui, String path,
                                              List<Double> values, boolean positive, WithdrawSelection selection,
                                              Map<String, String> baseReplacements, List<Integer> inputSlots) {
        WithdrawGuiProfile profile = context.getProfile();
        List<Integer> slots = getConfiguredSlots(profile, path, gui.getInventory().getSize());
        int count = Math.min(slots.size(), values.size());
        for (int i = 0; i < count; i++) {
            final double change = Math.max(0D, values.get(i));
            if (change <= 0D) {
                continue;
            }

            Map<String, String> replacements = new HashMap<>(baseReplacements);
            replacements.put("%amount%", profile.getAssetHandler().formatWithPreSuffix(change));
            replacements.put("%amount_raw%", formatCommandAmount(change));
            replacements.put("%change%", profile.getAssetHandler().formatWithPreSuffix(change));
            replacements.put("%change_raw%", formatCommandAmount(change));

            gui.setItem(slots.get(i), new GuiItem(
                    itemFactory.createItem(profile, player, path, replacements,
                            Material.PAPER),
                    event -> {
                        event.setCancelled(true);
                        if (!shouldHandleGuiAction(player, event, "amount-" + (positive ? "add-" : "remove-") + formatCommandAmount(change))) {
                            return;
                        }
                        selection.amount = positive ? selection.amount + change : selection.amount - change;
                        renderAmountEditor(context, player, gui, inputSlots, selection);
                    }
            ));
        }
    }

    private void applyStackAdjustmentButtons(WithdrawContext context, Player player, StorageGui gui, String path,
                                             List<Integer> values, boolean positive, WithdrawSelection selection,
                                             Map<String, String> baseReplacements, List<Integer> inputSlots) {
        WithdrawGuiProfile profile = context.getProfile();
        List<Integer> slots = getConfiguredSlots(profile, path, gui.getInventory().getSize());
        int count = Math.min(slots.size(), values.size());
        for (int i = 0; i < count; i++) {
            final int change = Math.max(1, values.get(i));
            Map<String, String> replacements = new HashMap<>(baseReplacements);
            replacements.put("%stack_change%", String.valueOf(change));
            replacements.put("%change%", String.valueOf(change));

            gui.setItem(slots.get(i), new GuiItem(
                    itemFactory.createItem(profile, player, path, replacements,
                            Material.PAPER),
                    event -> {
                        event.setCancelled(true);
                        if (!shouldHandleGuiAction(player, event, "stack-" + (positive ? "add-" : "remove-") + change)) {
                            return;
                        }
                        selection.stack = positive ? selection.stack + change : selection.stack - change;
                        renderAmountEditor(context, player, gui, inputSlots, selection);
                    }
            ));
        }
    }

    private void applyEditorButton(WithdrawGuiProfile profile, Player player, StorageGui gui, String path,
                                   Map<String, String> replacements, Material materialFallback,
                                   double selectedAmount, boolean maxSelected,
                                   me.mraxetv.beastlib.lib.tgui.gui.components.GuiAction<InventoryClickEvent> action) {
        if (!profile.getBoolean(path + ".Enabled", true)) {
            return;
        }

        int slot = configSlotToIndex(profile.getInt(path + ".Slot", -1), gui.getInventory().getSize());
        if (slot < 0) {
            return;
        }

        ItemStack itemStack = itemFactory.createItem(profile, player, path, replacements, materialFallback, selectedAmount, maxSelected);
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        gui.setItem(slot, new GuiItem(itemStack, action));
    }

    private void openManagedGui(Player player, StorageGui gui, int rows,
                                WithdrawGuiAnimationSettings.Menu menu,
                                Map<Integer, GuiItem> backgroundItems) {
        if (!animationSettings.shouldAnimate(menu)) {
            gui.open(player);
            soundSettings.playMenuOpen(player);
            return;
        }

        Map<Integer, GuiItem> finalItems = new LinkedHashMap<>(gui.getGuiItems());
        Map<Integer, GuiItem> animatedItems = new LinkedHashMap<>();
        GuiItem placeholderItem = getAnimationPlaceholderItem(player);
        for (Map.Entry<Integer, GuiItem> entry : finalItems.entrySet()) {
            Integer slot = entry.getKey();
            GuiItem item = entry.getValue();
            if (slot == null || item == null) {
                continue;
            }

            if (animationSettings.isAnimateEmptySlots() || !isBackgroundItem(slot, item, backgroundItems)) {
                animatedItems.put(slot, item);
                if (placeholderItem == null) {
                    gui.removeItem(slot);
                } else {
                    gui.setItem(slot, placeholderItem);
                }
            }
        }

        gui.open(player);
        soundSettings.playMenuOpen(player);

        if (!animatedItems.isEmpty()) {
            new WithdrawGuiOpenAnimationTask(plugin, player, gui, rows, animatedItems, animationSettings).start();
        }
    }

    private boolean isBackgroundItem(Integer slot, GuiItem item, Map<Integer, GuiItem> backgroundItems) {
        if (slot == null || backgroundItems == null || backgroundItems.isEmpty()) {
            return false;
        }
        return backgroundItems.get(slot) == item;
    }

    private GuiItem getAnimationPlaceholderItem(Player player) {
        if (!animationSettings.isPlaceholderItemEnabled()) {
            return null;
        }
        return itemFactory.createLockedGuiItem(mainProfile, player, "Animations.Open.PlaceholderItem");
    }

    private Map<Integer, GuiItem> applyFillers(WithdrawGuiProfile profile, Player player, StorageGui gui,
                                               int rows, String basePath) {
        if (!profile.getBoolean(basePath + ".Enabled", true) || !profile.isSection(basePath + ".Items")) {
            return new HashMap<>();
        }

        Section itemsSection = profile.getSection(basePath + ".Items");
        List<GuiItem> fillerItems = new ArrayList<>();
        for (Object rawKey : itemsSection.getKeys()) {
            String key = String.valueOf(rawKey);
            GuiItem item = itemFactory.createLockedGuiItem(profile, player, basePath + ".Items." + key);
            if (item != null) {
                fillerItems.add(item);
            }
        }

        Set<Integer> beforeSlots = new HashSet<>(gui.getGuiItems().keySet());
        WithdrawFillPattern.from(profile, basePath, rows).apply(gui, fillerItems);
        Map<Integer, GuiItem> backgroundItems = new HashMap<>();
        for (Map.Entry<Integer, GuiItem> entry : gui.getGuiItems().entrySet()) {
            if (beforeSlots.contains(entry.getKey())) {
                continue;
            }
            backgroundItems.put(entry.getKey(), entry.getValue());
        }
        return backgroundItems;
    }

    private void applyConfiguredButton(WithdrawGuiProfile profile, Player player, StorageGui gui, String path,
                                       me.mraxetv.beastlib.lib.tgui.gui.components.GuiAction<InventoryClickEvent> action) {
        applyConfiguredButton(profile, player, gui, path, null, null, action);
    }

    private void applyConfiguredButton(WithdrawGuiProfile profile, Player player, StorageGui gui, String path,
                                       Map<String, String> replacements, Material materialFallback,
                                       me.mraxetv.beastlib.lib.tgui.gui.components.GuiAction<InventoryClickEvent> action) {
        if (!profile.getBoolean(path + ".Enabled", true)) {
            return;
        }

        int slot = configSlotToIndex(profile.getInt(path + ".Slot", -1), gui.getInventory().getSize());
        if (slot < 0) {
            return;
        }

        ItemStack itemStack = itemFactory.createItem(profile, player, path, replacements, materialFallback);
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        gui.setItem(slot, new GuiItem(itemStack, action));
    }

    private void handleTopClick(WithdrawGuiProfile profile, WithdrawItemRequirement requirement, Player player,
                                InventoryClickEvent event, Set<Integer> inputSlots, int topSize) {
        if (!requirement.isEnabled()) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getSlot();
        if (!inputSlots.contains(slot)) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
            ItemStack hotbarItem = event.getHotbarButton() >= 0
                    ? player.getInventory().getItem(event.getHotbarButton())
                    : null;
            if (!isEmpty(hotbarItem) && !requirement.matches(hotbarItem)) {
                event.setCancelled(true);
                sendInvalidRequiredItemMessage(profile, player);
            }
            return;
        }

        ItemStack cursor = event.getCursor();
        if (placesItem(event.getAction()) && !isEmpty(cursor) && !requirement.matches(cursor)) {
            event.setCancelled(true);
            sendInvalidRequiredItemMessage(profile, player);
            return;
        }

        if (event.getRawSlot() >= topSize) {
            event.setCancelled(true);
        }
    }

    private void handlePlayerInventoryClick(WithdrawItemRequirement requirement, Player player, InventoryClickEvent event,
                                            Inventory inventory, List<Integer> inputSlots) {
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return;
        }

        event.setCancelled(true);
        if (!requirement.isEnabled()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (isEmpty(clicked)) {
            return;
        }

        if (!requirement.matches(clicked)) {
            sendInvalidRequiredItemMessage(null, player);
            return;
        }

        int originalAmount = clicked.getAmount();
        ItemStack remaining = moveToInputSlots(inventory, inputSlots, clicked);
        if (isEmpty(remaining)) {
            event.setCurrentItem(null);
            return;
        }

        event.setCurrentItem(remaining);
        if (remaining.getAmount() == originalAmount) {
            sendMessage(player, "WithdrawGui.RequiredItemSlotsFull", "%prefix% &cThe required item slots are full.");
        }
    }

    private void handleDrag(WithdrawItemRequirement requirement, Player player, InventoryDragEvent event,
                            Set<Integer> inputSlots, int topSize) {
        boolean touchesTop = false;
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot == null || rawSlot >= topSize) {
                continue;
            }

            touchesTop = true;
            if (!requirement.isEnabled() || !inputSlots.contains(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        if (touchesTop && !isEmpty(event.getOldCursor()) && !requirement.matches(event.getOldCursor())) {
            event.setCancelled(true);
            sendInvalidRequiredItemMessage(null, player);
        }
    }

    private ItemStack moveToInputSlots(Inventory inventory, List<Integer> inputSlots, ItemStack source) {
        ItemStack remaining = source.clone();
        fillSimilarInputStacks(inventory, inputSlots, remaining);
        if (isEmpty(remaining)) {
            return null;
        }

        fillEmptyInputSlots(inventory, inputSlots, remaining);
        return isEmpty(remaining) ? null : remaining;
    }

    private void fillSimilarInputStacks(Inventory inventory, List<Integer> inputSlots, ItemStack remaining) {
        for (Integer slot : inputSlots) {
            if (isEmpty(remaining) || slot == null || slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            ItemStack current = inventory.getItem(slot);
            if (isEmpty(current) || !current.isSimilar(remaining)) {
                continue;
            }

            int maxStack = Math.min(current.getMaxStackSize(), remaining.getMaxStackSize());
            int space = maxStack - current.getAmount();
            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining.getAmount());
            current.setAmount(current.getAmount() + moved);
            remaining.setAmount(remaining.getAmount() - moved);
        }
    }

    private void fillEmptyInputSlots(Inventory inventory, List<Integer> inputSlots, ItemStack remaining) {
        for (Integer slot : inputSlots) {
            if (isEmpty(remaining) || slot == null || slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            if (!isEmpty(inventory.getItem(slot))) {
                continue;
            }

            int moved = Math.min(remaining.getMaxStackSize(), remaining.getAmount());
            ItemStack placed = remaining.clone();
            placed.setAmount(moved);
            inventory.setItem(slot, placed);
            remaining.setAmount(remaining.getAmount() - moved);
        }
    }

    private void returnInputItems(Player player, Inventory inventory, List<Integer> inputSlots) {
        for (Integer slot : inputSlots) {
            if (slot == null || slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            ItemStack itemStack = inventory.getItem(slot);
            if (isEmpty(itemStack)) {
                continue;
            }

            inventory.setItem(slot, null);
            player.getInventory().addItem(itemStack).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    private List<Integer> getInputSlots(WithdrawGuiProfile profile, int size) {
        List<Integer> configured = profile.getIntList(GUI_BASE + ".RequiredItemSlots");
        if (configured.isEmpty()) {
            configured = defaultInputSlots();
        }

        Set<Integer> slots = new LinkedHashSet<>();
        for (Integer slot : configured) {
            int normalized = configSlotToIndex(slot == null ? -1 : slot, size);
            if (normalized >= 0) {
                slots.add(normalized);
            }
        }
        return new ArrayList<>(slots);
    }

    private List<Integer> getTypeSlots(int size, int typeCount) {
        if (mainProfile.getBoolean(MENU_BASE + ".CenterTypeRows", false)) {
            List<Integer> centeredSlots = centeredTypeSlots(typeCount, size);
            if (!centeredSlots.isEmpty()) {
                return centeredSlots;
            }
        }

        List<Integer> configured = mainProfile.getIntList(MENU_BASE + ".TypeSlots");
        if (configured.isEmpty()) {
            configured = defaultTypeSlots();
        }

        List<Integer> slots = new ArrayList<>();
        for (Integer slot : configured) {
            int normalized = configSlotToIndex(slot == null ? -1 : slot, size);
            if (normalized >= 0) {
                slots.add(normalized);
            }
        }
        return slots;
    }

    private List<Integer> defaultInputSlots() {
        List<Integer> slots = new ArrayList<>();
        slots.add(29);
        slots.add(30);
        slots.add(31);
        return slots;
    }

    private List<Integer> defaultTypeSlots() {
        List<Integer> slots = new ArrayList<>();
        int[] defaults = {12, 14, 16, 21, 23, 25, 30, 32, 34};
        for (int slot : defaults) {
            slots.add(slot);
        }
        return slots;
    }

    private List<Integer> centeredTypeSlots(int typeCount, int size) {
        if (typeCount <= 0 || typeCount > 9 || size < 45) {
            return new ArrayList<>();
        }

        int[][] layouts = {
                {},
                {23},
                {22, 24},
                {21, 23, 25},
                {22, 24, 31, 33},
                {21, 23, 25, 31, 33},
                {21, 23, 25, 30, 32, 34},
                {12, 14, 16, 21, 23, 25, 32},
                {12, 14, 16, 21, 23, 25, 31, 33},
                {12, 14, 16, 21, 23, 25, 30, 32, 34}
        };

        List<Integer> slots = new ArrayList<>();
        for (int slot : layouts[typeCount]) {
            int normalized = configSlotToIndex(slot, size);
            if (normalized >= 0) {
                slots.add(normalized);
            }
        }
        return slots;
    }

    private Map<String, String> typeReplacements(Player player, AssetHandler assetHandler) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%type%", assetHandler.getConfigName());
        replacements.put("%type_id%", assetHandler.getID());
        replacements.put("%withdraw_command%", assetHandler.getCommandName());
        replacements.put("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalanceAsDouble(player)));
        return replacements;
    }

    private Map<String, String> amountReplacements(WithdrawContext context, Player player, String amount, int stack) {
        AssetHandler assetHandler = context.getAssetHandler();
        WithdrawItemRequirement requirement = assetHandler.getWithdrawItemRequirement();
        double taxPercentage = getWithdrawTaxPercentage(assetHandler, player);
        double fee = getWithdrawFee(assetHandler, player, stack);
        double feeEach = getWithdrawFee(assetHandler, player, 1);
        double amountValue = resolveAmountValue(context, player, amount);
        double totalAmount = amountValue * stack;
        double taxAmount = calculateTaxAmount(amountValue, taxPercentage);
        double totalTaxAmount = taxAmount * stack;
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%button_amount%", formatButtonAmount(assetHandler, amount));
        replacements.put("%amount%", formatButtonAmount(assetHandler, amount));
        replacements.put("%stack%", String.valueOf(stack));
        replacements.put("%required_item_enabled%", String.valueOf(requirement.isEnabled()));
        replacements.put("%required_item%", requirement.getDisplayName());
        replacements.put("%required_amount%", String.valueOf(requirement.getRequiredAmount(stack)));
        replacements.put("%balance%", assetHandler.formatWithPreSuffix(context.getBalance(player)));
        replacements.put("%total_amount%", assetHandler.formatWithPreSuffix(totalAmount));
        replacements.put("%stacked_amount%", assetHandler.formatWithPreSuffix(totalAmount));
        replacements.put("%tax%", formatPercent(taxPercentage));
        replacements.put("%tax_percent%", formatPercent(taxPercentage));
        replacements.put("%tax_amount%", assetHandler.formatWithPreSuffix(taxAmount));
        replacements.put("%stacked_tax_amount%", assetHandler.formatWithPreSuffix(totalTaxAmount));
        replacements.put("%fee%", assetHandler.formatWithPreSuffix(fee));
        replacements.put("%fee_each%", assetHandler.formatWithPreSuffix(feeEach));
        applyContextReplacements(context, player, replacements);
        return replacements;
    }

    private void applyLockedConfiguredItem(WithdrawGuiProfile profile, Player player, StorageGui gui, String path,
                                           Map<String, String> replacements, Material materialFallback) {
        applyLockedConfiguredItem(profile, player, gui, path, replacements, materialFallback, Double.NaN, false);
    }

    private void applyLockedConfiguredItem(WithdrawGuiProfile profile, Player player, StorageGui gui, String path,
                                           Map<String, String> replacements, Material materialFallback,
                                           double selectedAmount, boolean maxSelected) {
        if (!profile.getBoolean(path + ".Enabled", true)) {
            return;
        }

        int slot = configSlotToIndex(profile.getInt(path + ".Slot", -1), gui.getInventory().getSize());
        if (slot < 0) {
            return;
        }

        ItemStack itemStack = itemFactory.createItem(profile, player, path, replacements, materialFallback, selectedAmount, maxSelected);
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        gui.setItem(slot, new GuiItem(itemStack, event -> event.setCancelled(true)));
    }

    private void applyAmountPreviewItem(WithdrawContext context, Player player, StorageGui gui,
                                        WithdrawSelection selection) {
        WithdrawGuiProfile profile = context.getProfile();
        AssetHandler assetHandler = context.getAssetHandler();
        String path = GUI_BASE + ".Items.AmountItem";
        if (assetHandler == null || !profile.getBoolean(path + ".Enabled", true)) {
            return;
        }

        int slot = configSlotToIndex(profile.getInt(path + ".Slot", -1), gui.getInventory().getSize());
        if (slot < 0) {
            return;
        }

        int stack = clamp(selection.stack, 1, getMaxStackSize(assetHandler));
        double amount = normalizeAmount(assetHandler, Math.max(0D, selection.amount));
        double tax = getWithdrawTaxPercentage(assetHandler, player);
        ItemStack itemStack;
        if (context.getSkillName() != null && assetHandler instanceof BeastMcMMORedeemHandler) {
            itemStack = ((BeastMcMMORedeemHandler) assetHandler).getSkillItem(
                    player.getName(), context.getSkillName(), amount, stack, true, tax, null
            );
        } else {
            itemStack = assetHandler.getItem(player.getName(), amount, stack, true, tax, null);
        }

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }
        gui.setItem(slot, new GuiItem(itemStack, event -> event.setCancelled(true)));
    }

    private double getWithdrawTaxPercentage(AssetHandler assetHandler, Player player) {
        if (assetHandler.hasBypassTaxPermission(player)) {
            return 0D;
        }

        double tax = assetHandler.getConfig().getDouble("Settings.Tax.Percentage", 0D);
        if (!assetHandler.getConfig().getBoolean("Settings.PermissionNotes.Enabled", false)
                || !assetHandler.getConfig().isSection("Settings.PermissionNotes")) {
            return Math.max(0D, tax);
        }

        Section permissionNotes = assetHandler.getConfig().getSection("Settings.PermissionNotes");
        for (String key : permissionNotes.getRoutesAsStrings(false)) {
            if (assetHandler.hasPermissionNote(player, key)) {
                tax = assetHandler.getConfig().getDouble("Settings.PermissionNotes." + key + ".Tax.Percentage", tax);
            }
        }
        return Math.max(0D, tax);
    }

    private double getWithdrawFee(AssetHandler assetHandler, Player player, int stack) {
        if (assetHandler.hasBypassFeePermission(player)
                || !assetHandler.getConfig().getBoolean("Settings.Charges.Fee.Enabled", false)) {
            return 0D;
        }
        return Math.max(0D, assetHandler.getConfig().getDouble("Settings.Charges.Fee.Cost", 0D)) * Math.max(1, stack);
    }

    private double resolveAmountValue(WithdrawContext context, Player player, String amount) {
        if (amount == null || amount.equalsIgnoreCase("all")) {
            return Math.max(0D, context.getBalance(player));
        }

        try {
            return Math.max(0D, Double.parseDouble(amount));
        } catch (NumberFormatException ignored) {
            return 0D;
        }
    }

    private Map<String, String> editorReplacements(WithdrawContext context, Player player,
                                                   WithdrawSelection selection, double maxAmount) {
        AssetHandler assetHandler = context.getAssetHandler();
        String rawAmount = formatCommandAmount(selection.amount);
        Map<String, String> replacements = amountReplacements(context, player, rawAmount, selection.stack);

        double balance = context.getBalance(player);
        double totalAmount = selection.amount * selection.stack;
        double fee = getWithdrawFee(assetHandler, player, selection.stack);
        double remainingBalance = Math.max(0D, balance - totalAmount - fee);
        double minAmount = getPermissionAmountLimit(assetHandler, player, "Min", 0D);
        double maxLimit = getPermissionAmountLimit(assetHandler, player, "Max", maxAmount);

        replacements.put("%selected_amount%", assetHandler.formatWithPreSuffix(selection.amount));
        replacements.put("%selected_amount_raw%", rawAmount);
        replacements.put("%selected_stack%", String.valueOf(selection.stack));
        replacements.put("%stack%", String.valueOf(selection.stack));
        replacements.put("%selected_total%", assetHandler.formatWithPreSuffix(totalAmount));
        replacements.put("%selected_total_raw%", formatCommandAmount(totalAmount));
        replacements.put("%max_amount%", assetHandler.formatWithPreSuffix(maxAmount));
        replacements.put("%max_amount_raw%", formatCommandAmount(maxAmount));
        replacements.put("%min_amount%", assetHandler.formatWithPreSuffix(minAmount));
        replacements.put("%max_limit%", assetHandler.formatWithPreSuffix(maxLimit));
        replacements.put("%remaining_balance%", assetHandler.formatWithPreSuffix(remainingBalance));
        replacements.put("%remaining%", assetHandler.formatWithPreSuffix(remainingBalance));
        replacements.put("%balance_raw%", formatCommandAmount(balance));
        applyContextReplacements(context, player, replacements);
        return replacements;
    }

    private double getMaxSelectableAmount(WithdrawContext context, Player player, int stack) {
        AssetHandler assetHandler = context.getAssetHandler();
        int normalizedStack = Math.max(1, stack);
        double balance = Math.max(0D, context.getBalance(player));
        double fee = getWithdrawFee(assetHandler, player, normalizedStack);
        double spendableBalance = Math.max(0D, balance - fee);
        double maxAmount = spendableBalance / normalizedStack;

        double configuredMax = getPermissionAmountLimit(assetHandler, player, "Max", maxAmount);
        if (configuredMax > 0D) {
            maxAmount = Math.min(maxAmount, configuredMax);
        }

        maxAmount = normalizeAmount(assetHandler, maxAmount);
        if (assetHandler.isToBigAmount(maxAmount * normalizedStack)) {
            maxAmount = normalizeAmount(assetHandler, maxAmount / 2D);
        }
        return Math.max(0D, maxAmount);
    }

    private double getPermissionAmountLimit(AssetHandler assetHandler, Player player, String key, double defaultValue) {
        double value = assetHandler.getConfig().getDouble("Settings." + key, defaultValue);

        if (!assetHandler.getConfig().getBoolean("Settings.PermissionNotes.Enabled", false)
                || !assetHandler.getConfig().isSection("Settings.PermissionNotes")) {
            return value;
        }

        Section permissionNotes = assetHandler.getConfig().getSection("Settings.PermissionNotes");
        for (String permissionKey : permissionNotes.getRoutesAsStrings(false)) {
            if (assetHandler.hasPermissionNote(player, permissionKey)) {
                value = assetHandler.getConfig().getDouble("Settings.PermissionNotes." + permissionKey + "." + key, value);
            }
        }
        return value;
    }

    private List<Double> getAmountAdjustmentValues(WithdrawGuiProfile profile) {
        List<Double> values = new ArrayList<>();
        for (String rawValue : profile.getStringList(AMOUNT_EDITOR_BASE + ".AdjustmentValues")) {
            double value = parseDouble(rawValue, 0D);
            if (value > 0D) {
                values.add(value);
            }
        }

        if (values.isEmpty()) {
            for (Integer rawValue : profile.getIntList(AMOUNT_EDITOR_BASE + ".AdjustmentValues")) {
                if (rawValue != null && rawValue > 0) {
                    values.add(rawValue.doubleValue());
                }
            }
        }

        if (values.isEmpty()) {
            values.add(1D);
            values.add(10D);
            values.add(100D);
        }
        return values;
    }

    private List<Integer> getStackAdjustmentValues(WithdrawGuiProfile profile) {
        List<Integer> values = new ArrayList<>();
        for (Integer rawValue : profile.getIntList(AMOUNT_EDITOR_BASE + ".StackAdjustmentValues")) {
            if (rawValue != null && rawValue > 0) {
                values.add(rawValue);
            }
        }

        if (values.isEmpty()) {
            for (String rawValue : profile.getStringList(AMOUNT_EDITOR_BASE + ".StackAdjustmentValues")) {
                int value = (int) parseDouble(rawValue, 0D);
                if (value > 0) {
                    values.add(value);
                }
            }
        }

        if (values.isEmpty()) {
            values.add(1);
            values.add(8);
            values.add(16);
        }
        return values;
    }

    private List<Integer> getConfiguredSlots(WithdrawGuiProfile profile, String path, int size) {
        List<Integer> configured = profile.getIntList(path + ".Slots");
        if (configured.isEmpty() && profile.contains(path + ".Slot")) {
            configured = new ArrayList<>();
            configured.add(profile.getInt(path + ".Slot", -1));
        }

        List<Integer> slots = new ArrayList<>();
        for (Integer slot : configured) {
            int normalized = configSlotToIndex(slot == null ? -1 : slot, size);
            if (normalized >= 0) {
                slots.add(normalized);
            }
        }
        return slots;
    }

    private List<Integer> getConfiguredSlotList(WithdrawGuiProfile profile, String path, int size, List<Integer> defaults) {
        List<Integer> configured = profile.getIntList(path);
        if (configured.isEmpty()) {
            configured = defaults == null ? new ArrayList<>() : defaults;
        }

        List<Integer> slots = new ArrayList<>();
        for (Integer slot : configured) {
            int normalized = configSlotToIndex(slot == null ? -1 : slot, size);
            if (normalized >= 0) {
                slots.add(normalized);
            }
        }
        return slots;
    }

    private int getMaxStackSize(AssetHandler assetHandler) {
        return Math.max(1, assetHandler.getConfig().getInt("Settings.MaxStackSize", 64));
    }

    private double normalizeAmount(AssetHandler assetHandler, double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0D) {
            return 0D;
        }

        int scale = assetHandler.getConfig().getBoolean("Settings.DisableDecimals", false) ? 0 : 2;
        return BigDecimal.valueOf(amount).setScale(scale, RoundingMode.DOWN).doubleValue();
    }

    private String formatCommandAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return "0";
        }

        BigDecimal formatted = BigDecimal.valueOf(Math.max(0D, amount)).stripTrailingZeros();
        if (formatted.scale() < 0) {
            formatted = formatted.setScale(0);
        }
        return formatted.toPlainString();
    }

    private double parseDouble(String rawValue, double defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private double calculateTaxAmount(double amount, double taxPercentage) {
        if (amount <= 0D || taxPercentage <= 0D) {
            return 0D;
        }
        return amount * (Math.min(taxPercentage, 100D) / 100D);
    }

    private String formatPercent(double percentage) {
        if (percentage <= 0D) {
            return "0%";
        }
        if (Math.floor(percentage) == percentage) {
            return String.format(Locale.ENGLISH, "%.0f%%", percentage);
        }
        return String.format(Locale.ENGLISH, "%.2f%%", percentage)
                .replaceAll("0+%$", "%")
                .replace(".%", "%");
    }

    private String formatButtonAmount(AssetHandler assetHandler, String amount) {
        if (amount == null || amount.equalsIgnoreCase("all")) {
            return "all";
        }
        try {
            return assetHandler.formatWithPreSuffix(Double.parseDouble(amount));
        } catch (NumberFormatException ignored) {
            return amount;
        }
    }

    private void openBackTarget(Player player, WithdrawContext context) {
        if (context.getMcMMORedeemHandler() != null) {
            openMcMMORedeemMenu(player, context.getMcMMORedeemHandler(), context.getBackPage());
            return;
        }
        openMenu(player);
    }

    private String applyContextPlaceholders(WithdrawContext context, Player player, String value) {
        Map<String, String> replacements = new HashMap<>();
        applyContextReplacements(context, player, replacements);
        return applyRawPlaceholders(player, context.getProfile(), value, replacements);
    }

    private String applyRawPlaceholders(Player player, String value, Map<String, String> replacements) {
        return applyRawPlaceholders(player, mainProfile, value, replacements);
    }

    private String applyRawPlaceholders(Player player, WithdrawGuiProfile profile, String value,
                                        Map<String, String> replacements) {
        String text = applyReplacements(value, replacements);
        return profile.applyPlaceholders(player, text);
    }

    private String applyReplacements(String value, Map<String, String> replacements) {
        String text = value == null ? "" : value;
        if (replacements == null) {
            return text;
        }

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return text;
    }

    private void applyContextReplacements(WithdrawContext context, Player player, Map<String, String> replacements) {
        AssetHandler assetHandler = context.getAssetHandler();
        String skillName = context.getSkillName();
        double balance = context.getBalance(player);

        replacements.put("%type%", skillName == null ? assetHandler.getConfigName() : skillName);
        replacements.put("%type_id%", assetHandler.getID());
        replacements.put("%skill%", skillName == null ? "" : skillName);
        replacements.put("%withdraw_command%", context.getCommandName());
        replacements.put("%balance%", assetHandler.formatWithPreSuffix(balance));
        replacements.put("%skill_balance%", skillName == null ? "" : assetHandler.formatWithPreSuffix(balance));
        if (context.getMcMMORedeemHandler() != null) {
            replacements.put("%credit_balance%", context.getMcMMORedeemHandler().formatWithPreSuffix(
                    context.getMcMMORedeemHandler().getBalanceAsDouble(player)
            ));
        } else {
            replacements.put("%credit_balance%", "");
        }
    }

    private void sendInvalidRequiredItemMessage(WithdrawGuiProfile profile, Player player) {
        sendCooldownMessage(player, "WithdrawGui.InvalidRequiredItem", "%prefix% &cOnly the configured required item can be placed in this GUI.");
    }

    private void sendCooldownMessage(Player player, String path, String fallback) {
        UUID uuid = player.getUniqueId();
        if (!invalidMessageCooldown.add(uuid)) {
            return;
        }

        sendMessage(player, path, fallback);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> invalidMessageCooldown.remove(uuid), 1L);
    }

    private boolean beginWithdrawAction(Player player) {
        UUID uuid = player.getUniqueId();
        if (!withdrawActionCooldown.add(uuid)) {
            return false;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> withdrawActionCooldown.remove(uuid), 2L);
        return true;
    }

    private boolean shouldHandleGuiAction(Player player, InventoryClickEvent event, String actionKey) {
        if (player == null || event == null) {
            return true;
        }

        UUID uuid = player.getUniqueId();
        String key = String.valueOf(actionKey) + ":" + event.getRawSlot();
        long now = System.nanoTime();
        ClickStamp previous = guiActionCooldown.put(uuid, new ClickStamp(key, now));
        boolean allowed = previous == null
                || !previous.key.equals(key)
                || now - previous.timestamp > DUPLICATE_GUI_ACTION_WINDOW_NANOS;
        if (allowed) {
            soundSettings.playClick(player);
        }
        return allowed;
    }

    private void sendMessage(CommandSender sender, String path, String fallback) {
        plugin.getUtils().sendMessage(sender, getMessage(path, fallback));
    }

    private String getMessage(String path, String fallback) {
        return plugin.getMessages().contains(path) ? plugin.getMessages().getString(path) : fallback;
    }

    private int configSlotToIndex(int slot, int size) {
        int normalized = slot - 1;
        return normalized >= 0 && normalized < size ? normalized : -1;
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

    private boolean placesItem(InventoryAction action) {
        return action == InventoryAction.PLACE_ALL
                || action == InventoryAction.PLACE_ONE
                || action == InventoryAction.PLACE_SOME
                || action == InventoryAction.SWAP_WITH_CURSOR;
    }

    private boolean isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0;
    }

    private static final class WithdrawContext {
        private final WithdrawGuiProfile profile;
        private final BeastMcMMORedeemHandler mcMMORedeemHandler;
        private final String skillName;
        private final int backPage;

        private WithdrawContext(WithdrawGuiProfile profile, BeastMcMMORedeemHandler mcMMORedeemHandler,
                                String skillName, int backPage) {
            this.profile = profile;
            this.mcMMORedeemHandler = mcMMORedeemHandler;
            this.skillName = skillName == null || skillName.trim().isEmpty() ? null : skillName.trim();
            this.backPage = Math.max(0, backPage);
        }

        private static WithdrawContext normal(WithdrawGuiProfile profile) {
            return new WithdrawContext(profile, null, null, 0);
        }

        private static WithdrawContext mcMMOCredit(WithdrawGuiProfile profile, BeastMcMMORedeemHandler handler, int backPage) {
            return new WithdrawContext(profile, handler, null, backPage);
        }

        private static WithdrawContext mcMMOSkill(WithdrawGuiProfile profile, BeastMcMMORedeemHandler handler,
                                                  String skillName, int backPage) {
            return new WithdrawContext(profile, handler, skillName, backPage);
        }

        private WithdrawGuiProfile getProfile() {
            return profile;
        }

        private AssetHandler getAssetHandler() {
            return profile.getAssetHandler();
        }

        private BeastMcMMORedeemHandler getMcMMORedeemHandler() {
            return mcMMORedeemHandler;
        }

        private String getSkillName() {
            return skillName;
        }

        private int getBackPage() {
            return backPage;
        }

        private double getBalance(Player player) {
            if (skillName != null && mcMMORedeemHandler != null) {
                return mcMMORedeemHandler.getSkillBalance(player, skillName);
            }
            return getAssetHandler().getBalanceAsDouble(player);
        }

        private String getCommandName() {
            return mcMMORedeemHandler == null ? getAssetHandler().getCommandName() : mcMMORedeemHandler.getCommandName();
        }

        private boolean withdraw(Player player, String amount, int stack,
                                 WithdrawItemRequirement.ItemSource itemSource) {
            if (skillName != null && mcMMORedeemHandler != null
                    && mcMMORedeemHandler.getMcMMORedeemCreditNoteCMD() != null) {
                return mcMMORedeemHandler.getMcMMORedeemCreditNoteCMD()
                        .withdrawSkill(player, skillName, amount, stack, itemSource);
            }
            return getAssetHandler().getWithdrawCMD() != null
                    && getAssetHandler().getWithdrawCMD().withdraw(player, amount, stack, itemSource);
        }
    }

    private static final class WithdrawSelection {
        private double amount;
        private int stack;

        private WithdrawSelection(double amount, int stack) {
            this.amount = amount;
            this.stack = stack;
        }
    }

    private static final class ClickStamp {
        private final String key;
        private final long timestamp;

        private ClickStamp(String key, long timestamp) {
            this.key = key;
            this.timestamp = timestamp;
        }
    }
}
