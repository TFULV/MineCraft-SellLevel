package lv.tfu.minecraft.selllevel;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedList;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class SellLevel extends JavaPlugin {
    private static ArrayList<ExchangeStack> exchangeStacks = new ArrayList<>();
    private static LinkedList<Player> openedInventory = new LinkedList<>();
    private static int CHEST_SIZE = 27;
    private static SellLevel instance;
    private Economy economy;

    public static SellLevel getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

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

        for (String exchangeLevel : getConfig().getConfigurationSection("exchanges").getKeys(false)) {
            exchangeStacks.add(new ExchangeStack(
                    Integer.parseInt(exchangeLevel),
                    getConfig().getDouble("exchanges." + exchangeLevel)
            ));
        }

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEvent(InventoryClickEvent event) {
                Player player = (Player) event.getWhoClicked();
                if (openedInventory.contains(player)) {
                    ItemStack currentItem = event.getCurrentItem();
                    if (currentItem != null && currentItem.getItemMeta().getLocalizedName().equalsIgnoreCase("sell-level")) {
                        ExchangeStack exchangeStack = exchangeStacks.get(event.getSlot());
                        if (makeProcess(player, exchangeStack.getLevel(), exchangeStack.getPrice())) {
                            player.closeInventory();
                            openExchangeGUI(player);
                        }
                    }
                    event.setCancelled(true);
                }
            }

            @EventHandler
            public void onEvent(InventoryCloseEvent event) {
                Player player = (Player) event.getPlayer();
                openedInventory.remove(player);
            }
        }, this);

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
                ExchangeStack exchange = isMax
                        ? getExchangeByLevel(player.getLevel(), true)
                        : getExchangeByLevel(Integer.parseInt(args[0]), false);
                if (exchange != null) {
                    if (isMax) {
                        player.sendMessage(getLanguage(
                                "foundMaximumExchange",
                                exchange.getParamKeys(),
                                exchange.getParamValues()
                        ));
                    }
                    makeProcess(player, exchange.getLevel(), exchange.getPrice());
                } else {
                    player.sendMessage(getLanguage("noSuchExchange"));
                }
            } else if (args.length == 0) {
                openExchangeGUI(player);
            } else {
                player.sendMessage(getLanguage("description"));
                player.sendMessage(getLanguage("exchangeList"));
                for (ExchangeStack exchange : exchangeStacks) {
                    player.sendMessage(getLanguage(
                            "exchangeEntry",
                            exchange.getParamKeys(),
                            exchange.getParamValues()
                    ));
                }
            }
        }
        return true;
    }

    private ExchangeStack getExchangeByLevel(int level, boolean isMaximal) {
        ExchangeStack suitableExchange = null;
        for (ExchangeStack exchange : exchangeStacks) {
            if (isMaximal) {
                if (level > exchange.getLevel()
                        && (suitableExchange == null || suitableExchange.getLevel() < exchange.getLevel())
                ) {
                    suitableExchange = exchange;
                }
            } else if (level == exchange.getLevel()) {
                suitableExchange = exchange;
            }
        }
        return suitableExchange;
    }

    public String getLanguage(String key, String[] pattern, String[] replacement) {
        String value = getConfig().getString("languages." + key);
        for (int index = 0; index < pattern.length; index++) {
            value = value.replace("%" + pattern[index] + "%", replacement[index]);
        }
        return value;
    }

    public String getLanguage(String key) {
        return getLanguage(key, new String[]{}, new String[]{});
    }

    private boolean makeProcess(Player player, int neededLevel, double levelPrice) {
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
                return true;
            } else {
                player.sendMessage("The server has no Vault");
            }
        } else {
            player.sendMessage(getLanguage("lowLevelException"));
        }
        return false;
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

    private void openExchangeGUI(Player player) {
        int itemIndex = 0;
        Inventory inventory = Bukkit.createInventory(null, CHEST_SIZE, getLanguage(
                "gui.exchangeTitle",
                new String[]{"level"},
                new String[]{String.valueOf(player.getLevel())}
        ));
        for (ExchangeStack exchange : exchangeStacks) {
            if (itemIndex == CHEST_SIZE) {
                break;
            }
            inventory.setItem(itemIndex++, exchange.createStack(player.getLevel()));
        }
        player.openInventory(inventory);
        openedInventory.add(player);
    }
}
