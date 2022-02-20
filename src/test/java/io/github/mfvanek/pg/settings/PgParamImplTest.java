/*
 * Copyright (c) 2019-2022. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/pg-index-health
 *
 * This file is a part of "pg-index-health" - a Java library for
 * analyzing and maintaining indexes health in PostgreSQL databases.
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.pg.settings;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PgParamImplTest {

    @Test
    void getNameAndValue() {
        final PgParam param = PgParamImpl.of("statement_timeout", "2s");
        assertThat(param).isNotNull();
        assertThat(param.getName()).isEqualTo("statement_timeout");
        assertThat(param.getValue()).isEqualTo("2s");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void withInvalidArguments() {
        assertThatThrownBy(() -> PgParamImpl.of(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PgParamImpl.of("", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PgParamImpl.of("  ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PgParamImpl.of("param_name", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void withEmptyValue() {
        PgParam param = PgParamImpl.of("statement_timeout", "");
        assertThat(param).isNotNull();
        assertThat(param.getValue()).isEmpty();

        param = PgParamImpl.of("statement_timeout", "       ");
        assertThat(param).isNotNull();
        assertThat(param.getValue()).isEmpty();
    }

    @Test
    void testToString() {
        final PgParam param = PgParamImpl.of("statement_timeout", "2s");
        assertThat(param).isNotNull();
        assertThat(param.toString()).isEqualTo("PgParamImpl{name='statement_timeout', value='2s'}");
    }

    @Test
    void equalsAndHashCode() {
        final PgParam first = PgParamImpl.of("statement_timeout", "2s");
        final PgParam theSame = PgParamImpl.of("statement_timeout", "2s");
        final PgParam second = PgParamImpl.of("lock_timeout", "2s");

        assertThat(first).isNotNull();
        //noinspection AssertBetweenInconvertibleTypes
        assertThat(BigDecimal.ZERO).isNotEqualTo(first);

        // self
        assertThat(first).isEqualTo(first);
        assertThat(first.hashCode()).isEqualTo(first.hashCode());

        // the same
        assertThat(theSame).isEqualTo(first);
        assertThat(theSame.hashCode()).isEqualTo(first.hashCode());

        // others
        assertThat(second).isNotEqualTo(first);
        assertThat(second.hashCode()).isNotEqualTo(first.hashCode());
    }

    @Test
    void equalsHashCodeShouldAdhereContracts() {
        EqualsVerifier.forClass(PgParamImpl.class)
                .withIgnoredFields("value")
                .verify();
    }
}
