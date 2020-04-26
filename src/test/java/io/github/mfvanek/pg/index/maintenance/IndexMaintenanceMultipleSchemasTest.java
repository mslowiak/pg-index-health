/*
 * Copyright (c) 2019-2020. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/pg-index-health
 *
 * This file is a part of "pg-index-health" - a Java library for
 * analyzing and maintaining indexes health in PostgreSQL databases.
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.pg.index.maintenance;

import io.github.mfvanek.pg.model.DuplicatedIndexes;
import io.github.mfvanek.pg.model.ForeignKey;
import io.github.mfvanek.pg.model.Index;
import io.github.mfvanek.pg.model.IndexWithBloat;
import io.github.mfvanek.pg.model.IndexWithNulls;
import io.github.mfvanek.pg.model.IndexWithSize;
import io.github.mfvanek.pg.model.PgContext;
import io.github.mfvanek.pg.model.Table;
import io.github.mfvanek.pg.model.TableNameAware;
import io.github.mfvanek.pg.model.TableWithBloat;
import io.github.mfvanek.pg.model.TableWithMissingIndex;
import io.github.mfvanek.pg.model.UnusedIndex;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

class IndexMaintenanceMultipleSchemasTest {

    private final IndexMaintenance indexMaintenance = Mockito.spy(IndexMaintenance.class);
    private final Collection<PgContext> contexts = Arrays.asList(
            PgContext.of("demo"), PgContext.of("test"), PgContext.ofPublic());

    @Test
    void getInvalidIndexes() {
        Mockito.when(indexMaintenance.getInvalidIndexes(any(PgContext.class)))
                .thenAnswer(invocation -> {
                    final PgContext ctx = invocation.getArgument(0);
                    return Collections.singletonList(
                            Index.of(ctx.enrichWithSchema("t"), ctx.enrichWithSchema("i1")));
                });
        final List<Index> indexes = indexMaintenance.getInvalidIndexes(contexts);
        assertNotNull(indexes);
        assertThat(indexes, hasSize(3));
        assertThat(indexes.stream()
                .map(TableNameAware::getTableName)
                .collect(Collectors.toSet()), containsInAnyOrder("t", "demo.t", "test.t"));
    }

    @Test
    void getDuplicatedIndexes() {
        Mockito.when(indexMaintenance.getDuplicatedIndexes(any(PgContext.class)))
                .thenAnswer(invocation -> {
                    final PgContext ctx = invocation.getArgument(0);
                    return Collections.singletonList(DuplicatedIndexes.of(
                            IndexWithSize.of(ctx.enrichWithSchema("t"), ctx.enrichWithSchema("i1"), 1L),
                            IndexWithSize.of(ctx.enrichWithSchema("t"), ctx.enrichWithSchema("i2"), 1L)));
                });
        final List<DuplicatedIndexes> indexes = indexMaintenance.getDuplicatedIndexes(contexts);
        assertNotNull(indexes);
        assertThat(indexes, hasSize(3));
        assertThat(indexes.stream()
                .map(TableNameAware::getTableName)
                .collect(Collectors.toSet()), containsInAnyOrder("t", "demo.t", "test.t"));
    }

    @Test
    void getIntersectedIndexes() {
        Mockito.when(indexMaintenance.getIntersectedIndexes(any(PgContext.class)))
                .thenAnswer(invocation -> {
                    final PgContext ctx = invocation.getArgument(0);
                    return Arrays.asList(
                            DuplicatedIndexes.of(
                                    IndexWithSize.of(ctx.enrichWithSchema("t1"), ctx.enrichWithSchema("i1"), 1L),
                                    IndexWithSize.of(ctx.enrichWithSchema("t1"), ctx.enrichWithSchema("i2"), 2L)),
                            DuplicatedIndexes.of(
                                    IndexWithSize.of(ctx.enrichWithSchema("t2"), ctx.enrichWithSchema("i3"), 3L),
                                    IndexWithSize.of(ctx.enrichWithSchema("t2"), ctx.enrichWithSchema("i4"), 4L)));
                });
        final List<DuplicatedIndexes> indexes = indexMaintenance.getIntersectedIndexes(contexts);
        assertNotNull(indexes);
        assertThat(indexes, hasSize(6));
        assertThat(indexes.stream()
                .map(TableNameAware::getTableName)
                .collect(Collectors.toSet()), containsInAnyOrder("t1", "demo.t1", "test.t1", "t2", "demo.t2", "test.t2"));
    }

    @Test
    void getPotentiallyUnusedIndexes() {
        Mockito.when(indexMaintenance.getPotentiallyUnusedIndexes(any(PgContext.class)))
                .thenAnswer(invocation -> {
                    final PgContext ctx = invocation.getArgument(0);
                    return Arrays.asList(
                            UnusedIndex.of(ctx.enrichWithSchema("t1"), ctx.enrichWithSchema("i1"), 1L, 1L),
                            UnusedIndex.of(ctx.enrichWithSchema("t2"), ctx.enrichWithSchema("i2"), 2L, 0L));
                });
        final List<UnusedIndex> indexes = indexMaintenance.getPotentiallyUnusedIndexes(contexts);
        assertNotNull(indexes);
        assertThat(indexes, hasSize(6));
        assertThat(indexes.stream()
                .map(TableNameAware::getTableName)
                .collect(Collectors.toSet()), containsInAnyOrder("t1", "demo.t1", "test.t1", "t2", "demo.t2", "test.t2"));
    }

    @Test
    void getForeignKeysNotCoveredWithIndex() {
        Mockito.when(indexMaintenance.getForeignKeysNotCoveredWithIndex(any(PgContext.class)))
                .thenAnswer(invocation -> {
                    final PgContext ctx = invocation.getArgument(0);
                    return Arrays.asList(
                            ForeignKey.ofColumn(ctx.enrichWithSchema("t1"), "f1", "col1"),
                            ForeignKey.ofColumn(ctx.enrichWithSchema("t1"), "f2", "col2"));
                });
        final List<ForeignKey> foreignKeys = indexMaintenance.getForeignKeysNotCoveredWithIndex(contexts);
        assertNotNull(foreignKeys);
        assertThat(foreignKeys, hasSize(6));
        assertThat(foreignKeys.stream()
                .map(TableNameAware::getTableName)
                .collect(Collectors.toSet()), containsInAnyOrder("t1", "demo.t1", "test.t1"));
    }

    @Test
    void getTablesWithMissingIndexes() {
        Mockito.when(indexMaintenance.getTablesWithMissingIndexes(any(PgContext.class)))
                .thenAnswer(invocation -> {
                    final PgContext ctx = invocation.getArgument(0);
                    return Collections.singletonList(TableWithMissingIndex.of(ctx.enrichWithSchema("t"), 1L, 100L, 2L));
                });
        final List<TableWithMissingIndex> tables = indexMaintenance.getTablesWithMissingIndexes(contexts);
        assertNotNull(tables);
        assertThat(tables, hasSize(3));
        assertThat(tables.stream()
                .map(TableNameAware::getTableName)
                .collect(Collectors.toSet()), containsInAnyOrder("t", "demo.t", "test.t"));
    }

    @Test
    void getTablesWithoutPrimaryKey() {
        Mockito.when(indexMaintenance.getTablesWithoutPrimaryKey(any(PgContext.class)))
                .thenAnswer(invocation -> {
                    final PgContext ctx = invocation.getArgument(0);
                    return Arrays.asList(
                            Table.of(ctx.enrichWithSchema("t1"), 1L),
                            Table.of(ctx.enrichWithSchema("t2"), 1L));
                });
        final List<Table> tables = indexMaintenance.getTablesWithoutPrimaryKey(contexts);
        assertNotNull(tables);
        assertThat(tables, hasSize(6));
        assertThat(tables.stream()
                .map(TableNameAware::getTableName)
                .collect(Collectors.toSet()), containsInAnyOrder("t1", "demo.t1", "test.t1", "t2", "demo.t2", "test.t2"));
    }

    @Test
    void getIndexesWithNullValues() {
        Mockito.when(indexMaintenance.getIndexesWithNullValues(any(PgContext.class)))
                .thenAnswer(invocation -> {
                    final PgContext ctx = invocation.getArgument(0);
                    return Collections.singletonList(
                            IndexWithNulls.of(ctx.enrichWithSchema("t1"), ctx.enrichWithSchema("i1"), 1L, "col1"));
                });
        final List<IndexWithNulls> indexes = indexMaintenance.getIndexesWithNullValues(contexts);
        assertNotNull(indexes);
        assertThat(indexes, hasSize(3));
        assertThat(indexes.stream()
                .map(TableNameAware::getTableName)
                .collect(Collectors.toSet()), containsInAnyOrder("t1", "demo.t1", "test.t1"));
    }

    @Test
    void getIndexesWithBloat() {
        Mockito.when(indexMaintenance.getIndexesWithBloat(any(PgContext.class)))
                .thenAnswer(invocation -> {
                    final PgContext ctx = invocation.getArgument(0);
                    return Collections.singletonList(
                            IndexWithBloat.of(ctx.enrichWithSchema("t1"), ctx.enrichWithSchema("i1"), 100L, 30L, 30));
                });
        final List<IndexWithBloat> indexes = indexMaintenance.getIndexesWithBloat(contexts);
        assertNotNull(indexes);
        assertThat(indexes, hasSize(3));
        assertThat(indexes.stream()
                .map(TableNameAware::getTableName)
                .collect(Collectors.toSet()), containsInAnyOrder("t1", "demo.t1", "test.t1"));
    }

    @Test
    void getTablesWithBloat() {
        Mockito.when(indexMaintenance.getTablesWithBloat(any(PgContext.class)))
                .thenAnswer(invocation -> {
                    final PgContext ctx = invocation.getArgument(0);
                    return Arrays.asList(
                            TableWithBloat.of(ctx.enrichWithSchema("t1"), 100L, 45L, 45),
                            TableWithBloat.of(ctx.enrichWithSchema("t2"), 10L, 9L, 90));
                });
        final List<TableWithBloat> tables = indexMaintenance.getTablesWithBloat(contexts);
        assertNotNull(tables);
        assertThat(tables, hasSize(6));
        assertThat(tables.stream()
                .map(TableNameAware::getTableName)
                .collect(Collectors.toSet()), containsInAnyOrder("t1", "demo.t1", "test.t1", "t2", "demo.t2", "test.t2"));
    }
}