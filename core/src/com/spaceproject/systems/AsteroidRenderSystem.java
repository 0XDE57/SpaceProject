package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Polygon;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.CustomShapeRenderer;
import com.spaceproject.utility.Mappers;

public class AsteroidRenderSystem extends IteratingSystem {
    
    CustomShapeRenderer shapeRenderer;
    
    public AsteroidRenderSystem() {
        super(Family.all(AsteroidComponent.class, TransformComponent.class).get());
        
        ShapeRenderer shapeRenderer = new ShapeRenderer();
        this.shapeRenderer = new CustomShapeRenderer(ShapeRenderer.ShapeType.Filled, shapeRenderer.getRenderer());
    }
    @Override
    public void update(float deltaTime) {
        shapeRenderer.setProjectionMatrix(GameScreen.cam.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        super.update(deltaTime);
        shapeRenderer.end();
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AsteroidComponent asteroid = Mappers.asteroid.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        
        Polygon polygon = asteroid.polygon;
        
        /*
        PhysicsComponent physics = Mappers.physics.get(entity);
        polygon.setPosition(physics.body.getPosition().x, physics.body.getPosition().y);
        polygon.setRotation(physics.body.getAngle());*/
        
        polygon.setPosition(transform.pos.x, transform.pos.y);
        polygon.setRotation(transform.rotation);
        
        shapeRenderer.fillPolygon(polygon.getTransformedVertices(), 0, polygon.getVertices().length, Color.WHITE);
    }
    
}
