package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class ItemComponent implements Component {

    public enum Resource {
        RED(1, Color.RED, 5, 0.2f, "break1.wav"),
        GREEN(2, Color.GREEN, 6, 0.2f, null),
        BLUE(3, new Color(0.2f, 0.2f, 0.9f, 1), 5, 0.2f, null),
        SILVER(4, new Color(0.97f, 0.97f, 1, 1), 10, 0.2f, null),
        GOLD(5, Color.GOLD, 15, 0.2f, null);

        /*
        NICKEL(0.1f, new Color(192f/255f, 192f/255f, 192f/255f, 1)),
        IRON(0.2f, new Color(128f/255f, 128f/255f, 128f/255f, 1)),
        GOLD(0.15f, new Color(1f, 128f/255f, 128f/255f, 1)),
        PLATINUM(0.05f, new Color(214f/255f, 214f/255f, 214f/255f, 1)),

        RUBY(0.08f, new Color(220f/255f, 20f/255f, 60f/255f, 1)),
        SAPPHIRE(0.08f, new Color(0f, 0f, 186f/255f, 1)),
        EMERALD(0.07f, new Color(0, 128f/255f, 0, 1)),
        DIAMOND(0.02f, new Color(1, 1, 1, 1)),

        WATER(0.25f, new Color(0, 0, 1, 1));
        */

        /*
        ice asteroids
        collect water from ice asteroids
        ship has water tank (upgradable capacity, cooling efficiency)
        water for cooling
        ship hull heat
        ship touch sun -> consumes water for cooling
        ship overheat -> ship explode
        */

        private final int id;
        private final Color color;
        private final int value;
        private final float rarity;// maybe not here but in composition?
        private final String sound; // ID for which sound to play when interacted with (asteroid hit, resource collected)

        Resource(int id, Color color, int value, float rarity, String sound) {
            this.id = id;
            this.color = color;
            this.value = value;
            this.rarity = rarity;
            this.sound = sound;
        }

        public int getId() {
            return id;
        }

        public Color getColor() {
            return color;
        }

        public int getValue() {
            return value;
        }

        public float getRarity() {
            return rarity;
        }

        public String getSound() {
            return sound;
        }

        public static Resource getById(int id) {
            for (Resource resource : Resource.values()) {
                if (resource.getId() == id) {
                    return resource;
                }
            }
            throw new IllegalArgumentException("No enum constant with id " + id);
        }

        public static Resource random() {
            return Resource.values()[MathUtils.random(Resource.values().length - 1)];
        }

    }

    public Resource resource;

}
