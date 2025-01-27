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

import io.github.mfvanek.pg.utils.Validators;

import java.util.Objects;
import javax.annotation.Nonnull;

public class PgParamImpl implements PgParam {

    private final String name;
    private final String value;

    private PgParamImpl(@Nonnull final String name, @Nonnull final String value) {
        this.name = Validators.notBlank(name, "name");
        this.value = Validators.paramValueNotNull(value, "value for '" + name + "' cannot be null");
    }

    @Override
    @Nonnull
    public String getName() {
        return name;
    }

    @Override
    @Nonnull
    public String getValue() {
        return value;
    }

    @Override
    @Nonnull
    public String toString() {
        return PgParamImpl.class.getSimpleName() + '{' +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof PgParam)) {
            return false;
        }

        final PgParam that = (PgParam) o;
        return Objects.equals(name, that.getName());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(name);
    }

    public static PgParam of(@Nonnull final String name, @Nonnull final String value) {
        return new PgParamImpl(name, value);
    }
}
