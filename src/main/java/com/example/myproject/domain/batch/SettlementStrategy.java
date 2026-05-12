package com.example.myproject.domain.batch;

public enum SettlementStrategy {
    BASIC_LOOP,
    GROUP_BY_QUERY,
    GROUP_BY_BULK_SAVE,
    GROUP_BY_BULK_INDEX
}
