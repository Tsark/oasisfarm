package com.hybridiize.oasisfarm.event.v2;

// This is an interface, representing any action that can be executed.
public interface PhaseAction {
    String getType();
    void execute(); // We will add parameters here later
}