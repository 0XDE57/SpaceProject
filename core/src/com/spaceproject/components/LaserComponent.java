package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class LaserComponent implements Component {

   public enum State {
      off, on;
   }

   public State state = State.off;

   public Vector2 a = new Vector2();
   public Vector2 b = new Vector2();
   public float maxLaserDist = 100;

}
