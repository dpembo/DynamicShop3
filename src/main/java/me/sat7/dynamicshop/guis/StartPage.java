package me.sat7.dynamicshop.guis;

import java.util.ArrayList;
import java.util.Arrays;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.utilities.ConfigUtil;
import me.sat7.dynamicshop.utilities.ItemsUtil;
import me.sat7.dynamicshop.utilities.LangUtil;
import me.sat7.dynamicshop.utilities.MenuPageUtil;
import me.sat7.dynamicshop.utilities.ShopNameFormatter;
import me.sat7.dynamicshop.utilities.ShopUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.constants.Constants;
import me.sat7.dynamicshop.files.CustomConfig;

import static me.sat7.dynamicshop.constants.Constants.P_ADMIN_SHOP_EDIT;
import static me.sat7.dynamicshop.utilities.LangUtil.t;

/**
 * Start page and extra menu pages (category hubs). Root config is {@link #ccStartPage}
 * (Startpage.yml). Other pages live under Pages/&lt;name&gt;.yml with the same layout.
 */
public final class StartPage extends InGameUI
{
    public StartPage()
    {
        uiType = UI_TYPE.StartPage;
    }

    public static CustomConfig ccStartPage = new CustomConfig();

    public static void setupStartPageFile()
    {
        ccStartPage.setup("Startpage", null);
        ccStartPage.get().options().header("LineBreak: Do not use \\, | and brackets. Recommended : /, _");
        ccStartPage.get().addDefault("Options.Title", "§3§lStart Page");
        ccStartPage.get().addDefault("Options.UiSlotCount", 27);
        ccStartPage.get().addDefault("Options.LineBreak", "/");

        if (ccStartPage.get().getKeys(false).size() == 0)
        {
            ccStartPage.get().set("Buttons.0.displayName", "§3§lExample Button");
            ccStartPage.get().set("Buttons.0.lore", "§fThis is Example Button/§aClick empty slot to create new button");
            ccStartPage.get().set("Buttons.0.icon", "SUNFLOWER");
            ccStartPage.get().set("Buttons.0.action", "");
        }
        ccStartPage.get().options().copyDefaults(true);
        ccStartPage.save();
    }

    /** Normalized page key; {@link MenuPageUtil#ROOT_PAGE_NAME} for Startpage.yml. */
    private String pageName = MenuPageUtil.ROOT_PAGE_NAME;
    private int selectedIndex = -1;

    public Inventory getGui(Player player)
    {
        return getGui(player, MenuPageUtil.ROOT_PAGE_NAME);
    }

    public Inventory getGui(Player player, String pageName)
    {
        selectedIndex = -1;
        this.pageName = MenuPageUtil.Normalize(pageName);

        CustomConfig pageCfg = MenuPageUtil.GetConfig(this.pageName);
        if (pageCfg == null)
        {
            inventory = Bukkit.createInventory(player, 9, "§cPage not found");
            return inventory;
        }

        inventory = Bukkit.createInventory(player, pageCfg.get().getInt("Options.UiSlotCount"),
                ShopNameFormatter.format(pageCfg.get().getString("Options.Title"), ConfigUtil.GetUseHexColorCode()));

        ConfigurationSection cs = pageCfg.get().getConfigurationSection("Buttons");
        if (cs == null)
            return inventory;

        String lineBreak = pageCfg.get().getString("Options.LineBreak", "/");

        for (String s : cs.getKeys(false))
        {
            try
            {
                int idx = Integer.parseInt(s);

                String name = " ";
                if (cs.contains(s + ".displayName"))
                {
                    name = cs.getConfigurationSection(s).getString("displayName");
                }

                ArrayList<String> tempList = new ArrayList<>();
                if (cs.contains(s + ".lore"))
                {
                    String[] lore = cs.getConfigurationSection(s).getString("lore").split(lineBreak);
                    tempList.addAll(Arrays.asList(lore));
                }

                if (player.hasPermission(P_ADMIN_SHOP_EDIT))
                {
                    String cmd = cs.getString(s + ".action");
                    if (cmd != null && cmd.length() > 0)
                    {
                        tempList.add(t(player, "START_PAGE.ITEM_MOVE_LORE"));
                    } else
                    {
                        tempList.add(t(player, "START_PAGE.ITEM_REMOVE_LORE"));
                        tempList.add(t(player, "START_PAGE.ITEM_COPY_LORE"));
                    }
                    tempList.add(t(player, "START_PAGE.ITEM_EDIT_LORE"));
                }

                ItemStack btn = new ItemStack(Material.getMaterial(cs.getConfigurationSection(s).getString("icon")));
                ItemMeta meta;

                if (cs.contains(s + ".itemStack"))
                {
                    ItemMeta tempMeta = (ItemMeta) cs.get(s + ".itemStack");
                    meta = tempMeta.clone();
                }
                else
                {
                    meta = btn.getItemMeta();
                }

                meta.displayName(ShopNameFormatter.formatItemName(name, ConfigUtil.GetUseHexColorCode()));
                meta.lore(ShopNameFormatter.formatLore(tempList, ConfigUtil.GetUseHexColorCode()));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                btn.setItemMeta(meta);
                inventory.setItem(idx, btn);

            } catch (Exception e)
            {
                DynamicShop.console.sendMessage(Constants.DYNAMIC_SHOP_PREFIX + "Fail to create menu page button");
                DynamicShop.console.sendMessage(Constants.DYNAMIC_SHOP_PREFIX + e);
            }
        }
        return inventory;
    }

    public String getPageName()
    {
        return pageName;
    }

    private CustomConfig pageConfig()
    {
        return MenuPageUtil.GetConfig(pageName);
    }

    private void reopen(Player player)
    {
        DynaShopAPI.openMenuPage(player, pageName);
    }

    @Override
    public void OnClickUpperInventory(InventoryClickEvent e)
    {
        Player player = (Player) e.getWhoClicked();
        CustomConfig pageCfg = pageConfig();
        if (pageCfg == null)
        {
            player.closeInventory();
            return;
        }

        String lineBreak = pageCfg.get().getString("Options.LineBreak", "/");

        if (e.isLeftClick())
        {
            if(e.isShiftClick() && player.hasPermission(P_ADMIN_SHOP_EDIT))
            {
                if(e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR)
                {
                    String actionString = pageCfg.get().getString("Buttons." + e.getSlot() + ".action");
                    if(actionString == null || actionString.isEmpty())
                    {
                        pageCfg.get().set("Buttons." + e.getSlot(), null);
                        pageCfg.save();
                        reopen(player);
                    }
                }
            }
            else
            {
                if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
                {
                    if (player.hasPermission(P_ADMIN_SHOP_EDIT))
                    {
                        pageCfg.get().set("Buttons." + e.getSlot() + ".displayName", "§3New Button");
                        pageCfg.get().set("Buttons." + e.getSlot() + ".lore", "§fnew button");
                        pageCfg.get().set("Buttons." + e.getSlot() + ".icon", Material.SUNFLOWER.name());
                        pageCfg.get().set("Buttons." + e.getSlot() + ".action", "");
                        pageCfg.save();

                        reopen(player);
                    } else
                    {
                        return;
                    }
                }

                String actionStr = pageCfg.get().getString("Buttons." + e.getSlot() + ".action");
                if (actionStr != null && actionStr.length() > 0)
                {
                    String[] action = actionStr.split(lineBreak);

                    for (String s : action)
                    {
                        Bukkit.dispatchCommand(player, s);
                    }
                }
            }
        }
        else if (player.hasPermission(P_ADMIN_SHOP_EDIT))
        {
            if (e.isShiftClick())
            {
                if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

                selectedIndex = e.getSlot();
                DynaShopAPI.openMenuPageSettingGui(player, pageName, selectedIndex);
            }
            else
            {
                if (selectedIndex == -1)
                {
                    if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

                    selectedIndex = e.getSlot();
                    player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "SHOP.ITEM_MOVE_SELECTED"));
                } else
                {
                    if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) return;

                    pageCfg.get().set("Buttons." + e.getSlot() + ".displayName", pageCfg.get().get("Buttons." + selectedIndex + ".displayName"));
                    pageCfg.get().set("Buttons." + e.getSlot() + ".lore", pageCfg.get().get("Buttons." + selectedIndex + ".lore"));
                    pageCfg.get().set("Buttons." + e.getSlot() + ".icon", pageCfg.get().get("Buttons." + selectedIndex + ".icon"));
                    pageCfg.get().set("Buttons." + e.getSlot() + ".itemStack", pageCfg.get().get("Buttons." + selectedIndex + ".itemStack"));
                    pageCfg.get().set("Buttons." + e.getSlot() + ".action", pageCfg.get().get("Buttons." + selectedIndex + ".action"));

                    String srcAction = pageCfg.get().getString("Buttons." + selectedIndex + ".action");
                    if (srcAction != null && srcAction.length() > 0)
                    {
                        pageCfg.get().set("Buttons." + selectedIndex, null);
                    }

                    pageCfg.save();

                    reopen(player);
                }
            }
        }
    }

    @Override
    public void OnClickLowerInventory(InventoryClickEvent e)
    {
        if(!ConfigUtil.GetEnableInventoryClickSearch_StartPage())
            return;

        Player player = (Player) e.getWhoClicked();
        ItemStack itemStack = e.getCurrentItem();

        if(itemStack == null || itemStack.getType().isAir())
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "MESSAGE.CLICK_YOUR_ITEM_START_PAGE"));
            return;
        }

        if(!e.isLeftClick() && !e.isRightClick())
            return;

        boolean isSell = e.isRightClick();

        String[] ret;

        if(isSell)
        {
            ret = ShopUtil.FindTheBestShopToSell(player, e.getCurrentItem());
        }
        else
        {
            ret = ShopUtil.FindTheBestShopToBuy(player, e.getCurrentItem());
        }

        if(ret[1].equals("-2"))
        {
            return;
        }

        if(ret[0].isEmpty())
            return;

        DynaShopAPI.openShopGui(player, ret[0], Integer.parseInt(ret[1]) / 45 + 1);

        boolean useLocalizedName = ConfigUtil.GetLocalizedItemName();
        String message;
        if(isSell)
        {
            message = DynamicShop.dsPrefix(player) + t(player, "MESSAGE.MOVE_TO_BEST_SHOP_SELL", !useLocalizedName);
        }
        else
        {
            message = DynamicShop.dsPrefix(player) + t(player, "MESSAGE.MOVE_TO_BEST_SHOP_BUY", !useLocalizedName);
        }

        if (useLocalizedName)
        {
            message = message.replace("{item}", "<item>");
            LangUtil.sendMessageWithLocalizedItemName(player, message, e.getCurrentItem().getType());
        }
        else
        {
            String itemName = ItemsUtil.getBeautifiedName(e.getCurrentItem().getType());
            player.sendMessage(message.replace("{item}", itemName));
        }
    }
}
