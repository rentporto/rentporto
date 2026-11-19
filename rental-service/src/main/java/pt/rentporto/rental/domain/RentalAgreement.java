package pt.rentporto.rental.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Rental agreement of a confirmed reservation. It keeps everything the pick-up and the settlement need, frozen at
 * booking, so it never asks the pricing or the customers again. The extras and the drivers are small and always
 * needed, so they are loaded with the agreement.
 */
@Entity
@Table(name = "rental_agreements")
public class RentalAgreement {

    /** Fuel level of a full tank, in eighths. */
    public static final int FULL_TANK = 8;

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

    @ElementCollection(fetch = FetchType.EAGER)
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

    @Column(name = "deposit_id")
    private UUID depositId;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "pickup_odometer_km")
    private Long pickupOdometerKm;

    @Column(name = "pickup_fuel_level_eighths")
    private Integer pickupFuelLevelEighths;

    @Column(name = "pickup_notes")
    private String pickupNotes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rental_agreement_drivers", joinColumns = @JoinColumn(name = "rental_id"))
    @OrderColumn(name = "driver_order")
    private List<AdditionalDriver> additionalDrivers = new ArrayList<>();

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "return_odometer_km")
    private Long returnOdometerKm;

    @Column(name = "return_fuel_level_eighths")
    private Integer returnFuelLevelEighths;

    @Column(name = "return_litres_to_fill")
    private BigDecimal returnLitresToFill;

    @Column(name = "return_notes")
    private String returnNotes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rental_agreement_damages", joinColumns = @JoinColumn(name = "rental_id"))
    @OrderColumn(name = "damage_order")
    private List<ReturnDamage> returnDamages = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rental_agreement_charges", joinColumns = @JoinColumn(name = "rental_id"))
    @OrderColumn(name = "charge_order")
    private List<ExtraCharge> extraCharges = new ArrayList<>();

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "settlement_captured_cents"))
    private Money settlementCaptured;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "settlement_released_cents"))
    private Money settlementReleased;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "settlement_outstanding_cents"))
    private Money settlementOutstanding;

    @Column(name = "closed_at")
    private Instant closedAt;

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
     * Checks that the vehicle was not picked up yet.
     *
     * @throws InvalidRentalStatusException if the agreement is not waiting for the pick-up
     */
    public void requireAwaitingPickup() {
        if (status != RentalStatus.AWAITING_PICKUP) {
            throw new InvalidRentalStatusException(id, status, RentalStatus.AWAITING_PICKUP);
        }
    }

    /**
     * Records the security deposit pre-authorised for the pick-up. It stays with the agreement, so that a pick-up
     * tried again uses it, and is released if the agreement is discarded.
     *
     * @param depositId identifier of the deposit
     * @throws InvalidRentalStatusException if the agreement is not waiting for the pick-up
     */
    public void recordDeposit(UUID depositId) {
        Objects.requireNonNull(depositId, "depositId");
        requireAwaitingPickup();
        this.depositId = depositId;
        updatedAt = Instant.now();
    }

    /**
     * Hands the vehicle over to the customer. It leaves with the odometer the fleet knows and a full tank, as the
     * full-to-full policy promises.
     *
     * @param plate      licence plate of the vehicle assigned
     * @param odometerKm odometer reading in kilometres
     * @param drivers    additional drivers, possibly none
     * @param notes      notes of the check-out inspection, or {@code null}
     * @param pickedUpAt when
     * @param today      the day of the pick-up, in Lisbon
     * @throws InvalidRentalStatusException   if the agreement is not waiting for the pick-up
     * @throws DriverLicenseNotValidException if the licence of an additional driver cannot be used today
     * @throws IllegalStateException          if there is no deposit yet
     */
    public void pickUp(
            String plate, long odometerKm, List<AdditionalDriver> drivers, String notes, Instant pickedUpAt,
            LocalDate today) {
        Objects.requireNonNull(plate, "plate");
        Objects.requireNonNull(pickedUpAt, "pickedUpAt");
        requireAwaitingPickup();
        if (depositId == null) {
            throw new IllegalStateException("The rental " + id + " has no deposit");
        }
        drivers.forEach(driver -> driver.requireValidLicenseOn(today));
        licensePlate = plate;
        pickupOdometerKm = odometerKm;
        pickupFuelLevelEighths = FULL_TANK;
        additionalDrivers = new ArrayList<>(drivers);
        pickupNotes = notes;
        this.pickedUpAt = pickedUpAt;
        status = RentalStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    /**
     * Takes the vehicle back: records the return inspection and works out the extra charges, see
     * {@link ReturnCharges}.
     *
     * @param station    code of the station where the vehicle is returned
     * @param inspection state of the vehicle
     * @param returnedAt when
     * @throws InvalidRentalStatusException if the vehicle was not picked up or was already returned
     * @throws WrongReturnStationException  if the station is not the one agreed at booking
     * @throws OdometerBelowPickupException if the odometer is below its reading at pick-up
     * @throws FuelMismatchException        if the litres to fill do not match the fuel level
     */
    public void returnAt(String station, ReturnInspection inspection, Instant returnedAt) {
        Objects.requireNonNull(station, "station");
        Objects.requireNonNull(inspection, "inspection");
        Objects.requireNonNull(returnedAt, "returnedAt");
        if (status != RentalStatus.ACTIVE) {
            throw new InvalidRentalStatusException(id, status, RentalStatus.ACTIVE);
        }
        if (!returnStation.equals(station)) {
            throw new WrongReturnStationException(station, returnStation);
        }
        if (inspection.odometerKm() < pickupOdometerKm) {
            throw new OdometerBelowPickupException(inspection.odometerKm(), pickupOdometerKm);
        }
        extraCharges = new ArrayList<>(ReturnCharges.of(terms(), period, returnedAt, inspection));
        this.returnedAt = returnedAt;
        returnOdometerKm = inspection.odometerKm();
        returnFuelLevelEighths = inspection.fuelLevelEighths();
        returnLitresToFill = inspection.litresToFill();
        returnDamages = new ArrayList<>(inspection.damages());
        returnNotes = inspection.notes();
        status = RentalStatus.RETURNED;
        updatedAt = Instant.now();
    }

    /**
     * Closes the agreement once its deposit is settled. The deposit released when a discarded agreement is settled
     * changes nothing, and neither does a settlement that arrives again.
     *
     * @param depositId  the deposit that was settled
     * @param settlement how it was settled
     * @return {@code true} if the agreement was closed now
     * @throws InconsistentEventException if the deposit is not the one of the agreement, or the vehicle was not
     *                                    returned yet
     */
    public boolean close(UUID depositId, Settlement settlement) {
        Objects.requireNonNull(depositId, "depositId");
        Objects.requireNonNull(settlement, "settlement");
        if (status == RentalStatus.CLOSED || status == RentalStatus.DISCARDED) {
            return false;
        }
        if (status != RentalStatus.RETURNED) {
            throw new InconsistentEventException("The rental " + id + " is " + status + " and cannot be closed");
        }
        if (!depositId.equals(this.depositId)) {
            throw new InconsistentEventException(
                    "The rental " + id + " has the deposit " + this.depositId + ", not " + depositId);
        }
        settlementCaptured = settlement.captured();
        settlementReleased = settlement.released();
        settlementOutstanding = settlement.outstanding();
        closedAt = Instant.now();
        status = RentalStatus.CLOSED;
        updatedAt = closedAt;
        return true;
    }

    /**
     * What the rental costs in the end: the advance payment plus the extra charges.
     *
     * @return the amount
     */
    public Money finalTotal() {
        return Money.ofCents(total.cents() + amountDue().cents());
    }

    /**
     * What the customer owes on top of the advance payment: the sum of the extra charges.
     *
     * @return the amount, zero when there is nothing to charge
     */
    public Money amountDue() {
        return Money.ofCents(extraCharges.stream().mapToLong(charge -> charge.amount().cents()).sum());
    }

    /**
     * Tells whether a security deposit is held for the agreement.
     *
     * @return {@code true} if one was pre-authorised
     */
    public boolean depositHeld() {
        return depositId != null;
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

    public UUID depositId() {
        return depositId;
    }

    public String licensePlate() {
        return licensePlate;
    }

    public Instant pickedUpAt() {
        return pickedUpAt;
    }

    public Long pickupOdometerKm() {
        return pickupOdometerKm;
    }

    public Integer pickupFuelLevelEighths() {
        return pickupFuelLevelEighths;
    }

    public String pickupNotes() {
        return pickupNotes;
    }

    public List<AdditionalDriver> additionalDrivers() {
        return List.copyOf(additionalDrivers);
    }

    public Instant returnedAt() {
        return returnedAt;
    }

    public Long returnOdometerKm() {
        return returnOdometerKm;
    }

    public Integer returnFuelLevelEighths() {
        return returnFuelLevelEighths;
    }

    public BigDecimal returnLitresToFill() {
        return returnLitresToFill;
    }

    public String returnNotes() {
        return returnNotes;
    }

    public List<ReturnDamage> returnDamages() {
        return List.copyOf(returnDamages);
    }

    public List<ExtraCharge> extraCharges() {
        return List.copyOf(extraCharges);
    }

    /**
     * How the deposit was settled.
     *
     * @return the settlement, or {@code null} until the agreement is closed
     */
    public Settlement settlement() {
        return settlementCaptured == null
                ? null
                : new Settlement(settlementCaptured, settlementReleased, settlementOutstanding);
    }

    public Instant closedAt() {
        return closedAt;
    }
}
