#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_blend;
uniform float u_invert;

void main() {
    vec4 color = v_color * texture2D(u_texture, v_texCoords);
    if (u_invert > 0.0) {
        //invert for hyperspace
        color.rgb = 1.0 - color.rgb;
    } else {
        //grayscale
        float avg = (color.r + color.g + color.b) / 3.0;
        color = mix(color, vec4(avg, avg, avg, color.a), u_blend);
    }
    gl_FragColor = color;
}