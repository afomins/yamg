// -----------------------------------------------------------------------------
#ifdef GL_ES
    precision mediump float;
#endif

// -----------------------------------------------------------------------------
attribute vec4 a_position;
attribute vec2 a_texCoord;
varying vec2 v_coord;

// -----------------------------------------------------------------------------
void main() {
    v_coord = a_texCoord;
    gl_Position = a_position;
}
