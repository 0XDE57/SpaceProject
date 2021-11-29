varying vec4 v_color;
varying vec2 v_texCoord0;

uniform sampler2D u_sampler2D;
uniform float u_shift;

void main() {
	vec4 color = texture2D(u_sampler2D, v_texCoord0) * v_color;

	float shift = sin(u_shift);
	vec3 shiftedColor = color.rgb + shift;
	shiftedColor.r = 0.0;
	
	/*
	vec4 mod = mod(color.rgba + shift, vec4(1.0));
	if (shiftedColor.r > 1.0) {
		shiftedColor.r = 1.0 - mod.r;
	}
	if (shiftedColor.g > 1.0) {
		shiftedColor.g = 1.0 - mod.g;
	}
	if (shiftedColor.b > 1.0) {
		shiftedColor.b = 1.0 - mod.b;
	}
	shiftedColor.rgb = abs(shiftedColor.rgb);
	*/

	gl_FragColor = vec4(shiftedColor, color.a);
}
