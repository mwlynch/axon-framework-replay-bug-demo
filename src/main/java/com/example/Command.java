package com.example;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

public class Command {

    @TargetAggregateIdentifier
    private String aggregateId;

    public Command(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateId() {
        return aggregateId;
    }


}
