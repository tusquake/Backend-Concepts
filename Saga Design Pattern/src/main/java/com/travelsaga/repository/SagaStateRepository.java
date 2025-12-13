package com.travelsaga.repository;

import com.travelsaga.entity.SagaStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SagaStateRepository extends JpaRepository<SagaStateEntity, Long> {
    Optional<SagaStateEntity> findBySagaId(String sagaId);
}