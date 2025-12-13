package com.travelsaga.saga.choreography;

import com.travelsaga.entity.SagaStateEntity;
import com.travelsaga.event.EventPublisher;
import com.travelsaga.event.SagaEvent;
import com.travelsaga.repository.SagaStateRepository;
import com.travelsaga.service.CarRentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class CarServiceListener {

    private final CarRentalService carRentalService;
    private final EventPublisher eventPublisher;
    private final SagaStateRepository sagaStateRepository;

    @Async
    @EventListener
    public void handleHotelBooked(SagaEvent event) {
        if (!"HOTEL_BOOKED".equals(event.getEventType())) return;

        log.info("🚗 [CAR SERVICE] Received HOTEL_BOOKED event");

        try {
            SagaStateEntity state = sagaStateRepository.findBySagaId(event.getSagaId()).orElseThrow();

            String carId = carRentalService.rentCar(
                    state.getDestination(),
                    state.getCheckInDate(),
                    state.getCheckOutDate()
            );

            state.setCarRentalId(carId);
            state.setCompletedSteps(state.getCompletedSteps() + ",CAR");
            state.setStatus(com.travelsaga.entity.SagaStatus.COMPLETED);
            sagaStateRepository.save(state);

            eventPublisher.publishEvent(event.getSagaId(), "BOOKING_COMPLETED", "All done!");

        } catch (Exception e) {
            log.error("🚗 [CAR SERVICE] Rental failed", e);
            eventPublisher.publishEvent(event.getSagaId(), "CAR_FAILED", e.getMessage());
            // Trigger compensation chain
            eventPublisher.publishEvent(event.getSagaId(), "COMPENSATE_HOTEL", "");
        }
    }

    @Async
    @EventListener
    public void handleCompensation(SagaEvent event) {
        if (!"COMPENSATE_CAR".equals(event.getEventType())) return;

        log.info("🚗 [CAR SERVICE] Compensating - canceling rental");

        SagaStateEntity state = sagaStateRepository.findBySagaId(event.getSagaId()).orElseThrow();
        if (state.getCarRentalId() != null) {
            carRentalService.cancelRental(state.getCarRentalId());
        }
        eventPublisher.publishEvent(event.getSagaId(), "COMPENSATE_HOTEL", "");
    }
}