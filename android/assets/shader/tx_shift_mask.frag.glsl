// -----------------------------------------------------------------------------
<import shader/common.frag.glsl>

// -----------------------------------------------------------------------------
varying vec2 v_coord;
uniform sampler2D u_tex, u_tex_mask;
uniform float u_shift_x, u_shift_y;

// -----------------------------------------------------------------------------
void main() {
    // Do alpha shift by mask
    float mask = texture2D(u_tex_mask, v_coord.st).a;
    if(mask > 0.0) {
        // Shift texture coordinates
        vec2 coord = vec2(v_coord.s + u_shift_x, v_coord.t + u_shift_y);
        gl_FragColor = texture2D(u_tex, coord);
    }
}
