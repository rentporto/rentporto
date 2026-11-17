package pt.rentporto.customer.domain;

/**
 * What decides whether a customer may drive on a date: their age, how long they have held a licence and whether the
 * licence can be used.
 *
 * @param age          age in full years
 * @param licenseYears full years since the licence was issued
 * @param licenseValid whether the licence was already issued and had not expired
 */
public record DriverProfile(int age, int licenseYears, boolean licenseValid) {
}
