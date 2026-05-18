package com.example.myproject.config;

import java.sql.Connection;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "local-schema-migration.enabled", havingValue = "true", matchIfMissing = true)
public class LocalSchemaMigrationRunner implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public LocalSchemaMigrationRunner(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String jdbcUrl;
        try (Connection connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
        }
        if (!jdbcUrl.startsWith("jdbc:postgresql:")) {
            return;
        }

        migrateProcessingStrategy();
        migrateSettlementSequence();
    }

    private void migrateProcessingStrategy() {
        if (!tableExists("settlements") || !tableExists("batch_job_histories")) {
            return;
        }

        migrateBatchJobHistoryRunningState();
        jdbcTemplate.execute("alter table settlements add column if not exists processing_strategy varchar(30)");
        jdbcTemplate.execute("alter table batch_job_histories add column if not exists processing_strategy varchar(30)");

        jdbcTemplate.update("""
                update settlements
                set processing_strategy = 'BASIC_LOOP'
                where processing_strategy is null
                """);
        jdbcTemplate.update("""
                update batch_job_histories
                set processing_strategy = 'BASIC_LOOP'
                where processing_strategy is null
                """);

        replaceCheckConstraint(
                "settlements",
                "processing_strategy",
                "ck_settlements_processing_strategy",
                "processing_strategy in ('BASIC_LOOP', 'GROUP_BY_QUERY', 'GROUP_BY_BULK_SAVE', 'GROUP_BY_BULK_INDEX')"
        );
        replaceCheckConstraint(
                "batch_job_histories",
                "processing_strategy",
                "ck_batch_job_histories_processing_strategy",
                "processing_strategy in ('BASIC_LOOP', 'GROUP_BY_QUERY', 'GROUP_BY_BULK_SAVE', 'GROUP_BY_BULK_INDEX')"
        );

        jdbcTemplate.execute("alter table settlements alter column processing_strategy set not null");
        jdbcTemplate.execute("alter table batch_job_histories alter column processing_strategy set not null");

        jdbcTemplate.execute("alter table settlements drop constraint if exists uk_settlements_merchant_settlement_date");
        jdbcTemplate.execute("alter table settlements drop constraint if exists uk_settlements_merchant_date");
        dropLegacySettlementUniqueConstraint();

        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_constraint
                        where conname = 'uk_settlements_merchant_date_strategy'
                          and conrelid = 'settlements'::regclass
                    ) then
                        alter table settlements
                            add constraint uk_settlements_merchant_date_strategy
                            unique (merchant_id, settlement_date, processing_strategy);
                    end if;
                end $$;
                """);
    }

    private void migrateSettlementSequence() {
        if (!tableExists("settlements")) {
            return;
        }

        jdbcTemplate.execute("create sequence if not exists settlement_seq increment by 1000");
        jdbcTemplate.execute("""
                select setval(
                    'settlement_seq',
                    greatest((select coalesce(max(id), 0) + 1000 from settlements), 1000),
                    false
                )
                """);
    }

    private void migrateBatchJobHistoryRunningState() {
        jdbcTemplate.execute("alter table batch_job_histories alter column ended_at drop not null");
        if (columnExists("batch_job_histories", "strategy")) {
            jdbcTemplate.execute("alter table batch_job_histories alter column strategy drop not null");
        }
        replaceCheckConstraint(
                "batch_job_histories",
                "status",
                "batch_job_histories_status_check",
                "status in ('RUNNING', 'SUCCESS', 'FAILED')"
        );
    }

    private boolean tableExists(String tableName) {
        Boolean exists = jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from information_schema.tables
                    where table_schema = current_schema()
                      and table_name = ?
                )
                """, Boolean.class, tableName);
        return Boolean.TRUE.equals(exists);
    }

    private boolean columnExists(String tableName, String columnName) {
        Boolean exists = jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from information_schema.columns
                    where table_schema = current_schema()
                      and table_name = ?
                      and column_name = ?
                )
                """, Boolean.class, tableName, columnName);
        return Boolean.TRUE.equals(exists);
    }

    private void dropLegacySettlementUniqueConstraint() {
        jdbcTemplate.queryForList("""
                        select c.conname
                        from pg_constraint c
                        join pg_class t on t.oid = c.conrelid
                        join pg_namespace n on n.oid = t.relnamespace
                        where n.nspname = current_schema()
                          and t.relname = 'settlements'
                          and c.contype = 'u'
                          and (
                              select array_agg(a.attname::text order by u.ordinality)
                              from unnest(c.conkey) with ordinality as u(attnum, ordinality)
                              join pg_attribute a on a.attrelid = t.oid and a.attnum = u.attnum
                          ) = array['merchant_id', 'settlement_date']
                        """, String.class)
                .stream()
                .filter(Objects::nonNull)
                .forEach(constraintName -> jdbcTemplate.execute(
                        "alter table settlements drop constraint " + quoteIdentifier(constraintName)
                ));
    }

    private void replaceCheckConstraint(
            String tableName,
            String columnName,
            String newConstraintName,
            String checkExpression
    ) {
        dropCheckConstraintsForColumn(tableName, columnName);
        jdbcTemplate.execute(
                "alter table " + quoteIdentifier(tableName)
                        + " add constraint " + quoteIdentifier(newConstraintName)
                        + " check (" + checkExpression + ")"
        );
    }

    private void dropCheckConstraintsForColumn(String tableName, String columnName) {
        jdbcTemplate.queryForList("""
                        select c.conname
                        from pg_constraint c
                        join pg_class t on t.oid = c.conrelid
                        join pg_namespace n on n.oid = t.relnamespace
                        where n.nspname = current_schema()
                          and t.relname = ?
                          and c.contype = 'c'
                          and exists (
                              select 1
                              from unnest(c.conkey) as cols(attnum)
                              join pg_attribute a on a.attrelid = t.oid and a.attnum = cols.attnum
                              where a.attname = ?
                          )
                        """, String.class, tableName, columnName)
                .stream()
                .filter(Objects::nonNull)
                .forEach(constraintName -> jdbcTemplate.execute(
                        "alter table " + quoteIdentifier(tableName) + " drop constraint " + quoteIdentifier(constraintName)
                ));
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
