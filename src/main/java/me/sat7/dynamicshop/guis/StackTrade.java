package me.sat7.dynamicshop.guis;

import java.util.ArrayList;
import java.util.Arrays;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.files.CustomConfig;
import me.sat7.dynamicshop.transactions.Buy;
import me.sat7.dynamicshop.transactions.Calc;
import me.sat7.dynamicshop.utilities.ConfigUtil;
import me.sat7.dynamicshop.utilities.ShopUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static me.sat7.dynamicshop.constants.Constants.P_ADMIN_SHOP_EDIT;
import static me.sat7.dynamicshop.utilities.LangUtil.n;
import static me.sat7.dynamicshop.utilities.LangUtil.t;

// "Buy in Stacks": lets a player dial in a purchase quantity in whole stacks (of the item's own
// max stack size - 1 for anything that doesn't stack) with -32/-16/-1 and +1/+16/+32 buttons, see
// the running total and price, then confirm. Buy-only: selling doesn't need a quantity picker the
// same way (players usually just want to offload everything they're carrying), and ItemTrade's
// existing shift-click "sell all matching items" already covers that case.
// Reuses the existing Buy.buy() transaction method directly, so pricing, stock, permissions,
// trade limits, delivery charges and logging all behave identically to a normal trade - this
// only changes how the quantity is chosen.
public final class StackTrade extends InGameUI
{
    public StackTrade()
    {
        uiType = UI_TYPE.StackTrade;
    }

    private static final int CLOSE = 0;
    private static final int DEC_32 = 1;
    private static final int DEC_16 = 2;
    private static final int DEC_1 = 3;
    private static final int CURRENT = 4;
    private static final int INC_1 = 5;
    private static final int INC_16 = 6;
    private static final int INC_32 = 7;
    private static final int CONFIRM = 8;

    private Player player;
    private String shopName;
    private String tradeIdx;
    private String material;
    private ItemMeta itemMeta;
    private int unitSize;
    private int maxStacks;
    private int stackCount = 1;

    public Inventory getGui(Player player, String shopName, String tradeIdx)
    {
        return getGui(player, shopName, tradeIdx, 1);
    }

    private Inventory getGui(Player player, String shopName, String tradeIdx, int initialStacks)
    {
        this.player = player;
        this.shopName = shopName;
        this.tradeIdx = tradeIdx;

        FileConfiguration shopData = ShopUtil.shopConfigFiles.get(shopName).get();
        this.material = shopData.getString(tradeIdx + ".mat");
        this.itemMeta = (ItemMeta) shopData.get(tradeIdx + ".itemStack");

        Material mat = Material.getMaterial(material);
        int vanillaStackSize = Math.max(1, mat == null ? 64 : mat.getMaxStackSize());
        // Per-item override: some items are worth pricing/trading in a "logical" stack size
        // that differs from the vanilla one (e.g. selling gunpowder in batches of 16 even
        // though it vanilla-stacks to 64). Set <idx>.stackTradeUnit in the shop file to use
        // that instead of the material's real max stack size.
        this.unitSize = Math.max(1, shopData.getInt(tradeIdx + ".stackTradeUnit", vanillaStackSize));
        this.maxStacks = Math.max(1, ConfigUtil.GetMaxStackTradeAmount());
        this.stackCount = Math.max(1, Math.min(initialStacks, maxStacks));

        String uiTitle = shopData.getBoolean("Options.enable", true) ? "" : t(player, "SHOP.DISABLED");
        uiTitle += t(player, "STACK_TRADE.TITLE_BUY");
        inventory = Bukkit.createInventory(player, 9, uiTitle);

        CreateCloseButton(player, CLOSE);
        CreateStepButton(DEC_32, -32);
        CreateStepButton(DEC_16, -16);
        CreateStepButton(DEC_1, -1);
        CreateCurrentButton();
        CreateStepButton(INC_1, 1);
        CreateStepButton(INC_16, 16);
        CreateStepButton(INC_32, 32);
        CreateConfirmButton();

        return inventory;
    }

    private void CreateStepButton(int slotIndex, int step)
    {
        String key = step < 0 ? "STACK_TRADE.DECREASE" : "STACK_TRADE.INCREASE";
        String name = t(player, key).replace("{num}", String.valueOf(Math.abs(step)));
        Material icon = step < 0 ? InGameUI.GetBuyToggleButtonIconMat() : InGameUI.GetSellToggleButtonIconMat();
        CreateButton(slotIndex, icon, name, (String) null);
    }

    private void CreateCurrentButton()
    {
        ItemStack itemStack = new ItemStack(Material.getMaterial(material), 1);
        if (itemMeta != null)
            itemStack.setItemMeta(itemMeta);

        ItemMeta meta = itemStack.getItemMeta();

        int totalAmount = stackCount * unitSize;
        double[] calcResult = Calc.calcTotalCost(shopName, tradeIdx, totalAmount);
        String priceText = FormatPrice(calcResult[0]);

        String name = t(player, "STACK_TRADE.SELECTED_NAME").replace("{stacks}", String.valueOf(stackCount));
        String lore = t(player, "STACK_TRADE.SELECTED_LORE")
                .replace("{amount}", n(totalAmount))
                .replace("{item}", ItemsBeautifiedName())
                .replace("{price}", priceText)
                .replace("{max}", String.valueOf(maxStacks));

        meta.setDisplayName(name);
        meta.setLore(new ArrayList<>(Arrays.asList(lore.split("\n"))));
        itemStack.setItemMeta(meta);

        inventory.setItem(CURRENT, itemStack);
    }

    private void CreateConfirmButton()
    {
        int totalAmount = stackCount * unitSize;
        double[] calcResult = Calc.calcTotalCost(shopName, tradeIdx, totalAmount);
        String priceText = FormatPrice(calcResult[0]);

        String title = t(player, "STACK_TRADE.CONFIRM_BUY")
                .replace("{amount}", n(totalAmount))
                .replace("{item}", ItemsBeautifiedName());
        String lore = "§7" + priceText;

        CreateButton(CONFIRM, InGameUI.GetStackTradeButtonIconMat(), title, lore);
    }

    private String ItemsBeautifiedName()
    {
        if (itemMeta != null && itemMeta.hasDisplayName())
            return itemMeta.getDisplayName();

        Material mat = Material.getMaterial(material);
        return mat == null ? material : me.sat7.dynamicshop.utilities.ItemsUtil.getBeautifiedName(mat);
    }

    private String FormatPrice(double price)
    {
        String currency = ShopUtil.GetCurrency(ShopUtil.shopConfigFiles.get(shopName).get());
        if (ShopUtil.IsMultiCurrency(currency))
            return me.sat7.dynamicshop.economyhook.MultiCurrencyHook.FormatAmount(ShopUtil.GetMultiCurrencyId(currency), price, java.math.RoundingMode.CEILING);
        return n(price);
    }

    @Override
    public void OnClickUpperInventory(InventoryClickEvent e)
    {
        player = (Player) e.getWhoClicked();

        if (!CheckShopIsEnable())
            return;

        switch (e.getSlot())
        {
            case CLOSE:
                DynaShopAPI.openItemTradeGui(player, shopName, tradeIdx);
                return;
            case DEC_32:
                ChangeStackCount(-32);
                return;
            case DEC_16:
                ChangeStackCount(-16);
                return;
            case DEC_1:
                ChangeStackCount(-1);
                return;
            case INC_1:
                ChangeStackCount(1);
                return;
            case INC_16:
                ChangeStackCount(16);
                return;
            case INC_32:
                ChangeStackCount(32);
                return;
            case CONFIRM:
                Confirm();
                return;
            default:
        }
    }

    private void ChangeStackCount(int step)
    {
        stackCount = Math.max(1, Math.min(maxStacks, stackCount + step));
        RefreshUI();
    }

    private void Confirm()
    {
        FileConfiguration shopData = ShopUtil.shopConfigFiles.get(shopName).get();
        ConfigurationSection optionS = shopData.getConfigurationSection("Options");

        String permission = optionS.getString("permission");
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission) && !player.hasPermission(permission + ".buy"))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.NO_PERMISSION"));
            return;
        }

        int totalAmount = stackCount * unitSize;

        ItemStack tempIS = new ItemStack(Material.getMaterial(material), totalAmount);
        if (itemMeta != null)
            tempIS.setItemMeta(itemMeta);

        boolean infiniteStock = shopData.getInt(tradeIdx + ".stock") <= 0;

        int deliveryCharge = ShopUtil.CalcShipping(shopName, player);
        if (optionS.contains("world") && optionS.contains("pos1") && optionS.contains("pos2") && optionS.contains("flag.deliverycharge"))
        {
            if (deliveryCharge == -1)
            {
                player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "MESSAGE.DELIVERY_CHARGE_NA"));
                return;
            }
        }

        Buy.buy(ShopUtil.GetCurrency(shopData), player, shopName, tradeIdx, tempIS, deliveryCharge, infiniteStock);

        // Buy.buy() already reopens the normal trade screen once the transaction completes,
        // so there's nothing left for this screen to do.
    }

    @Override
    public void RefreshUI()
    {
        if (!CheckShopIsEnable())
            return;

        CreateCurrentButton();
        CreateConfirmButton();
    }

    private boolean CheckShopIsEnable()
    {
        if (!ShopUtil.shopConfigFiles.containsKey(shopName))
        {
            player.closeInventory();
            return false;
        }

        CustomConfig data = ShopUtil.shopConfigFiles.get(shopName);
        if (!data.get().contains(tradeIdx) || !data.get().getString(tradeIdx + ".mat").equals(material))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.INVALID_TRANSACTION"));
            player.closeInventory();
            return false;
        }

        boolean ret = DynaShopAPI.IsShopEnable(shopName) || player.hasPermission(P_ADMIN_SHOP_EDIT);
        if (!ret)
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "MESSAGE.SHOP_IS_CLOSED_BY_ADMIN"));
            player.closeInventory();
        }

        return ret;
    }
}
