/*
 * Copyright (c) 2019-2022. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/pg-index-health
 *
 * This file is a part of "pg-index-health" - a Java library for
 * analyzing and maintaining indexes health in PostgreSQL databases.
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.pg.index.maintenance;

import io.github.mfvanek.pg.connection.PgConnection;
import io.github.mfvanek.pg.connection.PgConnectionImpl;
import io.github.mfvanek.pg.embedded.PostgresDbExtension;
import io.github.mfvanek.pg.embedded.PostgresExtensionFactory;
import io.github.mfvanek.pg.model.PgContext;
import io.github.mfvanek.pg.model.index.DuplicatedIndexes;
import io.github.mfvanek.pg.model.index.ForeignKey;
import io.github.mfvanek.pg.model.index.Index;
import io.github.mfvanek.pg.model.index.IndexWithBloat;
import io.github.mfvanek.pg.model.index.IndexWithNulls;
import io.github.mfvanek.pg.model.index.UnusedIndex;
import io.github.mfvanek.pg.utils.DatabaseAwareTestBase;
import io.github.mfvanek.pg.utils.DatabasePopulator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IndexesMaintenanceOnHostImplTest extends DatabaseAwareTestBase {

    @RegisterExtension
    static final PostgresDbExtension embeddedPostgres = PostgresExtensionFactory.database();

    private final IndexesMaintenanceOnHost indexesMaintenance;

    IndexesMaintenanceOnHostImplTest() {
        super(embeddedPostgres.getTestDatabase());
        final PgConnection pgConnection = PgConnectionImpl.ofPrimary(embeddedPostgres.getTestDatabase());
        this.indexesMaintenance = new IndexMaintenanceOnHostImpl(pgConnection);
    }

    @Test
    void getInvalidIndexesOnEmptyDatabase() {
        final List<Index> invalidIndexes = indexesMaintenance.getInvalidIndexes();
        assertNotNull(invalidIndexes);
        assertThat(invalidIndexes, empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getInvalidIndexesOnDatabaseWithoutThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                DatabasePopulator::withReferences,
                ctx -> {
                    final List<Index> invalidIndexes = indexesMaintenance.getInvalidIndexes(ctx);
                    assertNotNull(invalidIndexes);
                    assertThat(invalidIndexes, empty());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getInvalidIndexesOnDatabaseWithThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withData().withInvalidIndex(),
                ctx -> {
                    final List<Index> invalidIndexes = indexesMaintenance.getInvalidIndexes(ctx);
                    assertNotNull(invalidIndexes);
                    assertThat(invalidIndexes, hasSize(1));
                    final Index index = invalidIndexes.get(0);
                    assertEquals(ctx.enrichWithSchema("clients"), index.getTableName());
                    assertEquals(ctx.enrichWithSchema("i_clients_last_name_first_name"), index.getIndexName());
                });
    }

    @Test
    void getDuplicatedIndexesOnEmptyDatabase() {
        final List<DuplicatedIndexes> duplicatedIndexes = indexesMaintenance.getDuplicatedIndexes();
        assertNotNull(duplicatedIndexes);
        assertThat(duplicatedIndexes, empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getDuplicatedIndexesOnDatabaseWithoutThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                DatabasePopulator::withReferences,
                ctx -> {
                    final List<DuplicatedIndexes> duplicatedIndexes = indexesMaintenance.getDuplicatedIndexes(ctx);
                    assertNotNull(duplicatedIndexes);
                    assertThat(duplicatedIndexes, empty());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getDuplicatedIndexesOnDatabaseWithThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withDuplicatedIndex(),
                ctx -> {
                    final List<DuplicatedIndexes> duplicatedIndexes = indexesMaintenance.getDuplicatedIndexes(ctx);
                    assertNotNull(duplicatedIndexes);
                    assertThat(duplicatedIndexes, hasSize(1));
                    final DuplicatedIndexes entry = duplicatedIndexes.get(0);
                    assertEquals(ctx.enrichWithSchema("accounts"), entry.getTableName());
                    assertThat(entry.getIndexNames(), containsInAnyOrder(
                            ctx.enrichWithSchema("accounts_account_number_key"),
                            ctx.enrichWithSchema("i_accounts_account_number")));
                    assertThat(entry.getTotalSize(), greaterThanOrEqualTo(16384L));
                    assertThat(entry.getDuplicatedIndexes(), hasSize(2));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getDuplicatedHashIndexesOnDatabaseWithThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withDuplicatedHashIndex(),
                ctx -> {
                    final List<DuplicatedIndexes> duplicatedIndexes = indexesMaintenance.getDuplicatedIndexes(ctx);
                    assertNotNull(duplicatedIndexes);
                    assertThat(duplicatedIndexes, empty());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getDuplicatedIndexesWithDifferentOpclassShouldReturnNothing(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withDifferentOpclassIndexes(),
                ctx -> {
                    final List<DuplicatedIndexes> duplicatedIndexes = indexesMaintenance.getDuplicatedIndexes(ctx);
                    assertNotNull(duplicatedIndexes);
                    assertThat(duplicatedIndexes, empty());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getDuplicatedIndexesWithDifferentCollationShouldReturnNothing(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withCustomCollation().withDuplicatedCustomCollationIndex(),
                ctx -> {
                    final List<DuplicatedIndexes> duplicatedIndexes = indexesMaintenance.getDuplicatedIndexes(ctx);
                    assertNotNull(duplicatedIndexes);
                    assertThat(duplicatedIndexes, empty());
                });
    }

    @Test
    void getIntersectedIndexesOnEmptyDatabase() {
        final List<DuplicatedIndexes> intersectedIndexes = indexesMaintenance.getIntersectedIndexes();
        assertNotNull(intersectedIndexes);
        assertThat(intersectedIndexes, empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getIntersectedIndexesOnDatabaseWithoutThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                DatabasePopulator::withReferences,
                ctx -> {
                    final List<DuplicatedIndexes> intersectedIndexes = indexesMaintenance.getIntersectedIndexes(ctx);
                    assertNotNull(intersectedIndexes);
                    assertThat(intersectedIndexes, empty());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getIntersectedIndexesOnDatabaseWithThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withData().withDuplicatedIndex(),
                ctx -> {
                    final List<DuplicatedIndexes> intersectedIndexes = indexesMaintenance.getIntersectedIndexes(ctx);
                    assertNotNull(intersectedIndexes);
                    assertThat(intersectedIndexes, hasSize(2));
                    final DuplicatedIndexes firstEntry = intersectedIndexes.get(0);
                    final DuplicatedIndexes secondEntry = intersectedIndexes.get(1);
                    assertThat(firstEntry.getTotalSize(), greaterThanOrEqualTo(114688L));
                    assertThat(secondEntry.getTotalSize(), greaterThanOrEqualTo(106496L));
                    assertThat(firstEntry.getDuplicatedIndexes(), hasSize(2));
                    assertThat(secondEntry.getDuplicatedIndexes(), hasSize(2));
                    assertEquals(ctx.enrichWithSchema("accounts"), firstEntry.getTableName());
                    assertEquals(ctx.enrichWithSchema("clients"), secondEntry.getTableName());
                    assertThat(firstEntry.getIndexNames(), contains(
                            ctx.enrichWithSchema("i_accounts_account_number_not_deleted"),
                            ctx.enrichWithSchema("i_accounts_number_balance_not_deleted")));
                    assertThat(secondEntry.getIndexNames(), contains(
                            ctx.enrichWithSchema("i_clients_last_first"),
                            ctx.enrichWithSchema("i_clients_last_name")));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getIntersectedHashIndexesOnDatabaseWithThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withData().withDuplicatedHashIndex(),
                ctx -> {
                    final List<DuplicatedIndexes> intersectedIndexes = indexesMaintenance.getIntersectedIndexes(ctx);
                    assertNotNull(intersectedIndexes);
                    assertThat(intersectedIndexes, hasSize(1));
                    final DuplicatedIndexes entry = intersectedIndexes.get(0);
                    assertThat(entry.getDuplicatedIndexes(), hasSize(2));
                    assertEquals(ctx.enrichWithSchema("clients"), entry.getTableName());
                    assertThat(entry.getIndexNames(), contains(
                            ctx.enrichWithSchema("i_clients_last_first"),
                            ctx.enrichWithSchema("i_clients_last_name")));
                    assertThat(entry.getTotalSize(), greaterThanOrEqualTo(106496L));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getIntersectedIndexesWithDifferentOpclassShouldReturnNothing(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withDifferentOpclassIndexes(),
                ctx -> {
                    final List<DuplicatedIndexes> intersectedIndexes = indexesMaintenance.getIntersectedIndexes(ctx);
                    assertNotNull(intersectedIndexes);
                    assertThat(intersectedIndexes, empty());
                });
    }

    @Test
    void getUnusedIndexesOnEmptyDatabase() {
        final List<UnusedIndex> unusedIndexes = indexesMaintenance.getUnusedIndexes();
        assertNotNull(unusedIndexes);
        assertThat(unusedIndexes, empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getUnusedIndexesOnDatabaseWithoutThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                DatabasePopulator::withReferences,
                ctx -> {
                    final List<UnusedIndex> unusedIndexes = indexesMaintenance.getUnusedIndexes(ctx);
                    assertNotNull(unusedIndexes);
                    assertThat(unusedIndexes, empty());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getUnusedIndexesOnDatabaseWithThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withData().withDuplicatedIndex(),
                ctx -> {
                    final List<UnusedIndex> unusedIndexes = indexesMaintenance.getUnusedIndexes(ctx);
                    assertNotNull(unusedIndexes);
                    assertThat(unusedIndexes, hasSize(6));
                    final Set<String> names = unusedIndexes.stream().map(UnusedIndex::getIndexName).collect(toSet());
                    assertThat(names, containsInAnyOrder(
                            ctx.enrichWithSchema("i_clients_last_first"),
                            ctx.enrichWithSchema("i_clients_last_name"),
                            ctx.enrichWithSchema("i_accounts_account_number"),
                            ctx.enrichWithSchema("i_accounts_number_balance_not_deleted"),
                            ctx.enrichWithSchema("i_accounts_account_number_not_deleted"),
                            ctx.enrichWithSchema("i_accounts_id_account_number_not_deleted")));
                });
    }

    @Test
    void getForeignKeysNotCoveredWithIndexOnEmptyDatabase() {
        final List<ForeignKey> foreignKeys = indexesMaintenance.getForeignKeysNotCoveredWithIndex();
        assertNotNull(foreignKeys);
        assertThat(foreignKeys, empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getForeignKeysNotCoveredWithIndexOnDatabaseWithoutThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp,
                ctx -> {
                    final List<ForeignKey> foreignKeys = indexesMaintenance.getForeignKeysNotCoveredWithIndex(ctx);
                    assertNotNull(foreignKeys);
                    assertThat(foreignKeys, empty());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getForeignKeysNotCoveredWithIndexOnDatabaseWithThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                DatabasePopulator::withReferences,
                ctx -> {
                    final List<ForeignKey> foreignKeys = indexesMaintenance.getForeignKeysNotCoveredWithIndex(ctx);
                    assertNotNull(foreignKeys);
                    assertThat(foreignKeys, hasSize(1));
                    final ForeignKey foreignKey = foreignKeys.get(0);
                    assertEquals(ctx.enrichWithSchema("accounts"), foreignKey.getTableName());
                    assertEquals("c_accounts_fk_client_id", foreignKey.getConstraintName());
                    assertThat(foreignKey.getColumnsInConstraint(), contains("client_id"));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getForeignKeysNotCoveredWithIndexOnDatabaseWithNotSuitableIndex(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withNonSuitableIndex(),
                ctx -> {
                    final List<ForeignKey> foreignKeys = indexesMaintenance.getForeignKeysNotCoveredWithIndex(ctx);
                    assertNotNull(foreignKeys);
                    assertThat(foreignKeys, hasSize(1));
                    final ForeignKey foreignKey = foreignKeys.get(0);
                    assertEquals(ctx.enrichWithSchema("accounts"), foreignKey.getTableName());
                    assertEquals("c_accounts_fk_client_id", foreignKey.getConstraintName());
                    assertThat(foreignKey.getColumnsInConstraint(), contains("client_id"));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getForeignKeysNotCoveredWithIndexOnDatabaseWithSuitableIndex(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withSuitableIndex(),
                ctx -> {
                    final List<ForeignKey> foreignKeys = indexesMaintenance.getForeignKeysNotCoveredWithIndex(ctx);
                    assertNotNull(foreignKeys);
                    assertThat(foreignKeys, empty());
                });
    }

    @Test
    void getIndexesWithNullValuesOnEmptyDatabase() {
        final List<IndexWithNulls> indexes = indexesMaintenance.getIndexesWithNullValues();
        assertNotNull(indexes);
        assertThat(indexes, empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getIndexesWithNullValuesOnDatabaseWithoutThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withData(),
                ctx -> {
                    final List<IndexWithNulls> indexes = indexesMaintenance.getIndexesWithNullValues(ctx);
                    assertNotNull(indexes);
                    assertThat(indexes, empty());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getIndexesWithNullValuesOnDatabaseWithThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withData().withNullValuesInIndex(),
                ctx -> {
                    final List<IndexWithNulls> indexes = indexesMaintenance.getIndexesWithNullValues(ctx);
                    assertNotNull(indexes);
                    assertThat(indexes, hasSize(1));
                    final IndexWithNulls indexWithNulls = indexes.get(0);
                    assertEquals(ctx.enrichWithSchema("i_clients_middle_name"), indexWithNulls.getIndexName());
                    assertEquals("middle_name", indexWithNulls.getNullableField());
                });
    }

    @Test
    void getIndexesWithBloatOnEmptyDatabase() {
        final List<IndexWithBloat> indexes = indexesMaintenance.getIndexesWithBloat();
        assertNotNull(indexes);
        assertThat(indexes, empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getIndexesWithBloatOnDatabaseWithoutThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withStatistics(),
                ctx -> {
                    waitForStatisticsCollector();
                    final List<IndexWithBloat> indexes = indexesMaintenance.getIndexesWithBloat(ctx);
                    assertNotNull(indexes);
                    assertThat(indexes, empty());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "custom"})
    void getIndexesWithBloatOnDatabaseWithThem(final String schemaName) {
        executeTestOnDatabase(schemaName,
                dbp -> dbp.withReferences().withData().withStatistics(),
                ctx -> {
                    waitForStatisticsCollector();
                    assertTrue(existsStatisticsForTable(ctx, "accounts"));

                    final List<IndexWithBloat> indexes = indexesMaintenance.getIndexesWithBloat(ctx);
                    assertNotNull(indexes);
                    assertThat(indexes, hasSize(3));
                    final IndexWithBloat index = indexes.get(0);
                    assertEquals(ctx.enrichWithSchema("accounts_account_number_key"), index.getIndexName());
                    assertEquals(ctx.enrichWithSchema("accounts"), index.getTableName());
                    assertEquals(57344L, index.getIndexSizeInBytes());
                    assertEquals(8192L, index.getBloatSizeInBytes());
                    assertEquals(14, index.getBloatPercentage());
                });
    }

    @Test
    void securityTest() {
        executeTestOnDatabase("public",
                dbp -> dbp.withReferences().withData().withNullValuesInIndex(),
                ctx -> {
                    final long before = getRowsCount(ctx.getSchemaName(), "clients");
                    assertEquals(1001L, before);
                    List<IndexWithNulls> indexes = indexesMaintenance.getIndexesWithNullValues(PgContext.of("; truncate table clients;"));
                    assertNotNull(indexes);
                    assertThat(indexes, empty());
                    assertEquals(before, getRowsCount(ctx.getSchemaName(), "clients"));

                    indexes = indexesMaintenance.getIndexesWithNullValues(PgContext.of("; select pg_sleep(100000000);"));
                    assertNotNull(indexes);
                    assertThat(indexes, empty());

                    indexes = indexesMaintenance.getIndexesWithNullValues(ctx);
                    assertNotNull(indexes);
                    assertThat(indexes, hasSize(1));
                });
    }
}
