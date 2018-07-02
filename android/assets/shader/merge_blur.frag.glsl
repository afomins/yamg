// -----------------------------------------------------------------------------
<import shader/common.frag.glsl>

// -----------------------------------------------------------------------------
varying vec2 v_coord;

<iterate-line>
uniform sampler2D u_tex@;

<iterate-line>
uniform float u_alpha@;

// -----------------------------------------------------------------------------
void main() {
    // Get alpha of primary texture
    vec4 rc = vec4(0.0, 0.0, 0.0, 0.0);

    // Merge
    <iterate-line>
    MergeAlpha(u_tex@, v_coord.st, u_alpha@, rc);

    gl_FragColor = rc;
}
