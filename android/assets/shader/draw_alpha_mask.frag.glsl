// -----------------------------------------------------------------------------
<import shader/common.frag.glsl>

// -----------------------------------------------------------------------------
varying vec2 v_coord;
uniform sampler2D u_tex;
uniform float u_alpha; 

// -----------------------------------------------------------------------------
void main() {
    gl_FragColor = texture2D(u_tex, v_coord.st);
    if(gl_FragColor.a > 0.0) {
        gl_FragColor.a = u_alpha;
    }
}
