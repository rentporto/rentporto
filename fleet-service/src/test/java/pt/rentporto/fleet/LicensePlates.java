package pt.rentporto.fleet;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Licence plates for the tests that share the database, in each of the three formats.
 */
public final class LicensePlates {

    private static final AtomicInteger NEXT = new AtomicInteger();

    private LicensePlates() {
    }

    /**
     * A plate in the format since 2020 that no other test uses.
     *
     * @return the plate, e.g. {@code AB-07-TT}
     */
    public static String unique() {
        int n = NEXT.getAndIncrement();
        return letters(n) + "-%02d-TT".formatted(n % 100);
    }

    /**
     * A plate in the format of 2005 to 2020 that no other test uses.
     *
     * @return the plate, e.g. {@code 07-AB-99}
     */
    public static String uniqueFrom2005() {
        int n = NEXT.getAndIncrement();
        return "%02d-".formatted(n % 100) + letters(n) + "-99";
    }

    /**
     * A plate in the format of 1992 to 2005 that no other test uses.
     *
     * @return the plate, e.g. {@code 07-99-AB}
     */
    public static String uniqueFrom1992() {
        int n = NEXT.getAndIncrement();
        return "%02d-99-".formatted(n % 100) + letters(n);
    }

    private static String letters(int n) {
        int pair = n / 100;
        return "" + (char) ('A' + pair / 26 % 26) + (char) ('A' + pair % 26);
    }
}
