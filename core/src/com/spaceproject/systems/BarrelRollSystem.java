package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.utility.Mappers;

public class BarrelRollSystem extends IteratingSystem {
    
    private final float maxRollAngle = 40 * MathUtils.degRad;
    private final float strafeRotSpeed = 3f;
    
    public BarrelRollSystem() {
        super(Family.all(Sprite3DComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        ControllableComponent control = Mappers.controllable.get(entity);
    
        float strafe = strafeRotSpeed * deltaTime;
        if (control.moveLeft) {
            rollLeft(sprite3D, strafe);
        }
        if (control.moveRight) {
            rollRight(sprite3D, strafe);
        }
        if (!control.moveRight && !control.moveLeft) {
            stabilizeRoll(sprite3D, strafe);
        }
    }
    
    private void rollRight(Sprite3DComponent sprite3D, float strafe) {
        sprite3D.renderable.angle -= strafe;
        if (sprite3D.renderable.angle < -maxRollAngle) {
            sprite3D.renderable.angle = -maxRollAngle;
        }
    }
    
    private void rollLeft(Sprite3DComponent sprite3D, float strafe) {
        sprite3D.renderable.angle += strafe;
        if (sprite3D.renderable.angle > maxRollAngle) {
            sprite3D.renderable.angle = maxRollAngle;
        }
    }
    
    private void stabilizeRoll(Sprite3DComponent sprite3D, float strafe) {
        if (sprite3D.renderable.angle > 0) {
            sprite3D.renderable.angle -= strafe;
        }
        if (sprite3D.renderable.angle < 0) {
            sprite3D.renderable.angle += strafe;
        }
    }
    
}
