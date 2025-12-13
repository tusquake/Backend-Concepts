package com.travelsaga.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class FlightService {
    private final Random random = new Random();

    public String bookFlight(String destination, LocalDate date, int passengers) {
        simulateDelay();
        if (random.nextInt(10) == 0) {
            throw new RuntimeException("No available flights");
        }
        return "FLT-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void cancelFlight(String flightId) {
        simulateDelay();
        log.info("Flight {} canceled", flightId);
    }

    private void simulateDelay() {
        try { Thread.sleep(500); } catch (InterruptedException e) {}
    }
}