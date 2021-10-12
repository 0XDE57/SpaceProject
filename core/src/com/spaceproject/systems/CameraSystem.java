package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;

public class CameraSystem extends IteratingSystem {
    
    private final OrthographicCamera cam;
    
    private final float zoomSpeed = 3;
    private final float zoomSetThreshold = 0.2f;
    private final float minZoom = 0.001f;
    private final float maxZoom = 100000;
    private final float smoothFollowSpeed = 2f;
    private final float maxOffsetFromTarget = 9f;
    private final Vector2 offsetFromTarget = new Vector2();
    private final Vector3 tempVec = new Vector3();
    
    public CameraSystem() {
        super(Family.all(CameraFocusComponent.class, TransformComponent.class).get());
        cam = MyScreenAdapter.cam;
    }
    
    @Override
    public void processEntity(Entity entity, float delta) {
        CameraFocusComponent cameraFocus = Mappers.camFocus.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        
        //set camera position to entity
        lerpToTarget(transform.pos, delta);
        //lockToTarget(transform.pos);
        
        animateZoom(cameraFocus.zoomTarget, delta);
        
        cam.update();
    }
    
    private void lerpToTarget(Vector2 pos, float delta) {
        tempVec.set(pos, 0);
        cam.position.lerp(tempVec, smoothFollowSpeed * delta);
    
        //cam.position.x += (pos.x - cam.position.x) * smoothFollowSpeed * delta;
        //cam.position.y += (pos.y - cam.position.y) * smoothFollowSpeed * delta;
        
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
    
    private void animateZoom(float zoomTarget, float delta) {
        if (cam.zoom != zoomTarget) {
            //zoom in/out
            float scaleSpeed = zoomSpeed * delta;
            cam.zoom += (cam.zoom < zoomTarget) ? scaleSpeed : -scaleSpeed;
            
            //if zoom is close enough, just set it to target
            if (Math.abs(cam.zoom - zoomTarget) < zoomSetThreshold) {
                cam.zoom = zoomTarget;
            }
        }
        cam.zoom = MathUtils.clamp(cam.zoom, minZoom, maxZoom);
    }
    
}
