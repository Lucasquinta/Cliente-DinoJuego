package com.dinochrome.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Obstaculos {

    public static final int CACTUS = 0;
    public static final int PTERO = 1;

    float x, y;
    float width, height;
    int type;

    private Texture texture;
    private Rectangle bounds;

    public Obstaculos(float x, float y, float width, float height, int type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;

        // 👉 Textura según tipo
        if (type == CACTUS) {
            texture = new Texture("cactus.png");
        } else {
            texture = new Texture("ptero.png");
        }

        // 👉 Hitbox única (NO se recrea)
        bounds = new Rectangle(x, y, width, height);
    }

    public void update(float delta, float speed) {
        x -= speed * delta;
        bounds.setPosition(x, y);
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, x, y, width, height);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void dispose() {
        texture.dispose();
    }
}
