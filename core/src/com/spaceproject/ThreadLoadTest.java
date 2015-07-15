package com.spaceproject;

import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.systems.RenderingSystem;

public class ThreadLoadTest extends Thread {
	
	RenderingSystem render;
	/*
	SpaceBackgroundTile[][] spaceBackground;
	Vector2 tile;
	
	public ThreadLoadTest(SpaceBackgroundTile[][] spaceBackground, Vector2 currentCenterTile) {
		this.spaceBackground = spaceBackground;
		this.tile = currentCenterTile;
	}*/
	
	public ThreadLoadTest(RenderingSystem sys){
		this.render = sys;
	}

	public void run() {
		
		synchronized (render.spaceBackground) {
		TransformComponent pos = render.playerEntity.getComponent(TransformComponent.class);
		Vector2 tile = render.getTilePos(pos.pos.x, pos.pos.y);
		
		render.spaceBackground[0][0] = new SpaceBackgroundTile((int)tile.x - 1, (int)tile.y - 1, 0.5f); // left, bottom
		render.spaceBackground[0][1] = new SpaceBackgroundTile((int)tile.x - 1, (int)tile.y, 	   0.5f); // left, center
		render.spaceBackground[0][2] = new SpaceBackgroundTile((int)tile.x - 1, (int)tile.y + 1, 0.5f); // left, top
								
		render.spaceBackground[1][0] = new SpaceBackgroundTile((int)tile.x,	 (int)tile.y - 1, 0.5f); // center, bottom
		render.spaceBackground[1][1] = new SpaceBackgroundTile((int)tile.x, 	 (int)tile.y,     0.5f); // center, center
		render.spaceBackground[1][2] = new SpaceBackgroundTile((int)tile.x, 	 (int)tile.y + 1, 0.5f); // center, top
		
		render.spaceBackground[2][0] = new SpaceBackgroundTile((int)tile.x + 1, (int)tile.y - 1, 0.5f); // right, bottom
		render.spaceBackground[2][1] = new SpaceBackgroundTile((int)tile.x + 1, (int)tile.y, 	   0.5f); // right, center
		render.spaceBackground[2][2] = new SpaceBackgroundTile((int)tile.x + 1, (int)tile.y + 1, 0.5f); // right, top
		}
		
		/*
		while (true) {
			System.out.println("thread!");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}*/
		
	}
}
