package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.DodgeComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.GrowCannonComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.generation.BodyFactory;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.SimpleTimer;


public class ShipControlSystem extends IteratingSystem {
    
    private static EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    private static EntityConfig entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
    private ImmutableArray<Entity> planets;
    
    private float offsetDist = 1.5f;//TODO: dynamic based on ship size
    //TODO: move values to config
    private float maxRollAngle = 40 * MathUtils.degRad;
    private float strafeRot = 3f;
    private float faceRotSpeed = 8f;
    
    public ShipControlSystem() {
        super(Family.all(ControllableComponent.class, TransformComponent.class, VehicleComponent.class).get());
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        planets = engine.getEntitiesFor(Family.all(PlanetComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float delta) {
        controlShip(entity, delta);
    }
    
    private void controlShip(Entity entity, float delta) {
        if (Mappers.screenTrans.get(entity) != null) {
            //don't allow control of ship while landing or takeoff
            return;
        }
        
        ControllableComponent control = Mappers.controllable.get(entity);
        VehicleComponent vehicle = Mappers.vehicle.get(entity);
        TransformComponent transformComp = Mappers.transform.get(entity);
        PhysicsComponent physicsComp = Mappers.physics.get(entity);
        DodgeComponent dodgeComp = Mappers.dodge.get(entity);
        ShieldComponent shield = Mappers.shield.get(entity);
        
        boolean canAct = (dodgeComp == null);
        boolean canShoot = dodgeComp == null && shield == null;
        boolean canDodge = shield == null;

        
        barrelRoll(entity, dodgeComp);
        manageShield(entity, control, shield);
        
        if (GameScreen.isDebugMode) {
            applyDebugControls(entity, transformComp, physicsComp);
        }
        
        if (control.actionA) {
            toggleHyperDrive(entity, control, physicsComp);
        }
        
        if (!canAct) {
            return;
        }
        
        faceTarget(control, physicsComp, delta);
        
        if (control.moveForward) {
            accelerate(control, physicsComp.body, vehicle, delta);
        }
        if (control.moveBack) {
            decelerate(physicsComp.body, delta);
        }
        
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        float strafe = strafeRot * delta;
        if (control.moveLeft) {
            strafeLeft(vehicle, control, physicsComp, sprite3D, strafe, delta);
            
            if (canDodge && control.alter) {
                dodgeLeft(entity, transformComp, control);
            }
        }
    
        if (control.moveRight) {
            strafeRight(vehicle, control, physicsComp, sprite3D, strafe, delta);
            
            if (canDodge && control.alter) {
                dodgeRight(entity, transformComp, control);
            }
        }
    
        if (!control.moveRight && !control.moveLeft) {
            stabilizeRoll(sprite3D, strafe);
        }
        
        
        //fire cannon / attack
        CannonComponent cannon = Mappers.cannon.get(entity);
        if (cannon != null) {
            refillAmmo(cannon);
            if (control.attack && canShoot) {
                fireCannon(transformComp, cannon, entity);
            }
        }
        if (canShoot) {
            GrowCannonComponent growCannon = Mappers.growCannon.get(entity);
            manageGrowCannon(entity, control, transformComp, growCannon, physicsComp.body);
        }
        
        //exit vehicle
        if (control.changeVehicle) {
            exitVehicle(entity, control);
        }
        
        
        //transition or take off from planet
        if (GameScreen.inSpace()) {
            Entity planet = getPlanetNearPosition(transformComp.pos);
            control.canTransition = planet != null;
            if (control.transition && control.canTransition) {
                beginLandOnPlanet(entity, planet);
            }
        } else {
            control.canTransition = true;
            if (control.transition) {
                takeOffPlanet(entity);
            }
        }
    }
    
    private void faceTarget(ControllableComponent control, PhysicsComponent physicsComp, float delta) {
        float angle = MathUtils.lerpAngle(physicsComp.body.getAngle(), control.angleTargetFace, faceRotSpeed * delta);
        float impulse = MyMath.getAngularImpulse(physicsComp.body, angle, delta);
        physicsComp.body.applyAngularImpulse(impulse, true);
    }
    
    private void toggleHyperDrive(Entity entity, ControllableComponent control, PhysicsComponent physicsComp) {
        HyperDriveComponent hyperDrive = Mappers.hyper.get(entity);
        if (hyperDrive.active) {
            if (control.actionACooldownTimer.canDoEvent()) {
                //hyperDrive.active = false;
                control.actionACooldownTimer.reset();
            }
        } else {
            if (control.actionACooldownTimer.canDoEvent()) {
                control.actionACooldownTimer.reset();
                //hyperComp = new HyperDriveComponent();
                //hyperComp.coolDownTimer = new SimpleTimer(hyperModeTimeout, true);
                hyperDrive.velocity.set(MyMath.vector(physicsComp.body.getAngle(), hyperDrive.speed));
                hyperDrive.active = true;
                //entity.add(hyperComp);
    
                physicsComp.body.setActive(false);
            }
        }
        //} else {
            /*
            if (hyperComp.coolDownTimer.canDoEvent()) {
                //entity.remove(HyperDriveComponent.class);
    
                physicsComp.body.setTransform(transformComp.pos, transformComp.rotation);
                physicsComp.body.setActive(true);
                physicsComp.body.setLinearVelocity(MyMath.vector(transformComp.rotation, 60/*entity.getComponent(VehicleComponent.class).maxSpeed*));
                
                control.actionACooldownTimer.reset();
            }*/
        //}
    }
    
    private void applyDebugControls(Entity entity, TransformComponent transformComp, PhysicsComponent physicsComp) {
        //debug force insta-stop
        if (Gdx.input.isKeyJustPressed(Keys.X)) {
            physicsComp.body.setLinearVelocity(0, 0);
            //if (entity.remove(HyperDriveComponent.class) != null) {
                physicsComp.body.setActive(true);
                physicsComp.body.setTransform(transformComp.pos, transformComp.rotation);
                
            //}
            HyperDriveComponent hyperDrive = Mappers.hyper.get(entity);
            hyperDrive.active = false;
        }
        if (Gdx.input.isKeyJustPressed(Keys.Z)) {
            physicsComp.body.setLinearVelocity(physicsComp.body.getLinearVelocity().add(physicsComp.body.getLinearVelocity()));
        }
    }
    
    private static void accelerate(ControllableComponent control, Body body, VehicleComponent vehicle, float delta) {
        float thrust = vehicle.thrust * control.movementMultiplier * delta;
        body.applyForceToCenter(MyMath.vector(body.getAngle(), thrust), true);
    }
    
    private static void decelerate(Body body, float delta) {
        float stopThreshold = 0.2f;
        
        if (body.getLinearVelocity().len() <= stopThreshold) {
            //completely stop if moving really slowly
            body.setLinearVelocity(0, 0);
        } else {
            //add thrust opposite direction of velocity to slow down ship
            float thrust = body.getLinearVelocity().len() * 20 * delta;
            float angle = body.getLinearVelocity().angle();
            body.applyForceToCenter(MyMath.vector(angle, thrust), true);
        }
    }
    
    private void strafeRight(VehicleComponent vehicle, ControllableComponent control, PhysicsComponent physicsComp, Sprite3DComponent sprite3D, float strafe, float delta) {
        float thrust = vehicle.thrust * control.movementMultiplier * delta;
        Vector2 force = MyMath.vector(physicsComp.body.getAngle(), thrust).rotate90(-1);
        physicsComp.body.applyForceToCenter(force, true);
        
        rollRight(sprite3D, strafe);
    }
    
    private void strafeLeft(VehicleComponent vehicle, ControllableComponent control, PhysicsComponent physicsComp, Sprite3DComponent sprite3D, float strafe, float delta) {
        float thrust = vehicle.thrust * control.movementMultiplier * delta;
        Vector2 force = MyMath.vector(physicsComp.body.getAngle(), thrust).rotate90(1);
        physicsComp.body.applyForceToCenter(force, true);
    
        rollLeft(sprite3D, strafe);
    }
    
    private void rollRight(Sprite3DComponent sprite3D, float strafe) {
        if (sprite3D != null) {
            sprite3D.renderable.angle -= strafe;
            if (sprite3D.renderable.angle < -maxRollAngle) {
                sprite3D.renderable.angle = -maxRollAngle;
            }
        }
    }
    
    private void rollLeft(Sprite3DComponent sprite3D, float strafe) {
        if (sprite3D != null) {
            sprite3D.renderable.angle += strafe;
            if (sprite3D.renderable.angle > maxRollAngle) {
                sprite3D.renderable.angle = maxRollAngle;
            }
        }
    }
    
    private void stabilizeRoll(Sprite3DComponent sprite3D, float strafe) {
        if (sprite3D != null) {
            if (sprite3D.renderable.angle > 0) {
                sprite3D.renderable.angle -= strafe;
            }
            if (sprite3D.renderable.angle < 0) {
                sprite3D.renderable.angle += strafe;
            }
            //if (MathUtils.isEqual(sprite3D.renderable.angle, 0, 0.01f))
            //    sprite3D.renderable.angle = 0;
        }
    }
    
    private static void dodgeRight(Entity entity, TransformComponent transform, ControllableComponent control) {
        if (control.timerDodge.canDoEvent() && Mappers.dodge.get(entity) == null) {
            control.timerDodge.reset();
            
            applyDodge(entity, transform, control, DodgeComponent.FlipDir.right);
        }
    }
    
    private static void dodgeLeft(Entity entity, TransformComponent transform, ControllableComponent control) {
        if (control.timerDodge.canDoEvent() && Mappers.dodge.get(entity) == null) {
            control.timerDodge.reset();
    
            applyDodge(entity, transform, control, DodgeComponent.FlipDir.left);
        }
    }
    
    private static DodgeComponent createDodgeComponent(float dodgeForce, float rotation, DodgeComponent.FlipDir dir, long timer) {
        DodgeComponent d = new DodgeComponent();
        d.animationTimer = new SimpleTimer(timer, true);
        d.animInterpolation = Interpolation.pow2;
        d.revolutions = 1;
        d.direction = dir == DodgeComponent.FlipDir.left ? rotation + MathUtils.PI / 2 : rotation - MathUtils.PI / 2;
        d.dir = dir;
        d.force = dodgeForce;
        return d;
    }
    
    private static void applyDodge(Entity entity, TransformComponent transform, ControllableComponent control, DodgeComponent.FlipDir flipDir) {
        DodgeComponent d = createDodgeComponent(entityCFG.dodgeForce, transform.rotation, flipDir, entityCFG.dodgeAnimationTimer);
        entity.add(d);
        
        Body body = Mappers.physics.get(entity).body;
        //snap to angle to bypass rotation lerp to make dodge feel better/more responsive
        transform.rotation = control.angleTargetFace;
        body.setAngularVelocity(0);
        body.setTransform(body.getPosition(), control.angleTargetFace);
        
        body.applyLinearImpulse(MyMath.vector(d.direction, d.force), body.getPosition(), true);
    }
    
    private void barrelRoll(Entity entity, DodgeComponent dodgeComp) {
        if (dodgeComp == null) {
            return;
        }
        
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        switch (dodgeComp.dir) {
            case left:
                sprite3D.renderable.angle = dodgeComp.animInterpolation.apply(MathUtils.PI2 * dodgeComp.revolutions, 0, dodgeComp.animationTimer.ratio());
                break;
            case right:
                sprite3D.renderable.angle = dodgeComp.animInterpolation.apply(0, MathUtils.PI2 * dodgeComp.revolutions, dodgeComp.animationTimer.ratio());
                break;
        }
        
        /*
        //ensure bounding box follows sprite, ship should be thinner/harder to hit when rolling
        //TODO, this is wrong: need to find something that maps like below
        float scaleY = 1 - (sprite3D.renderable.angle / MathUtils.PI);
        //degrees, scale
        //0   = 1 	-> top
        //45  = 0.5
        //90  = 0
        //135 = 0.5
        //180 = 1 	-> bottom
        //225 = 0.5
        //270 = 0
        //315 = 0.5
        //360 = 1	-> top
        //System.out.println(sprite3D.renderable.angle + ", " + sy);
        physicsComponent.poly.setScale(1, scaleY);
        */
        
        
        if (dodgeComp.animationTimer.canDoEvent()) {
            //reset
            sprite3D.renderable.angle = 0;

            entity.remove(DodgeComponent.class);
        }
        
    }
    
    private void manageShield(Entity entity, ControllableComponent control, ShieldComponent shield) {
        if (shield == null) {
            if (control.defend) {
                Body body = entity.getComponent(PhysicsComponent.class).body;
                float radius = Math.max(MyMath.calculateBoundingBox(body).getWidth(), MyMath.calculateBoundingBox(body).getHeight());
                BodyFactory.addShieldFixtureToBody(body, radius);
                
                shield = new ShieldComponent();
                shield.animTimer = new SimpleTimer(300, true);
                shield.defence = 100f;
                shield.maxRadius = radius;
                shield.color = Color.BLUE;
                
                entity.add(shield);
            }
            return;
        }
        
        
        if (control.defend) {
            if (!shield.growing) {
                //reactivate
                if (shield.animTimer.ratio() >= 0.3f)
                    shield.animTimer.flipRatio();
            }
            shield.growing = true;
        } else {
            if (shield.growing) {
                //release
                shield.animTimer.flipRatio();
            }
            shield.growing = false;
        }
        
        if (shield.growing) {
            //charge
            shield.radius = shield.maxRadius * shield.animTimer.ratio();
        } else {
            //discharge
            shield.radius = shield.maxRadius * (1 - shield.animTimer.ratio());
        }
        
        //activate
        shield.active = shield.radius == shield.maxRadius;
        
        
        if (shield.radius <= 0) {
            Body body = entity.getComponent(PhysicsComponent.class).body;
            Fixture circleFixture = body.getFixtureList().get(body.getFixtureList().size-1);
            body.destroyFixture(circleFixture);
            
            entity.remove(ShieldComponent.class);
        }
    }
    
    private void exitVehicle(Entity vehicleEntity, ControllableComponent control) {
        //action timer
        if (!control.timerVehicle.tryEvent())
            return;
        control.changeVehicle = false;
        
        Entity characterEntity = Mappers.vehicle.get(vehicleEntity).driver;
        
        // create body and set position near vehicle
        Vector2 vehiclePosition = Mappers.transform.get(vehicleEntity).pos;
        Vector2 playerPosition = vehiclePosition.add(MyMath.vector(MathUtils.random(360) * MathUtils.degRad, offsetDist));
        Body body = BodyFactory.createPlayerBody(0, 0, characterEntity);
        body.setTransform(playerPosition, body.getAngle());
        body.setLinearVelocity(0, 0);
        Mappers.physics.get(characterEntity).body = body;
        
        //transfer focus and controls to character
        CameraFocusComponent cameraFocus = (CameraFocusComponent) ECSUtil.transferComponent(vehicleEntity, characterEntity, CameraFocusComponent.class);
        if (cameraFocus != null) {
            cameraFocus.zoomTarget = engineCFG.defaultZoomCharacter;
        }
        ECSUtil.transferComponent(vehicleEntity, characterEntity, ControlFocusComponent.class);
        ECSUtil.transferComponent(vehicleEntity, characterEntity, AIComponent.class);
        ECSUtil.transferComponent(vehicleEntity, characterEntity, ControllableComponent.class);
        
        // remove reference
        Mappers.vehicle.get(vehicleEntity).driver = null;
        
        // add player back into world
        getEngine().addEntity(characterEntity);
    }
    
    
    //region transition todo: move to ScreenTransitionSystem?
    private void takeOffPlanet(Entity entity) {
        if (Mappers.screenTrans.get(entity) != null)
            return;
        
        ScreenTransitionComponent screenTrans = new ScreenTransitionComponent();
        screenTrans.takeOffStage = ScreenTransitionComponent.TakeOffAnimStage.screenEffectFadeIn;
        screenTrans.timer = new SimpleTimer(entityCFG.shrinkGrowAnimTime, true);
        screenTrans.animInterpolation = Interpolation.pow2;
        entity.add(screenTrans);
        
        Gdx.app.log(this.getClass().getSimpleName(), "takeOffPlanet: " + Misc.objString(entity));
    }
    
    private void beginLandOnPlanet(Entity entity, Entity planet) {
        if (Mappers.screenTrans.get(entity) != null)
            return;
        
        if (planet == null) {
            Gdx.app.error(this.getClass().getSimpleName(), "can not land on null planet");
            return;
        }
        
        ScreenTransitionComponent screenTrans = new ScreenTransitionComponent();
        screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.shrink;//begin animation
        screenTrans.planet = planet;
        screenTrans.timer = new SimpleTimer(entityCFG.shrinkGrowAnimTime, true);
        screenTrans.animInterpolation = Interpolation.sineIn;
        entity.add(screenTrans);
        
        Gdx.app.log(this.getClass().getSimpleName(), "beginLandOnPlanet: " + Misc.objString(entity));
    }
    
    private Entity getPlanetNearPosition(Vector2 pos) {
        for (Entity planet : planets) {
            Vector2 planetPos = Mappers.transform.get(planet).pos;
            TextureComponent planetTex = Mappers.texture.get(planet);
            if (pos.dst(planetPos) <= planetTex.texture.getWidth() * 0.5 * planetTex.scale) {
                return planet;
            }
        }
        
        return null;
    }
    //endregion
    
    
    //region combat
    private void manageGrowCannon(Entity entity, ControllableComponent control, TransformComponent transform, GrowCannonComponent growCannon, Body body) {
        //TODO: clean up
        if (growCannon == null) {
            return;
        }
        
        if (growCannon.isCharging) {
            //update position to be in front of ship
            //Rectangle bounds = entity.getComponent(PhysicsComponent.class).poly.getBoundingRectangle();
            float offset = 2;//Math.max(bounds.getWidth(), bounds.getHeight()) + growCannon.maxSize;
            TransformComponent transformComponent = growCannon.projectile.getComponent(TransformComponent.class);
            transformComponent.pos.set(MyMath.vector(transform.rotation, offset).add(transform.pos));
            transformComponent.rotation = transform.rotation;
            
            //accumulate size
            growCannon.size = growCannon.growRateTimer.ratio() * growCannon.maxSize;
            growCannon.size = MathUtils.clamp(growCannon.size, 1, growCannon.maxSize);
            growCannon.projectile.getComponent(TextureComponent.class).scale = growCannon.size * engineCFG.entityScale;
            //growCannon.projectile.getComponent(PhysicsComponent.class).poly.setScale(growCannon.size, growCannon.size);
            
            //damage modifier
            DamageComponent damageComponent = growCannon.projectile.getComponent(DamageComponent.class);
            damageComponent.damage = growCannon.size * growCannon.baseDamage;
            if (growCannon.size == growCannon.maxSize) {
                damageComponent.damage *= 1.5;//bonus damage for maxed out
            }
            
            //release
            if (!control.attack) {
                Vector2 vec = MyMath.vector(transform.rotation, growCannon.velocity).add(body.getLinearVelocity());
                body.setLinearVelocity(vec);
                ExpireComponent expire = new ExpireComponent();
                expire.time = 5;
                growCannon.projectile.add(expire);
                
                growCannon.isCharging = false;
                growCannon.projectile = null;
            }
        } else {
            if (control.attack) {
                CannonComponent test = new CannonComponent();
                test.size = 1;
                growCannon.projectile = EntityFactory.createMissile(transform, test, entity);
                growCannon.projectile.remove(ExpireComponent.class);
                growCannon.projectile.getComponent(DamageComponent.class).source = entity;
                growCannon.isCharging = true;
                growCannon.growRateTimer.reset();
                
                getEngine().addEntity(growCannon.projectile);
            }
        }
    }
    
    private void fireCannon(TransformComponent transform, CannonComponent cannon, Entity owner) {
        if (GameScreen.isDebugMode) {
            //Cheat for debug: fast firing and infinite ammo
            cannon.curAmmo = cannon.maxAmmo;
            //cannon.timerFireRate.setLastEvent(0);
        }
        
        //check if can fire before shooting
        if (!(cannon.curAmmo > 0 && cannon.timerFireRate.canDoEvent()))
            return;
        
        //reset timer if ammo is full, to prevent instant recharge on next shot
        if (cannon.curAmmo == cannon.maxAmmo) {
            cannon.timerRechargeRate.reset();
        }
        
        //create missile
        cannon.anchorVec.setAngleRad(transform.rotation);
        cannon.aimAngle = transform.rotation;
        Entity missile = EntityFactory.createMissile(transform, cannon, owner);
        getEngine().addEntity(missile);
        
        //subtract ammo
        --cannon.curAmmo;
        
        //reset timer
        cannon.timerFireRate.reset();
    }
    
    private static void refillAmmo(CannonComponent cannon) {
        if (cannon.curAmmo < cannon.maxAmmo && cannon.timerRechargeRate.canDoEvent()) {
            cannon.curAmmo++;
            cannon.timerRechargeRate.reset();
        }
    }
    //endregion
    
}
