package pt.rentporto.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TransferTest {

    private final Transfer transfer =
            Vehicle.register(new LicensePlate("AA-12-BB"), new AcrissCode("ECMR"), "OPO", 0, null)
                    .startTransfer("MAT", "key-1");

    @Test
    void aTransferIsCompletedOnce() {
        assertThat(transfer.complete()).isTrue();
        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);

        assertThat(transfer.complete()).isFalse();
        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void onlyTheKeyOfTheRequestThatStartedItRepeatsIt() {
        assertThat(transfer.startedWith("key-1")).isTrue();
        assertThat(transfer.startedWith("key-2")).isFalse();
        assertThat(transfer.startedWith(null)).isFalse();
    }
}
