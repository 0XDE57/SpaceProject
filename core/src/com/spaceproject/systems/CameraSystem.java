package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;

public class CameraSystem extends IteratingSystem {
    
    private OrthographicCamera cam;
    
    public CameraSystem() {
        this(MyScreenAdapter.cam);
    }
    
    public CameraSystem(OrthographicCamera camera) {
        super(Family.all(CameraFocusComponent.class, TransformComponent.class).get());
        cam = camera;
    }
    
    public void processEntity(Entity entity, float delta) {
        TransformComponent transform = Mappers.transform.get(entity);
        
        //set camera position to entity
        cam.position.x = transform.pos.x;
        cam.position.y = transform.pos.y;
        cam.update();
    }
    
    /**
     * BUG: if engine is reassigned (engine = new Engine()) and a previously existing entity is re-added,
     * the underlying families will not see that entity unless removed first
     * <p>
     * SOLUTION: workaround is to engine.removeAllEntities() when changing levels
     * in the engine,when an entity is removed it calls updateFamilyMembership()
     * Maybe the problem lies in references to old bits that aren't discarded when a new Engine() is created?
     * <p>
     * not sure if bug or intended library behavior...
     */
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        for (Entity x : engine.getEntities()) {
            if (x.getComponent(CameraFocusComponent.class) != null && x.getComponent(TransformComponent.class) != null) {
                if (!engine.getEntitiesFor(Family.all(CameraFocusComponent.class, TransformComponent.class).get()).contains(x, true)) {
                    Gdx.app.log(this.getClass().getSimpleName(), "FOUND ENTITY IN ENGINE: FAMILY DID NOT PICK UP ENTITY!");
                    //throw new Exception("Family did not pick up entity in engine");
                }
            }
        }
    }
}
