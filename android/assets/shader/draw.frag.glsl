// -----------------------------------------------------------------------------
<import shader/common.frag.glsl>

// -----------------------------------------------------------------------------
varying vec2 v_coord;
uniform sampler2D u_tex;

// -----------------------------------------------------------------------------
void main() {
    gl_FragColor = texture2D(u_tex, v_coord.st);
}
