package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.generation.BodyFactory;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;


public class ShipControlSystem extends IteratingSystem {
    
    private final float offsetDist = 1.5f;//TODO: dynamic based on ship size
    private final float faceRotSpeed = 8f;
    private static final float boostMultiplier = 3.0f;
    
    private final EntityConfig entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
    private ImmutableArray<Entity> planets;
    
    public ShipControlSystem() {
        super(Family.all(ControllableComponent.class, TransformComponent.class, VehicleComponent.class).exclude(ScreenTransitionComponent.class).get());
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
        ControllableComponent control = Mappers.controllable.get(entity);
        VehicleComponent vehicle = Mappers.vehicle.get(entity);
        TransformComponent transformComp = Mappers.transform.get(entity);
        PhysicsComponent physicsComp = Mappers.physics.get(entity);
        
        if (GameScreen.isDebugMode) {
            applyDebugControls(entity, transformComp, physicsComp);
        }
        
        //rotate ship
        faceTarget(control, physicsComp, delta);
    
        if (!canControlShip(entity))
            return;
        
        //movement
        if (control.moveForward) {
            accelerate(control, physicsComp.body, vehicle, delta);
        }
        if (control.moveBack) {
            decelerate(control, physicsComp.body, vehicle, delta);
        }
        if (control.moveLeft) {
            strafeLeft(vehicle, control, physicsComp, delta);
        }
        if (control.moveRight) {
            strafeRight(vehicle, control, physicsComp, delta);
        }
        
        //exit vehicle
        if (control.changeVehicle) {
            boolean canExit = !GameScreen.inSpace();
            if (GameScreen.isDebugMode) {
                canExit = true;
            }
            
            if (canExit) {
                exitVehicle(entity, control);
            }
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
    
    private static void accelerate(ControllableComponent control, Body body, VehicleComponent vehicle, float delta) {
        float thrust = vehicle.thrust * control.movementMultiplier * delta;
        if (control.boost) {
            thrust *= boostMultiplier;//booost!
        }
        body.applyForceToCenter(MyMath.vector(body.getAngle(), thrust), true);
    }
    
    private static void decelerate(ControllableComponent control, Body body, VehicleComponent vehicle, float delta) {
        float thrust = vehicle.thrust * control.movementMultiplier * delta;
        if (control.boost) {
            thrust *= boostMultiplier;//booost!
        }
        body.applyForceToCenter(MyMath.vector(body.getAngle()-(180*MathUtils.degreesToRadians), thrust), true);
        /*
        float stopThreshold = 0.2f;
        if (body.getLinearVelocity().len() <= stopThreshold) {
            //completely stop if moving really slowly
            body.setLinearVelocity(0, 0);
        } else {
            //add thrust opposite direction of velocity to slow down ship
            float thrust = body.getLinearVelocity().len() * 20 * delta;
            float angle = body.getLinearVelocity().angleRad() - 180 * MathUtils.degRad;
            body.applyForceToCenter(MyMath.vector(angle, thrust), true);
        }*/
    }
    
    private void strafeRight(VehicleComponent vehicle, ControllableComponent control, PhysicsComponent physicsComp, float delta) {
        float thrust = vehicle.thrust * control.movementMultiplier * delta;
        Vector2 force = MyMath.vector(physicsComp.body.getAngle(), thrust).rotate90(-1);
        physicsComp.body.applyForceToCenter(force, true);
    }
    
    private void strafeLeft(VehicleComponent vehicle, ControllableComponent control, PhysicsComponent physicsComp, float delta) {
        float thrust = vehicle.thrust * control.movementMultiplier * delta;
        Vector2 force = MyMath.vector(physicsComp.body.getAngle(), thrust).rotate90(1);
        physicsComp.body.applyForceToCenter(force, true);
    }
    
    private void exitVehicle(Entity vehicleEntity, ControllableComponent control) {
        //action timer
        if (!control.timerVehicle.tryEvent())
            return;
        
        control.changeVehicle = false;
        
        Entity characterEntity = Mappers.vehicle.get(vehicleEntity).driver;
        Gdx.app.log(this.getClass().getSimpleName(), DebugUtil.objString(characterEntity)
                + " exiting vehicle " + DebugUtil.objString(vehicleEntity));
        
        // re-create box2D body and set position near vehicle
        Vector2 vehiclePosition = Mappers.transform.get(vehicleEntity).pos;
        Vector2 playerPosition = vehiclePosition.add(MyMath.vector(MathUtils.random(360) * MathUtils.degRad, offsetDist));
        Body body = BodyFactory.createPlayerBody(0, 0, characterEntity);
        body.setTransform(playerPosition, body.getAngle());
        body.setLinearVelocity(0, 0);
        Mappers.physics.get(characterEntity).body = body;
        
        //transfer focus and controls to character
        ECSUtil.transferControl(vehicleEntity, characterEntity);
        
        //set camera
        if (Mappers.camFocus.get(characterEntity) != null) {
            CameraSystem cameraSystem = getEngine().getSystem(CameraSystem.class);
            cameraSystem.setZoomToDefault(characterEntity);
        }
        
        // remove driver reference from vehicle
        Mappers.vehicle.get(vehicleEntity).driver = null;
        
        // add player back into world
        getEngine().addEntity(characterEntity);
    }
    
    private boolean canControlShip(Entity entity) {
        //don't allow engine activation while shield is active
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null && shield.state == ShieldComponent.State.on) {
            return false;
        }
        //don't allow engine activation while hypderdrive is active
        HyperDriveComponent hyperdrive = Mappers.hyper.get(entity);
        if (hyperdrive != null && hyperdrive.state == HyperDriveComponent.State.on) {
            return false;
        }
        
        return true;
    }
    
    private void applyDebugControls(Entity entity, TransformComponent transformComp, PhysicsComponent physicsComp) {
        //debug force insta-stop
        if (Gdx.input.isKeyJustPressed(Keys.X)) {
            physicsComp.body.setLinearVelocity(0, 0);
            physicsComp.body.setActive(true);
            physicsComp.body.setTransform(transformComp.pos, transformComp.rotation);
            
            HyperDriveComponent hyperDrive = Mappers.hyper.get(entity);
            hyperDrive.state = HyperDriveComponent.State.off;
        }
        if (Gdx.input.isKeyJustPressed(Keys.Z)) {
            physicsComp.body.setLinearVelocity(physicsComp.body.getLinearVelocity().add(physicsComp.body.getLinearVelocity()));
        }
    }
    
    //region transition
    private void takeOffPlanet(Entity entity) {
        if (Mappers.screenTrans.get(entity) != null)
            return;
        
        ScreenTransitionComponent screenTrans = new ScreenTransitionComponent();
        screenTrans.takeOffStage = ScreenTransitionComponent.TakeOffAnimStage.screenEffectFadeIn;
        screenTrans.timer = new SimpleTimer(entityCFG.shrinkGrowAnimTime, true);
        screenTrans.animInterpolation = Interpolation.pow2;
        entity.add(screenTrans);
        
        Gdx.app.log(this.getClass().getSimpleName(), "takeOffPlanet: " + DebugUtil.objString(entity));
    }
    
    private void beginLandOnPlanet(Entity entity, Entity planet) {
        if (Mappers.screenTrans.get(entity) != null) {
            Gdx.app.error(this.getClass().getSimpleName(), "transition already in progress, aborting.");
            return;
        }
        if (planet == null) {
            Gdx.app.error(this.getClass().getSimpleName(), "can not land on null planet.");
            return;
        }
        
        ScreenTransitionComponent screenTrans = new ScreenTransitionComponent();
        screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.shrink;//begin animation
        screenTrans.planet = planet;
        screenTrans.timer = new SimpleTimer(entityCFG.shrinkGrowAnimTime, true);
        screenTrans.animInterpolation = Interpolation.sineIn;
        entity.add(screenTrans);
        
        Gdx.app.log(this.getClass().getSimpleName(), "beginLandOnPlanet: " + DebugUtil.objString(entity));
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
    
}
