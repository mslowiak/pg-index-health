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

import io.github.mfvanek.pg.model.MemoryUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerSpecificationTest {

    @Test
    void getCpuCoresAmount() {
        final ServerSpecification specification = ServerSpecification.builder()
                .withCpuCores(2)
                .build();
        assertNotNull(specification);
        assertEquals(2, specification.getCpuCoresAmount());
        assertEquals(1024L * 1024L * 1024L, specification.getMemoryAmountInBytes());
        assertFalse(specification.hasSSD());

        assertThrows(IllegalArgumentException.class, () -> ServerSpecification.builder().withCpuCores(0));
        assertThrows(IllegalArgumentException.class, () -> ServerSpecification.builder().withCpuCores(-1));
    }

    @Test
    void getMemoryAmountInBytes() {
        final ServerSpecification specification = ServerSpecification.builder()
                .withMemoryAmount(160, MemoryUnit.GB)
                .build();
        assertNotNull(specification);
        assertEquals(1, specification.getCpuCoresAmount());
        assertEquals(160L * 1024L * 1024L * 1024L, specification.getMemoryAmountInBytes());
        assertFalse(specification.hasSSD());

        assertThrows(IllegalArgumentException.class, () -> ServerSpecification.builder().withMemoryAmount(0, MemoryUnit.GB));
        assertThrows(IllegalArgumentException.class, () -> ServerSpecification.builder().withMemoryAmount(-1, MemoryUnit.GB));
    }

    @Test
    void hasSSD() {
        final ServerSpecification specification = ServerSpecification.builder()
                .withSSD()
                .build();
        assertNotNull(specification);
        assertEquals(1, specification.getCpuCoresAmount());
        assertEquals(1024L * 1024L * 1024L, specification.getMemoryAmountInBytes());
        assertTrue(specification.hasSSD());
    }

    @Test
    void toStringTest() {
        final ServerSpecification specification = ServerSpecification.builder().build();
        assertNotNull(specification);
        assertEquals("ServerSpecification{cpuCoresAmount=1, memoryAmountInBytes=1073741824, hasSSD=false}",
                specification.toString());
    }
}
