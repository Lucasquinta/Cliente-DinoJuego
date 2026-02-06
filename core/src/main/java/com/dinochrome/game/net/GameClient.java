package com.dinochrome.game.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class GameClient {

    // Servidor
    private static final String HOST = "127.0.0.1"; // cambiá por IP real si hace falta
    private static final int PUERTO = 4321;

    private DatagramSocket socket;
    private InetAddress ipServidor;

    // Estado público que usa la pantalla
    public volatile int myId = 0;
    public volatile int playerCount = 0;
    public volatile boolean ready = false;
    public volatile boolean startGame = false;

    public volatile PlayerState otherPlayer = null;

    // Callbacks
    public interface ObstaculoCallback {
        void onRecibir(EstadoObstaculo o);
    }
    public interface StartCallback {
        void onStart();
    }

    public ObstaculoCallback onObstacleReceived;
    public StartCallback onStartGame;

    public GameClient() {
        try {
            ipServidor = InetAddress.getByName(HOST);
            socket = new DatagramSocket();
            socket.setSoTimeout(50);

            // Pedir entrar
            enviarTexto("JOIN");

            // Thread de recepción
            Thread t = new Thread(this::loopRecepcion, "Cliente-UDP");
            t.setDaemon(true);
            t.start();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendReady() {
        ready = true;
        enviarTexto("READY");
    }

    public void send(PlayerState estado) {
        // STATE;id=1;x=80.0;y=40.0;duck=0
        String msg = "STATE;id=" + estado.playerId
            + ";x=" + estado.x
            + ";y=" + estado.y
            + ";duck=" + (estado.ducking ? 1 : 0);
        enviarTexto(msg);
    }

    private void loopRecepcion() {
        byte[] buffer = new byte[2048];

        while (true) {
            try {
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                socket.receive(p);

                String msg = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8).trim();
                procesar(msg);

            } catch (Exception e) {
                // timeout y otros: ignoramos para mantener loop simple
            }
        }
    }

    private void procesar(String msg) {
        if (msg.startsWith("ASSIGN;")) {
            // ASSIGN;id=1
            Integer id = leerEntero(msg, "id");
            if (id != null) myId = id;
            return;
        }

        if (msg.startsWith("COUNT;")) {
            // COUNT;players=2
            Integer c = leerEntero(msg, "players");
            if (c != null) playerCount = c;
            return;
        }

        if (msg.equals("START")) {
            startGame = true;
            if (onStartGame != null) onStartGame.onStart();
            return;
        }

        if (msg.startsWith("STATE;")) {
            PlayerState ps = parsearPlayerState(msg);
            if (ps != null) otherPlayer = ps;
            return;
        }

        if (msg.startsWith("OBST;")) {
            EstadoObstaculo eo = parsearObstaculo(msg);
            if (eo != null && onObstacleReceived != null) onObstacleReceived.onRecibir(eo);
            return;
        }

        // FULL / ERROR / READY;... etc: opcional
    }

    private PlayerState parsearPlayerState(String msg) {
        // STATE;id=2;x=140.0;y=40.0;duck=1
        try {
            PlayerState ps = new PlayerState();

            Integer id = leerEntero(msg, "id");
            Float x = leerFloat(msg, "x");
            Float y = leerFloat(msg, "y");
            Integer duck = leerEntero(msg, "duck");

            if (id == null || x == null || y == null || duck == null) return null;

            ps.playerId = id;
            ps.x = x;
            ps.y = y;
            ps.ducking = (duck == 1);
            return ps;

        } catch (Exception e) {
            return null;
        }
    }

    private EstadoObstaculo parsearObstaculo(String msg) {
        // OBST;x=800;y=40;w=24;h=38;t=0
        try {
            EstadoObstaculo o = new EstadoObstaculo();

            Float x = leerFloat(msg, "x");
            Float y = leerFloat(msg, "y");
            Float w = leerFloat(msg, "w");
            Float h = leerFloat(msg, "h");
            Integer t = leerEntero(msg, "t");

            if (x == null || y == null || w == null || h == null || t == null) return null;

            o.x = x;
            o.y = y;
            o.width = w;
            o.height = h;
            o.type = t;

            return o;

        } catch (Exception e) {
            return null;
        }
    }

    private Integer leerEntero(String msg, String clave) {
        String v = leerValor(msg, clave);
        if (v == null) return null;
        return Integer.parseInt(v);
    }

    private Float leerFloat(String msg, String clave) {
        String v = leerValor(msg, clave);
        if (v == null) return null;
        return Float.parseFloat(v);
    }

    private String leerValor(String msg, String clave) {
        // Busca "clave=" y lee hasta ';' o fin
        String patron = clave + "=";
        int i = msg.indexOf(patron);
        if (i == -1) return null;
        i += patron.length();

        int fin = msg.indexOf(';', i);
        if (fin == -1) fin = msg.length();

        return msg.substring(i, fin);
    }

    private void enviarTexto(String msg) {
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket p = new DatagramPacket(data, data.length, ipServidor, PUERTO);
            socket.send(p);
        } catch (Exception e) {
            // si falla, no explota la pantalla; solo no hay red
        }
    }

    // Si querés cerrar prolijo
    public void cerrar() {
        if (socket != null && !socket.isClosed()) socket.close();
    }
}
