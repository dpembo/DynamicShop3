package me.sat7.dynamicshop.commands;

import me.sat7.dynamicshop.guis.StartPage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.utilities.ShopUtil;

import static me.sat7.dynamicshop.constants.Constants.P_ADMIN_RENAME_SHOP;
import static me.sat7.dynamicshop.utilities.LangUtil.t;

public final class RenameShop extends DSCMD
{
    public RenameShop()
    {
        inGameUseOnly = false;
        permission = P_ADMIN_RENAME_SHOP;
        validArgCount.add(3);
    }

    @Override
    public void SendHelpMessage(Player player)
    {
        player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "HELP.TITLE").replace("{command}", "renameshop"));
        player.sendMessage(" - " + t(player, "HELP.USAGE") + ": /ds renameshop <old name> <new name>");

        player.sendMessage("");
    }

    @Override
    public void RunCMD(String[] args, CommandSender sender)
    {
        if (!CheckValid(args, sender))
            return;

        if (ShopUtil.shopConfigFiles.containsKey(args[1]))
        {
            if (args[1].equals(args[2]))
            {
                sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "ERR.WRONG_USAGE"));
                return;
            }

            String newName = args[2].replace("/", "");
            ShopUtil.renameShop(args[1], newName);
            sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "MESSAGE.CHANGES_APPLIED") + newName);

            updateShopLinksInMenuPage(StartPage.ccStartPage, args[1], args[2]);
            for (me.sat7.dynamicshop.files.CustomConfig pageCfg : me.sat7.dynamicshop.utilities.MenuPageUtil.pageConfigFiles.values())
                updateShopLinksInMenuPage(pageCfg, args[1], args[2]);
        }
        else
        {
            sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "ERR.SHOP_NOT_FOUND"));
        }
    }

    private static void updateShopLinksInMenuPage(me.sat7.dynamicshop.files.CustomConfig pageCfg, String oldName, String newName)
    {
        if (pageCfg == null)
            return;
        ConfigurationSection cs = pageCfg.get().getConfigurationSection("Buttons");
        if (cs == null)
            return;
        for (String c : cs.getKeys(false))
        {
            String actionString = cs.getString(c + ".action");
            if (actionString == null || !actionString.contains(oldName) || !actionString.contains("ds shop"))
                continue;

            cs.set(c + ".action", actionString.replace(oldName, newName));

            String nameString = cs.getString(c + ".displayName");
            if (nameString == null || !nameString.contains(oldName))
                continue;

            cs.set(c + ".displayName", nameString.replace(oldName, newName));
        }
        pageCfg.save();
    }
}
