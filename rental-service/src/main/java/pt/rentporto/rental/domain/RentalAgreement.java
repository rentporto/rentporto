package pt.rentporto.rental.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Rental agreement of a confirmed reservation. It keeps everything the pick-up and the settlement need, frozen at
 * booking, so it never asks the pricing or the customers again.
 */
@Entity
@Table(name = "rental_agreements")
public class RentalAgreement {

    @Id
    private UUID id;

    @Column(name = "reservation_id", nullable = false, updatable = false)
    private UUID reservationId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "pickup_station", nullable = false, updatable = false)
    private String pickupStation;

    @Column(name = "return_station", nullable = false, updatable = false)
    private String returnStation;

    @Column(name = "category_code", nullable = false, updatable = false)
    private String categoryCode;

    @Embedded
    private RentalPeriod period;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "daily_rate_cents", nullable = false))
    private Money dailyRate;

    @ElementCollection
    @CollectionTable(name = "rental_agreement_extras", joinColumns = @JoinColumn(name = "rental_id"))
    @OrderColumn(name = "extra_order")
    private List<ContractExtra> extras = new ArrayList<>();

    @Column(name = "coverage", nullable = false, updatable = false)
    private String coverage;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "excess_cents", nullable = false))
    private Money excess;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "deposit_cents", nullable = false))
    private Money deposit;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "fuel_price_per_litre_cents", nullable = false))
    private Money fuelPricePerLitre;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "refuelling_fee_cents", nullable = false))
    private Money refuellingFee;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "total_cents", nullable = false))
    private Money total;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RentalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "discard_reason")
    private DiscardReason discardReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RentalAgreement() {
    }

    /**
     * Creates the agreement of a confirmed reservation, waiting for the customer to pick up the vehicle.
     *
     * @param reservation the reservation
     * @return the agreement
     */
    public static RentalAgreement awaitingPickup(ConfirmedReservation reservation) {
        Objects.requireNonNull(reservation, "reservation");
        ContractTerms terms = reservation.terms();
        RentalAgreement agreement = new RentalAgreement();
        agreement.id = UUID.randomUUID();
        agreement.reservationId = reservation.reservationId();
        agreement.customerId = reservation.customerId();
        agreement.pickupStation = reservation.pickupStation();
        agreement.returnStation = reservation.returnStation();
        agreement.categoryCode = reservation.categoryCode();
        agreement.period = reservation.period();
        agreement.dailyRate = terms.dailyRate();
        agreement.extras = new ArrayList<>(terms.extras());
        agreement.coverage = terms.coverage();
        agreement.excess = terms.excess();
        agreement.deposit = terms.deposit();
        agreement.fuelPricePerLitre = terms.fuelPricePerLitre();
        agreement.refuellingFee = terms.refuellingFee();
        agreement.total = reservation.total();
        agreement.status = RentalStatus.AWAITING_PICKUP;
        agreement.createdAt = Instant.now();
        agreement.updatedAt = agreement.createdAt;
        return agreement;
    }

    /**
     * Discards the agreement because the reservation was cancelled or not picked up.
     *
     * @param reason why
     * @return {@code true} if it was discarded now, {@code false} if it already was
     * @throws InconsistentEventException if the vehicle was already picked up
     */
    public boolean discard(DiscardReason reason) {
        Objects.requireNonNull(reason, "reason");
        if (status == RentalStatus.DISCARDED) {
            return false;
        }
        if (status != RentalStatus.AWAITING_PICKUP) {
            throw new InconsistentEventException(
                    "The agreement of the reservation " + reservationId + " is " + status + " and cannot be discarded");
        }
        status = RentalStatus.DISCARDED;
        discardReason = reason;
        updatedAt = Instant.now();
        return true;
    }

    public UUID id() {
        return id;
    }

    public UUID reservationId() {
        return reservationId;
    }

    public UUID customerId() {
        return customerId;
    }

    public String pickupStation() {
        return pickupStation;
    }

    public String returnStation() {
        return returnStation;
    }

    public String categoryCode() {
        return categoryCode;
    }

    public RentalPeriod period() {
        return period;
    }

    public ContractTerms terms() {
        return new ContractTerms(dailyRate, extras, coverage, excess, deposit, fuelPricePerLitre, refuellingFee);
    }

    public Money total() {
        return total;
    }

    public RentalStatus status() {
        return status;
    }

    public DiscardReason discardReason() {
        return discardReason;
    }
}
