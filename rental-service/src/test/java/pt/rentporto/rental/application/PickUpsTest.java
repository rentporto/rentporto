package pt.rentporto.rental.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.rental.Reservations;
import pt.rentporto.rental.domain.AdditionalDriver;
import pt.rentporto.rental.domain.DriverLicenseNotValidException;
import pt.rentporto.rental.domain.InvalidRentalStatusException;
import pt.rentporto.rental.domain.Money;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalNotFoundException;
import pt.rentporto.rental.domain.RentalStatus;

class PickUpsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-01T09:05:00Z"), ZoneId.of("Europe/Lisbon"));
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 1);
    private static final AdditionalDriver RUI = new AdditionalDriver(
            "Rui Costa", "P-7654321 0", "B", "PT", LocalDate.of(2015, 3, 1), LocalDate.of(2030, 3, 1));

    private final InMemoryAgreements agreements = new InMemoryAgreements();
    private final FakeDeposits deposits = new FakeDeposits();
    private final FakeFleet fleet = new FakeFleet();
    private final RecordedRentalEvents events = new RecordedRentalEvents();
    private final PickUps pickUps =
            new PickUps(agreements, deposits, fleet, events, CLOCK, mock(PlatformTransactionManager.class));
    private RentalAgreement agreement;

    @BeforeEach
    void setUp() {
        agreement = RentalAgreement.awaitingPickup(Reservations.confirmed());
        agreements.add(agreement);
    }

    @Test
    void theDepositIsHeldThenTheVehicleAssignedAndTheAgreementStarts() {
        RentalAgreement active = pickUps.pickUp(agreement.id(), List.of(RUI), "Scratch on the left door");

        assertThat(active.status()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(active.licensePlate()).isEqualTo("AA-12-BB");
        assertThat(active.pickupOdometerKm()).isEqualTo(12000);
        assertThat(active.pickupFuelLevelEighths()).isEqualTo(8);
        assertThat(active.pickedUpAt()).isEqualTo(CLOCK.instant());
        assertThat(active.additionalDrivers()).containsExactly(RUI);
        assertThat(active.pickupNotes()).isEqualTo("Scratch on the left door");
        assertThat(active.depositId()).isEqualTo(deposits.depositId);
        assertThat(deposits.amounts).containsExactly(Money.ofCents(60000));
        assertThat(fleet.calls).containsExactly("ECMR@OPO");
        assertThat(events.started).containsExactly(active);
    }

    @Test
    void aRefusedDepositAssignsNoVehicleAndLeavesTheAgreementWaiting() {
        deposits.answer = () -> {
            throw new DepositDeclinedException(agreement.id());
        };

        assertThatThrownBy(() -> pickUps.pickUp(agreement.id(), List.of(), null))
                .isInstanceOf(DepositDeclinedException.class);
        assertThat(fleet.calls).isEmpty();
        assertThat(agreement.status()).isEqualTo(RentalStatus.AWAITING_PICKUP);
        assertThat(agreement.depositHeld()).isFalse();
        assertThat(events.started).isEmpty();
    }

    @Test
    void paymentsThatDoNotAnswerAssignNoVehicle() {
        deposits.answer = () -> {
            throw new PaymentUnavailableException(agreement.id());
        };

        assertThatThrownBy(() -> pickUps.pickUp(agreement.id(), List.of(), null))
                .isInstanceOf(PaymentUnavailableException.class);
        assertThat(fleet.calls).isEmpty();
    }

    @Test
    void withoutAVehicleTheDepositStaysHeldAndTheNextTryUsesIt() {
        fleet.answer = () -> {
            throw new NoVehicleAvailableException("OPO", "ECMR");
        };

        assertThatThrownBy(() -> pickUps.pickUp(agreement.id(), List.of(), null))
                .isInstanceOf(NoVehicleAvailableException.class);
        assertThat(agreement.status()).isEqualTo(RentalStatus.AWAITING_PICKUP);
        assertThat(agreement.depositId()).isEqualTo(deposits.depositId);

        fleet.answer = () -> new Fleet.AssignedVehicle("CC-34-DD", 500);
        RentalAgreement active = pickUps.pickUp(agreement.id(), List.of(), null);

        assertThat(active.licensePlate()).isEqualTo("CC-34-DD");
        assertThat(deposits.amounts).hasSize(1);
        assertThat(fleet.calls).hasSize(2);
    }

    @Test
    void aFleetThatDoesNotAnswerLeavesTheDepositHeldForTheNextTry() {
        fleet.answer = () -> {
            throw new FleetUnavailableException(agreement.id());
        };

        assertThatThrownBy(() -> pickUps.pickUp(agreement.id(), List.of(), null))
                .isInstanceOf(FleetUnavailableException.class);
        assertThat(agreement.depositHeld()).isTrue();
    }

    @Test
    void anAdditionalDriverWithoutAValidLicenceStopsThePickUpBeforeAnyCall() {
        AdditionalDriver expired = new AdditionalDriver(
                "Rui Costa", "P-7654321 0", "B", "PT", LocalDate.of(2010, 3, 1), TODAY.minusDays(1));

        assertThatThrownBy(() -> pickUps.pickUp(agreement.id(), List.of(expired), null))
                .isInstanceOf(DriverLicenseNotValidException.class);
        assertThat(deposits.amounts).isEmpty();
        assertThat(fleet.calls).isEmpty();
    }

    @Test
    void anAgreementThatIsNotWaitingOrDoesNotExistIsRefusedBeforeAnyCall() {
        pickUps.pickUp(agreement.id(), List.of(), null);

        assertThatThrownBy(() -> pickUps.pickUp(agreement.id(), List.of(), null))
                .isInstanceOf(InvalidRentalStatusException.class);
        assertThatThrownBy(() -> pickUps.pickUp(UUID.randomUUID(), List.of(), null))
                .isInstanceOf(RentalNotFoundException.class);
        assertThat(deposits.amounts).hasSize(1);
        assertThat(fleet.calls).hasSize(1);
    }

    /**
     * Deposits that answer {@code answer}, by default a held deposit.
     */
    private static final class FakeDeposits implements Deposits {

        private final UUID depositId = UUID.randomUUID();
        private final List<Money> amounts = new ArrayList<>();
        private Supplier<UUID> answer = () -> depositId;

        @Override
        public UUID authorize(UUID rentalId, UUID reservationId, Money amount) {
            amounts.add(amount);
            return answer.get();
        }
    }

    /**
     * A fleet that answers {@code answer}, by default the vehicle AA-12-BB.
     */
    private static final class FakeFleet implements Fleet {

        private final List<String> calls = new ArrayList<>();
        private Supplier<AssignedVehicle> answer = () -> new AssignedVehicle("AA-12-BB", 12000);

        @Override
        public AssignedVehicle assign(UUID rentalId, String categoryCode, String station) {
            calls.add(categoryCode + "@" + station);
            return answer.get();
        }
    }
}
