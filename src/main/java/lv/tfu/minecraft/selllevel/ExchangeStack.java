package lv.tfu.minecraft.selllevel;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class ExchangeStack extends ItemStack {
    private int level;
    private double price;
    private String[] paramKeys;
    private String[] paramValues;

    public ExchangeStack(int level, double price) {
        this.level = level;
        this.price = price;

        paramKeys = new String[]{"level", "price"};
        paramValues = new String[]{String.valueOf(level), String.valueOf(price)};
    }

    public int getLevel() {
        return level;
    }

    public double getPrice() {
        return price;
    }

    public String[] getParamKeys() {
        return paramKeys;
    }

    public String[] getParamValues() {
        return paramValues;
    }

    public ItemStack createStack(int currentLevel) {
        SellLevel plugin = SellLevel.getInstance();
        boolean canUse = currentLevel > level;
        ItemStack itemStack = new ItemStack(
                canUse
                        ? Material.getMaterial(plugin.getConfig().getString("hasPossibilityBlock"))
                        : Material.getMaterial(plugin.getConfig().getString("noPossibilityBlock"))
        );
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLocalizedName("sell-level");
        itemMeta.setDisplayName(
                canUse
                        ? plugin.getLanguage("gui.hasPossibility")
                        : plugin.getLanguage("gui.noPossibility")
        );
        itemMeta.setLore(new ArrayList<String>() {{
            add(plugin.getLanguage("gui.requiredLevel", new String[]{"level"}, new String[]{String.valueOf(level)}));
            add(plugin.getLanguage("gui.exchangeReward", new String[]{"price"}, new String[]{String.valueOf(price)}));
        }});
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
