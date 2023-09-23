package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.MathUtils;

public class ItemComponent implements Component {

    public enum Resource {
        NICKEL(0.1f),
        IRON(0.2f),
        GOLD(0.15f),
        PLATINUM(0.05f),
        RUBY(0.08f),
        SAPPHIRE(0.08f),
        EMERALD(0.07f),
        DIAMOND(0.02f),
        WATER(0.25f);

        private final float dropChance;

        Resource(float dropChance) {
            this.dropChance = dropChance;
        }

        public float getDropChance() {
            return dropChance;
        }

        public static Resource random() {
            return Resource.values()[MathUtils.random(Resource.values().length - 1)];
        }
    }

    public Resource resource;

}
