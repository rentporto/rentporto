package pt.rentporto.booking.reservation.api.response;

/**
 * Vehicles of a category still available for a period.
 *
 * @param categoryCode ACRISS code of the category
 * @param categoryName name of the category, e.g. {@code Group B - Economy}
 * @param available    number of vehicles that can still be booked for the whole period
 */
public record AvailabilityResponse(String categoryCode, String categoryName, int available) {
}
