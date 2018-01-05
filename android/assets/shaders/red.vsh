attribute vec3 a_position;

uniform mat4 u_projTrans;

void main() {
	gl_Position = u_projTrans * vec4(a_position, 1.0);
}
