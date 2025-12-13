package com.travelsaga.repository;

import com.travelsaga.entity.BookingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingEventRepository extends JpaRepository<BookingEvent, Long> {
    List<BookingEvent> findBySagaIdOrderByTimestampAsc(String sagaId);
}