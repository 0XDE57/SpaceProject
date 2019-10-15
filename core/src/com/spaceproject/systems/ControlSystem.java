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
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.CharacterComponent;
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
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.SimpleTimer;


public class ControlSystem extends IteratingSystem {
    
    private EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    private static EntityConfig entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
    private ImmutableArray<Entity> vehicles;
    private ImmutableArray<Entity> planets;
    
    private float offsetDist = 1.5f;//TODO: dynamic based on ship size
    //TODO: move values to config
    private float strafeAngle = 40 * MathUtils.degRad;
    private float strafeRot = 3f;
    private float faceRotSpeed = 8f;
    private int hyperModeTimeout = 1000;
    
    public ControlSystem() {
        super(Family.all(ControllableComponent.class, TransformComponent.class).one(CharacterComponent.class, VehicleComponent.class).get());
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());
        planets = engine.getEntitiesFor(Family.all(PlanetComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float delta) {
        
        ControllableComponent control = Mappers.controllable.get(entity);
        
        CharacterComponent character = Mappers.character.get(entity);
        if (character != null) {
            controlCharacter(entity, character, control, delta);
            control.canTransition = false;
        }
        
        VehicleComponent vehicle = Mappers.vehicle.get(entity);
        if (vehicle != null) {
            controlShip(entity, vehicle, control, delta);
        }
        
    }
    
    
    //region character controls
    private void controlCharacter(Entity entity, CharacterComponent character, ControllableComponent control, float delta) {
        //players position
        TransformComponent transform = Mappers.transform.get(entity);
        PhysicsComponent physicsComp = Mappers.physics.get(entity);
        
        //make character face mouse/joystick
        float angle = MathUtils.lerpAngle(physicsComp.body.getAngle(), control.angleFacing, faceRotSpeed * delta);
        float impulse = MyMath.getAngularImpulse(physicsComp.body, angle, delta);
        physicsComp.body.applyAngularImpulse(impulse, true);
        
        if (control.moveForward) {
            float walkSpeed = character.walkSpeed * control.movementMultiplier * delta;
            physicsComp.body.applyForceToCenter(MyMath.vector(transform.rotation, walkSpeed), true);
        }
        
        if (control.changeVehicle) {
            enterVehicle(entity, control);
        }
    }
    //endregion
    
    
    //region ship controls
    private void controlShip(Entity entity, VehicleComponent vehicle, ControllableComponent control, float delta) {
        TransformComponent transform = Mappers.transform.get(entity);
        PhysicsComponent physicsComp = Mappers.physics.get(entity);
        DodgeComponent dodgeComp = Mappers.dodge.get(entity);
        ScreenTransitionComponent screenTransComp = Mappers.screenTrans.get(entity);
        ShieldComponent shield = Mappers.shield.get(entity);
    
        boolean canAct = (dodgeComp == null && screenTransComp == null);
        //boolean canMove = dodgeComp == null && screenTransComp == null;
        boolean canShoot = dodgeComp == null && shield == null;
        boolean canDodge = shield == null;

        
        barrelRoll(entity, dodgeComp);
        manageShield(entity, control, transform, shield);
    
        //debug force insta-stop
        if (Gdx.input.isKeyJustPressed(Keys.X))
            physicsComp.body.setLinearVelocity(0, 0);
        if (Gdx.input.isKeyJustPressed(Keys.Z))
            physicsComp.body.setLinearVelocity(physicsComp.body.getLinearVelocity().add(physicsComp.body.getLinearVelocity()));
    
        if (!canAct) {
            return;
        }
    
        HyperDriveComponent hyperDriveComponent = entity.getComponent(HyperDriveComponent.class);
        if (control.actionA) {
            if (hyperDriveComponent == null) {
                if (control.actionACooldownTimer.canDoEvent()) {
                    hyperDriveComponent = new HyperDriveComponent();
                    hyperDriveComponent.coolDownTimer = new SimpleTimer(hyperModeTimeout, true);
                    hyperDriveComponent.velocity.set(MyMath.vector(physicsComp.body.getAngle(), vehicle.hyperSpeed));
                    entity.add(hyperDriveComponent);
    
                    physicsComp.body.setActive(false);
                }
            } else {
                if (hyperDriveComponent.coolDownTimer.canDoEvent()) {
        
                    entity.remove(HyperDriveComponent.class);
        
                    physicsComp.body.setTransform(transform.pos, transform.rotation);
                    physicsComp.body.setLinearVelocity(MyMath.vector(transform.rotation, 0));
                    physicsComp.body.setActive(true);
                    
                    control.actionACooldownTimer.reset();
                }
            }
        }
        //if (control.actionB) { /*doSomething()*/ }
        //if (control.actionC) { /*doSomething()*/}
        
        if (hyperDriveComponent != null) {
            transform.pos.add(hyperDriveComponent.velocity.cpy().scl(delta));
        }
    
    
        //make vehicle face angle from mouse/joystick
        float angle = MathUtils.lerpAngle(physicsComp.body.getAngle(), control.angleFacing, faceRotSpeed * delta);
        float impulse = MyMath.getAngularImpulse(physicsComp.body, angle, delta);
        physicsComp.body.applyAngularImpulse(impulse, true);
    
    
        if (control.moveForward) {
            accelerate(control, physicsComp.body, vehicle, delta);
        }
        if (control.moveBack) {
            decelerate(physicsComp.body, delta);
        }
        
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        float strafe = strafeRot * delta;
        if (control.moveLeft) {
            strafeLeft(vehicle, control, delta, physicsComp, sprite3D, strafe);
            
            if (canDodge && control.alter) {
                dodgeLeft(entity, transform, control);
            }
        }
    
        if (control.moveRight) {
            strafeRight(vehicle, control, delta, physicsComp, sprite3D, strafe);
            
            if (canDodge && control.alter) {
                dodgeRight(entity, transform, control);
            }
        }
    
        if (!control.moveRight && !control.moveLeft) {
            stabilizeRotation(sprite3D, strafe);
        }
        
        
        //fire cannon / attack
        CannonComponent cannon = Mappers.cannon.get(entity);
        if (cannon != null) {
            refillAmmo(cannon);//TODO: ammo should refill on all entities regardless of player presence
            if (control.attack && canShoot) {
                fireCannon(transform, physicsComp, cannon, entity);
            }
        }
        if (canShoot) {
            GrowCannonComponent growCannon = Mappers.growCannon.get(entity);
            manageGrowCannon(entity, control, transform, growCannon, physicsComp.body);
        }
        
        //exit vehicle
        if (control.changeVehicle) {
            exitVehicle(entity, control);
        }
        
        //transition or take off from planet
        if (GameScreen.inSpace()) {
            control.canTransition = canLandOnPlanet(transform.pos);
        } else {
            control.canTransition = true;
        }
        if (control.transition) {
            if (GameScreen.inSpace()) {
                landOnPlanet(entity);
            } else {
                takeOffPlanet(entity);
            }
        }
        
    }
    
    private void stabilizeRotation(Sprite3DComponent sprite3D, float strafe) {
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
    
    private void strafeRight(VehicleComponent vehicle, ControllableComponent control, float delta, PhysicsComponent physicsComp, Sprite3DComponent sprite3D, float strafe) {
        float thrust = vehicle.thrust * control.movementMultiplier * delta;
        physicsComp.body.applyForceToCenter(MyMath.vector(physicsComp.body.getAngle(), thrust).rotate90(-1), true);
        
        //strafe right
        if (sprite3D != null) {
            sprite3D.renderable.angle -= strafe;
            if (sprite3D.renderable.angle < -strafeAngle) {
                sprite3D.renderable.angle = -strafeAngle;
            }
        }
    }
    
    private void strafeLeft(VehicleComponent vehicle, ControllableComponent control, float delta, PhysicsComponent physicsComp, Sprite3DComponent sprite3D, float strafe) {
        float thrust = vehicle.thrust * control.movementMultiplier * delta;
        physicsComp.body.applyForceToCenter(MyMath.vector(physicsComp.body.getAngle(), thrust).rotate90(1), true);
        
        //strafe left
        if (sprite3D != null) {
            sprite3D.renderable.angle += strafe;
            if (sprite3D.renderable.angle > strafeAngle) {
                sprite3D.renderable.angle = strafeAngle;
            }
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
        //bypass position lerp to make dodge feel better/more responsive
        transform.rotation = control.angleFacing;
        body.setAngularVelocity(0);
        body.setTransform(body.getPosition(), control.angleFacing);
        
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
            
            //apply counter force to slow dodge
            PhysicsComponent physicsComp = Mappers.physics.get(entity);
            physicsComp.body.applyLinearImpulse(MyMath.vector(dodgeComp.direction - (float) Math.PI, dodgeComp.force / 2), physicsComp.body.getPosition(), true);
            
            //end anim
            entity.remove(DodgeComponent.class);
        }
        
    }
    
    private void manageShield(Entity entity, ControllableComponent control, TransformComponent transform, ShieldComponent shield) {
        if (shield == null) {
            if (control.defend) {
                //add
                shield = new ShieldComponent();
                shield.animTimer = new SimpleTimer(400, true);
                shield.defence = 100f;
                //Polygon poly = entity.getComponent(PhysicsComponent.class).poly;
                //Rectangle rect = PolygonUtil.getBoundingRectangle(poly.getVertices());
                //float size = Math.min(rect.width, rect.height) * 1.3f;
                float size = entity.getComponent(PhysicsComponent.class).body.getFixtureList().first().getShape().getRadius() * 1.3f;
                shield.maxRadius = size;
                shield.color = Color.BLUE;
                
                entity.add(shield);
            }
            return;
        }
        
        
        if (control.defend) {
            shield.growing = true;
            shield.radius = shield.maxRadius * shield.animTimer.ratio(); //charge
            shield.active = shield.radius == shield.maxRadius; //activate
        } else {
            //release
            shield.active = false;
            if (shield.growing) {
                shield.growing = false;
                //shield.animTime
            }
            //TODO: hookup shrink process to timer
            shield.radius -= 0.5f;
            //shield.animTimer.getInterval();
            //shield.radius = shield.maxRadius * 1-shield.animTimer.ratio();
            if (shield.radius <= 0) {
                entity.remove(ShieldComponent.class);
            }
        }
        
        
    }
    
    private void enterVehicle(Entity characterEntity, ControllableComponent control) {
        //action timer
        if (!control.timerVehicle.canDoEvent())
            return;
        
        control.changeVehicle = false;
        
        //get all vehicles and check if player is close to one(bounds overlap)
        PhysicsComponent playerPhysics = Mappers.physics.get(characterEntity);
        for (Entity vehicle : vehicles) {
            
            //skip vehicle is occupied
            if (Mappers.vehicle.get(vehicle).driver != null) {
                Gdx.app.log(this.getClass().getSimpleName(), "Vehicle [" + Misc.objString(vehicle) + "] already has a driver [" + Misc.objString(Mappers.vehicle.get(vehicle).driver) + "]!");
                continue;
            }
            
            //check if character is near a vehicle
            PhysicsComponent vehiclePhysics = Mappers.physics.get(vehicle);
            if (playerPhysics.body.getPosition().dst(vehiclePhysics.body.getPosition()) < offsetDist) {
                
                //set reference
                Mappers.vehicle.get(vehicle).driver = characterEntity;
                
                // set focus to vehicle
                if (ECSUtil.transferComponent(characterEntity, vehicle, CameraFocusComponent.class)) {
                    MyScreenAdapter.setZoomTarget(1);// zoom out camera, TODO: add pan animation
                }
                
                ECSUtil.transferComponent(characterEntity, vehicle, ControllableComponent.class);
                ECSUtil.transferComponent(characterEntity, vehicle, AIComponent.class);
                ECSUtil.transferComponent(characterEntity, vehicle, ControlFocusComponent.class);
                
                // remove character
                getEngine().removeEntity(characterEntity);
                GameScreen.box2dWorld.destroyBody(characterEntity.getComponent(PhysicsComponent.class).body);
                characterEntity.getComponent(PhysicsComponent.class).body = null;
                
                control.timerVehicle.reset();
                
                return;
            }
        }
    }
    
    private void exitVehicle(Entity vehicleEntity, ControllableComponent control) {
        
        //action timer
        if (!control.timerVehicle.canDoEvent())
            return;
        control.timerVehicle.reset();
        control.changeVehicle = false;
        
        
        Entity characterEntity = Mappers.vehicle.get(vehicleEntity).driver;
        
        // set the player at the position of vehicle
        Vector2 vehiclePosition = Mappers.transform.get(vehicleEntity).pos;
        Body body = BodyFactory.createPlayerBody(0, 0, characterEntity);
        
        
        Vector2 offset = MyMath.vector(MathUtils.random(360) * MathUtils.degRad, offsetDist);//set player next to vehicle
        
        body.setTransform(vehiclePosition.add(offset), body.getAngle());
        body.setLinearVelocity(0, 0);
        Mappers.physics.get(characterEntity).body = body;
        
        
        //set focus to character
        if (ECSUtil.transferComponent(vehicleEntity, characterEntity, CameraFocusComponent.class)) {
            MyScreenAdapter.setZoomTarget(0.5f);// zoom in camera
        }
        
        ECSUtil.transferComponent(vehicleEntity, characterEntity, ControlFocusComponent.class);
        ECSUtil.transferComponent(vehicleEntity, characterEntity, AIComponent.class);
        ECSUtil.transferComponent(vehicleEntity, characterEntity, ControllableComponent.class);
        
        
        // remove reference
        Mappers.vehicle.get(vehicleEntity).driver = null;
        
        // add player back into world
        getEngine().addEntity(characterEntity);
        
        
    }
    //endregion
    
    
    //region transition
    private void takeOffPlanet(Entity entity) {
        if (Mappers.screenTrans.get(entity) != null)
            return;
        
        ScreenTransitionComponent screenTrans = new ScreenTransitionComponent();
        screenTrans.takeOffStage = ScreenTransitionComponent.TakeOffAnimStage.screenEffectFadeIn;
        screenTrans.timer = new SimpleTimer(entityCFG.shrinkGrowAnimTime, true);
        screenTrans.animInterpolation = Interpolation.pow2;
        entity.add(screenTrans);
        
        Gdx.app.log(this.getClass().getSimpleName(), "takeOffPlanet: " + Misc.objString(entity));
        //Misc.printEntity(entity);
    }
    
    private void landOnPlanet(Entity entity) {
        if (Mappers.screenTrans.get(entity) != null)
            return;
        
        Vector2 pos = Mappers.transform.get(entity).pos;
        for (Entity planet : planets) {
            Vector2 planetPos = Mappers.transform.get(planet).pos;
            TextureComponent planetTex = Mappers.texture.get(planet);
            
            if (pos.dst(planetPos) <= planetTex.texture.getWidth() * 0.5 * planetTex.scale) {
                
                ScreenTransitionComponent screenTrans = new ScreenTransitionComponent();
                screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.shrink;//begin animation
                screenTrans.planet = planet;
                screenTrans.timer = new SimpleTimer(entityCFG.shrinkGrowAnimTime, true);
                screenTrans.animInterpolation = Interpolation.sineIn;
                entity.add(screenTrans);
                
                Gdx.app.log(this.getClass().getSimpleName(), "landOnPlanet: " + Misc.objString(entity));
                //Misc.printObjectFields(entity);
                return;
            }
        }
    }
    
    private boolean canLandOnPlanet(Vector2 pos) {
        for (Entity planet : planets) {
            Vector2 planetPos = Mappers.transform.get(planet).pos;
            TextureComponent planetTex = Mappers.texture.get(planet);
            // if player is over planet
            if (pos.dst(planetPos) <= planetTex.texture.getWidth() * 0.5 * planetTex.scale) {
                return true;
            }
        }
        return false;
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
    
    
    private void fireCannon(TransformComponent transform, PhysicsComponent body, CannonComponent cannon, Entity owner) {
        /*
         * Cheat for debug:
         * fast firing and infinite ammo
         */
        boolean cheat = true;
        if (cheat) {
            cannon.curAmmo = cannon.maxAmmo;
            //cannon.timerFireRate.setLastEvent(0);
        }
        
        //check if can fire before shooting
        if (!(cannon.curAmmo > 0 && cannon.timerFireRate.canDoEvent()))
            return;
        
        
        //reset timer if ammo is full, to prevent instant recharge
        if (cannon.curAmmo == cannon.maxAmmo) {
            cannon.timerRechargeRate.reset();
        }
        
        //create missile
        //Vector2 vec = MyMath.vector(transform.rotation, cannon.velocity);//.add(body.body.getLinearVelocity());
        Entity missile = EntityFactory.createMissile(transform, cannon, owner);
        getEngine().addEntity(missile);
        
        
        //subtract ammo
        --cannon.curAmmo;
        
        //reset timer
        cannon.timerFireRate.reset();
    }
    
    private static void refillAmmo(CannonComponent cannon) {
        if (cannon.curAmmo < cannon.maxAmmo && cannon.timerRechargeRate.canDoEvent()) {
            cannon.curAmmo++; //refill ammo
            cannon.timerRechargeRate.reset();
        }
    }
    //endregion
    
    
}
