package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.components.TransformComponent;

public class HUDSystem  extends EntitySystem {
	
	private Matrix4 projectionMatrix = new Matrix4();
	
	private ShapeRenderer shape;
	
	private ComponentMapper<TransformComponent> transMap;
	private ComponentMapper<CannonComponent> canMap;
	
	private ImmutableArray<Entity> mapableObjects;
	private ImmutableArray<Entity> player;
	
	private boolean drawMap = true;
	
	public HUDSystem() {
		transMap = ComponentMapper.getFor(TransformComponent.class);
		canMap = ComponentMapper.getFor(CannonComponent.class);
		
		shape = new ShapeRenderer();
	}
	
	@Override
	public void addedToEngine(Engine engine) {		
		mapableObjects = engine.getEntitiesFor(Family.all(MapComponent.class).get());
		player = engine.getEntitiesFor(Family.one(PlayerFocusComponent.class).get());
	}
	
	@Override
	public void update(float delta) {
		if (Gdx.input.isKeyJustPressed(Keys.M)) {
			drawMap = !drawMap;
			System.out.println("Edge map: " + drawMap);
		}
		
		//set projection matrix so things render using correct coordinates
		//TODO: only needs to be called when screen size changes
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); 
		shape.setProjectionMatrix(projectionMatrix);
		
		//enable transparency
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		shape.begin(ShapeType.Filled);
		
		drawAmmo();
		
		if (drawMap) drawEdgeMap();
		
		shape.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	private void drawAmmo() {
		int posY = 30;
		int posX = Gdx.graphics.getWidth() / 2;
		int padding = 4;
		int indicatorSize = 15;
		
		CannonComponent cannon = canMap.get(player.first());
		
		for (int i = 0; i < cannon.maxAmmo; ++i) {
			shape.setColor(cannon.curAmmo <= i ? Color.RED : Color.WHITE);
			shape.rect((i * (indicatorSize + (padding * 2))) + posX , posY, indicatorSize/2, indicatorSize/2, indicatorSize, indicatorSize, 1, 1, 0);
		}
		
		
	}

	/**
	 * Mark off-screen objects on edge of screen for navigation.
	 */
	private void drawEdgeMap() {
		int markerSizeSmall = 5;
		int markerSizeLarge = 7;
		int padding = 10; //how close to draw from edge of screen (in pixels)
		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();	
		int centerX = width/2;
		int centerY = height/2;
		int verticleEdge = (height - padding * 2) / 2;
		int horizontalEdge = (width - padding * 2) / 2;
		
		shape.setColor(0.15f, 0.5f, 0.9f, 0.9f);
		
		for (Entity mapable : mapableObjects) {
			Vector3 screenPos = transMap.get(mapable).pos.cpy();
			
			//set entity co'ords relative to center of screen
			screenPos.x -= RenderingSystem.getCam().position.x;
			screenPos.y -= RenderingSystem.getCam().position.y;
			
			//skip on screen entities
			if (screenPos.x > -centerX && screenPos.x < centerX && screenPos.y > -centerY && screenPos.y < centerY) {
				continue;
			}
			
			//position to draw marker
			float markerX = 0, markerY = 0; 
			
			//calculate slope of line (y = mx+b)
			float slope = screenPos.y / screenPos.x;
			
			
			if (screenPos.y < 0) {
				//top
				markerX = -verticleEdge/slope;
				markerY = -verticleEdge;
			} else {
				//bottom
				markerX = verticleEdge/slope;
				markerY = verticleEdge;
			}
			
			if (markerX < -horizontalEdge) {
				//left
				markerX = -horizontalEdge;
				markerY = slope * -horizontalEdge;
			} else if (markerX > horizontalEdge) {
				//right
				markerX = horizontalEdge;
				markerY = slope * horizontalEdge;
			}
			
			//set co'ords relative to center screen
			markerX += centerX;
			markerY += centerY;
			
			//draw marker
			shape.circle(markerX, markerY, (screenPos.dst(screenPos.x, screenPos.y, 0, centerX, centerY, 0)) > 2000 ? markerSizeSmall : markerSizeLarge);
			
			//debug line
			//shape.line(centerX, centerY, markerX, markerY);				
		}
		
		/*
		//draw boarders
		Color outer = new Color(1, 1, 1, 0.22f);
		Color inner = new Color(1, 1, 1, 0.05f);
		//left
		shape.rect(0, 0, padding, height, outer, inner, inner, outer);
		//right
		shape.rect(width - padding, 0, padding, height, inner, outer, outer, inner);
		//bottom
		shape.rect(0, 0, width, padding, outer, outer, inner, inner);
		//top
		shape.rect(0, height - padding, width, padding, inner, inner, outer, outer);
		*/
	}
	

}
