package com.meltstakecommander;

import java.nio.charset.StandardCharsets;

public class Commands {
    static byte[] getDrillCommand(String leftTurns, String rightTurns) {
        return null;
    }

    static byte[] getLightCommand() {
        return "LIGHT 50".getBytes(StandardCharsets.UTF_8);
    }


}
