package com.dinochrome.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class PantallaControles implements Screen {

    private static final int ANCHO_VIRTUAL = 800;
    private static final int ALTO_VIRTUAL = 480;

    private final LanzadorDelJuego juego;
    private final OrthographicCamera camara;

    private final BitmapFont fuenteTitulo;
    private final BitmapFont fuenteTexto;

    public PantallaControles(LanzadorDelJuego juego) {
        this.juego = juego;

        camara = new OrthographicCamera();
        camara.setToOrtho(false, ANCHO_VIRTUAL, ALTO_VIRTUAL);

        fuenteTitulo = new BitmapFont();
        fuenteTitulo.getData().setScale(2.2f);
        fuenteTitulo.setColor(Color.BLACK);

        fuenteTexto = new BitmapFont();
        fuenteTexto.getData().setScale(1.2f);
        fuenteTexto.setColor(Color.BLACK);
    }

    @Override
    public void render(float delta) {
        limpiarPantalla();

        camara.update();
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();
        dibujarTextos();
        juego.batch.end();

        leerTeclasParaVolver();
    }

    private void limpiarPantalla() {
        Gdx.gl.glClearColor(0.53f, 0.81f, 0.92f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    private void dibujarTextos() {
        fuenteTitulo.draw(juego.batch, "CONTROLES", 290, 380);

        int xEtiqueta = 200;
        int xValor = 350;

        int y = 300;
        int salto = 40;

        fuenteTexto.draw(juego.batch, "SALTAR:", xEtiqueta, y);
        fuenteTexto.draw(juego.batch, "ESPACIO o FLECHA ARRIBA", xValor, y);

        y -= salto;
        fuenteTexto.draw(juego.batch, "AGACHARSE:", xEtiqueta, y);
        fuenteTexto.draw(juego.batch, "FLECHA ABAJO", xValor, y);

        y -= salto;
        fuenteTexto.draw(juego.batch, "REINICIAR:", xEtiqueta, y);
        fuenteTexto.draw(juego.batch, "R", xValor, y);

        y -= salto;
        fuenteTexto.draw(juego.batch, "VOLVER AL MENÚ:", xEtiqueta, y);
        fuenteTexto.draw(juego.batch, "M o ESC", xValor, y);

        fuenteTexto.draw(juego.batch, "Presioná M o ESC para volver", 260, 120);
    }

    private void leerTeclasParaVolver() {
        boolean presionoM = Gdx.input.isKeyJustPressed(Input.Keys.M);
        boolean presionoEscape = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);

        if (presionoM || presionoEscape) {
            juego.setScreen(new PantallaMenu(juego));
        }
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        fuenteTitulo.dispose();
        fuenteTexto.dispose();
    }
}
