package pt.rentporto.fleet;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ACRISS codes for the tests that share the database.
 */
public final class AcrissCodes {

    private static final String CLASSES = "MNEHCDIJSRFGPULWOX";
    private static final String TRANSMISSIONS = "MNCABD";
    private static final String FUELS = "RNDQHIECLSABMFVZUX";
    private static final AtomicInteger NEXT = new AtomicInteger();

    private AcrissCodes() {
    }

    /**
     * A code that no other test uses. The body type is always {@code K}, a commercial van, which none of the example
     * categories has.
     *
     * @return the code
     */
    public static String unique() {
        int n = NEXT.getAndIncrement();
        return "" + CLASSES.charAt(n % CLASSES.length()) + 'K'
                + TRANSMISSIONS.charAt(n / CLASSES.length() % TRANSMISSIONS.length())
                + FUELS.charAt(n / (CLASSES.length() * TRANSMISSIONS.length()) % FUELS.length());
    }
}
