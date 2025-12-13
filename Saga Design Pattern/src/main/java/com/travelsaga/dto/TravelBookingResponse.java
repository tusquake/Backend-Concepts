package com.travelsaga.dto;

import lombok.Data;

@Data
public class TravelBookingResponse {
    private String bookingId;
    private String status;
    private String message;
    private String sagaType;
}