package me.sat7.dynamicshop.commands;

import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.utilities.MenuPageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.sat7.dynamicshop.constants.Constants.P_ADMIN_DELETE_SHOP;
import static me.sat7.dynamicshop.utilities.LangUtil.t;

/**
 * /ds deletepage &lt;name&gt; — delete a menu page (not the root start page).
 */
public final class DeletePage extends DSCMD
{
    public DeletePage()
    {
        inGameUseOnly = false;
        permission = P_ADMIN_DELETE_SHOP;
        validArgCount.add(2);
    }

    @Override
    public void SendHelpMessage(Player player)
    {
        player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "HELP.TITLE").replace("{command}", "deletepage"));
        player.sendMessage(" - " + t(player, "HELP.USAGE") + ": /ds deletepage <name>");
        player.sendMessage("");
    }

    @Override
    public void RunCMD(String[] args, CommandSender sender)
    {
        if (!CheckValid(args, sender))
            return;

        String pageName = args[1].replace("/", "").replace("\\", "");

        if (MenuPageUtil.IsRootPage(pageName))
        {
            sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "ERR.PAGE_NAME_RESERVED"));
            return;
        }

        if (!MenuPageUtil.Exists(pageName))
        {
            sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "ERR.PAGE_NOT_FOUND"));
            return;
        }

        if (MenuPageUtil.DeletePage(pageName))
            sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "MESSAGE.PAGE_DELETED").replace("{page}", pageName));
        else
            sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "ERR.PAGE_NOT_FOUND"));
    }
}
