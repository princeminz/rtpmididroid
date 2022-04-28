package com.minz.midi;

import java.io.Serializable;

public class ControlPacket implements Serializable {
    int signature[] ;
    byte command[];
    int version;
    int initiatorToken;
    int ssrc;
    String name;

    public ControlPacket(int signature[] ,byte command[], int version, int initiatorToken, int ssrc, String name) {
        this.signature = signature;
        this.command=command;
        this.version = version;
        this.initiatorToken = initiatorToken;
        this.ssrc = ssrc;
        this.name = name;
    }
}
