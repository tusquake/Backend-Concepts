package com.travelsaga.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class HotelService {
    private final Random random = new Random();

    public String bookHotel(String destination, LocalDate checkIn, LocalDate checkOut, int guests) {
        simulateDelay();
        if (random.nextInt(100) < 15) {
            throw new RuntimeException("No rooms available");
        }
        return "HTL-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void cancelBooking(String hotelId) {
        simulateDelay();
        log.info("Hotel {} canceled", hotelId);
    }

    private void simulateDelay() {
        try { Thread.sleep(500); } catch (InterruptedException e) {}
    }
}