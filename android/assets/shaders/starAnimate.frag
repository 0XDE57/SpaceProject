varying vec4 v_color;
varying vec2 v_texCoord0;
uniform vec3 v_HSV;

uniform sampler2D u_sampler2D;
uniform float u_shift;
uniform vec3 u_colorTemp;


vec3 rgb2hsv(vec3 c) {
	vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
	vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
	vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

	float d = q.x - min(q.w, q.y);
	float e = 1.0e-10;
	return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
	vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
	vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
	return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
	vec4 color = texture2D(u_sampler2D, v_texCoord0) * v_color;

	//shift height values to animate colors
	float shift = u_shift;// sin(u_shift);
	//float normalizedShift = (shift * 0.5) + 0.5; //noramlize [1 to -1] -> [0 to 1]


	//vec3 shiftedColor = mod(color.rgb * shift, vec3(1.0));
	vec3 shiftedColor = mod((color.rgb + shift) * u_colorTemp, vec3(1.0)); //wrap values [0-1] with modulus
	//shiftedColor = mod(shiftedColor.rgb * u_colorTemp, vec3(1.0));
	// todo: calculate color from black body radiation
	//shiftedColor.rgb = mix(shiftedColor, u_colorTemp, 0.5);
	//shiftedColor.rgb *= (u_colorTemp * 0.5) + 0.5;


	// todo: could potentially mix red and yellow on boundary to smooth transition: color = mix(colorA, colorB, 0.5);
	// incoming image is assumed to be black and white: grayscale heightmap.
	// so only need to check one channel because the r,g,b values are equal.
	// if had color would require average. eg: average = (color.r + color.g + color.b) / 3.0;
	vec3 fireColor = mod(color.rgb + shift, vec3(1.0));
	if (fireColor.r > 0.5) {
		//set to shades of yellow
		fireColor.b = 0.0;
	} else {
		//set to shades of red
		fireColor.r = 1.0 - fireColor.r;
		fireColor.g = 0.0;
		fireColor.b = 0.0;
	}
	shiftedColor = mix(shiftedColor, fireColor, 0.5);


	/*
	vec3 additionalColor = shiftedColor;
	float average = (color.r + color.g + color.b) / 3.0;
	if (average > 0.5) {
		//set to shades of yellow
		additionalColor.r = average;
		additionalColor.g = average;
		additionalColor.b = 0.0;
	} else {
		//set to shades of red
		additionalColor.r = 1.0 - average;
		additionalColor.g = 0.0;
		additionalColor.b = 0.0;
	}
	shiftedColor = mix(shiftedColor, additionalColor, 0.5);
*/
	//todo: smooth edges, something along the lines of
	//uniform vec2 u_resolution;
	//vec2 st = gl_FragCoord.xy/u_resolution;
	//float dist = distance(st, vec2(0.5));
	//float radius = half texture width/height
	//float fade = 1.0 - smoothstep(0.0, radius, dist);
	//color.a = color.a * fade

	//not the effect i was going for but pretty neat effect
	//vec3 shiftedColor = mod((color.rgb * u_colorTemp) + shift, vec3(1.0)); //wrap values [0-1] with modulus

	//todo: saturation, its now kinda dim
	//vec3 fragHSV = rgb2hsv(shiftedColor).xyz;
	//fragHSV.x += v_HSV.x / 360.0 * u_shift;
	//fragHSV.z = 1;
	//fragHSV.yz *= v_HSV.yz;
	//fragHSV.xyz = mod(fragHSV.xyz, 1.0);
	//vec3 rainbowColor = hsv2rgb(fragHSV);

	gl_FragColor = vec4(shiftedColor, color.a);
}
