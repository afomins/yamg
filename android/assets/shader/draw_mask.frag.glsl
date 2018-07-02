// -----------------------------------------------------------------------------
<import shader/common.frag.glsl>

// -----------------------------------------------------------------------------
varying vec2 v_coord;
uniform sampler2D u_tex, u_tex_mask;

// -----------------------------------------------------------------------------
void main() {
    gl_FragColor = texture2D(u_tex, v_coord.st);

    float mask = texture2D(u_tex_mask, v_coord.st).a;
    if(mask == 0.0) {
        gl_FragColor.a = 0.0;
    }
}
