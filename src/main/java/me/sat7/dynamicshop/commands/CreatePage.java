package me.sat7.dynamicshop.commands;

import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.utilities.MenuPageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.sat7.dynamicshop.constants.Constants.P_ADMIN_CREATE_SHOP;
import static me.sat7.dynamicshop.utilities.LangUtil.t;

/**
 * /ds createpage &lt;name&gt; — create a menu page under Pages/&lt;name&gt;.yml
 * (same format as Startpage.yml).
 */
public final class CreatePage extends DSCMD
{
    public CreatePage()
    {
        inGameUseOnly = false;
        permission = P_ADMIN_CREATE_SHOP;
        validArgCount.add(2);
    }

    @Override
    public void SendHelpMessage(Player player)
    {
        player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "HELP.TITLE").replace("{command}", "createpage"));
        player.sendMessage(" - " + t(player, "HELP.USAGE") + ": /ds createpage <name>");
        player.sendMessage(" - " + t(player, "HELP.CREATE_PAGE"));
        player.sendMessage("");
    }

    @Override
    public void RunCMD(String[] args, CommandSender sender)
    {
        if (!CheckValid(args, sender))
            return;

        String pageName = args[1].replace("/", "").replace("\\", "");
        String err = MenuPageUtil.CreatePage(pageName);

        if (err == null)
        {
            sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "MESSAGE.PAGE_CREATED").replace("{page}", pageName));
            return;
        }

        switch (err)
        {
            case "reserved":
                sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "ERR.PAGE_NAME_RESERVED"));
                break;
            case "exists":
                sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "ERR.PAGE_ALREADY_EXISTS"));
                break;
            default:
                sender.sendMessage(DynamicShop.dsPrefix(sender) + t(sender, "ERR.WRONG_USAGE"));
                break;
        }
    }
}
