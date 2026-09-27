package me.sat7.dynamicshop.guis;

import java.util.UUID;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.events.OnChat;
import me.sat7.dynamicshop.files.CustomConfig;
import me.sat7.dynamicshop.utilities.MenuPageUtil;
import me.sat7.dynamicshop.utilities.ShopUtil;
import me.sat7.dynamicshop.utilities.UserUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import me.sat7.dynamicshop.DynamicShop;

import static me.sat7.dynamicshop.utilities.LangUtil.t;

public final class StartPageSettings extends InGameUI
{
    public StartPageSettings()
    {
        uiType = UI_TYPE.StartPageSettings;
    }

    private final int CLOSE = 0;
    private final int NAME = 2;
    private final int LORE = 3;
    private final int ICON = 4;
    private final int CMD = 5;
    private final int SHOP_SHORTCUT = 6;
    private final int DECO = 7;
    private final int DELETE = 8;

    private String pageName = MenuPageUtil.ROOT_PAGE_NAME;
    private int slotIndex;

    public Inventory getGui(Player player, int slotIndex)
    {
        return getGui(player, MenuPageUtil.ROOT_PAGE_NAME, slotIndex);
    }

    public Inventory getGui(Player player, String pageName, int slotIndex)
    {
        this.pageName = MenuPageUtil.Normalize(pageName);
        this.slotIndex = slotIndex;
        UserUtil.userInteractItem.put(player.getUniqueId(), MenuPageUtil.InteractKey(this.pageName, slotIndex));

        inventory = Bukkit.createInventory(player, 9, t(player, "START_PAGE.EDITOR_TITLE"));

        CreateCloseButton(player, CLOSE);

        CreateButton(NAME, Material.BOOK, t(player, "START_PAGE.EDIT_NAME"), "");
        CreateButton(LORE, Material.BOOK, t(player, "START_PAGE.EDIT_LORE"), "");

        CustomConfig pageCfg = MenuPageUtil.GetConfig(this.pageName);
        String iconName = pageCfg != null ? pageCfg.get().getString("Buttons." + slotIndex + ".icon") : "SUNFLOWER";
        Material iconMat = Material.getMaterial(iconName != null ? iconName : "SUNFLOWER");
        if (iconMat == null)
            iconMat = Material.SUNFLOWER;
        CreateButton(ICON, iconMat, t(player, "START_PAGE.EDIT_ICON"), "");

        String cmdString = pageCfg != null ? pageCfg.get().getString("Buttons." + slotIndex + ".action") : null;
        CreateButton(CMD, Material.REDSTONE_TORCH, t(player, "START_PAGE.EDIT_ACTION"), cmdString == null || cmdString.isEmpty() ? null : "§7/" + cmdString);
        CreateButton(SHOP_SHORTCUT, Material.EMERALD, t(player, "START_PAGE.SHOP_SHORTCUT"), "");
        CreateButton(DECO, Material.BLUE_STAINED_GLASS_PANE, t(player, "START_PAGE.CREATE_DECO"), "");
        CreateButton(DELETE, Material.BONE, t(player, "START_PAGE.REMOVE"), t(player, "START_PAGE.REMOVE_LORE"));

        return inventory;
    }

    @Override
    public void OnClickUpperInventory(InventoryClickEvent e)
    {
        Player player = (Player) e.getWhoClicked();
        UUID uuid = player.getUniqueId();
        CustomConfig pageCfg = MenuPageUtil.GetConfig(pageName);
        if (pageCfg == null)
        {
            player.closeInventory();
            return;
        }

        if (e.getSlot() == CLOSE)
        {
            DynaShopAPI.openMenuPage(player, pageName);
        }
        else if (e.getSlot() == DELETE)
        {
            pageCfg.get().set("Buttons." + slotIndex, null);
            pageCfg.save();
            DynaShopAPI.openMenuPage(player, pageName);
        }
        else if (e.getSlot() == NAME)
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "START_PAGE.ENTER_NAME"));
            ShopUtil.closeInventoryWithDelay(player);
            UserUtil.userTempData.put(uuid, "waitforInput" + "btnName");
            OnChat.WaitForInput(player);
        }
        else if (e.getSlot() == LORE)
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "START_PAGE.ENTER_LORE"));
            ShopUtil.closeInventoryWithDelay(player);
            UserUtil.userTempData.put(uuid, "waitforInput" + "btnLore");
            OnChat.WaitForInput(player);
        }
        else if (e.getSlot() == ICON)
        {
            UserUtil.userInteractItem.put(player.getUniqueId(), MenuPageUtil.InteractKey(pageName, slotIndex));
            DynaShopAPI.openItemPalette(player, 1, "", slotIndex, 1, "");
        }
        else if (e.getSlot() == CMD)
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "START_PAGE.ENTER_ACTION"));
            ShopUtil.closeInventoryWithDelay(player);
            UserUtil.userTempData.put(uuid, "waitforInput" + "btnAction");
            OnChat.WaitForInput(player);
        }
        else if (e.getSlot() == SHOP_SHORTCUT)
        {
            DynaShopAPI.openShopListUI(player, 1, pageName, slotIndex);
        }
        else if (e.getSlot() == DECO)
        {
            DynaShopAPI.openColorPicker(player, pageName, slotIndex);
        }
    }
}
