package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class ShieldRenderSystem extends IteratingSystem implements Disposable {

    private final ShapeRenderer shape;
    
    public ShieldRenderSystem() {
        super(Family.all(ShieldComponent.class, TransformComponent.class).get());
        shape = new ShapeRenderer();
    }
    
    @Override
    public void update(float delta) {
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    
        shape.setProjectionMatrix(GameScreen.cam.combined);
        
        super.update(delta);
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield.state == ShieldComponent.State.off) {
            return;
        }

        TransformComponent transform = Mappers.transform.get(entity);

        //draw overlay
        shape.begin(ShapeRenderer.ShapeType.Filled);
        if (shield.state == ShieldComponent.State.on) {
            shape.setColor(shield.heat, 0, 1, 0.25f + shield.heat);
        } else {
            shape.setColor(shield.heat, 0, 1, 0.15f + shield.heat);
        }
        if (shield.heat >= 0.95f) {
            shape.setColor(1, 0, 0, 1);
        }

        float hitTime = 500;
        long lastHit = GameScreen.getGameTimeCurrent() - shield.lastHit;
        if (lastHit < hitTime) {
            float green = lastHit / hitTime;
            shape.setColor(0, 1-green, green, Math.max(1-green, 0.25f));
        }
        shape.circle(transform.pos.x, transform.pos.y, shield.radius);
        shape.end();//flush inside loop = bad?
        
        //draw outline
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(1, 1 - shield.heat, 1 - shield.heat, 1f);
        if (lastHit < hitTime) {
            float green = lastHit / hitTime;
            //shape.setColor(1-green, 1-green, 1-green, 1);
            shape.setColor(1, green, 1, 1);
        }
        if (shield.heat >= 0.95f) {
            shape.setColor(1, 1, 0, 1);
        }
        shape.circle(transform.pos.x, transform.pos.y, shield.radius);
        shape.end();//double flush inside same loop?
    }
    
    @Override
    public void dispose() {
        shape.dispose();
    }
    
}
