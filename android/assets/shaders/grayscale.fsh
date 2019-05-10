#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main() {
  vec4 c = v_color * texture2D(u_texture, v_texCoords);
  float grey = (c.r + c.g + c.b) / 3.0;
  gl_FragColor = vec4(grey, grey, grey, c.a);
}