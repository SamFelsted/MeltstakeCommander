package com.meltstakecommander;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {

    public final LinkedBlockingQueue<String> OUTPUTQUEUE;
    public final LinkedBlockingQueue<String> INPUTQUEUE;

    private boolean lastCommandStatus;
    private boolean restartTimer;
    private boolean confirmedARovrdStatus = true;

    Map<String, Object> settings;
    Map<String, List<String>> meltStakeData;

    public Client(Map<String, Object> settingsMap) {
        this.OUTPUTQUEUE = new LinkedBlockingQueue<>();
        this.INPUTQUEUE = new LinkedBlockingQueue<>();

        setNewSettings(settingsMap);
        configDataMap();

        try {
            startSocket(INPUTQUEUE, OUTPUTQUEUE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void setNewSettings(Map<String, Object> settingsMap) {
        settings = settingsMap;
    }

    public void configDataMap() {
        meltStakeData = new HashMap<>();

        for (String data : Commands.getDataStrings()) {
            meltStakeData.put(data, new ArrayList<>(2));
            meltStakeData.get(data).add("NULL DATA");
            meltStakeData.get(data).add("NULL TIMESTAMP");
        }
    }

    public void setRestartTimer(boolean restartTimer) {
        this.restartTimer = restartTimer;
    }

    public boolean getRestartTimer() {
        return restartTimer;
    }

    public boolean getConfirmedARovrdStatus() {
        return confirmedARovrdStatus;
    }

    public void sendData(String data) {
        OUTPUTQUEUE.offer(data);
    }

    public int getAntennaID() {return Integer.parseInt(settings.get("antenna").toString());}

    public String getDataDisplay1() {
        String text = "";
        if (meltStakeData.get("ROT").get(0).contains("NULL")) {
            text += "ROTATIONS\n\tLeft: NULL\n\tRight: NULL\n\tLast Time: NULL\n\n";
        } else {
            String[] rotData = meltStakeData.get("ROT").get(0).split(" ");
            text += "ROTATIONS\n\tLeft: " + rotData[1] + "\n\tRight: " + rotData[2] + "\n\tLast Time: " +
                    meltStakeData.get("ROT").get(1) + "\n\n";
        }
        if (meltStakeData.get("IMU").get(0).contains("NULL")) {
            text += "IMU\n\tPitch: NULL\n\tTilt: NULL\n\tRoll: NULL\n\tLast Time: NULL\n";
        } else {
            String[] imuData = meltStakeData.get("IMU").get(0).split(" ");
            text += "IMU\n\tPitch: " + imuData[1] + "\n\tTilt: " + imuData[2] + "\n\tRoll: " + imuData[3]+"\n\tLast Time: " +
                    meltStakeData.get("IMU").get(1) + "\n";
        }

        return text;
    }

    public String getDataDisplay2() {
        String text = "";
        String powerData = meltStakeData.get("IV").get(0);
        if (powerData.contains("NULL")) {
            text += "POWER\n\tVoltage: NULL\n\tCurrentL: NULL\n\tCurrentR: NULL\n\tLast Time: NULL\n";
        } else {
            String[] splicedPowerData = powerData.split(" ");
            text += "POWER\n\tVoltage: "+ splicedPowerData[1] +
                    "\n\tCurrentL: " + splicedPowerData[2] + "\n\tCurrentR: " + splicedPowerData[3] +"\n\tLast Time: "
                    + meltStakeData.get("IV").get(1) + "\n";
        }
        String lsData = meltStakeData.get("LS").get(0);
        if (lsData.contains("NULL")) {
            text += "LS\n\tCurrent Reading: NULL\n\tTare Value: NULL\n\tThreshold: NULL\n\tLast Time: NULL\n";
        } else {
            String[] splicedLSdata = lsData.split(" ");
            text += "LS\n\tCurrent Reading: "+ splicedLSdata[1] +
                    "\n\tTare Value: " + splicedLSdata[2] + "\n\tThreshold: " + splicedLSdata[3] +"\n\tLast Time: "
                    + meltStakeData.get("IV").get(1) + "\n\n";
        }

        return text;
    }
    public String getReceivedData() {
        return INPUTQUEUE.poll();
    }

    /**
     *
     * @return (true) if the last command was successfully send **TO THE ROV**
     * Use the printout to see if the MeltStake received a command
     */
    public boolean testConnection() {
        sendData(Commands.getDataCommand(getAntennaID(), "ROT"));
        return lastCommandStatus;
    }

    /**
     * Starts the threading for handling client to ROV interactions
     * @param input outgoing command queue
     * @param output incoming data queue
     * @throws IOException hopefully this doesn't happen
     */
    private void startSocket(LinkedBlockingQueue<String> input, LinkedBlockingQueue<String> output) throws IOException {
        new Thread(() -> socketReader(input)).start();
        new Thread(() -> socketWriter(output)).start();
    }

    private void socketReader(LinkedBlockingQueue<String> input)  {
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);


        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(settings.get("portIN").toString())); //Should be IP of laptop
            System.out.println("Waiting for clients to connect...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientProcessingPool.submit(new ClientTask(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Unable to process client request");
            e.printStackTrace();
        }
    }

    private class ClientTask implements Runnable {
        private final Socket clientSocket;

        private ClientTask(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                byte[] inputRAW = clientSocket.getInputStream().readAllBytes();
                String input = new String(inputRAW, StandardCharsets.UTF_8);

                System.out.println("received: " + input);

                for (String data : Commands.getDataStrings()) {
                    if (input.contains(data) &&
                            !input.toLowerCase().contains("data") && !input.toLowerCase().contains("tare") &&
                            !input.toLowerCase().contains("thresh")
                    ) {
                        String lastValue = input.substring(4);
                        String lastTimestamp = "%02d:%02d:%02d".formatted(
                                LocalTime.now().getHour(), LocalTime.now().getMinute(),
                                LocalTime.now().getSecond()
                        );

                        meltStakeData.get(data).set(0, lastValue);
                        meltStakeData.get(data).set(1, lastTimestamp);
                    }
                }



                if (input.toLowerCase().contains("ar_ovrd")){
                    char status = input.charAt(input.length() - 1);
                    confirmedARovrdStatus = (status == 'T');
                }
                if (input.toLowerCase().contains("drill") || input.toLowerCase().contains("ar_reset")) {
                    restartTimer = true;
                }

                INPUTQUEUE.offer(input);

                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void socketWriter(LinkedBlockingQueue<String> queue) {
        try {
            while (true) {
                String message = queue.take();
                Socket socket;

                try {
                    socket = new Socket(
                            settings.get("ip").toString(),
                            Integer.parseInt(settings.get("portOUT").toString())
                    );
                } catch (Exception e) {
                    System.out.println("error " + e.getMessage());
                    lastCommandStatus = false;
                    return;
                }
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(), 4096);

                out.write(message.getBytes(StandardCharsets.UTF_8));

                out.flush();
                System.out.println("Sent: " + message);

                socket.close();
                lastCommandStatus = true;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}