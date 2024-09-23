package com.meltstakecommander;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {

    //Network Communication
    private Socket socket;
    private String address;
    private int port;

    private DataInputStream in;
    private DataOutputStream out;
    public final LinkedBlockingQueue<String> MESSAGES;
    private final LinkedBlockingQueue<String> MESSAGES_BY_CLIENT;


    public Client(String address, int port) {

        this.MESSAGES_BY_CLIENT = new LinkedBlockingQueue<>();
        this.MESSAGES = new LinkedBlockingQueue<>();


        this.address = address;
        this.port = port;
        connect();
    }


    public void connect() {
        try {
            socket = new Socket(address, port);

            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());

        } catch (Exception e) {
            System.out.println("error " + e.getMessage());
        }
        ReadMessagesFromServer server = new ReadMessagesFromServer(socket);
        new Thread(server).start();
    }

    public void sendData(byte[] data) throws IOException {
        socket.getOutputStream().write(data);
    }

    public boolean testConnection() {
        try {
            // Send a small ping message to check the connection
            OutputStream out = socket.getOutputStream();
            out.write(1);  // Sending a small byte
            out.flush();
        } catch (IOException | NullPointerException e) {
            // If an exception occurs, the socket is no longer active
            return false;
        }

        return true;
    }


    private class ReadMessagesFromServer implements Runnable{
        DataInputStream in;
        DataOutputStream out;
        Socket socket;

        ReadMessagesFromServer(Socket socket){
            this.socket = socket;
        }

        public void run() {
            try {
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(socket.getOutputStream());
                byte[] buffer = new byte[4096];

                while (true) {
                    if (in.available() > 0) {
                        int bytesRead = in.read(buffer);
                        if (bytesRead == -1) {
                            // End of stream, connection closed
                            System.out.println("Connection closed by server.");
                            break;
                        }
                        String receivedData = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                        System.out.println("Received: " + receivedData);
                        MESSAGES_BY_CLIENT.offer(receivedData);  // Add the message to the queue
                    } else {
                        // No data available, sleep briefly to avoid busy-waiting
                        try {
                            Thread.sleep(100);  // Adjust this value if needed
                        } catch (Exception ignored) {}
                    }

                }
            } catch (NullPointerException e) {
                System.out.println("no conn");
            } catch(IOException e){
                System.out.println(e.toString());
            }
        }
    }

}