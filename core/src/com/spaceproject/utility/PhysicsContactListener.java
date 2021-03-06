package com.spaceproject.utility;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.ShieldComponent;

public class PhysicsContactListener implements ContactListener {
    
    @Override
    public void beginContact(Contact contact) {
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();
        
        //System.out.println("A: " + Misc.objString(dataA) + " - B: " + Misc.objString(dataB));
        onCollision((Entity)dataA, (Entity)dataB);
    }
    
    @Override
    public void endContact(Contact contact) {
    }
    
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }
    
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }
    
    private void onCollision(Entity a, Entity b) {
        DamageComponent damageA = Mappers.damage.get(a);
        DamageComponent damageB = Mappers.damage.get(b);
        HealthComponent healthA = Mappers.health.get(a);
        HealthComponent healthB = Mappers.health.get(b);
        
        if (damageA != null && healthB != null) {
            onAttacked(a, b, damageA, healthB);
        }
        if (damageB != null && healthA != null) {
            onAttacked(b, a, damageB, healthA);
        }
    }
    
    
    private void onAttacked(Entity damageEntity, Entity attackedEntity, DamageComponent damageComponent, HealthComponent healthComponent) {
        //TODO: move this. collision/physics doesnt care about damage. a combat system should subscribe to this event.
        if (damageComponent.source == attackedEntity) {
            return;
        }
        
        //check if attacked entity was AI
        AIComponent ai = Mappers.AI.get(attackedEntity);
        if (ai != null) {
            ai.attackTarget = damageComponent.source;
            ai.state = AIComponent.State.attack;
            Gdx.app.log(this.getClass().getSimpleName(), "AI [" + Misc.objString(attackedEntity) + "] attacked by: [" + Misc.objString(damageComponent.source) + "]");
        }
        
        //check for shield
        ShieldComponent shieldComp = Mappers.shield.get(attackedEntity);
        if (shieldComp != null) {
            if (shieldComp.state == ShieldComponent.State.on) {
                //shieldComp.state == ShieldComponent.State.break;??
            }
            /*
            if (shieldComp.isActive) {
                //Body body = attackedEntity.getComponent(PhysicsComponent.class).body;
                //Fixture circleFixture = body.getFixtureList().get(body.getFixtureList().size - 1);
                //body.destroyFixture(circleFixture);//cant remove mid collision
                //attackedEntity.remove(ShieldComponent.class);
                shieldComp.radius = -1f;
                //shieldComp.animTimer.reset();
                
                damageEntity.add(new RemoveComponent());
                return;
            } else {
                //destroy shield if it isn't fully activated.
                //todo: "premature break effect", sound effect here, maybe particle effect
                attackedEntity.remove(shieldComp.getClass());
            }*/
        }
        
        //do damage
        healthComponent.health -= damageComponent.damage;
        
        //remove entity (kill)
        if (healthComponent.health <= 0) {
            attackedEntity.add(new RemoveComponent());
            Gdx.app.log(this.getClass().getSimpleName(), "[" + Misc.objString(attackedEntity) + "] killed by: [" + Misc.objString(damageComponent.source) + "]");
        }
        
        //remove missile
        damageEntity.add(new RemoveComponent());
    }
    
}
