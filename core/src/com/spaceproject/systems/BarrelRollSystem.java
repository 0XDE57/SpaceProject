package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.BarrelRollComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.utility.Mappers;
import com.spaceproject.math.MyMath;

public class BarrelRollSystem extends IteratingSystem {
    
    private final EntityConfig entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
    
    private final Interpolation animInterpolation = Interpolation.pow2;
    private final float strafeMaxRollAngle = 40 * MathUtils.degRad;
    private final float strafeRollSpeed = 3f;
    
    public BarrelRollSystem() {
        super(Family.all(Sprite3DComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        ControllableComponent control = Mappers.controllable.get(entity);
    
        float rollAmount = strafeRollSpeed * deltaTime;
        
        //don't allow dodging while shield is active
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null && shield.state != ShieldComponent.State.off) {
            //continue to stabilize while shield active
            stabilizeRoll(sprite3D, rollAmount);
            return;
        }
        
        //barrel roll
        BarrelRollComponent rollComp = Mappers.barrelRoll.get(entity);
        if (rollComp != null) {
            if (rollComp.flipState != BarrelRollComponent.FlipState.off) {
                barrelRoll(sprite3D, rollComp);
                return;
            }
            
            if (control != null) {
                if (control.moveLeft && control.alter) {
                    dodgeLeft(entity, rollComp);
                }
                if (control.moveRight && control.alter) {
                    dodgeRight(entity, rollComp);
                }
                if (control.moveForward && control.alter) {
                    boostForward(entity, rollComp);
                }
            }
        }
        
        //strafe roll
        if (control != null) {
            if (control.moveLeft) {
                rollLeft(sprite3D, rollAmount);
            }
            if (control.moveRight) {
                rollRight(sprite3D, rollAmount);
            }
            if (!control.moveLeft && !control.moveRight) {
                stabilizeRoll(sprite3D, rollAmount);
            }
        } else  {
            stabilizeRoll(sprite3D, rollAmount);
        }
    }
    
    private void rollLeft(Sprite3DComponent sprite3D, float roll) {
        sprite3D.renderable.angle += roll;
        if (sprite3D.renderable.angle > strafeMaxRollAngle) {
            sprite3D.renderable.angle = strafeMaxRollAngle;
        }
    }
    
    private void rollRight(Sprite3DComponent sprite3D, float roll) {
        sprite3D.renderable.angle -= roll;
        if (sprite3D.renderable.angle < -strafeMaxRollAngle) {
            sprite3D.renderable.angle = -strafeMaxRollAngle;
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
    
    public static void dodgeLeft(Entity entity, BarrelRollComponent roll) {
        if (roll.timeoutTimer.tryEvent()) {
            applyDodgeImpulse(entity, roll, BarrelRollComponent.FlipState.left);
        }
    }
    
    public static void dodgeRight(Entity entity, BarrelRollComponent roll) {
        if (roll.timeoutTimer.tryEvent()) {
            applyDodgeImpulse(entity, roll, BarrelRollComponent.FlipState.right);
        }
    }
    
    private void boostForward(Entity entity, BarrelRollComponent roll) {
        if (roll.timeoutTimer.tryEvent()) {
            applyDodgeImpulse(entity, roll, BarrelRollComponent.FlipState.forward);
        }
    }
    
    private static void applyDodgeImpulse(Entity entity, BarrelRollComponent roll, BarrelRollComponent.FlipState flipState) {
        //snap to angle to bypass rotation lerp to make dodge feel better/more responsive
        TransformComponent transform = Mappers.transform.get(entity);
        ControllableComponent control = Mappers.controllable.get(entity);
        transform.rotation = control.angleTargetFace;
        Body body = Mappers.physics.get(entity).body;
        body.setAngularVelocity(0);
        body.setTransform(body.getPosition(), transform.rotation);
        
        //apply left or right impulse
        float direction = transform.rotation;
        switch (flipState) {
            case left:
                direction += MathUtils.PI / 2;
                break;
            case right:
                direction -= MathUtils.PI / 2;
                break;
        }
        //float direction = (flipState == BarrelRollComponent.FlipState.left) ? transform.rotation + MathUtils.PI / 2 : transform.rotation - MathUtils.PI / 2;
        body.applyLinearImpulse(MyMath.vector(direction, roll.force), body.getPosition(), true);
    
        //set roll animation
        roll.flipState = flipState;
        roll.animationTimer.reset();
    }
    
    private void barrelRoll(Sprite3DComponent sprite3D, BarrelRollComponent rollComp) {
        //reset
        if (rollComp.animationTimer.canDoEvent()) {
            rollComp.flipState = BarrelRollComponent.FlipState.off;
            //rollComp.activate = false;
            sprite3D.renderable.angle = 0;
            return;
        }
        
        switch (rollComp.flipState) {
            case left:
                sprite3D.renderable.angle = animInterpolation.apply(MathUtils.PI2 * rollComp.revolutions, 0, rollComp.animationTimer.ratio());
                break;
            case right:
                sprite3D.renderable.angle = animInterpolation.apply(0, MathUtils.PI2 * rollComp.revolutions, rollComp.animationTimer.ratio());
                break;
        }
    }
    
}
