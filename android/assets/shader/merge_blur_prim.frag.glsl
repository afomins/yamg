// -----------------------------------------------------------------------------
<import shader/common.frag.glsl>

// -----------------------------------------------------------------------------
varying vec2 v_coord;
uniform sampler2D u_tex0, u_tex1;

// -----------------------------------------------------------------------------
void main() {
    vec4 rc = vec4(0.0, 0.0, 0.0, texture2D(u_tex0, v_coord.st).a);

    if(rc.a == 0.0) {
        rc.a = texture2D(u_tex1, v_coord.st).a;
    }

    gl_FragColor = rc;
}
