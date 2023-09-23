package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class ItemComponent implements Component {

    public enum Resource {
        NICKEL(0.1f, new Color(192f/255f, 192f/255f, 192f/255f, 1)),
        IRON(0.2f, new Color(128f/255f, 128f/255f, 128f/255f, 1)),
        GOLD(0.15f, new Color(1f, 128f/255f, 128f/255f, 1)),
        PLATINUM(0.05f, new Color(214f/255f, 214f/255f, 214f/255f, 1)),
        RUBY(0.08f, new Color(220f/255f, 20f/255f, 60f/255f, 1)),
        SAPPHIRE(0.08f, new Color(0f, 0f, 186f/255f, 1)),
        EMERALD(0.07f, new Color(0, 128f/255f, 0, 1)),
        DIAMOND(0.02f, new Color(1, 1, 1, 1)),
        WATER(0.25f, new Color(0, 0, 1, 1));

        private final float dropChance;
        private final Color color;

        Resource(float dropChance, Color color) {
            this.dropChance = dropChance;
            this.color = color;
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
