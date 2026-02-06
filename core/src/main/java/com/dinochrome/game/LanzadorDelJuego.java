package com.dinochrome.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;



public class LanzadorDelJuego extends Game {

    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true);
        setScreen(new PantallaMenu(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
