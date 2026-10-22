package com.gymapp.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class SessionSetTest {

    private static final Instant T1 = Instant.parse("2026-10-22T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-22T10:05:00Z");

    @Test
    void checkingASetStoresActualValuesAndCompletionTime() {
        SessionSet set = new SessionSet();

        set.record(9, new BigDecimal("62.5"), true, T1);

        assertThat(set.getActualReps()).isEqualTo(9);
        assertThat(set.getActualWeightKg()).isEqualByComparingTo("62.50");
        assertThat(set.isCompleted()).isTrue();
        assertThat(set.getCompletedAt()).isEqualTo(T1);
    }

    @Test
    void editingACheckedSetKeepsTheOriginalCompletionTime() {
        SessionSet set = new SessionSet();
        set.record(9, BigDecimal.TEN, true, T1);

        set.record(10, BigDecimal.TEN, true, T2);

        assertThat(set.getActualReps()).isEqualTo(10);
        assertThat(set.getCompletedAt()).isEqualTo(T1);
    }

    @Test
    void uncheckingASetClearsItsCompletionTime() {
        SessionSet set = new SessionSet();
        set.record(9, BigDecimal.TEN, true, T1);

        set.record(9, BigDecimal.TEN, false, T2);

        assertThat(set.isCompleted()).isFalse();
        assertThat(set.getCompletedAt()).isNull();
    }

    @Test
    void bodyweightSetAcceptsNullWeight() {
        SessionSet set = new SessionSet();

        set.record(12, null, true, T1);

        assertThat(set.getActualWeightKg()).isNull();
        assertThat(set.isCompleted()).isTrue();
    }
}
