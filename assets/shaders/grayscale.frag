#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_blend;

void main() {
    vec4 c = v_color * texture2D(u_texture, v_texCoords);
    float grey = (c.r + c.g + c.b) / 3.0; //avg color per pixel
    //blend beteen gray and actual color
    gl_FragColor = mix(c, vec4(grey, grey, grey, c.a), u_blend);
}