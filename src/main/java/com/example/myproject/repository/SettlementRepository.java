package com.example.myproject.repository;

import com.example.myproject.domain.settlement.Settlement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    boolean existsBySettlementDate(LocalDate settlementDate);

    @EntityGraph(attributePaths = "merchant")
    List<Settlement> findAllBySettlementDateOrderByFinalSettlementAmountDesc(LocalDate settlementDate);
}
