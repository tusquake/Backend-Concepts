package com.travelsaga.saga.choreography;

import com.travelsaga.entity.SagaStateEntity;
import com.travelsaga.event.EventPublisher;
import com.travelsaga.event.SagaEvent;
import com.travelsaga.repository.SagaStateRepository;
import com.travelsaga.service.FlightService;
import com.travelsaga.service.HotelService;
import com.travelsaga.service.CarRentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlightServiceListener {

    private final FlightService flightService;
    private final EventPublisher eventPublisher;
    private final SagaStateRepository sagaStateRepository;

    @Async
    @EventListener
    public void handleBookingInitiated(SagaEvent event) {
        if (!"BOOKING_INITIATED".equals(event.getEventType())) return;

        log.info("✈️ [FLIGHT SERVICE] Received BOOKING_INITIATED event");

        try {
            SagaStateEntity state = sagaStateRepository.findBySagaId(event.getSagaId()).orElseThrow();

            String flightId = flightService.bookFlight(
                    state.getDestination(),
                    state.getCheckInDate(),
                    state.getNumberOfGuests()
            );

            state.setFlightBookingId(flightId);
            state.setCompletedSteps("FLIGHT");
            sagaStateRepository.save(state);

            // Publish success event - Hotel service will listen
            eventPublisher.publishEvent(event.getSagaId(), "FLIGHT_BOOKED", flightId);

        } catch (Exception e) {
            log.error("✈️ [FLIGHT SERVICE] Booking failed", e);
            eventPublisher.publishEvent(event.getSagaId(), "FLIGHT_FAILED", e.getMessage());
        }
    }

    @Async
    @EventListener
    public void handleCompensation(SagaEvent event) {
        if (!"COMPENSATE_FLIGHT".equals(event.getEventType())) return;

        log.info("✈️ [FLIGHT SERVICE] Compensating - canceling flight");

        SagaStateEntity state = sagaStateRepository.findBySagaId(event.getSagaId()).orElseThrow();
        if (state.getFlightBookingId() != null) {
            flightService.cancelFlight(state.getFlightBookingId());
        }
    }
}