package me.sat7.dynamicshop.commands;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.utilities.MenuPageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.sat7.dynamicshop.constants.Constants.P_USE;
import static me.sat7.dynamicshop.utilities.LangUtil.t;

/**
 * /ds page &lt;name&gt; — open a menu page (Startpage-style hub under Pages/&lt;name&gt;.yml).
 * /ds page start — same as opening the root start page.
 */
public final class Page extends DSCMD
{
    public Page()
    {
        inGameUseOnly = true;
        permission = P_USE;
        validArgCount.add(2);
    }

    @Override
    public void SendHelpMessage(Player player)
    {
        player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "HELP.TITLE").replace("{command}", "page"));
        player.sendMessage(" - " + t(player, "HELP.USAGE") + ": /ds page <name>");
        player.sendMessage(" - " + t(player, "HELP.PAGE"));
        player.sendMessage("");
    }

    @Override
    public void RunCMD(String[] args, CommandSender sender)
    {
        if (!CheckValid(args, sender))
            return;

        Player player = (Player) sender;
        String pageName = args[1].replace("/", "").replace("\\", "");

        if (MenuPageUtil.IsRootPage(pageName))
        {
            DynaShopAPI.openStartPage(player);
            return;
        }

        if (!MenuPageUtil.Exists(pageName))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.PAGE_NOT_FOUND"));
            return;
        }

        DynaShopAPI.openMenuPage(player, pageName);
    }
}
