package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.CamTargetComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;

public class CameraSystem extends IteratingSystem {
    
    private final OrthographicCamera cam;
    
    private byte zoomLevel = 2;
    private float zoomTarget = 1;
    private final float zoomSpeed = 2;
    private final float zoomSetThreshold = 0.001f;
    private final float minZoom = 0f;
    private final float maxZoom = 100000;
    private final float smoothFollowSpeed = 2f;
    private final float maxOffsetFromTarget = 9f;
    private final Vector2 offsetFromTarget = new Vector2();
    private final Vector3 tempVec = new Vector3();
    
    private ImmutableArray<Entity> focalPoints;
    
    public CameraSystem() {
        super(Family.all(CameraFocusComponent.class, TransformComponent.class).get());
        cam = MyScreenAdapter.cam;
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        focalPoints = engine.getEntitiesFor(Family.all(CamTargetComponent.class, TransformComponent.class).get());
    }
    
    @Override
    public void processEntity(Entity entity, float delta) {
        CameraFocusComponent cameraFocus = Mappers.camFocus.get(entity);
        Vector2 playerPosition = Mappers.transform.get(entity).pos;
        
        
        if (focalPoints.size() > 0 && cam.zoom < 10) {
            focusCombatCamera(playerPosition, cameraFocus, delta);
        } else {
            //set camera position to entity
            lerpToTarget(playerPosition, delta);
            //lockToTarget(playerPosition);
        }
        
        /*
        //always keep player within viewport
        int padding = 0;
        Rectangle focalWindow = new Rectangle(padding, padding,
                Gdx.graphics.getWidth()-padding, Gdx.graphics.getHeight()-padding);
        Vector3 playerScreenPos = cam.project(new Vector3(playerPosition, 0));
        int edgeTop = Gdx.graphics.getWidth();
        int edgeBottom = 0;
        int edgeLeft = 0;
        int edgeRight = Gdx.graphics.getHeight();
        float distLeft = playerScreenPos.x - edgeLeft;
        float distRight = playerScreenPos.x - edgeRight;
        float distTop = playerScreenPos.y - edgeTop;
        float distBottom = playerScreenPos.y - edgeBottom;
        
        float closestEdge = distLeft;
        if (Math.abs(closestEdge) > Math.abs(distRight)) {
            closestEdge = distRight;
        }
        if (Math.abs(closestEdge) > Math.abs(distTop)) {
            closestEdge = distTop;
        }
        if (Math.abs(closestEdge) > Math.abs(distBottom)) {
            closestEdge = distBottom;
        }
        
        if (playerScreenPos.x < focalWindow.x) {
            cam.position.x += /*playerPosition.x -* (playerScreenPos.x - focalWindow.x);//playerPosition.x - (playerScreenPos.x - edgeLeft);
        }
        if (playerScreenPos.y < focalWindow.y) {
            //cam.position.x = playerPosition.y - focalWindow.y;
        }
        if (playerScreenPos.x > focalWindow.width) {
            //cam.position.x = playerPosition.x - focalWindow.width;
        }
        if (playerScreenPos.y > focalWindow.height) {
            //cam.position.y = playerPosition.y - focalWindow.height;
        }
        
        
        if (!focalWindow.contains(playerScreenPos.x, playerScreenPos.y)) {
            //Gdx.app.debug(this.getClass().getSimpleName(),
            //        "x: " + playerScreenPos.x + " y: " + playerScreenPos.y);// +
                            //" l: " + distLeft + " r: " +
                           // distRight + " t: " + distTop + " b: " + distBottom);
            Gdx.app.debug(this.getClass().getSimpleName(),
                    "x: " + playerScreenPos.x + " y: " + playerScreenPos.y +
                            " entity out of window: " + focalWindow.toString());
            
            //cameraFocus.zoomTarget += 2f * delta;
        }
    */
        
        animateZoom(delta);
        
        cam.update();
    }
    
    public void zoomIn() {
        if (zoomLevel <= 0) return;
        zoomTarget = getZoom(--zoomLevel);
        Gdx.app.debug(this.getClass().getSimpleName(), "zoomIn: " + zoomTarget + " : " + zoomLevel);
    }
    
    public void zoomOut() {
        if (zoomLevel >= 20) return;
        zoomTarget = getZoom(++zoomLevel);
        Gdx.app.debug(this.getClass().getSimpleName(), "zoomOut: " + zoomTarget + " : " + zoomLevel);
    }
    
    public float setZoomToDefault(Entity entity) {
        //EngineConfig engineConfig = SpaceProject.configManager.getConfig(EngineConfig.class);
        if (Mappers.vehicle.get(entity) != null) {
            zoomLevel = 2;//-> 1f : engineConfig.defaultZoomVehicle;
        } else {
            zoomLevel = 1;//-> 0.5f : engineConfig.defaultZoomCharacter;
        }
        zoomTarget = getZoom(zoomLevel);
        Gdx.app.debug(this.getClass().getSimpleName(), "default zoom: " + zoomTarget + " : " + zoomLevel);
        return zoomTarget;
    }
    
    public void setZoomZero() {
        zoomLevel = -1;
        zoomTarget = getZoom(zoomLevel);
        Gdx.app.debug(this.getClass().getSimpleName(), "zoomMIN: " + zoomTarget + " : " + zoomLevel);
    }
    
    /** iter: -1,    0,   1, 2, 3, 4, 5, 6,  7, 8...
     *  fib:    ,    0,   1, 1, 2, 3, 5, 8, 13, 21..
     *  out:   0, 0.25, 0.5, 1, 2, 3, 5, 8, 13, 21.. */
    private static float getZoom(byte level) {
        switch (level) {
            case -1: return 0;
            case 0: return 0.25f;
            case 1: return 0.5f; //default character zoom
            case 2: return 1.0f; //default vehicle zoom
            default: return MyMath.fibonacci(level);
        }
    }
    
    private void lerpToTarget(Vector2 pos, float delta) {
        tempVec.set(pos, 0);
        cam.position.lerp(tempVec, smoothFollowSpeed * delta);
        
        offsetFromTarget.set(pos.x - cam.position.x, pos.y - cam.position.y);
        if (offsetFromTarget.len() > maxOffsetFromTarget) {
            Vector2 clamped = offsetFromTarget.clamp(0, maxOffsetFromTarget);
            cam.position.x = pos.x - clamped.x;
            cam.position.y = pos.y - clamped.y;
        }
    }
    
    private void lockToTarget(Vector2 pos) {
        cam.position.x = pos.x;
        cam.position.y = pos.y;
    }
    
    private void animateZoom(float delta) {
        cam.zoom = MathUtils.lerp(cam.zoom, zoomTarget, zoomSpeed * delta);
    
        //if zoom is close enough, just set it to target
        if (Math.abs(cam.zoom - zoomTarget) < zoomSetThreshold) {
            cam.zoom = zoomTarget;
        }
        
        cam.zoom = MathUtils.clamp(cam.zoom, minZoom, maxZoom);
    }
    
    private void focusCombatCamera(Vector2 playerPosition, CameraFocusComponent cameraFocus,  float delta) {
        Entity targetEntity = focalPoints.first();
        //Gdx.app.log(this.getClass().getSimpleName(), "Combat Focus Cam on: " + Misc.objString(targetEntity));
        //first for now, todo:
        //should be closest not first,
        //actually...should be furthest entity within threshold?
        //eg: if a 3rd or 4rth is within threshold include it in cluster
        //
        
        Vector2 targetPos = Mappers.transform.get(targetEntity).pos;
        Vector2 midpoint = playerPosition.cpy().add(targetPos).scl(0.5f);
        
        //set camera to focal point between targets, lock once acquired.
        lockToTarget(midpoint);
        /*
        if (!focalWindow.contains(playerScreenPos.x, playerScreenPos.y)){
            adjust cam position until player is in
        }
        
        lerpToTarget(midpoint, delta);
        if (cam.position.epsilonEquals(midpoint.x, midpoint.y, 0)) {
            lockToTarget(midpoint);
        }
        }*/
        
        
        //zoom in or out to include focused entities
        //zoom in as close as possible at all times, until entities are near edge of camera
        //threshold of how far apart before ignore, don't lock onto midpoint
        //camera edge points?
        //Vector3 topLeft = cam.unproject(new Vector3(0, 0, 0));
        //Vector3 bottomRight = cam.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0));
        //Rectangle focalWindow = new Rectangle(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
        
        int padding = 32;
        float percent = 0.8f;
        Rectangle focalWindow = new Rectangle(padding, padding,
                Gdx.graphics.getWidth()-padding, Gdx.graphics.getHeight()-padding);
        Vector3 playerScreenPos = cam.project(new Vector3(playerPosition, 0));
        Vector3 targetScreenPos = cam.project(new Vector3(targetPos, 0));
    
        //zoom out if player or target is outside of camera view
        if (!focalWindow.contains(playerScreenPos.x, playerScreenPos.y)
                || !focalWindow.contains(targetScreenPos.x, targetScreenPos.y)) {
            Gdx.app.debug(this.getClass().getSimpleName(), "entity out of window: " + focalWindow.toString());
            
            zoomTarget += 1f * delta;
            //zoomOut();
        } else {
            if (cam.zoom > 1) {
                Rectangle threshHoldWindow = new Rectangle(0, 0, focalWindow.width * percent, focalWindow.height * percent);
                Vector2 center = new Vector2();
                focalWindow.getCenter(center);
                threshHoldWindow.setCenter(center);
    
                if (threshHoldWindow.contains(playerScreenPos.x, playerScreenPos.y) || threshHoldWindow.contains(targetScreenPos.x, targetScreenPos.y)) {
                    Gdx.app.debug(this.getClass().getSimpleName(), "inner window: " + threshHoldWindow.toString());
        
                    //zoom in if target and player are closer together relative to camera
                    zoomTarget -= 1f * delta;
                   // zoomIn();
                }
            }
        }
    }
    
}
