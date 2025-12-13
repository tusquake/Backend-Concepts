package com.travelsaga.saga.choreography;

import com.travelsaga.dto.TravelBookingRequest;
import com.travelsaga.dto.TravelBookingResponse;
import com.travelsaga.entity.SagaStateEntity;
import com.travelsaga.entity.SagaStatus;
import com.travelsaga.entity.SagaType;
import com.travelsaga.event.EventPublisher;
import com.travelsaga.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChoreographySaga {

    private final EventPublisher eventPublisher;
    private final SagaStateRepository sagaStateRepository;

    public TravelBookingResponse initiateBooking(TravelBookingRequest request) {
        log.info("💃 Starting CHOREOGRAPHY Saga (Event-Driven)");

        SagaStateEntity state = initializeSaga(request);

        // In choreography, we just publish the initial event
        // Each service listens and reacts independently
        eventPublisher.publishEvent(
                state.getSagaId(),
                "BOOKING_INITIATED",
                request.getDestination()
        );

        TravelBookingResponse response = new TravelBookingResponse();
        response.setBookingId(state.getSagaId());
        response.setStatus("INITIATED");
        response.setMessage("Booking initiated via CHOREOGRAPHY. Services will process independently.");
        response.setSagaType("CHOREOGRAPHY");

        return response;
    }

    private SagaStateEntity initializeSaga(TravelBookingRequest request) {
        SagaStateEntity state = new SagaStateEntity();
        state.setSagaId(UUID.randomUUID().toString());
        state.setCustomerId(request.getCustomerId());
        state.setDestination(request.getDestination());
        state.setCheckInDate(request.getCheckInDate());
        state.setCheckOutDate(request.getCheckOutDate());
        state.setNumberOfGuests(request.getNumberOfGuests());
        state.setStatus(SagaStatus.IN_PROGRESS);
        state.setSagaType(SagaType.CHOREOGRAPHY);
        state.setCreatedAt(LocalDateTime.now());
        state.setUpdatedAt(LocalDateTime.now());
        return sagaStateRepository.save(state);
    }
}