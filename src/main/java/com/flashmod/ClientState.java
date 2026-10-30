package com.flashmod;

public final class ClientState {
    public static int stam = 100, max = 100;
    public static boolean slow, tired, received;

    public static void set(StatePayload p) {
        stam = p.stamina(); max = Math.max(1, p.max()); slow = p.slow(); tired = p.tired(); received = true;
    }
    public static void reset() { received = false; slow = false; tired = false; }
    private ClientState() {}
}
