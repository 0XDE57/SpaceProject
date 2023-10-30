package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;

public class LaserComponent implements Component {

   public enum State {
      off, on;
   }

   public State state = State.off;

   public float maxDist;
   public float damage;
   public Color color;

}
