package com.spaceproject.screens.debug;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.RenderOrder;
import com.spaceproject.generation.BodyFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.systems.AsteroidRenderSystem;
import com.spaceproject.systems.B2DPhysicsSystem;
import com.spaceproject.systems.ClearScreenSystem;
import com.spaceproject.systems.ParallaxRenderSystem;

public class BlocksTestScreen extends MyScreenAdapter {
    
    private static Engine engine;
    public World box2dWorld;
    
    public BlocksTestScreen() {
        box2dWorld = new World(new Vector2(), true);
        
        engine = new Engine();
        engine.addSystem(new ClearScreenSystem());
        engine.addSystem(new ParallaxRenderSystem());
        engine.addSystem(new B2DPhysicsSystem());
        engine.addSystem(new AsteroidRenderSystem());
        
        Entity paddle = new Entity();
        engine.addEntity(paddle);
    
        Entity ball = new Entity();
        BodyFactory.createCircle(0,0,10, box2dWorld, ball);
        engine.addEntity(ball);
    
        Entity brick = createWall(10, 10, 100, 1000);
        engine.addEntity(brick);
    }
    
    public static Entity createWall(float x, float y, int width, int height) {
        Entity entity = new Entity();
        
        int pixelPerUnit = 2;
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureFactory.generateWall(
                width * pixelPerUnit,
                height * pixelPerUnit,
                new Color(0.4f, 0.4f, 0.4f, 1));
        texture.scale = 0.05f;
        entity.add(texture);
        
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createWall(x, y, width, height, entity);
        entity.add(physics);
        
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.WORLD_OBJECTS.getHierarchy();
        entity.add(transform);
        
        return entity;
    }
    
    @Override
    public void render(float delta) {
        super.render(delta);
        engine.update(delta);
    }
    
}
