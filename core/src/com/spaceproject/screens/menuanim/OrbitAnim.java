package com.spaceproject.screens.menuanim;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.utility.MyMath;

public class OrbitAnim extends MainMenuAnimation {

    Array<OrbitObject> objects;
    public OrbitAnim() {
        objects = new Array<OrbitObject>();
        OrbitObject center = new OrbitObject(null, 0);

        objects.add(center);
        for (int i = 0; i < 5; i++)
            objects.add(new OrbitObject(center, 100 + (i*75)));

    }

    @Override
    public void render(float delta, ShapeRenderer shape) {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.BLACK);
        for (OrbitObject orbit : objects) {
            orbit.update(delta);
            shape.circle(orbit.pos.x, orbit.pos.y, orbit.size);
        }
        shape.end();

        /*
        shape.begin(ShapeRenderer.ShapeType.Line);
        float i = Math.abs(objects.first().angle % MathUtils.PI2)/MathUtils.PI2;
        System.out.println(i);
        shape.setColor(1,1,1, i);
        for (OrbitObject orbit : objects) {
            orbit.update(delta);
            if (orbit.parent != null)
                shape.circle(orbit.parent.pos.x, orbit.parent.pos.y, orbit.distance);
        }
        shape.end();*/
    }
}

class OrbitObject {
    OrbitObject parent;
    Vector2 pos;
    float angle, rotSpeed;
    float distance;
    int size;

    public OrbitObject(OrbitObject parent, float distance) {
        this.parent = parent;
        this.distance = distance;
        angle = MathUtils.random(MathUtils.PI2);
        rotSpeed = MathUtils.random(0.15f, 0.8f);
        if (MathUtils.randomBoolean())
            rotSpeed = -rotSpeed;

        size = MathUtils.random(5, 10);


        if (parent != null)
            pos = MyMath.Vector(angle, distance).add(parent.pos);
        else
            pos = new Vector2(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
    }

    public void update(float delta) {
        angle += rotSpeed * delta;

        if (parent != null) {
            pos = MyMath.Vector(angle, distance).add(parent.pos);
        }
    }
}
