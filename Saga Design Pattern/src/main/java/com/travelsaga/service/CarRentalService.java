package com.travelsaga.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class CarRentalService {
    private final Random random = new Random();

    public String rentCar(String destination, LocalDate pickupDate, LocalDate returnDate) {
        simulateDelay();
        if (random.nextInt(100) < 12) {
            throw new RuntimeException("No vehicles available");
        }
        return "CAR-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void cancelRental(String carId) {
        simulateDelay();
        log.info("Car rental {} canceled", carId);
    }

    private void simulateDelay() {
        try { Thread.sleep(500); } catch (InterruptedException e) {}
    }
}