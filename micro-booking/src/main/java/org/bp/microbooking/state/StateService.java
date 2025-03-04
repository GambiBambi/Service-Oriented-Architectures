package org.bp.microbooking.state;

import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.bp.microbooking.model.BookVisitRequest;
import org.bp.microbooking.model.BookingInfo;
import org.springframework.stereotype.Service;


public class StateService {
    private HashMap<String, StateMachine> processingStates=new HashMap<>();

    public StateService(StateMachineBuilder stateMachineBuilder) {
        this.stateMachineBuilder = stateMachineBuilder;
    }


    private StateMachineBuilder stateMachineBuilder = null;

    public ProcessingState sendEvent(String bookingId, ProcessingEvent event) {
        StateMachine stateMachine;
        synchronized(this){
            stateMachine = processingStates.get(bookingId);
            if (stateMachine==null) {
                stateMachine=stateMachineBuilder.build();
                processingStates.put(bookingId, stateMachine);
            }
        }
        return stateMachine.sendEvent(event);

    }

    public void removeState(String bookingId) {
        processingStates.remove(bookingId);
    }

}