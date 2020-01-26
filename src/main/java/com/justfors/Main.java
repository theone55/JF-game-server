package com.justfors;

import com.dosse.upnp.UPnP;
import com.justfors.protocol.TransferData;
import com.justfors.server.NetConnectionServer;
import com.justfors.server.Server;
import com.justfors.stream.InputStream;
import com.justfors.stream.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main implements NetConnectionServer {

    public Main() {
        new Shot2().start();
    }

    private static final String RIGHT = "RIGHT";
    private static final String LEFT = "LEFT";
    private static final String UP = "UP";
    private static final String DOWN = "DOWN";
    private static final String SHOT = "SHOT";

    private static final Map<String, GameObject> gameObjects = new ConcurrentHashMap<>();

    private static Map<String, List<String>> userCoordinates = new HashMap<>();

    private static final double STEP = 10;
    private static final double BULLET_STEP = 4;

    public static void main(String[] args) {
        Main main = new Main();
        if(UPnP.openPortTCP(7777)) {
            new Server(7777, main).start();
        }
    }


    @Override
    public void serverConnectionExecute(InputStream inputStream, OutputStream outputStream, Socket socket) throws IOException {
        while (true) {
            try {
                String userMessage = inputStream.readLine();
                if (userMessage != null && !userMessage.equals("")) {
                    TransferData data = TransferData.reciveTransferData(userMessage);
                    receiveMessage(data, socket);
                }
            } catch (SocketException e) {
                System.out.println("Connection close..." + socket);
                break;
            }
        }
    }

    private void receiveMessage(TransferData data, Socket socket){
        String[] coordinates = data.getData().split(":");
        TransferData transferData = new TransferData();
        String direction = null;
        boolean shot = false;
        switch (data.getToken()) {
            case UP:
                direction = UP;
                transferData.setData(coordinates[0] + ":" + (Double.valueOf(coordinates[1]) - STEP));
                break;
            case DOWN:
                direction = DOWN;
                transferData.setData(coordinates[0] + ":" + (Double.valueOf(coordinates[1]) + STEP));
                break;
            case LEFT:
                direction = LEFT;
                transferData.setData((Double.valueOf(coordinates[0]) - STEP) + ":" + coordinates[1]);
                break;
            case RIGHT:
                direction = RIGHT;
                transferData.setData((Double.valueOf(coordinates[0]) + STEP) + ":" + coordinates[1]);
                break;
            case SHOT:
                direction = userCoordinates.get(data.getUser()).get(1);
                shot = true;
                break;
        }
        transferData.setUser(data.getUser());
        if (shot) {
            String[] bulletCoordinates = userCoordinates.get(data.getUser()).get(0).split(":");
            gameObjects.put(UUID.randomUUID().toString(), new GameObject(data.getUser(), "BULLET", Double.valueOf(bulletCoordinates[0]), Double.valueOf(bulletCoordinates[1]), direction));
        } else {
            userCoordinates.put(data.getUser(), Arrays.asList(transferData.getData() == null ? "0:0" : transferData.getData(), direction));
        }
        if (data.getToken().equals("LOGIN")){
            transferData.setToken("LOGIN");
            for (Server.ServerConnection connection : Server.connections) {
                if (connection.getSocket().equals(socket)) {
                    TransferData previousPlayersData = new TransferData();
                    userCoordinates.forEach((k,v) -> {
                        previousPlayersData.setUser(k);
                        previousPlayersData.setToken("LOGIN");
                        previousPlayersData.setData(v.get(0));
                        connection.getOut().send(previousPlayersData.build());
                    });
                }
            }
        }
        for (Server.ServerConnection connection : Server.connections) {
            connection.getOut().send(transferData.build());
        }
    }

    private class Shot2 extends Thread {

        @Override
        public void run() {
            TransferData transferData = new TransferData();
            while (true) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                gameObjects.forEach((name,gameObject) -> {
                    if (gameObject.getObjectType().equals("BULLET")) {
                        transferData.setToken(gameObject.getObjectType());
                        transferData.setUser(name);
                        long i = gameObject.getCountOfCalls();
                        String data = null;
                        switch (gameObject.getDirection()) {
                            case UP:
                                data = gameObject.getX()  + ":" + (gameObject.getY() - i * BULLET_STEP);
                                break;
                            case DOWN:
                                data = gameObject.getX()  + ":" + (gameObject.getY() + i * BULLET_STEP);
                                break;
                            case LEFT:
                                data = (gameObject.getX()  - i * BULLET_STEP) + ":" + gameObject.getY();
                                break;
                            case RIGHT:
                                data = (gameObject.getX() + i * BULLET_STEP) + ":" + gameObject.getY();
                                break;
                        }
                        gameObject.incrimentCountOfCall();

                        transferData.setData(data);
                        String[] bulletCoordinates = transferData.getData().split(":");
                        userCoordinates.forEach((k,v) -> {
                            if (!k.equals(gameObject.getOwner())) {
                                String[] playerCoordinates = v.get(0).split(":");
                                if (checkOverlay(Double.valueOf(playerCoordinates[0]), Double.valueOf(playerCoordinates[1]), Double.valueOf(bulletCoordinates[0]), Double.valueOf(bulletCoordinates[1]))) {
                                    transferData.setData("REMOVE:"+k);
                                    for (Server.ServerConnection connection : Server.connections) {
                                        connection.getOut().send(transferData.build());
                                    }
                                }
                            }
                        });
                        if (gameObject.getCountOfCalls() >= 50 || transferData.getData().contains("REMOVE:")) {
                            gameObjects.remove(name);
                            transferData.setData("REMOVE");
                            for (Server.ServerConnection connection : Server.connections) {
                                connection.getOut().send(transferData.build());
                            }
                            return;
                        }
                        for (Server.ServerConnection connection : Server.connections) {
                            connection.getOut().send(transferData.build());
                        }
                    }
                });
            }
        }
    }

    private boolean checkOverlay(Double xP, Double yP, Double xB, Double yB){
        if (xP <= xB && xP+STEP >= xB+BULLET_STEP){
            if (yP <= yB && yP+STEP >= yB+BULLET_STEP){
                return true;
            }
        }
        return false;
    }

}
