package com.dinochrome.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector3;


public class PantallaMenu implements Screen {

    private final LanzadorDelJuego game;
    private OrthographicCamera camera;

    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private ShapeRenderer shape;

    private Rectangle onlineButton;
    private Rectangle playButton;
    private Rectangle controlsButton;
    private Rectangle exitButton;
    private float playHover = 0f;
    private float onlineHover = 0f;
    private float controlsHover = 0f;
    private float exitHover = 0f;
    private static final float HOVER_SPEED = 6f;

    private static final int BUTTON_START_Y = 230;
    private static final int BUTTON_GAP = 70;
    private float fadeAlpha = 0f;     // 0 = negro, 1 = visible
    private boolean fadingIn = true;
    private boolean fadingOut = false;
    private Screen nextScreen;
 // 🌥️ Nubes
    private float cloud1X = 100;
    private float cloud2X = 400;
    private float cloud3X = 650;

    private float cloudSpeed = 15f;


    public PantallaMenu(LanzadorDelJuego game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);
        titleFont.setColor(Color.BLACK);

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(1.5f);
        buttonFont.setColor(Color.BLACK);

        shape = new ShapeRenderer();

        playButton = new Rectangle(300, BUTTON_START_Y, 200, 50);
        onlineButton = new Rectangle(300, BUTTON_START_Y - BUTTON_GAP, 200, 50);
        controlsButton = new Rectangle(300, BUTTON_START_Y - BUTTON_GAP * 2, 200, 50);
        exitButton = new Rectangle(300, BUTTON_START_Y - BUTTON_GAP * 3, 200, 50);

    }

    private boolean isHover(Rectangle rect) {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        return rect.contains(mouse.x, mouse.y);
    }

    private float updateHover(float current, boolean hovering, float delta) {
        if (hovering) {
            current += delta * HOVER_SPEED;
        } else {
            current -= delta * HOVER_SPEED;
        }
        return Math.max(0f, Math.min(1f, current));
    }


    @Override
    public void render(float delta) {

    	Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
    	Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        // =========================
        // ACTUALIZAR NUBES
        // =========================
        cloud1X -= cloudSpeed * delta;
     	cloud2X -= cloudSpeed * 0.8f * delta;
     	cloud3X -= cloudSpeed * 1.2f * delta;

     	if (cloud1X < -120) cloud1X = camera.viewportWidth + 50;
     	if (cloud2X < -120) cloud2X = camera.viewportWidth + 80;
     	if (cloud3X < -120) cloud3X = camera.viewportWidth + 110;


        playHover = updateHover(playHover, isHover(playButton), delta);
        onlineHover = updateHover(onlineHover, isHover(onlineButton), delta);
        controlsHover = updateHover(controlsHover, isHover(controlsButton), delta);
        exitHover = updateHover(exitHover, isHover(exitButton), delta);


     // Fade in
        if (fadingIn) {
            fadeAlpha += delta * 1.5f; // velocidad del fade
            if (fadeAlpha >= 1f) {
                fadeAlpha = 1f;
                fadingIn = false;
            }
        }

        // Fade out
        if (fadingOut) {
            fadeAlpha -= delta * 1.5f;
            if (fadeAlpha <= 0f) {
                fadeAlpha = 0f;
                game.setScreen(nextScreen);

            }
        }

        float vw = camera.viewportWidth;
        float vh = camera.viewportHeight;
        float groundHeight = vh * 0.30f;

        // =========================
        // SHAPE RENDERER
        // =========================
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Capa superior
        shape.setColor(0.40f, 0.72f, 0.90f, 1f);
        shape.rect(0, vh - vh * 0.33f, vw, vh * 0.33f);

        // Capa media
        shape.setColor(0.52f, 0.82f, 0.94f, 1f);
        shape.rect(0, groundHeight + vh * 0.18f, vw, vh * 0.25f);

        // Capa inferior del cielo
        shape.setColor(0.68f, 0.90f, 0.97f, 1f);
        shape.rect(0, groundHeight, vw, vh * 0.18f);

        // =========================
        // 🌥️ NUBES
	        // =========================
	    shape.setColor(1f, 1f, 1f, 0.85f);

	    // Nube 1
	    shape.circle(cloud1X, vh * 0.75f, 18);
	    shape.circle(cloud1X + 18, vh * 0.75f + 6, 22);
	    shape.circle(cloud1X + 40, vh * 0.75f, 18);

	    // Nube 2
	    shape.circle(cloud2X, vh * 0.65f, 16);
	    shape.circle(cloud2X + 16, vh * 0.65f + 5, 20);
     	shape.circle(cloud2X + 36, vh * 0.65f, 16);

     	// Nube 3
     	shape.circle(cloud3X, vh * 0.80f, 14);
     	shape.circle(cloud3X + 14, vh * 0.80f + 4, 18);
     	shape.circle(cloud3X + 30, vh * 0.80f, 14);


        // 🟤 SUELO (arena)
        shape.setColor(0.58f, 0.46f, 0.30f, 1f);
        shape.rect(0, 0, vw, groundHeight);

        drawButton(playButton, playHover, Color.WHITE);
        drawButton(onlineButton, onlineHover, Color.WHITE);
        drawButton(controlsButton, controlsHover, Color.WHITE);
        drawButton(exitButton, exitHover, new Color(1f, 0.9f, 0.9f, 1f));



        shape.end();

        // =========================
        // TEXTO
        // =========================
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        GlyphLayout titleLayout = new GlyphLayout(titleFont, "DINOSAURIO SAURIO");
        float titleX = (vw - titleLayout.width) / 2;
        titleFont.draw(game.batch, titleLayout, titleX, vh - 80);

        buttonFont.setColor(Color.BLACK);
        buttonFont.draw(game.batch, "JUGAR", playButton.x + 60, playButton.y + 35);

        buttonFont.setColor(Color.BLACK);
        buttonFont.draw(game.batch, "ONLINE", onlineButton.x + 50, onlineButton.y + 35);

        buttonFont.setColor(Color.BLACK);
        buttonFont.draw(game.batch, "CONTROLES", controlsButton.x + 30, controlsButton.y + 35);

        buttonFont.draw(game.batch, "SALIR", exitButton.x + 65, exitButton.y + 35);

        game.batch.end();

        // =========================
        // INPUT
        // =========================
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);

            float x = touchPos.x;
            float y = touchPos.y;

            if (playButton.contains(x, y)) {
                nextScreen = new GameScreen(game);
                fadingOut = true;
            }

            if (controlsButton.contains(x, y)) {
                nextScreen = new PantallaControles(game);
                fadingOut = true;
            }
            if (exitButton.contains(x, y)) {
                Gdx.app.exit();
            }
            if (onlineButton.contains(x, y)) {
                nextScreen = new PantallaMultijugador(game);
                fadingOut = true;
            }
        }

     // =========================
     // FADE (SIEMPRE AL FINAL)
     // =========================
     Gdx.gl.glEnable(GL20.GL_BLEND);
     Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

     shape.setProjectionMatrix(camera.combined);
     shape.begin(ShapeRenderer.ShapeType.Filled);
     shape.setColor(0f, 0f, 0f, 1f - fadeAlpha);
     shape.rect(0, 0, camera.viewportWidth, camera.viewportHeight);
     shape.end();

     Gdx.gl.glDisable(GL20.GL_BLEND);

    }

    @Override
    public void show() {
        // Se llama cuando esta pantalla pasa a ser la activa
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    private void drawButton(Rectangle r, float hover, Color color) {
        float scale = 1f + hover * 0.05f;

        float w = r.width * scale;
        float h = r.height * scale;

        float x = r.x - (w - r.width) / 2;
        float y = r.y - (h - r.height) / 2;

        // sombra diagonal suave
        shape.setColor(0f, 0f, 0f, 0.06f);
        shape.rect(x - 6 * hover, y - 6 * hover, w, h);

        // botón
        shape.setColor(color);
        shape.rect(x, y, w, h);
    }


    @Override
    public void dispose() {
        titleFont.dispose();
        buttonFont.dispose();
        shape.dispose();
    }
}
