package com.travelsaga.saga.orchestrator;

import com.travelsaga.dto.TravelBookingRequest;
import com.travelsaga.dto.TravelBookingResponse;
import com.travelsaga.entity.SagaStateEntity;
import com.travelsaga.entity.SagaStatus;
import com.travelsaga.entity.SagaType;
import com.travelsaga.repository.SagaStateRepository;
import com.travelsaga.service.FlightService;
import com.travelsaga.service.HotelService;
import com.travelsaga.service.CarRentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrchestrationSaga {

    private final FlightService flightService;
    private final HotelService hotelService;
    private final CarRentalService carRentalService;
    private final SagaStateRepository sagaStateRepository;

    public TravelBookingResponse executeBooking(TravelBookingRequest request) {
        log.info("🎯 Starting ORCHESTRATION Saga");

        SagaStateEntity state = initializeSaga(request, SagaType.ORCHESTRATION);

        try {
            state.setStatus(SagaStatus.IN_PROGRESS);
            sagaStateRepository.save(state);

            // Orchestrator controls all steps sequentially

            // Step 1: Book Flight
            log.info("🎯 [ORCHESTRATOR] Step 1: Booking flight");
            String flightId = flightService.bookFlight(
                    request.getDestination(),
                    request.getCheckInDate(),
                    request.getNumberOfGuests()
            );
            state.setFlightBookingId(flightId);
            state.setCompletedSteps("FLIGHT");
            sagaStateRepository.save(state);

            // Step 2: Book Hotel
            log.info("🎯 [ORCHESTRATOR] Step 2: Booking hotel");
            String hotelId = hotelService.bookHotel(
                    request.getDestination(),
                    request.getCheckInDate(),
                    request.getCheckOutDate(),
                    request.getNumberOfGuests()
            );
            state.setHotelBookingId(hotelId);
            state.setCompletedSteps(state.getCompletedSteps() + ",HOTEL");
            sagaStateRepository.save(state);

            // Step 3: Rent Car
            log.info("🎯 [ORCHESTRATOR] Step 3: Renting car");
            String carId = carRentalService.rentCar(
                    request.getDestination(),
                    request.getCheckInDate(),
                    request.getCheckOutDate()
            );
            state.setCarRentalId(carId);
            state.setCompletedSteps(state.getCompletedSteps() + ",CAR");
            state.setStatus(SagaStatus.COMPLETED);
            sagaStateRepository.save(state);

            log.info("✅ [ORCHESTRATOR] Saga completed successfully");
            return buildSuccessResponse(state);

        } catch (Exception e) {
            log.error("❌ [ORCHESTRATOR] Saga failed: {}", e.getMessage());
            state.setStatus(SagaStatus.COMPENSATING);
            state.setFailureReason(e.getMessage());
            sagaStateRepository.save(state);

            compensate(state);

            return buildFailureResponse(state);
        }
    }

    private void compensate(SagaStateEntity state) {
        log.info("🔄 [ORCHESTRATOR] Starting compensation");

        String[] steps = state.getCompletedSteps() != null ?
                state.getCompletedSteps().split(",") : new String[0];

        // Compensate in reverse order
        for (int i = steps.length - 1; i >= 0; i--) {
            String step = steps[i];
            try {
                switch (step) {
                    case "CAR":
                        log.info("🔄 [ORCHESTRATOR] Canceling car rental");
                        carRentalService.cancelRental(state.getCarRentalId());
                        break;
                    case "HOTEL":
                        log.info("🔄 [ORCHESTRATOR] Canceling hotel");
                        hotelService.cancelBooking(state.getHotelBookingId());
                        break;
                    case "FLIGHT":
                        log.info("🔄 [ORCHESTRATOR] Canceling flight");
                        flightService.cancelFlight(state.getFlightBookingId());
                        break;
                }
            } catch (Exception e) {
                log.error("Failed to compensate step: {}", step, e);
            }
        }

        state.setStatus(SagaStatus.COMPENSATED);
        sagaStateRepository.save(state);
        log.info("✅ [ORCHESTRATOR] Compensation completed");
    }

    private SagaStateEntity initializeSaga(TravelBookingRequest request, SagaType type) {
        SagaStateEntity state = new SagaStateEntity();
        state.setSagaId(UUID.randomUUID().toString());
        state.setCustomerId(request.getCustomerId());
        state.setDestination(request.getDestination());
        state.setCheckInDate(request.getCheckInDate());
        state.setCheckOutDate(request.getCheckOutDate());
        state.setNumberOfGuests(request.getNumberOfGuests());
        state.setStatus(SagaStatus.PENDING);
        state.setSagaType(type);
        state.setCreatedAt(LocalDateTime.now());
        state.setUpdatedAt(LocalDateTime.now());
        return sagaStateRepository.save(state);
    }

    private TravelBookingResponse buildSuccessResponse(SagaStateEntity state) {
        TravelBookingResponse response = new TravelBookingResponse();
        response.setBookingId(state.getSagaId());
        response.setStatus("SUCCESS");
        response.setMessage("All bookings completed successfully via ORCHESTRATION!");
        response.setSagaType("ORCHESTRATION");
        return response;
    }

    private TravelBookingResponse buildFailureResponse(SagaStateEntity state) {
        TravelBookingResponse response = new TravelBookingResponse();
        response.setBookingId(state.getSagaId());
        response.setStatus("FAILED");
        response.setMessage("Booking failed: " + state.getFailureReason() +
                ". Compensated via ORCHESTRATION.");
        response.setSagaType("ORCHESTRATION");
        return response;
    }
}