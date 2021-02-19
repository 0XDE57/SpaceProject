package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class ShieldRenderSystem extends IteratingSystem {
    
    private final OrthographicCamera cam;
    private final ShapeRenderer shape;
    
    public ShieldRenderSystem() {
        super(Family.all(ShieldComponent.class, TransformComponent.class).get());
        
        this.cam = GameScreen.cam;
        this.shape = new ShapeRenderer();
    }
    
    @Override
    public void update(float delta) {
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    
        shape.setProjectionMatrix(cam.combined);
        
        super.update(delta);
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = Mappers.transform.get(entity);
        ShieldComponent shield = Mappers.shield.get(entity);

        Color c = shield.color;
        
        //draw overlay
        shape.begin(ShapeRenderer.ShapeType.Filled);
        if (shield.state == ShieldComponent.State.on) {
            shape.setColor(c.r, c.g, c.b, 0.25f);
        } else {
            shape.setColor(c.r, c.g, c.b, 0.15f);
        }
        shape.circle(transform.pos.x, transform.pos.y, shield.radius);
        shape.end();//flush inside loop = bad! (allegedly)
        
        //draw outline
        shape.begin(ShapeRenderer.ShapeType.Line);
        if (shield.state == ShieldComponent.State.on) {
            shape.setColor(Color.WHITE);
        } else {
            shape.setColor(c.r, c.g, c.b, 1f);
        }
        shape.circle(transform.pos.x, transform.pos.y, shield.radius);
        shape.end();//double flush inside same loop. im a horrible person ;p
    }
    
}
