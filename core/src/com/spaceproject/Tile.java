package com.spaceproject;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.utility.MyMath;

public class Tile implements Comparable<Tile> {
	private static int nextID;
	
	private final int id;
	private String name;
	private float height;
	private Color color;
	
	public Tile(String name, float height, Color color) {
		id = nextID++;
				
		if (name == null) {
			name = "color" + id;
		}
		
		this.name = name;
		this.height = height;
		this.color = color.cpy();		
	}
	
	public int getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public float getHeight() {
		return height;
	}
	
	public void setHeight(float height) {
		this.height = MathUtils.clamp(height, 0, 1);
	}

	@Override
	public int compareTo(Tile o) {
		return (int)Math.signum(o.getHeight() - this.getHeight());
	}
	
	@Override
	public String toString() {
		return String.format("%-5s", MyMath.round(getHeight(),3)).replace(' ', '0')  + " " + getName();
	}

}