package com.spaceproject.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ArbitraryQuadrilateralsActivity extends ScreenAdapter {
    //porting from: github.com/bitlush/android-arbitrary-quadrilaterals-in-opengl-es-2-0


    //private Gdx. GLSurfaceView view;
    int[] status = new int[1];
    //int program;
    FloatBuffer attributeBuffer;
    ShortBuffer indicesBuffer;
    short[] indicesData;
    float[] attributesData;
    private int[] textureIds = new int[1];
    //int textureId;
    int attributePosition;
    int attributeRegion;

    ShaderProgram shader;
    SpriteBatch batch = new SpriteBatch();

    public ArbitraryQuadrilateralsActivity() {

        /*
        view = new Gdx.gl20. GLSurfaceView(this);
        view.setEGLContextClientVersion(2);
        view.setRenderer(this);

        setContentView(view);*/

    }

    @Override
    public void pause () {
    }

    @Override
    public void resume () {
    }

    @Override
    public void render(float delta) {
        Gdx.gl20.glClearColor(1f, 1f, 1f, 1f);
        Gdx.gl20.glClear(Gdx.gl20.GL_COLOR_BUFFER_BIT);

        //Gdx.gl20.glActiveTexture(Gdx.gl20.GL_TEXTURE0);
        //Gdx.gl20.glBindTexture(Gdx.gl20.GL_TEXTURE_2D, textureId);

        drawNonAffine(100, 100, 600, 100, 500, 400, 200, 600);


        attributeBuffer.position(0);
        attributeBuffer.put(attributesData);

        attributeBuffer.position(0);
        Gdx.gl20.glVertexAttribPointer(attributePosition, 2, Gdx.gl20.GL_FLOAT, false, 5 * 4, attributeBuffer);
        Gdx.gl20.glEnableVertexAttribArray(attributePosition);

        attributeBuffer.position(2);
        Gdx.gl20.glVertexAttribPointer(attributeRegion, 3, Gdx.gl20.GL_FLOAT, false, 5 * 4, attributeBuffer);
        Gdx.gl20.glEnableVertexAttribArray(attributeRegion);

        indicesBuffer.position(0);
        Gdx.gl20.glDrawElements(Gdx.gl20.GL_TRIANGLES, 6, Gdx.gl20.GL_UNSIGNED_SHORT, indicesBuffer);
    }

    @Override
    public void show ()  {
        String vertexShaderSource =
                "attribute vec2 a_Position;" +
                        "attribute vec3 a_Region;" +
                        "varying vec3 v_Region;" +
                        "uniform mat3 u_World;" +
                        "void main()" +
                        "{" +
                        "   v_Region = a_Region;" +
                        "   vec3 xyz = u_World * vec3(a_Position, 1);" +
                        "   gl_Position = vec4(xyz.xy, 0, 1);" +
                        "}";

        String fragmentShaderSource =
                "precision mediump float;" +
                        "varying vec3 v_Region;" +
                        "uniform sampler2D u_TextureId;" +
                        "void main()" +
                        "{" +
                        "   gl_FragColor = texture2D(u_TextureId, v_Region.xy / v_Region.z);" +
                        "}";

        attributeBuffer = ByteBuffer.allocateDirect(5 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        attributesData = new float[5 * 4];

        indicesBuffer = ByteBuffer.allocateDirect(6 * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        indicesData = new short[] { 0, 1, 2, 2, 3, 0 };

        indicesBuffer.position(0);
        indicesBuffer.put(indicesData);

        /*
        program = loadProgram(vertexShaderSource, fragmentShaderSource);
        textureId = loadTexture("Grid.png");

        Gdx.gl20.glUseProgram(program);*/
        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(Gdx.files.internal("shaders/quadRotation.vsh"), Gdx.files.internal("shaders/quadRotation.fsh"));
        System.out.println("Shader compiled: " + shader.isCompiled() + ": " + shader.getLog());
        batch.setShader(shader);


        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        float world[] = new float[] {
                2f / width, 0, 0,
                0, 2f / height, 0,
                -1f, -1f, 1
        };


        int uniformWorld = shader.getUniformLocation("u_World");
        int uniformTextureId = shader.getUniformLocation("u_TextureId");

        Gdx.gl20.glUniformMatrix3fv(uniformWorld, 1, false, world, 0);
        Gdx.gl20.glUniform1i(uniformTextureId, 0);

        attributePosition = shader.getUniformLocation("a_Position");
        attributeRegion = shader.getUniformLocation("a_Region");
    }

    public void setFilters(int minFilter, int magFilter) {
        Gdx.gl20.glTexParameterf(Gdx.gl20.GL_TEXTURE_2D, Gdx.gl20.GL_TEXTURE_MIN_FILTER, minFilter);
        Gdx.gl20.glTexParameterf(Gdx.gl20.GL_TEXTURE_2D, Gdx.gl20.GL_TEXTURE_MAG_FILTER, magFilter);
    }

    public void setWrapping(int wrapS, int wrapT) {
        Gdx.gl20.glTexParameteri(Gdx.gl20.GL_TEXTURE_2D, Gdx.gl20.GL_TEXTURE_WRAP_S, wrapS);
        Gdx.gl20.glTexParameteri(Gdx.gl20.GL_TEXTURE_2D, Gdx.gl20.GL_TEXTURE_WRAP_T, wrapT);
    }

    public void drawNonAffine(float bottomLeftX, float bottomLeftY, float bottomRightX, float bottomRightY, float topRightX, float topRightY, float topLeftX, float topLeftY) {
        float ax = topRightX - bottomLeftX;
        float ay = topRightY - bottomLeftY;
        float bx = topLeftX - bottomRightX;
        float by = topLeftY - bottomRightY;

        float cross = ax * by - ay * bx;

        boolean rendered = false;

        if (cross != 0) {
            float cy = bottomLeftY - bottomRightY;
            float cx = bottomLeftX - bottomRightX;

            float s = (ax * cy - ay * cx) / cross;

            if (s > 0 && s < 1) {
                float t = (bx * cy - by * cx) / cross;

                if (t > 0 && t < 1) {
                    //uv coordinates for texture
                    float u0 = 0; // texture bottom left u
                    float v0 = 0; // texture bottom left v
                    float u2 = 1; // texture top right u
                    float v2 = 1; // texture top right v

                    int bufferIndex = 0;

                    float q0 = 1 / (1 - t);
                    float q1 = 1 / (1 - s);
                    float q2 = 1 / t;
                    float q3 = 1 / s;

                    attributesData[bufferIndex++] = bottomLeftX;
                    attributesData[bufferIndex++] = bottomLeftY;
                    attributesData[bufferIndex++] = u0 * q0;
                    attributesData[bufferIndex++] = v2 * q0;
                    attributesData[bufferIndex++] = q0;

                    attributesData[bufferIndex++] = bottomRightX;
                    attributesData[bufferIndex++] = bottomRightY;
                    attributesData[bufferIndex++] = u2 * q1;
                    attributesData[bufferIndex++] = v2 * q1;
                    attributesData[bufferIndex++] = q1;

                    attributesData[bufferIndex++] = topRightX;
                    attributesData[bufferIndex++] = topRightY;
                    attributesData[bufferIndex++] = u2 * q2;
                    attributesData[bufferIndex++] = v0 * q2;
                    attributesData[bufferIndex++] = q2;

                    attributesData[bufferIndex++] = topLeftX;
                    attributesData[bufferIndex++] = topLeftY;
                    attributesData[bufferIndex++] = u0 * q3;
                    attributesData[bufferIndex++] = v0 * q3;
                    attributesData[bufferIndex++] = q3;

                    rendered = true;
                }
            }
        }

        if (!rendered) {
            throw new RuntimeException("Shape must be concave and vertices must be clockwise.");
        }
    }

    @Override
    public void resize (int width, int height) {
        Gdx.gl20.glViewport(0, 0, width, height);
    }

    /*
    private int loadTexture(String assetName) {
        Bitmap bitmap;

        try {
            bitmap = BitmapFactory.decodeStream(getAssets().open(assetName));
        }
        catch (Exception e) {
            throw new RuntimeException("Couldn't load image '" + assetName + "'.", e);
        }

        if (bitmap == null) {
            throw new RuntimeException("Couldn't load image '" + assetName + "'.");
        }

        Gdx.gl20.glGenTextures(1, textureIds, 0);

        int textureId = textureIds[0];

        if (textureId == 0) {
            throw new RuntimeException("Could not generate texture.");
        }

        Gdx.gl20.glBindTexture(Gdx.gl20.GL_TEXTURE_2D, textureId);

        setFilters(Gdx.gl20.GL_LINEAR, Gdx.gl20.GL_LINEAR);
        setWrapping(Gdx.gl20.GL_CLAMP_TO_EDGE, Gdx.gl20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(Gdx.gl20.GL_TEXTURE_2D, 0, bitmap, 0);
        Gdx.gl20.glBindTexture(Gdx.gl20.GL_TEXTURE_2D, 0);

        bitmap.recycle();

        return textureId;
    }

    private int loadProgram(String vertexShaderSource, String fragmentShaderSource) {
        int id = Gdx.gl20.glCreateProgram();

        int vertexShaderId = loadShader(Gdx.gl20.GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShaderId = loadShader(Gdx.gl20.GL_FRAGMENT_SHADER, fragmentShaderSource);

        Gdx.gl20.glAttachShader(id, vertexShaderId);
        Gdx.gl20.glAttachShader(id, fragmentShaderId);
        Gdx.gl20.glLinkProgram(id);
        Gdx.gl20.glDeleteShader(vertexShaderId);
        Gdx.gl20.glDeleteShader(fragmentShaderId);
        Gdx.gl20.glGetProgramiv(id, Gdx.gl20.GL_LINK_STATUS, status, 0);

        if (status[0] == 0) {
            String log = Gdx.gl20.glGetProgramInfoLog(id);

            Gdx.gl20.glDeleteProgram(id);

            throw new RuntimeException("Shader error:" + log);
        }

        return id;
    }

    private int loadShader(int type, String source) {
        int id = Gdx.gl20.glCreateShader(type);

        Gdx.gl20.glShaderSource(id, source);
        Gdx.gl20.glCompileShader(id);
        Gdx.gl20.glGetShaderiv(id, Gdx.gl20.GL_COMPILE_STATUS, status, 0);

        if (status[0] == 0) {
            String log = Gdx.gl20.glGetShaderInfoLog(id);

            Gdx.gl20.glDeleteShader(id);

            throw new RuntimeException("Shader error:" + log);
        }

        return id;
    }*/

    private void checkGlError(String op) {
        int error;

        if ((error = Gdx.gl20.glGetError()) != Gdx.gl20.GL_NO_ERROR) {
            throw new RuntimeException("GL error code: " + error + " for " + op + ".");
        }
    }
}
