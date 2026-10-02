package me.sat7.dynamicshop.commands;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.constants.Constants;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.sat7.dynamicshop.utilities.LangUtil.t;

public class SellGuiCmd implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player player))
            return false;

        if (!player.hasPermission(Constants.P_SELL_GUI))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.NO_PERMISSION"));
            return true;
        }

        if (player.getGameMode() == GameMode.CREATIVE && !player.hasPermission(Constants.P_ADMIN_CREATIVE))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.CREATIVE"));
            return true;
        }

        if (!DynaShopAPI.recreateUserData(player))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.NO_USER_ID"));
            return true;
        }

        DynaShopAPI.openSellGui(player);
        return true;
    }
}
