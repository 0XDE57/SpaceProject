package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.CustomShapeRenderer;
import com.spaceproject.utility.Mappers;

public class AsteroidRenderSystem extends IteratingSystem {
    
    CustomShapeRenderer shapeRenderer;
    Color color = new Color();
    
    public AsteroidRenderSystem() {
        super(Family.all(AsteroidComponent.class, TransformComponent.class).get());
        shapeRenderer = new CustomShapeRenderer();
    }
    
    @Override
    public void update(float deltaTime) {
        shapeRenderer.setProjectionMatrix(GameScreen.cam.combined);
        //render filled inner poly
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        super.update(deltaTime);
        shapeRenderer.end();
    
        //render outer polygon triangle mesh outline
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        super.update(deltaTime);
        shapeRenderer.end();
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AsteroidComponent asteroid = Mappers.asteroid.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        HealthComponent health = Mappers.health.get(entity);
        
        Polygon polygon = asteroid.polygon;
        polygon.setRotation(transform.rotation * MathUtils.radiansToDegrees);
        polygon.setPosition(transform.pos.x, transform.pos.y);
        
        
        
        if (shapeRenderer.getCurrentType() == ShapeRenderer.ShapeType.Filled) {
            //inner body
            //color = asteroid.debugColor.cpy();
            float ratio = health.health / health.maxHealth;
            color.set(1, ratio, ratio, 1);
        } else {
            //mesh outline
            color = Color.WHITE;
            //color = asteroid.debugColor;
            if (asteroid.parentOrbitBody == null) {
                color = Color.RED;
            }
        }
        
        shapeRenderer.fillPolygon(polygon.getTransformedVertices(), 0, polygon.getVertices().length, color);
    }
    
}
