package com.spaceproject;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;

/**
 * A copy of @IteratingSystem with support for engine access.
 */
public abstract class CustomIteratingSystem extends EntitySystem {
	protected Engine engine;
	
	private Family family;
	private ImmutableArray<Entity> entities;

	/**
	 * Instantiates a system that will iterate over the entities described by the Family.
	 * @param family The family of entities iterated over in this System
	 */
	public CustomIteratingSystem (Family family) {
		this(family, 0);
	}

	/**
	 * Instantiates a system that will iterate over the entities described by the Family, with a specific priority.
	 * @param family The family of entities iterated over in this System
	 * @param priority The priority to execute this system with (lower means higher priority)
	 */
	public CustomIteratingSystem (Family family, int priority) {
		super(priority);

		this.family = family;
	}

	@Override
	public void addedToEngine (Engine engine) {
		this.engine = engine;
		entities = engine.getEntitiesFor(family);
	}

	@Override
	public void removedFromEngine (Engine engine) {
		entities = null;
	}

	@Override
	public void update (float deltaTime) {
		for (int i = 0; i < entities.size(); ++i) {
			processEntity(entities.get(i), deltaTime);
		}
	}

	/**
	 * @return set of entities processed by the system
	 */
	public ImmutableArray<Entity> getEntities () {
		return entities;
	}

	/**
	 * This method is called on every entity on every update call of the EntitySystem. Override this to implement your system's
	 * specific processing.
	 * @param entity The current Entity being processed
	 * @param deltaTime The delta time between the last and current frame
	 */
	protected abstract void processEntity (Entity entity, float deltaTime);
}
