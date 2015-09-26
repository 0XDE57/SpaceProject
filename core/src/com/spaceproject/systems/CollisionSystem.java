package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.MissileComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.utility.Mappers;

public class CollisionSystem extends EntitySystem {

	private Engine engine;
	
	private ImmutableArray<Entity> missiles;
	private ImmutableArray<Entity> vehicles;
	
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
				//if missile not from self (don't shoot self)
				if (vehicle.getId() != Mappers.missile.get(missle).ownerID) {
					BoundsComponent mis = Mappers.bounds.get(missle);
					BoundsComponent veh = Mappers.bounds.get(vehicle);
					if (mis.poly.getBoundingRectangle().overlaps(veh.poly.getBoundingRectangle())) {
						if (Intersector.overlapConvexPolygons(mis.poly, veh.poly)) {						
							
							//do damage
							HealthComponent health = Mappers.health.get(vehicle);
							MissileComponent misl = Mappers.missile.get(missle);
							
							double chance = MathUtils.random(-misl.damage/10, misl.damage/10); // damage +/- 10%
							health.health -= misl.damage + chance;
							
							//remove ship (kill)
							if (health.health <= 0) {
								vehicle.getComponent(TextureComponent.class).texture.dispose();
								engine.removeEntity(vehicle);
								System.out.println("[" + vehicle.getId() + "] killed by: [" + misl.ownerID + "]");
							}
							
							//remove missile
							missle.getComponent(TextureComponent.class).texture.dispose();
							engine.removeEntity(missle);
						}
					}
				}
			}
		}
	}
	
}
