package net.dbd.demode.util.format;

import lombok.experimental.UtilityClass;

import java.text.NumberFormat;

@UtilityClass
public class NumberFormatUtil {

    private final NumberFormat numberFormat = NumberFormat.getInstance();


    public static String format(long num) {
        return numberFormat.format(num);
    }
}
