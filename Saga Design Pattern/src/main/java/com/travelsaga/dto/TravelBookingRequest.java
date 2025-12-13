package com.travelsaga.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TravelBookingRequest {
    private String customerId;
    private String destination;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int numberOfGuests;
    private String sagaType; // "ORCHESTRATION" or "CHOREOGRAPHY"
}