// -----------------------------------------------------------------------------
<import shader/common.frag.glsl>

// -----------------------------------------------------------------------------
varying vec2 v_coord;
uniform sampler2D u_tex;
uniform float u_shift;

// -----------------------------------------------------------------------------
void main() {
    // Shift alpha value
    gl_FragColor = texture2D(u_tex, v_coord.st);
    gl_FragColor.a += u_shift;

    // Handle wrapping
    if(gl_FragColor.a > 1.0) {
        gl_FragColor.a = mod(gl_FragColor.a, 1.0);
    }
}
