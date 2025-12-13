package com.travelsaga.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "saga_state")
@Data
public class SagaStateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sagaId;
    private String customerId;
    private String destination;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfGuests;

    private String flightBookingId;
    private String hotelBookingId;
    private String carRentalId;

    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    @Enumerated(EnumType.STRING)
    private SagaType sagaType; // ORCHESTRATION or CHOREOGRAPHY

    private String completedSteps;
    private String compensatedSteps;
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}