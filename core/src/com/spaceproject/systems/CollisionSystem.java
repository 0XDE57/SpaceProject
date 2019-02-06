package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.MissileComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.PolygonUtil;

public class CollisionSystem extends EntitySystem {

	private Engine engine;
	
	private ImmutableArray<Entity> missiles;
	private ImmutableArray<Entity> vehicles;
	private ImmutableArray<Entity> characters;
	private ImmutableArray<Entity> collidables;
	
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;
		
		missiles = engine.getEntitiesFor(Family.all(MissileComponent.class, BoundsComponent.class).get());
		vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class, BoundsComponent.class).get());
		characters = engine.getEntitiesFor(Family.all(CharacterComponent.class, BoundsComponent.class).get());

		//collidables = engine.getEntitiesFor(Family.all(BoundsComponent.class).get());
	}


	
	@Override
	public void update(float delta) {
		/*
		for (Entity eA : collidables) {
			for (Entity eB : collidables) {
				if (eA.equals(eB)) continue;

				BoundsComponent boundsA = Mappers.bounds.get(eA);
				BoundsComponent boundsB = Mappers.bounds.get(eB);
				if (overlaps(boundsA.poly, boundsB.poly)) {
					//if missile & character: damage character, remove missile
					//if missile & ship: damage ship
					//if character & ship: resolve
					//if ship & ship: resolve
				}

			}
		}*/

		
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
							TextureComponent textureComponent = character.getComponent(TextureComponent.class);
							if (textureComponent != null) {
								textureComponent.texture.dispose();
							}
							Gdx.app.log(this.getClass().getSimpleName(), "[" + character + "] killed by: [" + misl.owner + "]");
							engine.removeEntity(character);

						}
						
						
						AIComponent ai = Mappers.AI.get(character);
						if (ai != null) {
							ai.attackTarget = misl.owner;
							ai.state = AIComponent.testState.attack;
							Gdx.app.log(this.getClass().getSimpleName(),"AI attacked");
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
				if (Mappers.missile.get(missle).owner == vehicle) {
					continue;
				}

				//check for shield
				ShieldComponent shieldComp = Mappers.shield.get(vehicle);
				if (shieldComp != null) {
					if (shieldComp.active) {
						Vector2 pos = Mappers.transform.get(vehicle).pos;
						Circle c = new Circle(pos, shieldComp.radius);
						if (PolygonUtil.overlaps(missBounds.poly, c)) {
							vehicle.remove(ShieldComponent.class);
							engine.removeEntity(missle);
							continue;
						}
					}
				}

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
							TextureComponent textureComponent = vehicle.getComponent(TextureComponent.class);
							if (textureComponent != null) {
								textureComponent.texture.dispose();
							}
							engine.removeEntity(vehicle);
							Gdx.app.log(this.getClass().getSimpleName(),"[" + vehicle + "] killed by: [" + misl.owner + "]");
						}

						//remove missile
						missle.getComponent(TextureComponent.class).texture.dispose();
						engine.removeEntity(missle);
					}
				}
			}

		}
	}

	public boolean overlaps(Polygon polyA, Polygon polyB) {

		if (!polyA.getBoundingRectangle().overlaps(polyB.getBoundingRectangle()))
			return false;

		Intersector.MinimumTranslationVector mtv = new Intersector.MinimumTranslationVector();
		boolean overlap = Intersector.overlapConvexPolygons(polyA, polyB, mtv);
		//if (overlap) {
		//transform.pos.add( mtv.normal.x * mtv.depth, mtv.normal.y * mtv.depth );
		//}
		return overlap;
	}
	
}
