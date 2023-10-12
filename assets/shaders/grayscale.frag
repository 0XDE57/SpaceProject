#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_blend;

void main() {
    vec4 c = v_color * texture2D(u_texture, v_texCoords);
    //CIE weighted avg color per pixel
    float grey = (0.21 * c.r + 0.72 * c.g + 0.07 * c.b) / 3.0;
    //blend between gray and actual color
    gl_FragColor = mix(c, vec4(grey, grey, grey, c.a), u_blend);
}