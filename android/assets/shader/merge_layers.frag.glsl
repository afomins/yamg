// -----------------------------------------------------------------------------
<import shader/common.frag.glsl>

// -----------------------------------------------------------------------------
varying vec2 v_coord;

<iterate-line>
uniform sampler2D @;

<iterate-line>
uniform vec3 @;

// -----------------------------------------------------------------------------
void main() {
    <iterate-line>
    vec4 rc = vec4(@.rgb, 1.0);

    <iterate-line>
    MergeRgba(@, v_coord.st, @, rc);

    gl_FragColor = rc;
}
