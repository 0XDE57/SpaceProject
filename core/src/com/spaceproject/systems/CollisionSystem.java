package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Intersector;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.MissileComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.utility.Mappers;

public class CollisionSystem extends EntitySystem {

	private Engine engine;
	
	private ImmutableArray<Entity> missiles;
	private ImmutableArray<Entity> vehicles;
	private ImmutableArray<Entity> characters;
	
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;
		
		missiles = engine.getEntitiesFor(Family.all(MissileComponent.class, BoundsComponent.class).get());
		vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class, BoundsComponent.class).get());
		characters = engine.getEntitiesFor(Family.all(CharacterComponent.class, BoundsComponent.class).get());
	}
	
	@Override
	public void update(float delta) {		
		
		for (Entity missle : missiles) {
			BoundsComponent missBounds = Mappers.bounds.get(missle);
			
			//check for bullet collision against characters
			for (Entity character : characters) {
				BoundsComponent charBounds = Mappers.bounds.get(character);
				if (missBounds.poly.getBoundingRectangle().overlaps(charBounds.poly.getBoundingRectangle())) {
					if (Intersector.overlapConvexPolygons(missBounds.poly, missBounds.poly)) {						
						
						//do damage
						HealthComponent health = Mappers.health.get(character);
						MissileComponent misl = Mappers.missile.get(missle);
						
						//double chance = MathUtils.random(-misl.damage/10, misl.damage/10); // damage +/- 10%
						health.health -= misl.damage;// + chance;
						
						//remove character (kill)
						if (health.health <= 0) {
							character.getComponent(TextureComponent.class).texture.dispose();
							engine.removeEntity(character);
							//System.out.println("[" + Mappers.vehicle.get(character).id + "] killed by: [" + misl.ownerID + "]");
						}
						
						
						AIComponent ai = Mappers.AI.get(character);
						if (ai != null) {
							ai.attackTarget = misl.owner;
						}
						
						
						//remove missile
						missle.getComponent(TextureComponent.class).texture.dispose();
						engine.removeEntity(missle);
					}
				}
			}
			
			
			//check for bullet collision against ships
			for (Entity vehicle : vehicles) {
				//if missile not from self (don't shoot self)
				if (Mappers.missile.get(missle).owner != vehicle) {					
					BoundsComponent vehBounds = Mappers.bounds.get(vehicle);
					if (missBounds.poly.getBoundingRectangle().overlaps(vehBounds.poly.getBoundingRectangle())) {
						if (Intersector.overlapConvexPolygons(missBounds.poly, vehBounds.poly)) {						
							
							//do damage
							HealthComponent health = Mappers.health.get(vehicle);
							MissileComponent misl = Mappers.missile.get(missle);
							
							//double chance = MathUtils.random(-misl.damage/10, misl.damage/10); // damage +/- 10%
							health.health -= misl.damage;// + chance;
							
							//remove ship (kill)
							if (health.health <= 0) {
								vehicle.getComponent(TextureComponent.class).texture.dispose();
								engine.removeEntity(vehicle);
								System.out.println("[" + vehicle + "] killed by: [" + misl + "]");
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
