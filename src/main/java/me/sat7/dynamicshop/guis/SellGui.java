package me.sat7.dynamicshop.guis;

import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.transactions.Sell;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.sat7.dynamicshop.utilities.LangUtil.t;

/**
 * Deposit-style sell GUI: player places items in the upper inventory, then clicks Sell or Cancel.
 * Shulker boxes and bundles sell their contents only; the container itself is returned empty.
 */
public final class SellGui extends InGameUI
{
    public static final int UI_SIZE = 54;
    public static final int DEPOSIT_SLOTS = 45; // 0..44
    public static final int CANCEL_SLOT = 48;
    public static final int SELL_SLOT = 50;

    /** True after a successful Sell click so close does not return sold items. */
    private boolean transactionDone = false;

    public SellGui()
    {
        uiType = UI_TYPE.SellGui;
    }

    public Inventory getGui(Player player)
    {
        inventory = Bukkit.createInventory(player, UI_SIZE, t(player, "SELL_GUI_TITLE"));

        // Bottom row decoration
        for (int i = 45; i < UI_SIZE; i++)
        {
            if (i == CANCEL_SLOT || i == SELL_SLOT)
                continue;
            CreateButton(i, Material.GRAY_STAINED_GLASS_PANE, " ", 1);
        }

        CreateButton(CANCEL_SLOT, Material.RED_STAINED_GLASS_PANE,
                t(player, "SELL_GUI.CANCEL"), t(player, "SELL_GUI.CANCEL_LORE"));
        CreateButton(SELL_SLOT, Material.LIME_STAINED_GLASS_PANE,
                t(player, "SELL_GUI.SELL"), t(player, "SELL_GUI.SELL_LORE"));

        return inventory;
    }

    public boolean isDepositSlot(int slot)
    {
        return slot >= 0 && slot < DEPOSIT_SLOTS;
    }

    public boolean isButtonSlot(int slot)
    {
        return slot == CANCEL_SLOT || slot == SELL_SLOT || (slot >= DEPOSIT_SLOTS && slot < UI_SIZE);
    }

    @Override
    public void OnClickUpperInventory(InventoryClickEvent e)
    {
        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot == SELL_SLOT)
        {
            e.setCancelled(true);
            processSell(player);
            return;
        }
        if (slot == CANCEL_SLOT || (slot >= DEPOSIT_SLOTS && slot < UI_SIZE && slot != SELL_SLOT))
        {
            e.setCancelled(true);
            if (slot == CANCEL_SLOT)
            {
                cancelAndClose(player);
            }
            return;
        }

        // Deposit slots: allow normal item placement / pickup
        e.setCancelled(false);
    }

    /**
     * Called from OnClick for lower inventory when this UI is open.
     * Shift-click from player inventory into deposit area is allowed if there is space.
     */
    @Override
    public void OnClickLowerInventory(InventoryClickEvent e)
    {
        // Allow shift-click transfer into deposit slots; Bukkit will handle slot placement.
        // Button/filler slots are already occupied so they won't receive items from shift-click into empty slots only.
        e.setCancelled(false);
    }

    public void OnDrag(InventoryDragEvent e)
    {
        // Only allow drag if all affected raw slots are deposit slots
        for (int raw : e.getRawSlots())
        {
            if (raw < UI_SIZE && !isDepositSlot(raw))
            {
                e.setCancelled(true);
                return;
            }
        }
        e.setCancelled(false);
    }

    private void processSell(Player player)
    {
        Inventory top = player.getOpenInventory().getTopInventory();
        List<ItemStack> containersToReturn = new ArrayList<>();
        List<ItemStack> leftover = new ArrayList<>();
        int totalSold = 0;

        for (int i = 0; i < DEPOSIT_SLOTS; i++)
        {
            ItemStack stack = top.getItem(i);
            if (stack == null || stack.getType().isAir())
                continue;

            if (isShulkerBox(stack.getType()))
            {
                List<ItemStack> contents = extractShulkerContents(stack);
                ItemStack emptyBox = clearShulkerContents(stack.clone());
                for (ItemStack content : contents)
                {
                    int sold = Sell.sellExternalStack(player, content, false);
                    totalSold += sold;
                    if (sold < content.getAmount())
                    {
                        ItemStack remain = content.clone();
                        remain.setAmount(content.getAmount() - sold);
                        leftover.add(remain);
                    }
                }
                containersToReturn.add(emptyBox);
                top.setItem(i, null);
            }
            else if (stack.getType() == Material.BUNDLE)
            {
                List<ItemStack> contents = extractBundleContents(stack);
                ItemStack emptyBundle = clearBundleContents(stack.clone());
                for (ItemStack content : contents)
                {
                    int sold = Sell.sellExternalStack(player, content, false);
                    totalSold += sold;
                    if (sold < content.getAmount())
                    {
                        ItemStack remain = content.clone();
                        remain.setAmount(content.getAmount() - sold);
                        leftover.add(remain);
                    }
                }
                containersToReturn.add(emptyBundle);
                top.setItem(i, null);
            }
            else
            {
                int amount = stack.getAmount();
                int sold = Sell.sellExternalStack(player, stack, false);
                totalSold += sold;
                if (sold >= amount)
                {
                    top.setItem(i, null);
                }
                else if (sold > 0)
                {
                    stack.setAmount(amount - sold);
                    leftover.add(stack.clone());
                    top.setItem(i, null);
                }
                else
                {
                    leftover.add(stack.clone());
                    top.setItem(i, null);
                }
            }
        }

        transactionDone = true;

        // Return unsold items and emptied containers
        Map<Integer, ItemStack> overflow = new HashMap<>();
        for (ItemStack item : leftover)
        {
            overflow.putAll(player.getInventory().addItem(item));
        }
        for (ItemStack item : containersToReturn)
        {
            overflow.putAll(player.getInventory().addItem(item));
        }
        for (ItemStack drop : overflow.values())
        {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        if (totalSold > 0)
        {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "MESSAGE.NO_ITEM_TO_SELL_2"));
        }

        player.closeInventory();
    }

    private void cancelAndClose(Player player)
    {
        returnDepositItems(player);
        transactionDone = true;
        player.closeInventory();
    }

    /**
     * Return all deposit-slot items to the player. Called on cancel or on close without selling.
     */
    public void returnDepositItems(Player player)
    {
        if (transactionDone)
            return;

        Inventory top = player.getOpenInventory().getTopInventory();
        Map<Integer, ItemStack> overflow = new HashMap<>();
        for (int i = 0; i < DEPOSIT_SLOTS; i++)
        {
            ItemStack stack = top.getItem(i);
            if (stack == null || stack.getType().isAir())
                continue;
            overflow.putAll(player.getInventory().addItem(stack));
            top.setItem(i, null);
        }
        for (ItemStack drop : overflow.values())
        {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        transactionDone = true;
    }

    public static boolean isShulkerBox(Material mat)
    {
        return mat == Material.SHULKER_BOX
                || mat == Material.WHITE_SHULKER_BOX
                || mat == Material.ORANGE_SHULKER_BOX
                || mat == Material.MAGENTA_SHULKER_BOX
                || mat == Material.LIGHT_BLUE_SHULKER_BOX
                || mat == Material.YELLOW_SHULKER_BOX
                || mat == Material.LIME_SHULKER_BOX
                || mat == Material.PINK_SHULKER_BOX
                || mat == Material.GRAY_SHULKER_BOX
                || mat == Material.LIGHT_GRAY_SHULKER_BOX
                || mat == Material.CYAN_SHULKER_BOX
                || mat == Material.PURPLE_SHULKER_BOX
                || mat == Material.BLUE_SHULKER_BOX
                || mat == Material.BROWN_SHULKER_BOX
                || mat == Material.GREEN_SHULKER_BOX
                || mat == Material.RED_SHULKER_BOX
                || mat == Material.BLACK_SHULKER_BOX;
    }

    private static List<ItemStack> extractShulkerContents(ItemStack shulker)
    {
        List<ItemStack> result = new ArrayList<>();
        ItemMeta meta = shulker.getItemMeta();
        if (!(meta instanceof BlockStateMeta bsm))
            return result;
        if (!(bsm.getBlockState() instanceof ShulkerBox box))
            return result;
        for (ItemStack item : box.getInventory().getContents())
        {
            if (item != null && !item.getType().isAir())
                result.add(item.clone());
        }
        return result;
    }

    private static ItemStack clearShulkerContents(ItemStack shulker)
    {
        ItemMeta meta = shulker.getItemMeta();
        if (meta instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox box)
        {
            box.getInventory().clear();
            bsm.setBlockState(box);
            shulker.setItemMeta(bsm);
        }
        return shulker;
    }

    @SuppressWarnings("deprecation")
    private static List<ItemStack> extractBundleContents(ItemStack bundle)
    {
        List<ItemStack> result = new ArrayList<>();
        ItemMeta meta = bundle.getItemMeta();
        if (!(meta instanceof BundleMeta bm))
            return result;
        for (ItemStack item : bm.getItems())
        {
            if (item != null && !item.getType().isAir())
                result.add(item.clone());
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack clearBundleContents(ItemStack bundle)
    {
        ItemMeta meta = bundle.getItemMeta();
        if (meta instanceof BundleMeta bm)
        {
            bm.setItems(new ArrayList<>());
            bundle.setItemMeta(bm);
        }
        return bundle;
    }
}
