//Source: github.com/bitlush/android-arbitrary-quadrilaterals-in-opengl-es-2-0
//OpenGL ES 2.0 support: github.com/libgdx/libgdx/wiki/Opengl-es-support#precision-modifiers
#ifdef GL_ES
    #define LOW lowp
    #define MED mediump
    #define HIGH highp
    precision mediump float;
#else
    #define MED
    #define LOW
    #define HIGH
#endif

varying vec3 v_Region;
uniform sampler2D u_TextureId;

void main() {
   gl_FragColor = texture2D(u_TextureId, v_Region.xy / v_Region.z);
}