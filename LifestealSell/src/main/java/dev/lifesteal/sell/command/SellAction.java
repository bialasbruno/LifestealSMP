package dev.lifesteal.sell.command;

public enum SellAction {
    OPEN_GUI,
    SELL_HELD_MATERIAL,
    SHOW_HELP;

    public static SellAction from(String[] arguments) {
        if (arguments.length == 0) {
            return OPEN_GUI;
        }
        if (arguments.length == 1 && arguments[0].equalsIgnoreCase("hand")) {
            return SELL_HELD_MATERIAL;
        }
        return SHOW_HELP;
    }
}
