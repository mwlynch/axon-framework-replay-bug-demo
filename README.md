# axon-framework-replay-bug-demo
Demonstrates a bug in axon framework 3.4.2

## How to run
Run the provided entrypoint script: ```run.sh```

## Expectation

When replaying events, no handlers marked with ```@AllowReplay(false)``` should be invoked. 

## Reality
 
For the last event in the sequence, the handler marked with ```@AllowReplay(false)``` is invoked when it shouldn't be.

Example output:
```
[EventProcessor[projections]-0] INFO org.axonframework.eventhandling.TrackingEventProcessor - Using current Thread for last segment worker: TrackingSegmentWorker{processor=projections, segment=Segment[0/0]}
[EventProcessor[projections]-0] INFO org.axonframework.eventhandling.TrackingEventProcessor - Fetched token: null for segment: Segment[0/0]
[EventProcessor[projections]-0] INFO com.example.ReplayableProjection - REPLAYABLE: id=1, isReplay=false
[EventProcessor[projections]-0] INFO com.example.NonReplayableProjection - NON REPLAYABLE: id=1, isReplay=false
[EventProcessor[projections]-0] INFO com.example.ReplayableProjection - REPLAYABLE: id=2, isReplay=false
[EventProcessor[projections]-0] INFO com.example.NonReplayableProjection - NON REPLAYABLE: id=2, isReplay=false
[EventProcessor[projections]-0] INFO com.example.ReplayableProjection - REPLAYABLE: id=3, isReplay=false
[EventProcessor[projections]-0] INFO com.example.NonReplayableProjection - NON REPLAYABLE: id=3, isReplay=false
[com.example.Main.main()] INFO org.axonframework.eventhandling.TrackingEventProcessor - Shutdown state set for Processor 'projections'. Awaiting termination...
[EventProcessor[projections]-1] INFO org.axonframework.eventhandling.TrackingEventProcessor - Using current Thread for last segment worker: TrackingSegmentWorker{processor=projections, segment=Segment[0/0]}
[EventProcessor[projections]-1] INFO org.axonframework.eventhandling.TrackingEventProcessor - Fetched token: ReplayToken{currentToken=IndexTrackingToken{globalIndex=-1}, tokenAtReset=IndexTrackingToken{globalIndex=2}} for segment: Segment[0/0]
[EventProcessor[projections]-1] INFO com.example.ReplayableProjection - REPLAYABLE: id=1, isReplay=true
[EventProcessor[projections]-1] INFO com.example.ReplayableProjection - REPLAYABLE: id=2, isReplay=true
[EventProcessor[projections]-1] INFO com.example.ReplayableProjection - REPLAYABLE: id=3, isReplay=true
[EventProcessor[projections]-1] INFO com.example.NonReplayableProjection - NON REPLAYABLE: id=3, isReplay=true
[com.example.Main.main()] INFO org.axonframework.eventhandling.TrackingEventProcessor - Shutdown state set for Processor 'projections'. Awaiting termination...
```

Note the line with ```NON REPLAYABLE: id=3, isReplay=true``` where a non replayable event handler is invoked.

## Likely cause
This is likely due to a bad implementation of the covers method in ```GlobalSequenceTrackingToken```:
```java 
@Override
public boolean covers(TrackingToken other) {
    Assert.isTrue(other instanceof GlobalSequenceTrackingToken, () -> "Incompatible token type provided.");
    GlobalSequenceTrackingToken otherToken = (GlobalSequenceTrackingToken) other;

    return otherToken.globalIndex < this.globalIndex;
}
```

I expect that it should rather return ```return otherToken.globalIndex <= this.globalIndex;```.
i.e. less than or equals