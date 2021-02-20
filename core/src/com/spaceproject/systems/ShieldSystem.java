package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.generation.BodyFactory;
import com.spaceproject.utility.Mappers;

public class ShieldSystem extends IteratingSystem {
    
    public ShieldSystem() {
        super(Family.all(ShieldComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ShieldComponent shield = Mappers.shield.get(entity);
        
        switch (shield.state) {
            case off:
                if (shield.defend) {
                    engage(shield);
                }
                break;
            case on:
                if (!shield.defend) {
                    disengage(entity, shield);
                }
                break;
            case charge:
                if (!shield.defend) {
                    shield.state = ShieldComponent.State.discharge;
                    break;
                }
                
                //charge: gain energy
                shield.radius = shield.maxRadius * shield.animTimer.ratio();
                if (shield.radius == shield.maxRadius || shield.animTimer.canDoEvent()) {
                    shield.state = ShieldComponent.State.on;
                    
                    //add shield fixture to body for protection
                    Body body = entity.getComponent(PhysicsComponent.class).body;
                    BodyFactory.addShieldFixtureToBody(body, shield.radius);
                }
                break;
            case discharge:
                if (shield.defend) {
                    engage(shield);
                    break;
                }
                
                //discharge: loose energy
                shield.radius = shield.maxRadius * (1 - shield.animTimer.ratio());
                if (shield.radius <= 0  || shield.animTimer.canDoEvent()) {
                    shield.state = ShieldComponent.State.off;
                }
                break;
        }
    }
    
    private void engage(ShieldComponent shield) {
        float shieldReactivateThreshold = 0.3f;
        
        //reactivate if still enough charge
        if (shield.animTimer.ratio() >= shieldReactivateThreshold) {
            shield.animTimer.flipRatio();
        } else {
            shield.animTimer.reset();
        }
        
        shield.state = ShieldComponent.State.charge;
    }
    
    private void disengage(Entity entity, ShieldComponent shield) {
        shield.state = ShieldComponent.State.discharge;
        
        //destroy shield fixture
        Body body = entity.getComponent(PhysicsComponent.class).body;
        Fixture circleFixture = body.getFixtureList().get(body.getFixtureList().size - 1);
        body.destroyFixture(circleFixture);
        
        shield.animTimer.flipRatio();
    }
    
}
