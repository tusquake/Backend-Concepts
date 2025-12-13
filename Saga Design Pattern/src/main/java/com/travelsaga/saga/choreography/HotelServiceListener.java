package com.travelsaga.saga.choreography;

import com.travelsaga.entity.SagaStateEntity;
import com.travelsaga.event.EventPublisher;
import com.travelsaga.event.SagaEvent;
import com.travelsaga.repository.SagaStateRepository;
import com.travelsaga.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class HotelServiceListener {

    private final HotelService hotelService;
    private final EventPublisher eventPublisher;
    private final SagaStateRepository sagaStateRepository;

    @Async
    @EventListener
    public void handleFlightBooked(SagaEvent event) {
        if (!"FLIGHT_BOOKED".equals(event.getEventType())) return;

        log.info("🏨 [HOTEL SERVICE] Received FLIGHT_BOOKED event");

        try {
            SagaStateEntity state = sagaStateRepository.findBySagaId(event.getSagaId()).orElseThrow();

            String hotelId = hotelService.bookHotel(
                    state.getDestination(),
                    state.getCheckInDate(),
                    state.getCheckOutDate(),
                    state.getNumberOfGuests()
            );

            state.setHotelBookingId(hotelId);
            state.setCompletedSteps(state.getCompletedSteps() + ",HOTEL");
            sagaStateRepository.save(state);

            eventPublisher.publishEvent(event.getSagaId(), "HOTEL_BOOKED", hotelId);

        } catch (Exception e) {
            log.error("🏨 [HOTEL SERVICE] Booking failed", e);
            eventPublisher.publishEvent(event.getSagaId(), "HOTEL_FAILED", e.getMessage());
            // Trigger compensation
            eventPublisher.publishEvent(event.getSagaId(), "COMPENSATE_FLIGHT", "");
        }
    }

    @Async
    @EventListener
    public void handleCompensation(SagaEvent event) {
        if (!"COMPENSATE_HOTEL".equals(event.getEventType())) return;

        log.info("🏨 [HOTEL SERVICE] Compensating - canceling hotel");

        SagaStateEntity state = sagaStateRepository.findBySagaId(event.getSagaId()).orElseThrow();
        if (state.getHotelBookingId() != null) {
            hotelService.cancelBooking(state.getHotelBookingId());
        }
        eventPublisher.publishEvent(event.getSagaId(), "COMPENSATE_FLIGHT", "");
    }
}