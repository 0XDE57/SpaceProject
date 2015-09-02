package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Intersector;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.MissileComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.VehicleComponent;

public class CollisionSystem extends EntitySystem {

	private Engine engine;
	
	private ComponentMapper<BoundsComponent> boundMap;
	private ComponentMapper<MissileComponent> missileMap;
	
	private ImmutableArray<Entity> missiles;
	private ImmutableArray<Entity> vehicles;
	
	public CollisionSystem() {
		boundMap = ComponentMapper.getFor(BoundsComponent.class);
		missileMap = ComponentMapper.getFor(MissileComponent.class);
	}
	
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;
		
		missiles = engine.getEntitiesFor(Family.all(MissileComponent.class).get());
		vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());
	
	}
	
	@Override
	public void update(float delta) {		
		//check for bullet collision against ships
		for (Entity vehicle : vehicles) {
			for (Entity missle : missiles) {
				//if missle not from self (don't shoot self)
				if (vehicle.getId() != missileMap.get(missle).ownerID) {
					BoundsComponent mis = boundMap.get(missle);
					BoundsComponent veh = boundMap.get(vehicle);
					if (mis.poly.getBoundingRectangle().overlaps(veh.poly.getBoundingRectangle())) {
						if (Intersector.overlapConvexPolygons(mis.poly, veh.poly)){
							//remove missle
							missle.getComponent(TextureComponent.class).texture.dispose();
							engine.removeEntity(missle);
							
							//do damage
							//health -= damage
						}
						
					}
				}
			}
		}
		
	}
	
}
