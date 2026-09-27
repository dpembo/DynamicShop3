package me.sat7.dynamicshop.guis;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.constants.Constants;
import me.sat7.dynamicshop.transactions.Buy;
import me.sat7.dynamicshop.utilities.LangUtil;
import me.sat7.dynamicshop.utilities.ShopUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

import static me.sat7.dynamicshop.utilities.LangUtil.money;
import static me.sat7.dynamicshop.utilities.LangUtil.n;
import static me.sat7.dynamicshop.utilities.LangUtil.t;

/**
 * Shown when a buy request is reduced because the player cannot afford the full amount.
 * Confirm runs the purchase for the affordable quantity; Cancel returns to the trade screen.
 */
public final class BuyConfirm extends InGameUI
{
    public BuyConfirm()
    {
        uiType = UI_TYPE.BuyConfirm;
    }

    private static final int CANCEL = 2;
    private static final int INFO = 4;
    private static final int OK = 6;

    private Player player;
    private String currency;
    private String shopName;
    private String tradeIdx;
    private ItemStack itemTemplate;
    private int requestedAmount;
    private int affordableAmount;
    private double totalCost;
    private double deliveryCharge;
    private boolean infiniteStock;

    public Inventory getGui(Player player,
                            String currency,
                            String shopName,
                            String tradeIdx,
                            ItemStack itemTemplate,
                            int requestedAmount,
                            int affordableAmount,
                            double totalCost,
                            double deliveryCharge,
                            boolean infiniteStock)
    {
        this.player = player;
        this.currency = currency;
        this.shopName = shopName;
        this.tradeIdx = tradeIdx;
        this.itemTemplate = itemTemplate.clone();
        this.requestedAmount = requestedAmount;
        this.affordableAmount = affordableAmount;
        this.totalCost = totalCost;
        this.deliveryCharge = deliveryCharge;
        this.infiniteStock = infiniteStock;

        inventory = Bukkit.createInventory(player, 9, t(player, "BUY_CONFIRM.TITLE"));

        CreateButton(CANCEL, Material.RED_STAINED_GLASS_PANE, t(player, "BUY_CONFIRM.CANCEL"), t(player, "BUY_CONFIRM.CANCEL_LORE"));
        CreateInfoButton();
        CreateButton(OK, Material.LIME_STAINED_GLASS_PANE, t(player, "BUY_CONFIRM.OK"), t(player, "BUY_CONFIRM.OK_LORE"));

        return inventory;
    }

    private void CreateInfoButton()
    {
        ItemStack display = itemTemplate.clone();
        display.setAmount(Math.max(1, Math.min(affordableAmount, display.getMaxStackSize())));

        ItemMeta meta = display.getItemMeta();
        if (meta != null)
        {
            String priceStr = FormatCost(totalCost);
            String name = t(player, "BUY_CONFIRM.INFO_NAME")
                    .replace("{amount}", n(affordableAmount))
                    .replace("{requested}", n(requestedAmount));
            String lore = t(player, "BUY_CONFIRM.INFO_LORE")
                    .replace("{amount}", n(affordableAmount))
                    .replace("{requested}", n(requestedAmount))
                    .replace("{price}", priceStr);

            meta.setDisplayName(name);
            meta.setLore(new ArrayList<>(Arrays.asList(lore.split("\n"))));
            display.setItemMeta(meta);
        }

        inventory.setItem(INFO, display);
    }

    private String FormatCost(double cost)
    {
        if (ShopUtil.IsMultiCurrency(currency))
            return me.sat7.dynamicshop.economyhook.MultiCurrencyHook.FormatAmount(
                    ShopUtil.GetMultiCurrencyId(currency), cost, java.math.RoundingMode.CEILING);
        if (currency.equalsIgnoreCase(Constants.S_JOBPOINT)
                || currency.equalsIgnoreCase(Constants.S_PLAYERPOINT)
                || currency.equalsIgnoreCase(Constants.S_EXP))
            return n(cost);
        return money(cost);
    }

    @Override
    public void OnClickUpperInventory(InventoryClickEvent e)
    {
        player = (Player) e.getWhoClicked();

        if (e.getSlot() == CANCEL)
        {
            DynaShopAPI.openItemTradeGui(player, shopName, tradeIdx);
            return;
        }

        if (e.getSlot() == OK)
        {
            ItemStack toBuy = itemTemplate.clone();
            toBuy.setAmount(affordableAmount);
            // skipConfirm=true: already confirmed this reduced quantity
            Buy.buy(currency, player, shopName, tradeIdx, toBuy, deliveryCharge, infiniteStock, true);
        }
    }
}
