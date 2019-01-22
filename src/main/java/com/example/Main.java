package com.example;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.Configuration;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.config.EventHandlingConfiguration;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        //create projections
        ReplayableProjection replayableProjection = new ReplayableProjection();
        NonReplayableProjection nonReplayableProjection = new NonReplayableProjection();

        //configure axon
        Configuration configuration = DefaultConfigurer.defaultConfiguration()
                .configureAggregate(Aggregate.class)
                .configureEventStore(c -> new EmbeddedEventStore(new InMemoryEventStorageEngine()))
                .registerModule(new EventHandlingConfiguration()
                        .registerEventHandler(c -> replayableProjection)
                        .registerEventHandler(c -> nonReplayableProjection)
                        .assignHandlersMatching("projections", o -> true)
                        .registerTokenStore("projections", c -> new InMemoryTokenStore())
                        .usingTrackingProcessors())
                .buildConfiguration();


        //start
        configuration.start();
        CommandGateway commandGateway = configuration.commandGateway();

        //fire commands
        Stream.of("1", "2", "3").forEach(id -> commandGateway.sendAndWait(new Command(id)));

        //wait for the correct initial event handling
        while (replayableProjection.getEventsProcessed() < 3 || nonReplayableProjection.getEventsProcessed() < 3) {
            LOGGER.info("Waiting for events to be processed by projections.");
            Thread.sleep(100);
        }

        //replay events
        configuration.eventProcessingConfiguration().eventProcessorByProcessingGroup("projections", TrackingEventProcessor.class).ifPresent(trackingEventProcessor -> {
            trackingEventProcessor.shutDown();
            trackingEventProcessor.resetTokens();
            trackingEventProcessor.start();
        });

        //wait for the correct and incorrect replays
        while (replayableProjection.getEventsProcessed() < 6 || nonReplayableProjection.getEventsProcessed() < 4) {
            LOGGER.info("Waiting for events to be processed by projections.");
            Thread.sleep(100);
        }

        //fire one more command
        commandGateway.sendAndWait(new Command("4"));

        //wait for the correct continuation of event processing
        while (replayableProjection.getEventsProcessed() < 7 || nonReplayableProjection.getEventsProcessed() < 5) {
            LOGGER.info("Waiting for events to be processed by projections.");
            Thread.sleep(100);
        }

        //shutdown
        configuration.shutdown();
        System.exit(0);
    }

}
