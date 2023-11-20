package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.spaceproject.math.Physics;

public class LaserComponent implements Component {

   public LaserComponent(float wavelength, float maxDist, float damage, float power) {
      this.wavelength = wavelength;
      frequency = (float) Physics.wavelengthToFrequency(wavelength);
      int[] rgb = Physics.wavelengthToRGB(wavelength, 1);
      color = new Color(rgb[0]/255f, rgb[1]/255f, rgb[2]/255f, 1);
      this.maxDist = maxDist;
      this.damage = damage;
      this.power = power;
   }

   public enum State {
      off, on;
   }

   public State state = State.off;

   public float maxDist;

   public float damage;

   public Color color;

   public float wavelength; //in nanometers

   public float frequency;

   public float power; // [0-1] where 1 = full output

}
