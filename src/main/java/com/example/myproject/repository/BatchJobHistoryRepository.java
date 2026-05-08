package com.example.myproject.repository;

import com.example.myproject.domain.batch.BatchJobHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchJobHistoryRepository extends JpaRepository<BatchJobHistory, Long> {

    List<BatchJobHistory> findTop20ByOrderByStartedAtDesc();
}
