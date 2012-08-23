
package eu.masconsult.bgbanking.utils;

public class Convert {

    public static float strToFloat(String text) {
        return Float.valueOf(text.trim().replace(',', '.').replace("\u00a0", ""));
    }
}
