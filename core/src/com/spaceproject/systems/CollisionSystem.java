package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.PolygonUtil;
import com.spaceproject.utility.ResourceDisposer;

//based off of:
//https://gamedevelopment.tutsplus.com/series/how-to-create-a-custom-physics-engine--gamedev-12715
public class CollisionSystem extends EntitySystem {

	private Engine engine;
	
	private ImmutableArray<Entity> collidables;
	
	private Array<CollisionPair> collisionPairs = new Array<CollisionPair>();
	
	private final CollisionPair collisionPair = new CollisionPair();
	private final Vector2 normal = new Vector2();
	private final Vector2 impulse = new Vector2();
	
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;

		collidables = engine.getEntitiesFor(Family.all(BoundsComponent.class, TransformComponent.class).get());
	}
	

	@Override
	public void update(float delta) {
		collisionPairs.clear();
		
		for (int indexA = 0; indexA < collidables.size(); indexA++) {
			for (int indexB = 0; indexB < collidables.size(); indexB++) {
				if (indexA == indexB) continue; //don't compare against self
				
				//don't duplicate compares
				collisionPair.a = indexA;
				collisionPair.b = indexB;
				if (collisionPairs.contains(collisionPair, false))
					continue;
				collisionPairs.add(new CollisionPair(collisionPair));
				

				Entity eA = collidables.get(indexA);
				Entity eB = collidables.get(indexB);
				BoundsComponent boundsA = Mappers.bounds.get(eA);
				BoundsComponent boundsB = Mappers.bounds.get(eB);
				
				Intersector.MinimumTranslationVector mtv = overlaps(boundsA, boundsB);
				if (mtv != null) {
					onCollision(eA, eB);

					
					TransformComponent transformA = Mappers.transform.get(eA);
					TransformComponent transformB = Mappers.transform.get(eB);
					resolveCollision(transformA, transformB, mtv);
					
				}
				
			}
		}
		
	}
	
	private void onCollision(Entity a, Entity b) {
		//TODO: this should just fire an event for other systems to care about
		//if missile & character: damage character, remove missile
		//if missile & ship: damage ship, remove missile
		//if AI: mark attacked
		//if character & ship: resolve (do nothing here)
		//if ship & ship: resolve (do nothing here)

		DamageComponent mA = Mappers.damage.get(a);
		DamageComponent mB = Mappers.damage.get(b);
		HealthComponent hA = Mappers.health.get(a);
		HealthComponent hB = Mappers.health.get(b);
		
		if (mA != null && hB != null) {
			onAttacked(a, b, mA, hB);
		}
		if (mB != null && hA != null) {
			onAttacked(b, a, mB, hA);
		}

	}
	
	
	private void onAttacked(Entity damageEntity, Entity attackedEntity, DamageComponent damageComponent, HealthComponent healthComponent) {
		//TODO: move this. collision/physics doesnt care about damage. a combat system should subscribe to this event.
		if (damageComponent.source == attackedEntity) {
			return;
		}
		
		//check for AI
		AIComponent ai = Mappers.AI.get(attackedEntity);
		if (ai != null) {
			ai.attackTarget = damageComponent.source;
			ai.state = AIComponent.testState.attack;
			Gdx.app.log(this.getClass().getSimpleName(),"AI [" + Misc.objString(attackedEntity) + "] attacked by: [" + Misc.objString(damageComponent.source) + "]");
		}
		
		
		//check for shield
		ShieldComponent shieldComp = Mappers.shield.get(attackedEntity);
		if (shieldComp != null) {
			if (shieldComp.active) {
				Vector2 pos = Mappers.transform.get(attackedEntity).pos;
				Circle c = new Circle(pos, shieldComp.radius);
				BoundsComponent boundsComponent = Mappers.bounds.get(attackedEntity);
				if (PolygonUtil.overlaps(boundsComponent.poly, c)) {
					attackedEntity.remove(ShieldComponent.class);
					ResourceDisposer.dispose(damageEntity);
					engine.removeEntity(damageEntity);
					return;
				}
			}
		}
		
		
		//do damage
		healthComponent.health -= damageComponent.damage;
		
		//remove entity (kill)
		if (healthComponent.health <= 0) {
			ResourceDisposer.dispose(attackedEntity);
			engine.removeEntity(attackedEntity);
			Gdx.app.log(this.getClass().getSimpleName(),"[" + Misc.objString(attackedEntity) + "] killed by: [" + Misc.objString(damageComponent.source) + "]");
		}
		
		//remove missile
		ResourceDisposer.dispose(damageEntity);
		engine.removeEntity(damageEntity);
	}
	
	
	private void resolveCollision(TransformComponent transformA, TransformComponent transformB, Intersector.MinimumTranslationVector mtv) {
		//TODO: this is very broken and primitive
		
		//normal = b - a
		normal.set(transformA.velocity).sub(transformB.velocity);
		float relativeVelocity = normal.dot(mtv.normal);
		
		DebugUISystem.addDebugVec(transformA.pos, mtv.normal.cpy().add(transformA.pos), Color.WHITE);
		
		//don't resolve if velocities are separating (object moving away from each other)
		if (relativeVelocity > 0) return;
		
		//DebugUISystem.addDebugText(Misc.vecString(normal, 1), transformA.pos.x, transformB.pos.y, true);
		
		//calculate impulse
		
		//inverse mass, avoid division of 0 mass
		float invMassA = transformA.mass == 0 ? 0 : 1 / transformA.mass;
		float invMassB = transformB.mass == 0 ? 0 : 1 / transformB.mass;
		
		float restitution = Math.min(transformA.restitution, transformB.restitution);
		
		float impulseScalar = -(1 + restitution) * relativeVelocity;
		impulseScalar /= invMassA + invMassB;
		
		
		//apply impulse
		impulse.set(invMassA * impulseScalar, invMassB * impulseScalar);
		
		
		transformA.velocity.add(invMassA * impulse.x, invMassA * impulse.y);
		transformB.velocity.sub(invMassB * impulse.x, invMassB * impulse.y);
		
		DebugUISystem.addDebugVec(transformA.pos, new Vector2(invMassA * impulse.x, invMassA * impulse.y), Color.BLUE);
		DebugUISystem.addDebugVec(transformB.pos, new Vector2(invMassB * impulse.x, invMassB * impulse.y), Color.RED);
		
		//transformA.pos.add( mtv.normal.x * mtv.depth, mtv.normal.y * mtv.depth);
		//transformB.pos.sub( mtv.normal.x * mtv.depth, mtv.normal.y * mtv.depth);
		
		//keep objects outside of each other
		float percent = 0.2f; // usually 20% to 80%
		float slop = 0.01f; // usually 0.01 to 0.1
		Vector2 c = mtv.normal.scl(Math.max(mtv.depth - slop, 0) / (invMassA + invMassB) * percent);
		DebugUISystem.addDebugVec(transformA.pos, new Vector2(invMassA * c.x, invMassA * c.y), Color.YELLOW);
		DebugUISystem.addDebugVec(transformB.pos, new Vector2(invMassB * c.x, invMassB * c.y), Color.GREEN);
		transformA.pos.add(invMassA * c.x, invMassA * c.y);
		transformB.pos.sub(invMassB * c.x, invMassB * c.y);
		
	}
	
	public Intersector.MinimumTranslationVector overlaps(BoundsComponent boundsA, BoundsComponent boundsB) {
		Polygon polyA = boundsA.poly;
		Polygon polyB = boundsB.poly;
		
		if (!polyA.getBoundingRectangle().overlaps(polyB.getBoundingRectangle()))
			return null;

		Intersector.MinimumTranslationVector mtv = new Intersector.MinimumTranslationVector();
		if (Intersector.overlapConvexPolygons(polyA, polyB, mtv))
			return  mtv;
		
		return null;
	}
	
}

class CollisionPair {
	public int a;
	public int b;
	
	CollisionPair() {}
	
	CollisionPair(CollisionPair copy) {
		this.a = copy.a;
		this.b = copy.b;
	}
	
	@Override
	public boolean equals(Object o) {
		CollisionPair other = (CollisionPair)o;
		return (this.a == other.a && this.b == other.b) || (this.a == other.b && this.b == other.a);
	}
}