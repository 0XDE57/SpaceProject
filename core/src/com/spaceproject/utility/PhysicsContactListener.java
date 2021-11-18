package com.spaceproject.utility;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.CamTargetComponent;
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.Sprite3DComponent;

public class PhysicsContactListener implements ContactListener {
    
    private final Engine engine;
    
    public PhysicsContactListener(Engine engine) {
        this.engine = engine;
    }
    
    @Override
    public void beginContact(Contact contact) {
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();
        
        onCollision((Entity)dataA, (Entity)dataB);
    }
    
    @Override
    public void endContact(Contact contact) {}
    
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {}
    
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    
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
        if (damageComponent.source == attackedEntity) {
            return; //ignore self-inflicted damage
        }
        Gdx.app.log(this.getClass().getSimpleName(),
                "[" + DebugUtil.objString(attackedEntity) + "] attacked by: [" + DebugUtil.objString(damageComponent.source) + "]");
        
        //check if attacked entity was AI
        AIComponent ai = Mappers.AI.get(attackedEntity);
        if (ai != null) {
            //focus camera on target
            attackedEntity.add(new CamTargetComponent());
            
            //focus ai on player
            ai.attackTarget = damageComponent.source;
            ai.state = AIComponent.State.attack;
        } else if (Mappers.controlFocus.get(damageEntity) != null) {
            //someone attacked player, focus on enemy
            damageEntity.add(new CamTargetComponent());
        }
        
        //check for shield
        ShieldComponent shieldComp = Mappers.shield.get(attackedEntity);
        if (shieldComp != null) {
            if (shieldComp.state == ShieldComponent.State.on) {
                //shieldComp.state == ShieldComponent.State.break;??
                damageEntity.add(new RemoveComponent());
                return;
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
    
        //add roll to hit body
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(attackedEntity);
        if (sprite3D != null) {
            float roll = 50 * MathUtils.degRad;
            sprite3D.renderable.angle += MathUtils.randomBoolean() ? roll : -roll;
        }
        
        
        //do damage
        healthComponent.health -= damageComponent.damage;
        
        //remove entity (kill)
        if (healthComponent.health <= 0) {
            Array<Entity> cluster = ECSUtil.getAttachedEntities(engine, attackedEntity);
            for (Entity e : cluster) {
                e.add(new RemoveComponent());
            }
            ChargeCannonComponent chargeCannon = Mappers.chargeCannon.get(attackedEntity);
            if (chargeCannon != null && chargeCannon.projectileEntity != null) {
                //destroy or release
                chargeCannon.projectileEntity.add(new RemoveComponent());
            }
            Gdx.app.log(this.getClass().getSimpleName(),
                    "[" + DebugUtil.objString(attackedEntity) + "] killed by: [" + DebugUtil.objString(damageComponent.source) + "]");
        }
        
        //remove projectile
        damageEntity.add(new RemoveComponent());
    }
    
}
