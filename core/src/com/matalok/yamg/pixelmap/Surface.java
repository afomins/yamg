// -----------------------------------------------------------------------------
package com.fomin.yamg.pixelmap;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.fomin.yamg.CommonObject;
import com.fomin.yamg.Logger;
import com.fomin.yamg.Utils;

// -----------------------------------------------------------------------------
public class Surface {
    // =========================================================================
    // VertexDesc
    private static class VertexDesc {
        // ---------------------------------------------------------------------
        public int size, mask;
        public int pos_idx, col_idx, tex_pos_idx, norm_idx;
        public VertexAttribute [] list;

        // ---------------------------------------------------------------------
        public VertexDesc(int mask) {
            // Create attribute list
            int idx = 0, attrib_size = 0, set_bit_cnt = Utils.GetBitCnt(mask);
            this.list = new VertexAttribute[set_bit_cnt];

            // Save mask
            this.mask = mask;

            // Create attribute
            for(int i = 0; i < Surface.ATTRIB_CNT; i++) {
                VertexAttribute attrib = null;
                switch(mask & (1 << i)) {
                    case Surface.ATTRIB_POS:
                        this.pos_idx = this.size;

                        attrib_size = 3; // x + y + z
                        attrib = new VertexAttribute(Usage.Position, attrib_size,
                          ShaderProgram.POSITION_ATTRIBUTE);
                    break;

                    case Surface.ATTRIB_COL:
                        this.col_idx = this.size;

                        attrib_size = 1;    // r + g + b + a
                        attrib = new VertexAttribute(Usage.ColorPacked, 4, 
                          ShaderProgram.COLOR_ATTRIBUTE);
                    break;

                    case Surface.ATTRIB_TEX_POS:
                        this.tex_pos_idx = this.size;

                        attrib_size = 2;    // x + y
                        attrib = new VertexAttribute(Usage.TextureCoordinates, attrib_size, 
                          ShaderProgram.TEXCOORD_ATTRIBUTE);
                    break;

                    case Surface.ATTRIB_NORM:
                        this.norm_idx = this.size;

                        attrib_size = 3;    // x, y, z
                        attrib = new VertexAttribute(Usage.Normal, attrib_size, 
                          ShaderProgram.NORMAL_ATTRIBUTE);
                    break;
                }

                // Add attribute to list
                if(attrib != null) {
                    this.list[idx++] = attrib;
                    this.size += attrib_size;
                }
            }
        }
    }

    // =========================================================================
    // Flat
    public static class Flat extends Surface {
        // ---------------------------------------------------------------------
        public Flat(boolean is_detailed, boolean is_smooth, Utils.Vector2i size, 
          float tx_factor, float ty_factor, float pos_z) {
            // Construct base surface
            super(is_detailed, is_smooth, Surface.ATTRIB_POS | Surface.ATTRIB_TEX_POS | Surface.ATTRIB_NORM,
              size.x, size.y);

            // Surface attributes
            int vert_x_cnt, vert_y_cnt, step_x, step_y;
            if(this.is_detailed) {
                vert_x_cnt = this.size.x + 1; 
                vert_y_cnt = this.size.y + 1;
                step_x = step_y = 1;
            } else {
                vert_x_cnt = vert_y_cnt = 2;
                step_x = this.size.x;
                step_y = this.size.y;
            }

            int poly_x_cnt = vert_x_cnt - 1;
            int poly_y_cnt = vert_y_cnt - 1;
            int polygon_cnt = poly_x_cnt * poly_y_cnt;
            int vertex_cnt = vert_x_cnt * vert_y_cnt;

            // Log
            Logger.d(Logger.MOD_MISC, "  [type=FLAT] [vert-cnt=%d:%d:%d] [step=%d:%d] [poly-cnt=%d] [pos_z=%.1f]",
              vert_x_cnt, vert_y_cnt, vertex_cnt, step_x, step_y, polygon_cnt, pos_z);

            // Mesh
            this.CreateDataArray(vertex_cnt, polygon_cnt);

            // Init vertex data
            int idx = 0; for(int y = 0; y < vert_y_cnt; y++) {
                for(int x = 0; x < vert_x_cnt; x++) {
                    // Texture coordinates
                    float pos_x = x * step_x;
                    float pos_y = y * step_y;
                    float tx = pos_x / (this.size.x / tx_factor);
                    float ty = pos_y / (this.size.y / ty_factor);

                    // Init vertex
                    idx = this.InitVertexData(idx, 
                      pos_x, pos_y, pos_z,  // x, y, z
                      0.0f,                 // color
                      tx, ty,               // texture x, y
                      0.0f, 0.0f, 1.0f);    // normal
                }
            }

            // Init index data
            this.InitIndexData(polygon_cnt, poly_x_cnt, vert_x_cnt);

            // Create mesh
            this.CreateMesh(vertex_cnt);
        }
    }

    // =========================================================================
    // Surface
    private final static int ATTRIB_POS = 1 << 0;
    private final static int ATTRIB_COL = 1 << 1;
    private final static int ATTRIB_TEX_POS = 1 << 2;
    private final static int ATTRIB_NORM = 1 << 3;
    private final static int ATTRIB_CNT = 5;

    // -------------------------------------------------------------------------
    protected float[] vertex_data;
    protected short[] index_data;
    protected CommonObject.CommonMesh mesh;
    protected Matrix4 mvp_matrix;

    protected Utils.Vector2i size, visible_size;
    protected boolean is_detailed;

    protected Surface.VertexDesc vertex_desc;
    protected TextureFilter min_filter, mag_filter;

    // -------------------------------------------------------------------------
    private Surface(boolean is_detailed, boolean is_smooth, int attrib_mask, int width, 
      int height) {
        this.min_filter = TextureFilter.Nearest; 
        this.mag_filter = (is_smooth) ? TextureFilter.Linear : TextureFilter.Nearest;
        this.is_detailed = is_detailed;
        this.size = new Utils.Vector2i(width, height);
        this.visible_size = new Utils.Vector2i(width, height);
        this.mvp_matrix = new Matrix4();

        // Log
        Logger.d(Logger.MOD_MISC, "Creating surface :: [detailed=%s] [size=%d:%d:%d]",
          Utils.Bool2Str(is_detailed), width, height, width * height);

        // Vertex descriptor
        this.vertex_desc = new Surface.VertexDesc(attrib_mask);
    }

    // -------------------------------------------------------------------------
    public Utils.Vector2i GetSize() { return this.size; }
    public Utils.Vector2i GetVisibleSize() { return this.visible_size; }

    // -------------------------------------------------------------------------
    public int InitVertexData(int idx, float x, float y, float z, float color, 
      float tx, float ty, float nx, float ny, float nz) {
        VertexDesc desc = this.vertex_desc;

        // Log
        Logger.d(Logger.MOD_MISC, "  [vidx=%d] [pos=%.1f:%.1f:%.1f] [col=%.1f] [tex=%.1f:%.1f] [norm=%.1f:%.1f:%.1f]",
          idx / this.vertex_desc.size, x, y, z, color, tx, ty, nx, ny, nz);

        // Position
        if((desc.mask & Surface.ATTRIB_POS) != 0) {
            this.vertex_data[idx + desc.pos_idx + 0] = x;
            this.vertex_data[idx + desc.pos_idx + 1] = y;
            this.vertex_data[idx + desc.pos_idx + 2] = z;
        }

        // Color
        if((desc.mask & Surface.ATTRIB_COL) != 0) {
            this.vertex_data[idx + desc.col_idx] = color;
        }

        // Texture position
        if((desc.mask & Surface.ATTRIB_TEX_POS) != 0) {
            this.vertex_data[idx + desc.tex_pos_idx + 0] = tx;
            this.vertex_data[idx + desc.tex_pos_idx + 1] = ty;
        }

        // Normal 
        if((desc.mask & Surface.ATTRIB_NORM) != 0) {
            this.vertex_data[idx + desc.norm_idx + 0] = nx;
            this.vertex_data[idx + desc.norm_idx + 1] = ny;
            this.vertex_data[idx + desc.norm_idx + 2] = nz;
        }

        // Return next index
        return idx + desc.size;
    }

    // -------------------------------------------------------------------------
    // Vertex order in polygon  0 line: 0 - 1
    //                                  |   |
    //                          1 line: 2 - 3
    //                          
    public void InitIndexData(int poly_cnt, int poly_x_cnt, int vert_x_cnt) {
        int idx = 0; for(int j = 0; j < poly_cnt; j++) {
            int y = j / poly_x_cnt;
            int v0 = j + y;
            int v1 = v0 + 1;
            int v2 = v1 + vert_x_cnt - 1;
            int v3 = v2 + 1;

            this.index_data[idx++] = (short)v0; // 1st triangle (CW)
            this.index_data[idx++] = (short)v1; 
            this.index_data[idx++] = (short)v3;

            this.index_data[idx++] = (short)v0; // 2nd triangle (CW)
            this.index_data[idx++] = (short)v3; 
            this.index_data[idx++] = (short)v2;
        }
    }

    // -------------------------------------------------------------------------
    protected void CreateDataArray(int vertex_cnt, int polygon_cnt) {
        int vertex_data_size = vertex_cnt * this.vertex_desc.size;
        int index_data_size = polygon_cnt * 3 * 2; // 1 polygon = 3 vertex in 2 triangles

        this.vertex_data = new float[vertex_data_size];
        this.index_data = new short[index_data_size];
    }

    // -------------------------------------------------------------------------
    protected void CreateMesh(int vertex_cnt) {
        // Create mesh
        this.mesh = new CommonObject.CommonMesh(true, vertex_cnt, 
          this.index_data.length, this.vertex_desc.list);

        // Create vertices/indices
        this.mesh.obj.setVertices(this.vertex_data);
        this.mesh.obj.setIndices(this.index_data);
    }

    // -------------------------------------------------------------------------
    public void Render(ShaderProgram shader, Matrix4 vp_matrix, float x, float y, float z) {
        // Init mvp-matrix 
        this.mvp_matrix.set(vp_matrix);

        // Apply translation to vp matrix
        if(x != 0.0f && x != y && x != z) {
            this.mvp_matrix.translate(x, y, z);
        }

        // Render
        this.Render(shader, this.mvp_matrix);
    }

    // -------------------------------------------------------------------------
    public void Render(ShaderProgram shader, Matrix4 mvp_matrix) {
        shader.setUniformMatrix("u_mvp_matrix", mvp_matrix);
        this.mesh.obj.render(shader, GL20.GL_TRIANGLES);
    }

    // -------------------------------------------------------------------------
    public void UpdateVisibleSize(float zoom_factor) {
        this.visible_size.x = (int)(this.size.x * zoom_factor);
        this.visible_size.y = (int)(this.size.y * zoom_factor);
    }
}
