package com.dinochrome.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Preferences;
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

import java.util.Random;

public class GameScreen implements Screen {

    private final LanzadorDelJuego juego;
    private final OrthographicCamera camara;

    // Mundo
    private static final int ANCHO = 800;
    private static final int ALTO = 240;
    private static final float Y_SUELO = 40;

    // Velocidad del juego
    private static final float VELOCIDAD_BASE = 200f;
    private static final float VELOCIDAD_MAX  = 500f;
    private float velocidadJuego = 250f;

    // Velocidad de animación del dino según la velocidad del juego
    private static final float ANIM_LENTA = 0.22f;
    private static final float ANIM_RAPIDA = 0.14f;

    // Estado general
    private boolean enPausa = false;
    private boolean finDelJuego = false;

    // Tiempo (para animación y día/noche)
    private float tiempoAnimacion = 0f;
    private float tiempoDiaNoche = 0f;
    private boolean esDeNoche = false;

    // Puntaje
    private int puntaje = 0;
    private final Preferences preferencias;
    private int record;

    // Sonidos
    private final Sound sonidoSalto;
    private final Sound sonidoGolpe;

    // Texturas
    private final Texture texturaCactus;
    private final Texture texturaPtero;
    private final Texture texturaSuelo;

    // Dino: posición y física
    private final float dinoX = 80;
    private float dinoY = Y_SUELO;

    private final float anchoDinoHitbox = 30;
    private final float altoDinoParadoHitbox = 40;
    private final float altoDinoAgachadoHitbox = 25;

    private final Rectangle hitboxDino;

    private float velocidadY = 0f;
    private static final float GRAVEDAD = -900f;
    private static final float FUERZA_SALTO = 350f;
    private boolean estaEnSuelo = true;
    private boolean estaAgachado = false;

    // Dino: animación
    private final Texture texturaDinoRun1;
    private final Texture texturaDinoRun2;
    private final Texture texturaDinoAgachado;

    private final Animation<TextureRegion> animacionCorrer;
    private final TextureRegion regionAgachado;

    // Obstáculos
    private final Array<Obstaculos> obstaculos;
    private final Random random;
    private float temporizadorSpawn = 0f;

    // Fuentes
    private final BitmapFont fuentePuntaje;
    private final BitmapFont fuenteFin;

    public GameScreen(LanzadorDelJuego juego) {
        this.juego = juego;

        camara = new OrthographicCamera();
        camara.setToOrtho(false, ANCHO, ALTO);

        // Fuentes desde TTF
        FreeTypeFontGenerator generador = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));

        FreeTypeFontParameter paramPuntaje = new FreeTypeFontParameter();
        paramPuntaje.size = 20;
        paramPuntaje.color = Color.BLACK;
        paramPuntaje.characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789:!?. ";

        fuentePuntaje = generador.generateFont(paramPuntaje);

        FreeTypeFontParameter paramFin = new FreeTypeFontParameter();
        paramFin.size = 32;
        paramFin.color = Color.BLACK;
        paramFin.characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789:!?. ";

        fuenteFin = generador.generateFont(paramFin);

        generador.dispose();

        // Hitbox del dino
        hitboxDino = new Rectangle(dinoX, dinoY, anchoDinoHitbox, altoDinoParadoHitbox);

        // Sonidos
        sonidoSalto = Gdx.audio.newSound(Gdx.files.internal("jump.wav"));
        sonidoGolpe = Gdx.audio.newSound(Gdx.files.internal("hit.wav"));

        // Preferencias
        preferencias = Gdx.app.getPreferences("DinoChromePrefs");
        record = preferencias.getInteger("highScore", 0);

        // Texturas
        texturaCactus = new Texture("cactus.png");
        texturaPtero = new Texture("ptero.png");
        texturaSuelo = new Texture("ground.png");

        texturaDinoRun1 = new Texture("dino_run1.png");
        texturaDinoRun2 = new Texture("dino_run2.png");
        texturaDinoAgachado = new Texture("dino_duck.png");

        TextureRegion[] framesCorrer = new TextureRegion[] {
            new TextureRegion(texturaDinoRun1),
            new TextureRegion(texturaDinoRun2)
        };

        animacionCorrer = new Animation<>(0.30f, framesCorrer);
        regionAgachado = new TextureRegion(texturaDinoAgachado);

        // Obstáculos
        obstaculos = new Array<>();
        random = new Random();
    }

    @Override
    public void render(float delta) {
        actualizar(delta);
        dibujar();
    }

    private void dibujar() {
        if (esDeNoche) {
            Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        } else {
            Gdx.gl.glClearColor(0.53f, 0.81f, 0.92f, 1);
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camara.update();
        juego.batch.setProjectionMatrix(camara.combined);

        if (esDeNoche) {
            juego.batch.setColor(0.7f, 0.7f, 0.7f, 1f);
        } else {
            juego.batch.setColor(1f, 1f, 1f, 1f);
        }

        juego.batch.begin();

        // Suelo
        juego.batch.draw(texturaSuelo, 0, Y_SUELO - 40, ANCHO, 40);

        // Dino
        TextureRegion frameActual = estaAgachado
            ? regionAgachado
            : animacionCorrer.getKeyFrame(tiempoAnimacion, true);

        juego.batch.draw(frameActual, dinoX, dinoY);

        // Obstáculos
        for (Obstaculos obs : obstaculos) {
            if (obs.type == Obstaculos.CACTUS) {
                juego.batch.draw(texturaCactus, obs.x, obs.y, obs.width, obs.height);
            } else {
                juego.batch.draw(texturaPtero, obs.x, obs.y, obs.width, obs.height);
            }
        }

        juego.batch.setColor(1f, 1f, 1f, 1f);

        // HUD
        float hudX = camara.position.x + camara.viewportWidth / 2 - 300;
        float hudY = camara.position.y + camara.viewportHeight / 2 - 20;

        fuentePuntaje.setColor(Color.BLACK);
        fuentePuntaje.draw(juego.batch, "PUNTAJE: " + puntaje, hudX, hudY);
        fuentePuntaje.draw(juego.batch, "RECORD: " + record, hudX, hudY - 25);

        if (finDelJuego) {
            float margenIzq = 20;
            float margenArriba = 20;

            float xIzq = camara.position.x - camara.viewportWidth / 2 + margenIzq;
            float yArriba = camara.position.y + camara.viewportHeight / 2 - margenArriba;

            fuenteFin.setColor(Color.BLACK);
            fuentePuntaje.setColor(Color.BLACK);

            fuenteFin.draw(juego.batch, "FIN DEL JUEGO", xIzq, yArriba);
            fuentePuntaje.draw(juego.batch, "R - Reiniciar", xIzq, yArriba - 35);
            fuentePuntaje.draw(juego.batch, "M - Menu", xIzq, yArriba - 55);
        }

        if (enPausa) {
            juego.batch.setColor(0f, 0f, 0f, 0.5f);
            juego.batch.draw(texturaSuelo, 0, 0, ANCHO, ALTO);
            juego.batch.setColor(1f, 1f, 1f, 1f);

            fuenteFin.draw(juego.batch, "PAUSA", camara.position.x - 45, camara.position.y + 20);
            fuentePuntaje.draw(juego.batch, "ESC - Continuar", camara.position.x - 70, camara.position.y - 10);
            fuentePuntaje.draw(juego.batch, "M - Menu", camara.position.x - 55, camara.position.y - 30);
        }

        juego.batch.end();
    }

    private void actualizar(float delta) {
        // Alternar pausa
        if (!finDelJuego && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            enPausa = !enPausa;
        }

        if (enPausa) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                juego.setScreen(new PantallaMenu(juego));
            }
            return;
        }

        if (finDelJuego) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                reiniciarPartida();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                juego.setScreen(new PantallaMenu(juego));
            }
            return;
        }

        // Tiempos
        tiempoAnimacion += delta;
        tiempoDiaNoche += delta;

        if (tiempoDiaNoche > 15f) {
            tiempoDiaNoche = 0f;
            esDeNoche = !esDeNoche;
        }

        // Agacharse
        estaAgachado = Gdx.input.isKeyPressed(Input.Keys.DOWN) && estaEnSuelo;

        // Saltar
        boolean presionoSalto = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP);

        if (presionoSalto && estaEnSuelo) {
            velocidadY = FUERZA_SALTO;
            estaEnSuelo = false;
            sonidoSalto.play();
        }

        // Física
        velocidadY += GRAVEDAD * delta;
        dinoY += velocidadY * delta;

        if (dinoY <= Y_SUELO) {
            dinoY = Y_SUELO;
            velocidadY = 0f;
            estaEnSuelo = true;
        }

        // Spawn de obstáculos
        temporizadorSpawn += delta;
        if (temporizadorSpawn > 1.2f + random.nextFloat()) {
            temporizadorSpawn = 0f;

            if (random.nextBoolean()) {
                obstaculos.add(new Obstaculos(
                    ANCHO,
                    Y_SUELO,
                    20 + random.nextInt(10),
                    30 + random.nextInt(10),
                    Obstaculos.CACTUS
                ));
            } else {
                float yPtero = random.nextBoolean() ? Y_SUELO + 15 : Y_SUELO + 30;
                obstaculos.add(new Obstaculos(
                    ANCHO,
                    yPtero,
                    40,
                    20,
                    Obstaculos.PTERO
                ));
            }
        }

        // Hitbox del dino
        float altoActual = estaAgachado ? altoDinoAgachadoHitbox : altoDinoParadoHitbox;
        hitboxDino.set(dinoX, dinoY, anchoDinoHitbox, altoActual);

        // Mover obstáculos, sumar puntaje, colisiones
        for (int i = obstaculos.size - 1; i >= 0; i--) {
            Obstaculos obs = obstaculos.get(i);
            obs.update(delta, velocidadJuego);

            if (obs.x + obs.width < 0) {
                obstaculos.removeIndex(i);
                puntaje++;
                continue;
            }

            if (obs.getBounds().overlaps(hitboxDino)) {
                sonidoGolpe.play();
                finDelJuego = true;

                if (puntaje > record) {
                    record = puntaje;
                    preferencias.putInteger("highScore", record);
                    preferencias.flush();
                }
            }
        }

        // Aumentar velocidad y ajustar animación
        velocidadJuego += delta * 2;

        float ratioVelocidad = Math.min(
            1f,
            (velocidadJuego - VELOCIDAD_BASE) / (VELOCIDAD_MAX - VELOCIDAD_BASE)
        );

        float duracionFrame = ANIM_LENTA - ratioVelocidad * (ANIM_LENTA - ANIM_RAPIDA);
        animacionCorrer.setFrameDuration(duracionFrame);
    }

    private void reiniciarPartida() {
        obstaculos.clear();
        dinoY = Y_SUELO;
        velocidadY = 0f;
        puntaje = 0;
        velocidadJuego = 250f;
        finDelJuego = false;
        tiempoAnimacion = 0f;
        tiempoDiaNoche = 0f;
        esDeNoche = false;
        enPausa = false;
    }

    @Override public void resize(int width, int height) {}
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        texturaDinoRun1.dispose();
        texturaDinoRun2.dispose();
        texturaDinoAgachado.dispose();
        texturaCactus.dispose();
        texturaPtero.dispose();
        texturaSuelo.dispose();
        sonidoSalto.dispose();
        sonidoGolpe.dispose();
        fuentePuntaje.dispose();
        fuenteFin.dispose();
    }
}
