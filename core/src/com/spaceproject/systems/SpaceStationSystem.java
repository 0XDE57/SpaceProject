package com.spaceproject.systems;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.SpaceStationComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.Mappers;

public class SpaceStationSystem extends EntitySystem {
    
    private ImmutableArray<Entity> stations;
    
    @Override
    public void addedToEngine(Engine engine) {
        stations = engine.getEntitiesFor(Family.all(SpaceStationComponent.class, TransformComponent.class).get());
    }
    
    @Override
    public void update(float deltaTime) {
        updateStationOrbit();
    }
    
    private void updateStationOrbit() {
        //keep station in orbit around parent body, don't fling out into universe...
        for (Entity entity : stations) {
            SpaceStationComponent spaceStation = Mappers.spaceStation.get(entity);
            PhysicsComponent physics = Mappers.physics.get(entity);
            if (spaceStation.parentOrbitBody != null) {
                TransformComponent parentTransform = Mappers.transform.get(spaceStation.parentOrbitBody);
                //set velocity perpendicular to parent body, (simplified 2-body model)
                float angle = MyMath.angleTo(parentTransform.pos, physics.body.getPosition()) + (spaceStation.velocity > 0 ? -MathUtils.HALF_PI : MathUtils.HALF_PI);
                physics.body.setLinearVelocity(MyMath.vector(angle, spaceStation.velocity));
            }
        }
    }
    
}
