package com.dinochrome.game;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import com.dinochrome.game.net.GameClient;
import com.dinochrome.game.net.PlayerState;

public class PantallaMultijugador implements Screen {

    // =========================
    // CONSTANTES DEL MUNDO
    // =========================
    private static final int ANCHO = 800;
    private static final int ALTO = 240;
    private static final float Y_SUELO = 40f;

    // Física
    private static final float GRAVEDAD = -900f;
    private static final float FUERZA_SALTO = 350f;

    // Velocidad de juego y animación
    private static final float VELOCIDAD_BASE = 200f;
    private static final float VELOCIDAD_MAX = 500f;
    private static final float ANIM_LENTA = 0.22f;
    private static final float ANIM_RAPIDA = 0.14f;

    // Día/noche (visual)
    private static final float TIEMPO_CAMBIO_DIA_NOCHE = 15f;

    // =========================
    // REFERENCIAS PRINCIPALES
    // =========================
    private final LanzadorDelJuego juego;
    private final OrthographicCamera camara;

    // =========================
    // RED
    // =========================
    private final GameClient cliente;

    private boolean estoyEnLobby = true;

    // Importante: mi id (1 o 2). Si no está asignado todavía, vale 0.
    private int miId = 0;

    // =========================
    // RECURSOS (TEXTURAS / SONIDOS / FUENTES)
    // =========================
    private final Texture texturaSuelo;
    private final Texture texturaCactus;
    private final Texture texturaPtero;

    private final Texture texturaDinoRun1;
    private final Texture texturaDinoRun2;
    private final Texture texturaDinoAgachado;

    private final Animation<TextureRegion> animacionCorrer;
    private final TextureRegion regionAgachado;

    private final Sound sonidoSalto;
    private final Sound sonidoGolpe;

    private final BitmapFont fuenteHud;
    private final BitmapFont fuenteTitulo;

    // =========================
    // ESTADO VISUAL
    // =========================
    private float tiempoAnimacion = 0f;
    private float tiempoDiaNoche = 0f;
    private boolean esDeNoche = false;

    // =========================
    // PUNTAJE / RECORD
    // =========================
    private int puntaje = 0;
    private final Preferences preferencias;
    private int record;

    // =========================
    // JUEGO: FIN / GANADOR
    // =========================
    private boolean finDelJuego = false;
    private int ganador = 0; // 0 ninguno, 1 jugador 1, 2 jugador 2

    // =========================
    // JUGADORES: POSICIÓN Y ESTADO
    // =========================
    private float xJ1 = 80f;
    private float yJ1 = Y_SUELO;
    private boolean agachadoJ1 = false;
    private float velYJ1 = 0f;
    private boolean enSueloJ1 = true;

    private float xJ2 = 140f;
    private float yJ2 = Y_SUELO;
    private boolean agachadoJ2 = false;
    private float velYJ2 = 0f;
    private boolean enSueloJ2 = true;

    private static final float ANCHO_HITBOX = 30f;
    private static final float ALTO_PARADO = 40f;
    private static final float ALTO_AGACHADO = 25f;

    private final Rectangle hitboxJ1 = new Rectangle();
    private final Rectangle hitboxJ2 = new Rectangle();

    // =========================
    // OBSTÁCULOS
    // =========================
    private final Array<Obstaculos> obstaculos = new Array<>();
    private final Random random = new Random(); // por si querés cosas visuales, no para lógica de red
    private float velocidadJuego = 250f;

    public PantallaMultijugador(LanzadorDelJuego juego) {
        this.juego = juego;

        camara = new OrthographicCamera();
        camara.setToOrtho(false, ANCHO, ALTO);

        // Red
        cliente = new GameClient();
        configurarCallbacksDeRed();

        // Preferencias
        preferencias = Gdx.app.getPreferences("DinoChromePrefs");
        record = preferencias.getInteger("highScore", 0);

        // Recursos
        texturaSuelo = new Texture("ground.png");
        texturaCactus = new Texture("cactus.png");
        texturaPtero = new Texture("ptero.png");

        texturaDinoRun1 = new Texture("dino_run1.png");
        texturaDinoRun2 = new Texture("dino_run2.png");
        texturaDinoAgachado = new Texture("dino_duck.png");

        TextureRegion[] frames = new TextureRegion[] {
            new TextureRegion(texturaDinoRun1),
            new TextureRegion(texturaDinoRun2)
        };
        animacionCorrer = new Animation<>(0.30f, frames);
        regionAgachado = new TextureRegion(texturaDinoAgachado);

        sonidoSalto = Gdx.audio.newSound(Gdx.files.internal("jump.wav"));
        sonidoGolpe = Gdx.audio.newSound(Gdx.files.internal("hit.wav"));

        // Fuentes
        FreeTypeFontGenerator generador = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));

        FreeTypeFontParameter paramHud = new FreeTypeFontParameter();
        paramHud.size = 14;
        paramHud.color = Color.BLACK;
        paramHud.characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789:!?. ";
        fuenteHud = generador.generateFont(paramHud);

        FreeTypeFontParameter paramTitulo = new FreeTypeFontParameter();
        paramTitulo.size = 28;
        paramTitulo.color = Color.BLACK;
        paramTitulo.characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789:!?. ";
        fuenteTitulo = generador.generateFont(paramTitulo);

        generador.dispose();

        // Inicializar hitboxes
        actualizarHitboxes();
    }

    private void configurarCallbacksDeRed() {
        // Recibir obstáculos desde red: siempre en el hilo de render
        cliente.onObstacleReceived = (os) -> {
            Gdx.app.postRunnable(() -> {
                int tipo = (os.type == 0) ? Obstaculos.CACTUS : Obstaculos.PTERO;

                Obstaculos nuevo = new Obstaculos(os.x, os.y, os.width, os.height, tipo);
                obstaculos.add(nuevo);
            });
        };

        // Arranque de partida
        cliente.onStartGame = () -> {
            Gdx.app.postRunnable(() -> iniciarPartidaDesdeLobby());
        };
    }

    private void iniciarPartidaDesdeLobby() {
        // ¿Está bien arrancar aunque no tenga id? Yo diría que no, pero si tu red ya lo garantiza, ok.
        estoyEnLobby = false;
        finDelJuego = false;
        ganador = 0;
        puntaje = 0;

        // Reset posiciones por si venís de una partida anterior
        xJ1 = 80f;  yJ1 = Y_SUELO;  agachadoJ1 = false; velYJ1 = 0f; enSueloJ1 = true;
        xJ2 = 140f; yJ2 = Y_SUELO; agachadoJ2 = false; velYJ2 = 0f; enSueloJ2 = true;

        velocidadJuego = 250f;
        tiempoAnimacion = 0f;
        tiempoDiaNoche = 0f;
        esDeNoche = false;

        // Obstáculos limpitos
        for (Obstaculos o : obstaculos) {
            o.dispose();
        }
        obstaculos.clear();

        actualizarHitboxes();
    }

    @Override
    public void render(float delta) {
        // Actualizar id por si el cliente lo asigna después
        miId = cliente.myId;

        actualizar(delta);
        dibujar();
    }

    private void actualizar(float delta) {
        // =========================
        // LOBBY
        // =========================
        if (estoyEnLobby) {
            // Si el cliente marca que ya arranca (por flag)
            if (cliente.startGame) {
                cliente.startGame = false;
                iniciarPartidaDesdeLobby();
                return;
            }

            // Marcar ready
            if (!cliente.ready && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                cliente.sendReady();
            }

            return;
        }

        // =========================
        // FIN DEL JUEGO
        // =========================
        if (finDelJuego) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                juego.setScreen(new PantallaMenu(juego));
            }
            return;
        }

        // =========================
        // TIEMPOS
        // =========================
        tiempoAnimacion += delta;
        tiempoDiaNoche += delta;

        if (tiempoDiaNoche >= TIEMPO_CAMBIO_DIA_NOCHE) {
            tiempoDiaNoche = 0f;
            esDeNoche = !esDeNoche;
        }

        // =========================
        // ENTRADA Y FÍSICA SOLO DEL LOCAL
        // =========================
        if (miId == 1) {
            leerEntradaYFisicaLocal(delta, true);
        } else if (miId == 2) {
            leerEntradaYFisicaLocal(delta, false);
        } else {
            // Si miId todavía no está listo, ¿qué hacemos?
            // Yo prefiero no simular nada todavía para evitar estados raros.
            return;
        }

        // =========================
        // ENVIAR MI ESTADO
        // =========================
        enviarMiEstado();

        // =========================
        // APLICAR ESTADO REMOTO (SIN FÍSICA LOCAL)
        // =========================
        aplicarEstadoRemoto();

        // =========================
        // OBSTÁCULOS + COLISIONES
        // =========================
        actualizarHitboxes();

        for (int i = obstaculos.size - 1; i >= 0; i--) {
            Obstaculos o = obstaculos.get(i);

            // Movimiento local (asumiendo que ambos simulan igual)
            o.update(delta, velocidadJuego);

            if (o.x + o.width < 0) {
                obstaculos.removeIndex(i);
                puntaje++;
                continue;
            }

            if (o.getBounds().overlaps(hitboxJ1)) {
                terminarPartidaConGanador(2);
                break;
            }

            if (o.getBounds().overlaps(hitboxJ2)) {
                terminarPartidaConGanador(1);
                break;
            }
        }

        // =========================
        // VELOCIDAD Y ANIMACIÓN
        // =========================
        velocidadJuego += delta * 2f;

        float ratio = Math.min(1f, (velocidadJuego - VELOCIDAD_BASE) / (VELOCIDAD_MAX - VELOCIDAD_BASE));
        float duracionFrame = ANIM_LENTA - ratio * (ANIM_LENTA - ANIM_RAPIDA);
        animacionCorrer.setFrameDuration(duracionFrame);
    }

    private void leerEntradaYFisicaLocal(float delta, boolean soyJugador1) {
        // Entrada
        boolean teclaAgachar = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
        boolean teclaSaltar = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP)
            || Gdx.input.isKeyJustPressed(Input.Keys.W);

        if (soyJugador1) {
            agachadoJ1 = teclaAgachar && enSueloJ1;

            if (teclaSaltar && enSueloJ1) {
                velYJ1 = FUERZA_SALTO;
                enSueloJ1 = false;
                sonidoSalto.play();
            }

            // Física
            velYJ1 += GRAVEDAD * delta;
            yJ1 += velYJ1 * delta;

            if (yJ1 <= Y_SUELO) {
                yJ1 = Y_SUELO;
                velYJ1 = 0f;
                enSueloJ1 = true;
            }

        } else {
            agachadoJ2 = teclaAgachar && enSueloJ2;

            if (teclaSaltar && enSueloJ2) {
                velYJ2 = FUERZA_SALTO;
                enSueloJ2 = false;
                sonidoSalto.play();
            }

            // Física
            velYJ2 += GRAVEDAD * delta;
            yJ2 += velYJ2 * delta;

            if (yJ2 <= Y_SUELO) {
                yJ2 = Y_SUELO;
                velYJ2 = 0f;
                enSueloJ2 = true;
            }
        }
    }

    private void enviarMiEstado() {
        PlayerState estado = new PlayerState();
        estado.playerId = miId;

        if (miId == 1) {
            estado.x = xJ1;
            estado.y = yJ1;
            estado.ducking = agachadoJ1;
        } else {
            estado.x = xJ2;
            estado.y = yJ2;
            estado.ducking = agachadoJ2;
        }

        cliente.send(estado);
    }

    private void aplicarEstadoRemoto() {
        PlayerState otro = cliente.otherPlayer;
        if (otro == null) return;
        if (otro.playerId == miId) return;

        // Si yo soy 1, el otro es 2; si yo soy 2, el otro es 1
        if (miId == 1 && otro.playerId == 2) {
            xJ2 = otro.x;
            yJ2 = otro.y;
            agachadoJ2 = otro.ducking;

            // Nota: enSuelo/velY del remoto no se simulan. Se dejan como estaban.

        } else if (miId == 2 && otro.playerId == 1) {
            xJ1 = otro.x;
            yJ1 = otro.y;
            agachadoJ1 = otro.ducking;
        }
    }

    private void actualizarHitboxes() {
        float alto1 = agachadoJ1 ? ALTO_AGACHADO : ALTO_PARADO;
        hitboxJ1.set(xJ1, yJ1, ANCHO_HITBOX, alto1);

        float alto2 = agachadoJ2 ? ALTO_AGACHADO : ALTO_PARADO;
        hitboxJ2.set(xJ2, yJ2, ANCHO_HITBOX, alto2);
    }

    private void terminarPartidaConGanador(int ganador) {
        this.finDelJuego = true;
        this.ganador = ganador;
        sonidoGolpe.play();

        // Guardar record local
        if (puntaje > record) {
            record = puntaje;
            preferencias.putInteger("highScore", record);
            preferencias.flush();
        }
    }

    private void dibujar() {
        // Fondo
        if (esDeNoche) {
            Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        } else {
            Gdx.gl.glClearColor(0.53f, 0.81f, 0.92f, 1f);
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camara.update();
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();

        if (estoyEnLobby) {
            dibujarLobby();
            juego.batch.end();
            return;
        }

        dibujarJuego();

        juego.batch.end();
    }

    private void dibujarLobby() {
        fuenteTitulo.setColor(Color.BLACK);
        fuenteHud.setColor(Color.BLACK);

        fuenteTitulo.draw(juego.batch, "LOBBY", camara.position.x - 60, camara.position.y + 40);
        fuenteHud.draw(juego.batch, "Jugadores conectados: " + cliente.playerCount, camara.position.x - 120, camara.position.y);

        if (!cliente.ready) {
            fuenteHud.draw(juego.batch, "Presiona ENTER para listo", camara.position.x - 130, camara.position.y - 30);
        } else {
            fuenteHud.draw(juego.batch, "Esperando al otro jugador...", camara.position.x - 140, camara.position.y - 30);
        }
    }

    private void dibujarJuego() {
        // Suelo
        juego.batch.draw(texturaSuelo, 0, Y_SUELO - 40, ANCHO, 40);

        // Dino 1
        TextureRegion frameJ1 = agachadoJ1
            ? regionAgachado
            : animacionCorrer.getKeyFrame(tiempoAnimacion, true);

        juego.batch.setColor(1f, 1f, 1f, 1f);
        juego.batch.draw(frameJ1, xJ1, yJ1);

        // Dino 2 (tinte leve para distinguir)
        TextureRegion frameJ2 = agachadoJ2
            ? regionAgachado
            : animacionCorrer.getKeyFrame(tiempoAnimacion, true);

        juego.batch.setColor(0.85f, 1f, 0.85f, 1f);
        juego.batch.draw(frameJ2, xJ2, yJ2);

        juego.batch.setColor(1f, 1f, 1f, 1f);

        // Indicadores
        fuenteHud.setColor(Color.BLACK);
        fuenteHud.draw(juego.batch, "P1", xJ1 + 5, yJ1 + ALTO_PARADO + 15);
        fuenteHud.draw(juego.batch, "P2", xJ2 + 5, yJ2 + ALTO_PARADO + 15);

        // Obstáculos
        for (Obstaculos o : obstaculos) {
            // Si tu clase Obstaculos tiene render, mejor:
            // o.render(juego.batch);
            // Si no, lo dibujamos según type:
            if (o.type == Obstaculos.CACTUS) {
                juego.batch.draw(texturaCactus, o.x, o.y, o.width, o.height);
            } else {
                juego.batch.draw(texturaPtero, o.x, o.y, o.width, o.height);
            }
        }

        // HUD
        float hudX = camara.position.x + camara.viewportWidth / 2 - 300;
        float hudY = camara.position.y + camara.viewportHeight / 2 - 20;

        fuenteHud.draw(juego.batch, "PUNTAJE: " + puntaje, hudX, hudY);
        fuenteHud.draw(juego.batch, "RECORD: " + record, hudX, hudY - 25);

        if (finDelJuego) {
            String texto = "GANA JUGADOR " + ganador;
            float x = camara.position.x - 200;
            float y = camara.position.y + 100;

            fuenteTitulo.draw(juego.batch, texto, x, y);
            fuenteHud.draw(juego.batch, "Presiona M para volver al menu", x - 60, y - 40);
        }
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        // Obstáculos
        for (Obstaculos o : obstaculos) {
            o.dispose();
        }
        obstaculos.clear();

        // Texturas
        texturaSuelo.dispose();
        texturaCactus.dispose();
        texturaPtero.dispose();

        texturaDinoRun1.dispose();
        texturaDinoRun2.dispose();
        texturaDinoAgachado.dispose();

        // Sonidos
        sonidoSalto.dispose();
        sonidoGolpe.dispose();

        // Fuentes
        fuenteHud.dispose();
        fuenteTitulo.dispose();

        // Si tu GameClient tiene cierre, sería ideal:
        // cliente.cerrar();
    }
}
