//Source: github.com/bitlush/android-arbitrary-quadrilaterals-in-opengl-es-2-0
attribute vec2 a_Position;
attribute vec3 a_Region;
varying vec3 v_Region;
uniform mat3 u_World;

void main(){
   v_Region = a_Region;
   vec3 xyz = u_World * vec3(a_Position, 1);
   gl_Position = vec4(xyz.xy, 0, 1);
}