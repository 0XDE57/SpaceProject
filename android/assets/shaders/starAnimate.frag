varying vec4 v_color;
varying vec2 v_texCoord0;

uniform sampler2D u_sampler2D;
uniform float u_shift;

void main() {
	vec4 color = texture2D(u_sampler2D, v_texCoord0) * v_color;

	//shift height values to animate colors
	float shift = sin(u_shift);
	//float normalizedShift = (shift * 0.5) + 0.5; //noramlize [1 to -1] -> [0 to 1]
	vec3 shiftedColor = mod(color.rgb + shift, vec3(1.0)); //wrap values [0-1] with modulus

	// incoming image is assumed to be black and white.
	// so only need to check one channel because the r,g,b values are equal.
	// if had color would require average. eg: average = (color.r + color.g + color.b) / 3.0;
	// todo: calculate color from black body radiation
	if (shiftedColor.r > 0.5) {
		//set to shades of yellow
		shiftedColor.b = 0.0;
	} else {
		//set to shades of red
		shiftedColor.r = 1.0 - shiftedColor.r;
		shiftedColor.g = 0.0;
		shiftedColor.b = 0.0;
	}

	//todo: smooth edges, something along the lines of
	//vec2 st = gl_FragCoord.xy/u_resolution;
	//float dist = distance(st, vec2(0.5));
	//float radius = half texture width/height
	//float fade = 1.0 - smoothstep(0.0, radius, dist);
	//color.a = color.a * fade

	gl_FragColor = vec4(shiftedColor, color.a);
}
