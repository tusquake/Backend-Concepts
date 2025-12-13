package com.travelsaga.event;

import com.travelsaga.entity.BookingEvent;
import com.travelsaga.repository.BookingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final BookingEventRepository eventRepository;

    public void publishEvent(String sagaId, String eventType, String eventData) {
        log.info("📢 Publishing event: {} for saga {}", eventType, sagaId);

        BookingEvent event = new BookingEvent();
        event.setSagaId(sagaId);
        event.setEventType(eventType);
        event.setEventData(eventData);
        event.setTimestamp(LocalDateTime.now());
        eventRepository.save(event);

        // Publish to application event bus
        SagaEvent sagaEvent = new SagaEvent(sagaId, eventType, eventData);
        applicationEventPublisher.publishEvent(sagaEvent);
    }
}