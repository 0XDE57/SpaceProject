package com.spaceproject.screens.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;
import com.spaceproject.utility.MyMath;


public class Test3DScreen extends ScreenAdapter {

    CameraInputController camController;
    PerspectiveCamera cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    //OrthographicCamera cam = new OrthographicCamera();
    SpriteBatch batch = new SpriteBatch();
    ModelBatch modelBatch = new ModelBatch();

    Texture t = TextureFactory.generateCharacter();
    Texture test = TextureFactory.createTestTile();
    Thing ship3d;
    Sprite front, back;

    Texture combinedTex;

    public Model model;
    public ModelInstance instance;


    DecalBatch decalBatch;
    Decal shipDecalA, shipDecalB, shipDecalC, shipDecalD;
    public Test3DScreen() {
        cam.position.set(0, 0, 100/*350*/);
        cam.lookAt(0, 0, 0);
        //cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 1f;
        cam.far = 400f;
        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);

        //create ship textures
        Texture shipTop = TextureFactory.generateShip(123, 20);
        Texture shipBottom = TextureFactory.generateShipUnderSide(123, 20);

        //combine textures
        int width = shipTop.getWidth(), height = shipTop.getHeight();
        Pixmap pixmap = new Pixmap(width, height*2, Pixmap.Format.RGBA8888);
        pixmap.drawPixmap(shipTop.getTextureData().consumePixmap(), 0, 0);
        pixmap.drawPixmap(shipBottom.getTextureData().consumePixmap(),0, height);
        combinedTex = new Texture(pixmap);

        /*//put combined texture into atlas
        TextureAtlas texAtlas = new TextureAtlas();
        texAtlas.addRegion("front", new TextureRegion(combinedTex, 0, 0, width, height));
        texAtlas.addRegion("back", new TextureRegion(combinedTex, 0, height, width, height));
        front = texAtlas.createSprite("front");
        back = texAtlas.createSprite("back");
        */

        //create 3D ship with front and back texture
        front = new Sprite(combinedTex,0,0, width, height);
        back = new Sprite(combinedTex, 0, height, width, height);
        ship3d = new Thing(front, back);


        /*
        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(10f, 10f, 10f,
                new Material(TextureAttribute.createDiffuse(test)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        instance = new ModelInstance(model);


        decalBatch = new DecalBatch(new CameraGroupStrategy(cam));
        shipDecalA = Decal.newDecal(new TextureRegion(shipTop));
        shipDecalB = Decal.newDecal(new TextureRegion(shipTop));
        shipDecalC = Decal.newDecal(new TextureRegion(shipTop));
        shipDecalD = Decal.newDecal(new TextureRegion(shipTop));
        */
        t = combinedTex;
    }

    float rotX = 0, rotY = 0, rotZ = 0;
    @Override
    public void render(float delta) {
        //Gdx.gl20.glClearColor(0,0,0,0);
        //Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glClear(Gdx.gl20.GL_COLOR_BUFFER_BIT | Gdx.gl20.GL_DEPTH_BUFFER_BIT);

        camController.update();
        cam.update();
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            cam.position.x += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            cam.position.x -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            cam.position.y += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            cam.position.y -= 1f;
        }



        //Texture t = test.getComponent(TextureComponent.class).texture;
        //Vector3 pos = test.getComponent(TransformComponent.class).pos;
        batch.setProjectionMatrix(cam.combined);
        //batch.setTransformMatrix(cam.combined);
        batch.begin();
        batch.draw(combinedTex, -100,-100,50,50);
        batch.draw(t, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);




        float width = t.getWidth();
        float height = t.getHeight();
        float originX = width * 0.5f; //center
        float originY = height * 0.5f; //center
        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight()-Gdx.input.getY();
        float scale = SpaceProject.entitycfg.renderScale;

        //draw texture
        batch.draw(t, (x - originX), (y - originY),
                originX, originY,
                width, height,
                scale, scale,
                0,
                0, 0, (int)width, (int)height, false, false);


        batch.end();

        modelBatch.begin(cam);
        //modelBatch.render(instance);
        modelBatch.render(ship3d);
        modelBatch.end();

        //ship3d.worldTransform.rotate(Vector3.X, 90 * delta);
        //ship3d.worldTransform.rotate(Vector3.Y, 60 * delta);
        //ship3d.worldTransform.rotate(Vector3.Z, 90 * delta);
        ship3d.worldTransform.setToRotation(Vector3.Z, MyMath.angleTo(
                (int) Gdx.graphics.getWidth()/2,//ship3d.worldTransform.getTranslation(Vector3.X).x,
                (int)Gdx.graphics.getHeight()/2,//ship3d.worldTransform.getTranslation(Vector3.Y).y,
                Gdx.input.getX(),
                Gdx.graphics.getHeight()-Gdx.input.getY())* MathUtils.radDeg);

        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            rotX+=10f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            rotX-=10f;
        }
        //ship3d.worldTransform.setToRotation(Vector3.X, rotX);
        ship3d.worldTransform.rotate(Vector3.X, rotX);




        /*
        shipDecalA.setPosition(0, 5, 0);
        //shipDecalA.rotateX(shipDecalA.getX() + 1f);
        //shipDecalA.rotateY(shipDecalA.getY() + 0.2f);
        //shipDecalA.rotateZ(shipDecalA.getZ() + 1f);

        shipDecalA.setRotationX(0);
        shipDecalA.setRotationY(0);
        shipDecalA.setRotationZ(0);
        shipDecalA.rotateZ(MyMath.angleTo(
                (int)shipDecalA.getPosition().x,
                (int)shipDecalA.getPosition().y,
                Gdx.input.getX(),
                Gdx.graphics.getHeight()-Gdx.input.getY())* MathUtils.radDeg
        );

        //shipDecalA.rotateY(rotY+=1f);
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            rotX+=10f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            rotX-=10f;
        }
        shipDecalA.rotateX(rotX);
        //shipDecalA.rotateX(rotX+=30f);


        decalBatch.add(shipDecalA);

        shipDecalB.setPosition(30, 5, 0);
        shipDecalB.rotateX(shipDecalB.getX() + 1f);
        decalBatch.add(shipDecalB);

        shipDecalC.setPosition(60, 5, 0);
        shipDecalC.rotateY(shipDecalC.getY() + 1f);
        decalBatch.add(shipDecalC);

        shipDecalD.setPosition(90, 5, 0);
        shipDecalD.rotateZ(shipDecalD.getZ() + 1f);
        decalBatch.add(shipDecalD);
        decalBatch.flush();
        //cam.update();
        */

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            MyScreenAdapter.game.setScreen(new TitleScreen(MyScreenAdapter.game));
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void hide() {
        super.hide();
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        batch.dispose();
    }
}


//xoppa.github.io/blog/a-simple-card-game/
class Thing extends Renderable {

    public Thing(Sprite front, Sprite back) {

        material = new Material(
                TextureAttribute.createDiffuse(front.getTexture()),
                new BlendingAttribute(false, 1f)
                //,FloatAttribute.createAlphaTest(0.5f)
        );

        float scale = 10;
        float height = scale * front.getHeight()/front.getWidth();

        front.setSize(scale, height);
        back.setSize(scale, height);

        front.setPosition(-front.getWidth() * 0.5f, -front.getHeight() * 0.5f);
        back.setPosition(-back.getWidth() * 0.5f, -back.getHeight() * 0.5f);

        float[] vertices = convert(front.getVertices(), back.getVertices());
        short[] indices = new short[] { 0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4 };

        // FIXME: this Mesh needs to be disposed
        meshPart.mesh = new Mesh(true, 8, 12, VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
        meshPart.mesh.setVertices(vertices);
        meshPart.mesh.setIndices(indices);
        meshPart.offset = 0;
        meshPart.size = meshPart.mesh.getNumIndices();
        meshPart.primitiveType = GL20.GL_TRIANGLES;
        meshPart.update();
    }

    private static float[] convert(float[] front, float[] back) {
        return new float[] {
                front[Batch.X2], front[Batch.Y2], 0, 0, 0, 1, front[Batch.U2], front[Batch.V2],
                front[Batch.X1], front[Batch.Y1], 0, 0, 0, 1, front[Batch.U1], front[Batch.V1],
                front[Batch.X4], front[Batch.Y4], 0, 0, 0, 1, front[Batch.U4], front[Batch.V4],
                front[Batch.X3], front[Batch.Y3], 0, 0, 0, 1, front[Batch.U3], front[Batch.V3],

                back[Batch.X1], back[Batch.Y1], 0, 0, 0, -1, back[Batch.U1], back[Batch.V1],
                back[Batch.X2], back[Batch.Y2], 0, 0, 0, -1, back[Batch.U2], back[Batch.V2],
                back[Batch.X3], back[Batch.Y3], 0, 0, 0, -1, back[Batch.U3], back[Batch.V3],
                back[Batch.X4], back[Batch.Y4], 0, 0, 0, -1, back[Batch.U4], back[Batch.V4]
        };
    }



}
