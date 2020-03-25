package lv.tfu.minecraft.selllevel;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

@SuppressWarnings("unused")
public class SellLevel extends JavaPlugin {
    private Economy economy;

    @Override
    public void onEnable() {
        getLogger().info("Initialization plugin");

        getConfig().options().copyDefaults(true);
        saveConfig();

        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                throw new Exception();
            }
            RegisteredServiceProvider<Economy> registeredServiceProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
            economy = registeredServiceProvider.getProvider();
            getLogger().info("Vault plugin connected - economy rewards enabled");
        } catch (Exception e) {
            getLogger().warning("Economy plugin not installed!");
        }

        getLogger().info("Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = ((Player) sender).getPlayer();

            if (args.length == 1 && (isNumeric(args[0]) || args[0].equalsIgnoreCase("max"))) {
                boolean isMax = args[0].equalsIgnoreCase("max");
                HashMap<String, Object> exchange = isMax
                        ? getExchangeByLevel(player.getLevel(), true)
                        : getExchangeByLevel(Integer.parseInt(args[0]), false);
                if (exchange != null) {
                    if (isMax) {
                        player.sendMessage(getLanguage(
                                "foundMaximumExchange",
                                new String[]{"level", "price"},
                                new String[]{String.valueOf(exchange.get("level")), String.valueOf(exchange.get("price"))}
                        ));
                    }
                    makeProcess(player, (Integer) exchange.get("level"), (Double) exchange.get("price"));
                } else {
                    player.sendMessage(getLanguage("noSuchExchange"));
                }
            } else {
                player.sendMessage(getLanguage("description"));
                player.sendMessage(getLanguage("exchangeList"));
                for (String exchangeLevel : getConfig().getConfigurationSection("exchanges").getKeys(false)) {
                    player.sendMessage(getLanguage(
                            "exchangeEntry",
                            new String[]{"level", "price"},
                            new String[]{exchangeLevel, getConfig().getString("exchanges." + exchangeLevel)}
                    ));
                }
            }
        }
        return true;
    }

    private HashMap<String, Object> getExchangeByLevel(int level, boolean isMaximal) {
        HashMap<String, Object> suitableExchange = null;

        ConfigurationSection exchanges = getConfig().getConfigurationSection("exchanges");
        for (String key : exchanges.getKeys(false)) {
            int exchangeLevel = Integer.parseInt(key);
            double exchangePrice = getConfig().getDouble("exchanges." + key);
            if (isMaximal) {
                if (level > exchangeLevel
                        && (suitableExchange == null || (Integer) suitableExchange.get("level") < exchangeLevel)
                ) {
                    suitableExchange = new HashMap<String, Object>() {{
                        put("level", exchangeLevel);
                        put("price", exchangePrice);
                    }};
                }
            } else if (level == exchangeLevel) {
                suitableExchange = new HashMap<String, Object>() {{
                    put("level", exchangeLevel);
                    put("price", exchangePrice);
                }};
            }
        }

        return suitableExchange;
    }

    private String getLanguage(String key, String[] pattern, String[] replacement) {
        String value = getConfig().getString("languages." + key);
        for (int index = 0; index < pattern.length; index++) {
            value = value.replace("%" + pattern[index] + "%", replacement[index]);
        }
        return value;
    }

    private String getLanguage(String key) {
        return getLanguage(key, new String[]{}, new String[]{});
    }

    private void makeProcess(Player player, int neededLevel, double levelPrice) {
        int currentLevel = player.getLevel();
        if (currentLevel >= neededLevel) {
            if (economy != null) {
                player.setLevel(currentLevel - neededLevel);
                economy.depositPlayer(player, levelPrice);
                player.sendMessage(getLanguage(
                        "exchangeConfirmation",
                        new String[]{"level", "price", "currentLevel"},
                        new String[]{String.valueOf(neededLevel), String.valueOf(levelPrice), String.valueOf(player.getLevel())}
                ));
            } else {
                player.sendMessage("The server has no Vault");
            }
        } else {
            player.sendMessage(getLanguage("lowLevelException"));
        }
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
