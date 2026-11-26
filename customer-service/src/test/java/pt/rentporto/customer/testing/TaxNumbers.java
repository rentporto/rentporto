package pt.rentporto.customer.testing;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Valid tax numbers that no other test uses, since a tax number belongs to one customer only.
 */
public final class TaxNumbers {

    private TaxNumbers() {
    }

    public static String unique() {
        String firstEight = "2" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += (firstEight.charAt(i) - '0') * (9 - i);
        }
        int rest = sum % 11;
        return firstEight + (rest < 2 ? 0 : 11 - rest);
    }
}
