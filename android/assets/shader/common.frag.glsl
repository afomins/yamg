// -----------------------------------------------------------------------------
#ifdef GL_ES
    precision mediump float;
#endif

// -----------------------------------------------------------------------------
void MergeAlpha(in sampler2D tex, in vec2 coord, in float alpha, inout vec4 rc) {
    float tex_alpha = texture2D(tex, coord).a;
    rc.a += (alpha * tex_alpha);
}

// -----------------------------------------------------------------------------
void MergeRgba(in sampler2D tex, in vec2 coord, in vec3 col, inout vec4 rc) {
    float alpha = texture2D(tex, coord).a;
    if(alpha == 1.0) {
        rc.rgb = col;
    } else if(alpha > 0.0) {
        rc.rgb += (col - rc.rgb) * alpha;
    }
}
