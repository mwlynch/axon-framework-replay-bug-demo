package com.example;

public class Event {

    private String aggregateId;

    public Event(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

}
