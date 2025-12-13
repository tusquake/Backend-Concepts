package com.travelsaga.event;

public class SagaEvent {
    private String sagaId;
    private String eventType;
    private String eventData;

    public SagaEvent(String sagaId, String eventType, String eventData) {
        this.sagaId = sagaId;
        this.eventType = eventType;
        this.eventData = eventData;
    }

    public String getSagaId() { return sagaId; }
    public String getEventType() { return eventType; }
    public String getEventData() { return eventData; }
}