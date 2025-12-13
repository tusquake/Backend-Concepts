package com.travelsaga.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_events")
@Data
public class BookingEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sagaId;
    private String eventType; // FLIGHT_BOOKED, HOTEL_BOOKED, etc.
    private String eventData;
    private LocalDateTime timestamp;
}

