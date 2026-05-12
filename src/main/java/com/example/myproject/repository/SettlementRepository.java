package com.example.myproject.repository;

import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.settlement.Settlement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    boolean existsBySettlementDateAndProcessingStrategy(LocalDate settlementDate, SettlementStrategy processingStrategy);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Settlement")
    int deleteAllInBulk();

    @EntityGraph(attributePaths = "merchant")
    List<Settlement> findAllBySettlementDateOrderByFinalSettlementAmountDesc(LocalDate settlementDate);

    @EntityGraph(attributePaths = "merchant")
    List<Settlement> findAllBySettlementDateAndProcessingStrategyOrderByFinalSettlementAmountDesc(
            LocalDate settlementDate,
            SettlementStrategy processingStrategy
    );
}
