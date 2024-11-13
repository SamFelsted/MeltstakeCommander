package com.meltstakecommander;

import java.util.ArrayList;
import java.util.List;

public class Commands {

    /**
     * Static method for interfacing with MeltStake data keys,
     * To add another request-able data simply add the key the same way
     * the rest of the keys are formatted
     * @return Keys of request-able MeltStake Data
     */
    static List<String> getDataStrings() {
        List<String> dataStrings = new ArrayList<>();
        dataStrings.add("ROT");
        dataStrings.add("IV");
        dataStrings.add("IMU");
        dataStrings.add("LS");
        return dataStrings;
    }

    static String getOffCommand(int id) {
        return (id + " OFF");
    }
    static String getRotResetCommand(int id) {
        return (id + " SETROT 0 0");
    }
    static String getReleaseCommand(int id) {
        return (id + " RELEASE");
    }
    static String getTareCommand(int id) {
        return (id + " LS_TARE");
    }

    static String getLSThreshCommand(int id, String thresh) {
        return (id + " LS_THRESH " + thresh);
    }
    static String getDrillCommand(int id, String leftTurns, String rightTurns) {
        return (id + " DRILL " + leftTurns + " " + rightTurns);
    }
    static String getARReset(int id) {
        return (id + " AR_RESET");
    }
    static String getLightCommand(int id, int power) {
        return (id + " LIGHT " + power);
    }

    static String getCurrentLimitCommand(int id, String limit) {
        return (id + " CLA " + limit);
    }


    static String getSpeedCommand(int id, double power) {
        return (id + " SETSPD " + power);
    }
    static String getDataCommand(int id, String type) {
        return (id + " DATA " + type);
    }

    static String getAutoCommand(int id, String rotPerDrill, String timeBetween, String depTime) {
        return (id + " AUTO " + rotPerDrill + timeBetween + depTime);
    }

    static String getDisengageToggle(int id, boolean state) {
        return (id + " AR_OVRD " + (state ? "T" : "F"));
    }
}
