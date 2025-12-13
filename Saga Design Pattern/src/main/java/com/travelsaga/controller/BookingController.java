package com.travelsaga.controller;

import com.travelsaga.dto.TravelBookingRequest;
import com.travelsaga.dto.TravelBookingResponse;
import com.travelsaga.saga.choreography.ChoreographySaga;
import com.travelsaga.saga.orchestrator.OrchestrationSaga;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final OrchestrationSaga orchestrationSaga;
    private final ChoreographySaga choreographySaga;

    @PostMapping("/travel")
    public ResponseEntity<TravelBookingResponse> createBooking(
            @RequestBody TravelBookingRequest request) {

        TravelBookingResponse response;

        if ("CHOREOGRAPHY".equalsIgnoreCase(request.getSagaType())) {
            response = choreographySaga.initiateBooking(request);
        } else {
            response = orchestrationSaga.executeBooking(request);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Travel Saga Service Running - Both Patterns Available!");
    }
}