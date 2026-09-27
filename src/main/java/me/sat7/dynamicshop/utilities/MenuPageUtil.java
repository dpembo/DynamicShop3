package me.sat7.dynamicshop.utilities;

import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.constants.Constants;
import me.sat7.dynamicshop.files.CustomConfig;
import me.sat7.dynamicshop.guis.StartPage;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Extra menu pages (category hubs, etc.) stored under {@code Pages/<name>.yml}.
 * Same YAML layout as {@code Startpage.yml}. The root start page remains
 * {@link StartPage#ccStartPage}; its logical name is {@link #ROOT_PAGE_NAME}.
 */
public final class MenuPageUtil
{
    /** Logical name of the root start page (Startpage.yml). Not a file under Pages/. */
    public static final String ROOT_PAGE_NAME = "start";

    public static final HashMap<String, CustomConfig> pageConfigFiles = new HashMap<>();

    private MenuPageUtil()
    {
    }

    public static boolean IsRootPage(String pageName)
    {
        return pageName == null || pageName.isEmpty() || pageName.equalsIgnoreCase(ROOT_PAGE_NAME);
    }

    /** Normalized key for maps / interact strings. Root is always {@link #ROOT_PAGE_NAME}. */
    public static String Normalize(String pageName)
    {
        if (IsRootPage(pageName))
            return ROOT_PAGE_NAME;
        return pageName;
    }

    public static CustomConfig GetConfig(String pageName)
    {
        if (IsRootPage(pageName))
            return StartPage.ccStartPage;

        return pageConfigFiles.get(pageName);
    }

    public static boolean Exists(String pageName)
    {
        if (IsRootPage(pageName))
            return true;
        return pageConfigFiles.containsKey(pageName);
    }

    public static Set<String> GetPageNames()
    {
        return pageConfigFiles.keySet();
    }

    public static void Setup()
    {
        pageConfigFiles.clear();

        File dir = new File(DynamicShop.plugin.getDataFolder(), "Pages");
        if (!dir.exists())
        {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            return;
        }

        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File f : files)
        {
            if (!f.isFile() || !f.getName().toLowerCase(Locale.ROOT).endsWith(".yml"))
                continue;

            String name = f.getName().substring(0, f.getName().length() - 4);
            if (name.isEmpty() || name.equalsIgnoreCase(ROOT_PAGE_NAME))
            {
                DynamicShop.console.sendMessage(Constants.DYNAMIC_SHOP_PREFIX
                        + " Skipping invalid menu page file: " + f.getName()
                        + " (name '" + ROOT_PAGE_NAME + "' is reserved for the start page)");
                continue;
            }

            CustomConfig data = new CustomConfig();
            if (!data.open(name, "Pages"))
                continue;

            EnsureDefaults(data.get(), name);
            data.save();
            pageConfigFiles.put(name, data);
        }
    }

    public static void Reload()
    {
        for (CustomConfig cc : pageConfigFiles.values())
            cc.reload();
        Setup();
    }

    /**
     * Creates a new page file under Pages/ with the same structure as Startpage.yml.
     * @return null on success, or an error key / message on failure
     */
    public static String CreatePage(String pageName)
    {
        pageName = pageName.replace("/", "").replace("\\", "").trim();
        if (pageName.isEmpty())
            return "empty";
        if (pageName.equalsIgnoreCase(ROOT_PAGE_NAME))
            return "reserved";
        if (pageConfigFiles.containsKey(pageName))
            return "exists";

        CustomConfig data = new CustomConfig();
        data.setup(pageName, "Pages");
        EnsureDefaults(data.get(), pageName);
        data.save();
        pageConfigFiles.put(pageName, data);
        return null;
    }

    public static boolean DeletePage(String pageName)
    {
        if (IsRootPage(pageName))
            return false;

        CustomConfig data = pageConfigFiles.remove(pageName);
        if (data == null)
            return false;

        File f = new File(DynamicShop.plugin.getDataFolder(), "Pages/" + pageName + ".yml");
        //noinspection ResultOfMethodCallIgnored
        f.delete();
        return true;
    }

    private static void EnsureDefaults(FileConfiguration cfg, String pageName)
    {
        if (!cfg.contains("Options.Title"))
            cfg.set("Options.Title", "§3§l" + pageName);
        if (!cfg.contains("Options.UiSlotCount"))
            cfg.set("Options.UiSlotCount", 27);
        if (!cfg.contains("Options.LineBreak"))
            cfg.set("Options.LineBreak", "/");

        if (cfg.getConfigurationSection("Buttons") == null
                || cfg.getConfigurationSection("Buttons").getKeys(false).isEmpty())
        {
            cfg.set("Buttons.0.displayName", "§3§lBack");
            cfg.set("Buttons.0.lore", "§fReturn to the start page");
            cfg.set("Buttons.0.icon", "ARROW");
            cfg.set("Buttons.0.action", "ds");
        }
    }

    /** Build interact token: menuPage/&lt;pageKey&gt;/&lt;slot&gt; */
    public static String InteractKey(String pageName, int slotIndex)
    {
        return "menuPage/" + Normalize(pageName) + "/" + slotIndex;
    }

    /** @return [pageKey, slot] or null if not a menu-page interact key */
    public static String[] ParseInteractKey(String interact)
    {
        if (interact == null)
            return null;

        // New form: menuPage/<page>/<slot>
        if (interact.startsWith("menuPage/"))
        {
            String rest = interact.substring("menuPage/".length());
            int slash = rest.lastIndexOf('/');
            if (slash <= 0 || slash >= rest.length() - 1)
                return null;
            return new String[]{rest.substring(0, slash), rest.substring(slash + 1)};
        }

        // Legacy start-page form: startPage/<slot>
        if (interact.startsWith("startPage/"))
        {
            String slot = interact.substring("startPage/".length());
            return new String[]{ROOT_PAGE_NAME, slot};
        }

        return null;
    }
}
