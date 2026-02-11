// =====================================================
// ARCHIVO: GameClient.java
// PAQUETE: com.dinochrome.game.net
// =====================================================
package com.dinochrome.game.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class GameClient {

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
    public interface ObstaculoCallback { void onRecibir(EstadoObstaculo o); }
    public interface StartCallback { void onStart(); }

    public ObstaculoCallback onObstacleReceived;
    public StartCallback onStartGame;

    public GameClient() {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(300);

            // 1) Descubrir servidor por broadcast
            ipServidor = descubrirServidor();
            if (ipServidor == null) {
                throw new RuntimeException("No se encontró servidor (broadcast)");
            }

            // 2) Thread de recepción
            Thread t = new Thread(this::loopRecepcion, "Cliente-UDP");
            t.setDaemon(true);
            t.start();

            // 3) Pedir entrar con reintentos (UDP)
            Thread joinRetry = new Thread(() -> {
                while (myId == 0) {
                    enviarTexto("JOIN");
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }
            }, "Join-Retry");
            joinRetry.setDaemon(true);
            joinRetry.start();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------
    // Descubrimiento por broadcast
    // -------------------------
    private InetAddress descubrirServidor() {
        try {
            InetAddress broadcast = InetAddress.getByName("255.255.255.255");

            byte[] data = "BUSCAR_SERVIDOR".getBytes(StandardCharsets.UTF_8);
            DatagramPacket p = new DatagramPacket(data, data.length, broadcast, PUERTO);

            // Reintentos
            for (int i = 0; i < 8; i++) {
                socket.send(p);

                DatagramPacket resp = new DatagramPacket(new byte[256], 256);
                try {
                    socket.receive(resp);
                    String msg = new String(
                        resp.getData(),
                        0,
                        resp.getLength(),
                        StandardCharsets.UTF_8
                    ).trim();

                    if (msg.equals("SERVIDOR_AQUI")) {
                        return resp.getAddress();
                    }

                } catch (SocketTimeoutException timeout) {
                    // sigue intentando
                }
            }

        } catch (Exception ignored) {}

        return null;
    }

    // -------------------------
    // API
    // -------------------------
    public void sendReady() {
        ready = true;
        enviarTexto("READY");
    }

    public void send(PlayerState estado) {
        String msg = "STATE;id=" + estado.playerId
            + ";x=" + estado.x
            + ";y=" + estado.y
            + ";duck=" + (estado.ducking ? 1 : 0);
        enviarTexto(msg);
    }

    public void cerrar() {
        if (socket != null && !socket.isClosed()) socket.close();
    }

    // -------------------------
    // Recepción
    // -------------------------
    private void loopRecepcion() {
        byte[] buffer = new byte[2048];

        while (true) {
            try {
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                socket.receive(p);

                String msg = new String(
                    p.getData(),
                    0,
                    p.getLength(),
                    StandardCharsets.UTF_8
                ).trim();

                procesar(msg);

            } catch (Exception e) {
                // ignoramos timeouts y demás
            }
        }
    }

    private void procesar(String msg) {

        if (msg.startsWith("ASSIGN;")) {
            Integer id = leerEntero(msg, "id");
            if (id != null) myId = id;
            return;
        }

        if (msg.startsWith("COUNT;")) {
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
            if (eo != null && onObstacleReceived != null) {
                onObstacleReceived.onRecibir(eo);
            }
        }
    }

    // -------------------------
    // Parseos
    // -------------------------
    private PlayerState parsearPlayerState(String msg) {
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

    // -------------------------
    // Helpers
    // -------------------------
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
            if (ipServidor == null) return;
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket p = new DatagramPacket(data, data.length, ipServidor, PUERTO);
            socket.send(p);
        } catch (Exception ignored) {}
    }
}
