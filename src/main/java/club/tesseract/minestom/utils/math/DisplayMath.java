package club.tesseract.minestom.utils.math;

import java.math.BigInteger;

public final class DisplayMath {

    private DisplayMath(){}

    /**
     * convert to the smallest amount with scaler i.e. 1m = 1000000
     * @param bigInteger
     * @return String as a short display value
     */
    public static String toShortDisplay(BigInteger bigInteger){
        String[] suffixes = new String[]{"", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Oc", "No", "D"};
        BigInteger thousand = BigInteger.valueOf(1000);
        int suffixIndex = 0;
        BigInteger displayValue = bigInteger;

        while (displayValue.compareTo(thousand) >= 0 && suffixIndex < suffixes.length - 1) {
            displayValue = displayValue.divide(thousand);
            suffixIndex++;
        }

        return displayValue + suffixes[suffixIndex];
    }

    public static String toShortDisplay(long longValue){
        return toShortDisplay(BigInteger.valueOf(longValue));
    }

    public static String toShortDisplay(int intValue){
        return toShortDisplay(BigInteger.valueOf(intValue));
    }

    public static String toDisplay(BigInteger bigInteger){
        return bigInteger.toString();
    }


}
