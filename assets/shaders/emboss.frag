varying vec2 v_texCoord0;

uniform sampler2D u_sampler2D;
uniform vec2 u_resolution;

const float contrast = 5.;

void main() {
	vec2 onePixel = vec2(1. / u_resolution.x, 1. / u_resolution.y); // calculating the size of one pixel on the screen for the current resolution
	
	vec2 texCoord = v_texCoord0; // convenience variable; we cannot write to v_texCoord0, so we use this

	vec3 color = vec3(.5); // initialize color with half value on all channels

	// swap `+/- onePixel` to invert emboss effect
	color += texture2D(u_sampler2D, texCoord - onePixel).rgb * contrast; // we need `.rgb` for older OpenGL versions (e.g. 2.0) which cannot yet do add a vec4 to a vec3 via += 
	color -= texture2D(u_sampler2D, texCoord + onePixel).rgb * contrast;

	// grayscale
	color = vec3((color.r + color.g + color.b) / 3.);

	gl_FragColor = vec4(color, 1);
}
