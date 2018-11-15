package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Iterator;

public class DropAnim extends TitleAnimation {

    private ArrayList<Drop> drops = new ArrayList<Drop>();

    @Override
    public void render(float delta, ShapeRenderer shape) {

        if (drops.size() < 10 && MathUtils.random(20) == 0) {
            drops.add(new Drop(MathUtils.random(0, Gdx.graphics.getWidth()), MathUtils.random(0, Gdx.graphics.getHeight()), 3, 80f));
        }


        shape.begin(ShapeRenderer.ShapeType.Line);
        for (Iterator<Drop> dropIter = drops.iterator(); dropIter.hasNext();) {
            Drop d = dropIter.next();
            d.render(delta, shape);

            if (d.rings.size() == 0 && d.numRings == d.maxRings)
                dropIter.remove();
        }
        shape.end();

    }

    @Override
    public void resize(int width, int height) { }


    private class Drop {
        int x, y;
        int numRings, maxRings;
        float ringRad;
        ArrayList<Ring> rings;

        Drop(int x, int y, int maxRings, float ringRad) {
            this.x = x;
            this.y = y;
            this.maxRings = maxRings;
            this.ringRad = ringRad;
            rings = new ArrayList<Ring>();
        }

        public void render(float delta, ShapeRenderer shape) {
            if (numRings < maxRings) {
                if (rings.size() == 0) {
                    rings.add(new Ring(ringRad));
                    numRings++;
                } else if (rings.get(rings.size()-1).radius > 10f) {
                    rings.add(new Ring(ringRad));
                    numRings++;
                }
            }

            for (Iterator<Ring> ringIter = rings.iterator(); ringIter.hasNext(); ) {
                Ring r = ringIter.next();
                shape.setColor(0,0,0,1-(r.radius/r.maxRad));
                shape.circle(x, y, r.radius);

                r.radius += 25f * delta;

                if (r.radius > r.maxRad)
                    ringIter.remove();
            }

        }

    }

    private class Ring {
        float radius = 0;
        float maxRad;

        Ring(float maxRadius) {
            maxRad = maxRadius;
        }
    }
}
