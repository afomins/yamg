// -----------------------------------------------------------------------------
<import shader/common.frag.glsl>

// -----------------------------------------------------------------------------
varying vec2 v_coord;
uniform sampler2D u_tex, u_tex_mask;
uniform float u_shift;

// -----------------------------------------------------------------------------
void main() {
    gl_FragColor = texture2D(u_tex, v_coord.st);

    // Do alpha shift by mask
    float mask = texture2D(u_tex_mask, v_coord.st).a;
    if(mask > 0.0) {
        // Shift alpha value
        gl_FragColor.a += u_shift;

        // Handle wrapping
        if(gl_FragColor.a > 1.0) {
            gl_FragColor.a = mod(gl_FragColor.a, 1.0);
        }
    }
}
