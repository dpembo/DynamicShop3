package me.sat7.dynamicshop;

public class Globeworks {
    
    private static final String RESET = "\u001B[0m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";
    private static final String RED = "\u001B[31m";
    private static final String BROWN = "\u001B[33m";


    public static String logo(String pluginName, String version) {
        
        String interleaved1 = YELLOW + "  __ ";
        String interleaved2 = YELLOW + "  \\/ ";
        String interleaved3 = YELLOW + "  /\\ ";
        String interleaved4 = YELLOW + " /" + GREEN + "$$" + YELLOW + "\\";
        String interleaved5 = YELLOW + " \\__/";
        String interleaved6 = YELLOW + "     ";   
        String logo = "\n" +

        
        interleaved1 +
        GREEN + " _______ _____   " + BLUE + "_______ " + GREEN + "______ " + BLUE + "_______\n" +
        interleaved2 +
        GREEN + "|     __|     |_|" + BLUE + "       |" + GREEN + "   __ \\" + BLUE + "    ___|\n" +
        interleaved3 +
        GREEN + "|    |  |       |" + BLUE + "   -   |" + GREEN + "   __ <" + BLUE + "    ___|\n" +
        interleaved4 +
        BROWN + "|_______|_______|" + BLUE + "_______|" + BROWN + "______/" + BLUE + "_______\n" +
        interleaved5 +
        RED + " ________ _______ ______ __  __ _______\n" +
        interleaved6 +
        RED + "|  |  |  |       |   __ \\  |/  |     __|\n" +
        YELLOW + "             " +
        RED + "|  |  |  |   -   |      <     <|__     |\n" +
        YELLOW + "             " +
        RED + "|________|_______|___|__|__|\\__|_______|\n" +
        YELLOW + "\n" +
        YELLOW + pluginName + " v" + version + "\n" +
        RESET;
        return logo;
    }
}
