package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class ItemComponent implements Component {

    public enum Resource {
        RED(1, Color.RED, 50, 0.2f, 1.5f, 0.5f, 1.5f, "break0.wav"),
        GREEN(2, Color.GREEN, 60, 0.2f, 3f, 0.5f,1.7f, "break1.wav"),
        BLUE(3, new Color(0.1f, 0.3f, 1f, 1), 50, 0.2f, 1f, 0.4f, 1.3f, "break2.wav"),
        SILVER(4, new Color(0.97f, 0.97f, 1, 1), 100, 0.2f, 0.3f, 0.3f,1f, "break3.wav"),
        GOLD(5, Color.GOLD, 400, 0.2f, 8f, 0.8f, 3, "break4.wav"),
        GLASS(6, new Color(0.5f, 0.5f, 0.5f, 0.5f), 10, 0, 0.1f, 0.05f, 1.5f, "glass.wav");
        //BLACK(7, Color.BLACK, value 9999); // bullets bounce (no damage) and laser absorb (no reflect) but does lots of damage (maybe 1.5x - 2x ?)
        //glass soft, bullets break easy, but laser refracts and passes through and does no damage
        //this forces you to get a laser to mine black!
        //this would allow a multi-material asteroid to have a black outer polygon, and gold inned polygon
        /*
        ROCK()

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
        private final float hardness;
        private final float density;
        private final float refractiveIndex;
        private final String sound; // ID for which sound to play when interacted with (asteroid hit, resource collected)

        Resource(int id, Color color, int value, float rarity, float hardness, float density, float refractiveIndex, String sound) {
            this.id = id;
            this.color = color;
            this.value = value;
            this.rarity = rarity;
            this.hardness = hardness;
            this.density = density;
            this.refractiveIndex = refractiveIndex;
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

        public float getHardness() {
            return hardness;
        }

        public float getDensity() {
            return density;
        }

        public float getRefractiveIndex() {
            return refractiveIndex;
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

        final static Resource[] VALUES = Resource.values();
        
        public static Resource random() {
            return VALUES[MathUtils.random(VALUES.length - 1)];
        }

    }

    public Resource resource;

}
