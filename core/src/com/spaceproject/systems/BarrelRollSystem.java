package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.BarrelRollComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;

public class BarrelRollSystem extends IteratingSystem {
    
    private final EntityConfig entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
    private final float maxRollAngle = 40 * MathUtils.degRad;
    private final float strafeRotSpeed = 3f;
    
    public BarrelRollSystem() {
        super(Family.all(Sprite3DComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        ControllableComponent control = Mappers.controllable.get(entity);
        
        //barrel roll
        BarrelRollComponent rollComp = Mappers.barrelRoll.get(entity);
        if (rollComp != null && rollComp.dir != BarrelRollComponent.FlipDir.none) {
            barrelRoll(sprite3D, rollComp);
            
            return;
        }
        if (control.moveLeft && control.alter) {
            dodgeLeft(entity, control);
        }
        if (control.moveRight && control.alter) {
            dodgeRight(entity, control);
        }
        
        //strafe roll
        float rollAmount = strafeRotSpeed * deltaTime;
        if (control.moveLeft) {
            rollLeft(sprite3D, rollAmount);
        }
        if (control.moveRight) {
            rollRight(sprite3D, rollAmount);
        }
        if (!control.moveLeft && !control.moveRight) {
            stabilizeRoll(sprite3D, rollAmount);
        }
    }
    
    private void rollLeft(Sprite3DComponent sprite3D, float roll) {
        sprite3D.renderable.angle += roll;
        if (sprite3D.renderable.angle > maxRollAngle) {
            sprite3D.renderable.angle = maxRollAngle;
        }
    }
    
    private void rollRight(Sprite3DComponent sprite3D, float roll) {
        sprite3D.renderable.angle -= roll;
        if (sprite3D.renderable.angle < -maxRollAngle) {
            sprite3D.renderable.angle = -maxRollAngle;
        }
    }
    
    private void stabilizeRoll(Sprite3DComponent sprite3D, float roll) {
        if (sprite3D.renderable.angle != 0) {
            if (sprite3D.renderable.angle < 0) {
                sprite3D.renderable.angle += roll;
            }
            if (sprite3D.renderable.angle > 0) {
                sprite3D.renderable.angle -= roll;
            }
        }
    }
    
    private void dodgeLeft(Entity entity, ControllableComponent control) {
        if (control.timerDodge.tryEvent()) {
            applyDodgeImpulse(entity, control, BarrelRollComponent.FlipDir.left);
        }
    }
    
    private void dodgeRight(Entity entity, ControllableComponent control) {
        if (control.timerDodge.tryEvent()) {
            applyDodgeImpulse(entity, control, BarrelRollComponent.FlipDir.right);
        }
    }
    
    private void applyDodgeImpulse(Entity entity, ControllableComponent control, BarrelRollComponent.FlipDir flipDir) {
        //snap to angle to bypass rotation lerp to make dodge feel better/more responsive
        TransformComponent transform = Mappers.transform.get(entity);
        transform.rotation = control.angleTargetFace;
        Body body = Mappers.physics.get(entity).body;
        body.setAngularVelocity(0);
        body.setTransform(body.getPosition(), transform.rotation);
        
        //apply left or right impulse
        float direction = (flipDir == BarrelRollComponent.FlipDir.left) ? transform.rotation + MathUtils.PI / 2 : transform.rotation - MathUtils.PI / 2;
        body.applyLinearImpulse(MyMath.vector(direction, entityCFG.dodgeForce), body.getPosition(), true);
    
        //set roll animation
        BarrelRollComponent rollComponent = Mappers.barrelRoll.get(entity);
        if (rollComponent != null) {
            rollComponent.dir = flipDir;
            rollComponent.animationTimer.reset();
        }
    }
    
    private void barrelRoll(Sprite3DComponent sprite3D, BarrelRollComponent rollComp) {
        //reset
        if (rollComp.animationTimer.canDoEvent()) {
            rollComp.dir = BarrelRollComponent.FlipDir.none;
            sprite3D.renderable.angle = 0;
        }
        
        switch (rollComp.dir) {
            case left:
                sprite3D.renderable.angle = rollComp.animInterpolation.apply(MathUtils.PI2 * rollComp.revolutions, 0, rollComp.animationTimer.ratio());
                break;
            case right:
                sprite3D.renderable.angle = rollComp.animInterpolation.apply(0, MathUtils.PI2 * rollComp.revolutions, rollComp.animationTimer.ratio());
                break;
        }
    }
    
}
