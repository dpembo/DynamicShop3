package me.sat7.dynamicshop.guis;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.files.CustomConfig;
import me.sat7.dynamicshop.utilities.MathUtil;
import me.sat7.dynamicshop.utilities.MenuPageUtil;
import me.sat7.dynamicshop.utilities.ShopUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;

import static me.sat7.dynamicshop.utilities.LangUtil.t;

public class ShopList extends InGameUI
{
    public ShopList()
    {
        uiType = InGameUI.UI_TYPE.StartPage_ShopList;
    }

    private final int CLOSE = 45;
    private final int PAGE = 49;

    private int page;
    private int maxPage;
    private String menuPageName = MenuPageUtil.ROOT_PAGE_NAME;
    private int slotIndex;

    public Inventory getGui(Player player, int page, int slotIndex)
    {
        return getGui(player, page, MenuPageUtil.ROOT_PAGE_NAME, slotIndex);
    }

    public Inventory getGui(Player player, int page, String menuPageName, int slotIndex)
    {
        inventory = Bukkit.createInventory(player, 54, t(player, "START_PAGE.SHOP_LIST_TITLE"));

        this.maxPage = ShopUtil.shopConfigFiles.size() / 45 + 1;
        this.page = MathUtil.Clamp(page, 1, maxPage);
        this.menuPageName = MenuPageUtil.Normalize(menuPageName);
        this.slotIndex = slotIndex;

        CreateExistShopList();
        CreateShopButtons();
        CreateCloseButton(player, CLOSE);
        CreateButton(PAGE, GetPageButtonIconMat(),
                t(player, "START_PAGE.SHOP_LIST.PAGE_TITLE").replace("{curPage}", String.valueOf(this.page)).replace("{maxPage}", String.valueOf(this.maxPage)),
                t(player, "START_PAGE.SHOP_LIST.PAGE_LORE"));

        return inventory;
    }

    @Override
    public void OnClickUpperInventory(InventoryClickEvent e)
    {
        Player player = (Player) e.getWhoClicked();

        if (e.getSlot() == CLOSE)
        {
            DynaShopAPI.openMenuPageSettingGui(player, menuPageName, slotIndex);
        } else if (e.getSlot() == PAGE)
        {
            if (e.isLeftClick())
            {
                page--;
                if (page < 1)
                    page = maxPage;
            } else if (e.isRightClick())
            {
                page++;
                if (page > maxPage)
                    page = 1;
            }

            DynaShopAPI.openShopListUI(player, page, menuPageName, slotIndex);
        } else if (e.getCurrentItem() != null &&
                (e.getCurrentItem().getType() == Material.GREEN_STAINED_GLASS || e.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS))
        {
            String shopName = e.getCurrentItem().getItemMeta().getDisplayName();
            CustomConfig pageCfg = MenuPageUtil.GetConfig(menuPageName);
            if (pageCfg == null)
            {
                player.closeInventory();
                return;
            }
            pageCfg.get().set("Buttons." + slotIndex + ".displayName", "§3" + shopName);
            pageCfg.get().set("Buttons." + slotIndex + ".lore", t(player, "START_PAGE.DEFAULT_SHOP_LORE"));
            pageCfg.get().set("Buttons." + slotIndex + ".action", "ds shop " + shopName);
            pageCfg.save();

            DynaShopAPI.openMenuPage(player, menuPageName);
        }
    }

    private void CreateShopButtons()
    {
        int idx = 0;
        int slotIdx = 0;
        for (String shopName : ShopUtil.shopConfigFiles.keySet())
        {
            if (idx > page * 45)
                break;

            if (idx >= (page - 1) * 45)
            {
                CreateButton(slotIdx, existShopList.contains(shopName) ? Material.GREEN_STAINED_GLASS : Material.GRAY_STAINED_GLASS, shopName, "");
                slotIdx++;
            }

            idx++;
        }
    }

    ArrayList<String> existShopList = new ArrayList<>();
    private void CreateExistShopList()
    {
        existShopList.clear();
        CustomConfig pageCfg = MenuPageUtil.GetConfig(menuPageName);
        if (pageCfg == null)
            return;
        ConfigurationSection cs = pageCfg.get().getConfigurationSection("Buttons");
        if (cs != null)
        {
            for (String c : cs.getKeys(false))
            {
                String actionString = cs.getString(c + ".action");
                if (actionString == null || !actionString.contains("ds shop"))
                    continue;

                existShopList.add(actionString.replace("ds shop ", ""));
            }
        }
    }
}
