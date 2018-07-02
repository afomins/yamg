// -----------------------------------------------------------------------------
package com.fomin.yamg.pixelmap;

// -----------------------------------------------------------------------------
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.fomin.yamg.CommonObject;
import com.fomin.yamg.Logger;
import com.fomin.yamg.Map2d;
import com.fomin.yamg.Obj;
import com.fomin.yamg.ShaderMan;
import com.fomin.yamg.Timer;
import com.fomin.yamg.Utils;

// -----------------------------------------------------------------------------
public class Pixelmap extends Obj.CommonObject {
    // =========================================================================
    // LayerParam
    public static class LayerParam {
        public int id, mask_id;
        public int fx;
        public long fx_freq;
        public boolean is_fading, is_visible;
        public float[] blur_alpha;
    }

    // =========================================================================
    // Layer
    private static class Layer extends FadingColor {
        // ---------------------------------------------------------------------
        protected boolean is_updated;

        protected LayerParam param;
        protected Layer mask;
        protected float[] color_buf;

        protected Snapshot primary_sn;
        protected Snapshot[] blur_sn;
        protected TextureDesc blur_merge_tex, final_tex;
        protected boolean dispose_final_tex;

        protected long blur_shift_time, alpha_shift_time, tx_shift_time;
        protected Utils.Vector2i size;

        protected float alpha_shift;
        protected Utils.Vector2f tx_shift;

        protected ShaderProgram sh_merge_blur;

        protected String uni_tex_name, uni_col_name;

        protected static CommonObject.CommonMesh fbo_mesh;
        protected static ShaderProgram sh_draw, sh_draw_mask;
        protected static ShaderProgram sh_merge_blur_prim;
        protected static ShaderProgram sh_alpha_shift, sh_alpha_shift_mask;
        protected static ShaderProgram sh_tx_shift, sh_tx_shift_mask;

        // ---------------------------------------------------------------------
        public Layer(Color color, Utils.Vector2i size, LayerParam param) {
            // Base constructor
            super(color);

            // Common attributes
            this.size = size;
            this.param = param;
            this.mask = null; // Will be set when all layers are initialized 
            this.color_buf = new float[3]; // RGB
            this.blur_shift_time = Timer.Get();
            this.alpha_shift_time = Timer.Get();
            this.tx_shift_time = Timer.Get();
            this.tx_shift = new Utils.Vector2f();

            // Uniform names
            this.uni_tex_name = this.GetUniformName("u_tex_");
            this.uni_col_name = this.GetUniformName("u_col_");

            // Primary snapshot
            this.primary_sn = new Snapshot(false, size, 8);

            // Make blur snapshots
            if(this.param.fx == Pixelmap.FX_BLUR) {
                this.blur_sn = new Snapshot[param.blur_alpha.length];
                for(int i = 0; i < this.blur_sn.length; i++) {
                    this.blur_sn[i] = new Snapshot(true, size, 32);
                }

                // Final blur texture
                this.blur_merge_tex = new TextureDesc(true, size, 32);
            }

            // If no-fx and no-mask then primary snapshot is final
            if(this.param.fx == Pixelmap.FX_DEFAULT && this.param.mask_id == -1) {
                this.final_tex = this.primary_sn.desc;
                this.dispose_final_tex = false;

            // Otherwise create separate texture
            } else {
                this.final_tex = new TextureDesc(true, size, 32);
                this.dispose_final_tex = true;
            }
        }

        // ---------------------------------------------------------------------
        private String GetUniformName(String prefix) {
            String name = prefix + Pixelmap.ID_GROUP.GetEntryName(this.param.id, true); 
            return name.replace('-', '_'); 
        }

        // ---------------------------------------------------------------------
        public boolean IsBlurActive() {
            return (this.primary_sn.time > this.blur_sn[this.blur_sn.length - 1].time) ? true : false;
        }

        // ---------------------------------------------------------------------
        public boolean IsShiftBlurTime() {
            long cur_time = Timer.Get();
            return (cur_time > this.blur_shift_time + this.param.fx_freq) ? true : false;
        }

        // ---------------------------------------------------------------------
        public boolean IsShiftAlphaTime() {
            long cur_time = Timer.Get();
            return (cur_time > this.alpha_shift_time + this.param.fx_freq) ? true : false;
        }

        // ---------------------------------------------------------------------
        public boolean IsShiftTxTime() {
            long cur_time = Timer.Get();
            return (cur_time > this.tx_shift_time + this.param.fx_freq) ? true : false;
        }

        // ---------------------------------------------------------------------
        public void SetPixel(int x, int y, float alpha) {
            this.SetPixelLite(x, y, alpha);

            this.primary_sn.state = Pixelmap.ST_SET_LOCAL;
            this.primary_sn.time = Timer.Get();
        }

        // ---------------------------------------------------------------------
        private void SetPixelLite(int x, int y, float alpha) {
            this.primary_sn.desc.pm.obj.drawPixel(x, y, (int) (255.0f * alpha));
        }

        // ---------------------------------------------------------------------
        public void SetMap(int x, int y, Map2d<Float> map) {
            // Get wrap context
            Map2d.WrapContext wrap_ctx = Map2d.GetWrapContext(map.size, this.size, x, y);
            if (wrap_ctx == null) return;

            // Update state nd time
            this.primary_sn.state = Pixelmap.ST_SET_LOCAL;
            this.primary_sn.time = Timer.Get();

            // Test active pixels
            for(int i = 0; i < map.active_cnt; i++) {
                Map2d.Entry<Float> b = map.active[i];
                Utils.Vector2i rc = Utils.v2i_tmp;
                if (wrap_ctx.Test(b.x, b.y, rc)) {
                    this.SetPixelLite(rc.x, rc.y, b.val);
                }
            }
        }

        // ---------------------------------------------------------------------
        private void ClearSnapshot(Snapshot sn) {
            sn.desc.pm.obj.setColor(0);
            sn.desc.pm.obj.fill();
            sn.state = Pixelmap.ST_UNSET_LOCAL;
        }

        // ---------------------------------------------------------------------
        public void Clear() {
            this.ClearSnapshot(this.primary_sn);
        }

        // ---------------------------------------------------------------------
        private void SendToGpu(Snapshot sn) {
            // Update primary snapshot in GPU
            sn.desc.tex_native.draw(sn.desc.pm.obj, 0, 0);

            // Select next state state when locally updated
            if(sn.IsLocallyUpdated()) sn.state++;
        }

        // ---------------------------------------------------------------------
        private void FxMask() {
            // Render to layer final
            ShaderProgram sh = Layer.sh_draw_mask;
            FrameBuffer fbo = this.final_tex.fbo.obj;
            sh.begin(); fbo.begin();

            // Primary snapshot uniform
            Pixelmap.BindShaderTexture(sh, 0, this.primary_sn.desc.tex_native, "u_tex");
            Pixelmap.BindShaderTexture(sh, 1, this.mask.final_tex.tex_native, "u_tex_mask");

            // Render
            Layer.fbo_mesh.obj.render(sh, GL20.GL_TRIANGLES);
            fbo.end(); sh.end();
        }

        // ---------------------------------------------------------------------
        public void FxBlur() {
            // Shift each blur snapshot one position lower
            if(this.blur_sn.length > 1) {
                int last_idx = this.blur_sn.length - 1;
                Snapshot last = this.blur_sn[last_idx];
                for(int i = last_idx; i > 0; i--) {
                    this.blur_sn[i] = this.blur_sn[i - 1];
                }
                this.blur_sn[0] = last;
            }

            // Top blur snapshot inherits timestamp of primary snapshot
            this.blur_sn[0].time = this.primary_sn.time;
            this.blur_shift_time = Timer.Get();

            // Copy primary snapshot to top blur snapshot
            this.FxBlurCopyPrimary();

            // Merger blur snapshots 
            this.FxBlurMerge();
        }

        // ---------------------------------------------------------------------
        private void FxBlurCopyPrimary() {
            // Render to top blur
            ShaderProgram sh = Layer.sh_draw;
            FrameBuffer fbo = this.blur_sn[0].desc.fbo.obj;
            sh.begin(); fbo.begin();

            // Primary texture
            Pixelmap.BindShaderTexture(sh, 0, this.primary_sn.desc.tex_native, "u_tex");

            // Render
            Layer.fbo_mesh.obj.render(sh, GL20.GL_TRIANGLES);
            fbo.end(); sh.end();
        }

        // ---------------------------------------------------------------------
        private void FxBlurMerge() {
            // Render to final blur merge
            ShaderProgram sh = this.sh_merge_blur;
            FrameBuffer fbo = this.blur_merge_tex.fbo.obj;
            sh.begin(); fbo.begin();

            // Blur snaphsots
            for(int i = 0; i < this.blur_sn.length; i++) {
                // Texture
                String tex_name = String.format("u_tex%d", i);
                Pixelmap.BindShaderTexture(sh, i, this.blur_sn[i].desc.tex_native, tex_name);

                // Alpha
                String alpha_name = String.format("u_alpha%d", i);
                sh.setUniformf(alpha_name, this.param.blur_alpha[i]);
            }

            // Render
            Layer.fbo_mesh.obj.render(sh, GL20.GL_TRIANGLES);
            fbo.end(); sh.end();
        }

        // ---------------------------------------------------------------------
        private void FxBlurPrimMerge() {
            // Render to layer final
            ShaderProgram sh = Layer.sh_merge_blur_prim;
            FrameBuffer fbo = this.final_tex.fbo.obj;
            sh.begin(); fbo.begin();

            // Primary & blur_merged textures
            Pixelmap.BindShaderTexture(sh, 0, this.primary_sn.desc.tex_native, "u_tex0");
            Pixelmap.BindShaderTexture(sh, 1, this.blur_merge_tex.tex_native, "u_tex1");

            // Render
            Layer.fbo_mesh.obj.render(sh, GL20.GL_TRIANGLES);
            fbo.end(); sh.end();
        }

        // ---------------------------------------------------------------------
        private void FxAlphaShift() {
            // Update time
            this.alpha_shift_time = Timer.Get();

            // Render to layer final
            ShaderProgram sh = (this.mask == null) ? Layer.sh_alpha_shift : 
              Layer.sh_alpha_shift_mask;
            FrameBuffer fbo = this.final_tex.fbo.obj;
            sh.begin(); fbo.begin();

            // Primary texture
            Pixelmap.BindShaderTexture(sh, 0, this.primary_sn.desc.tex_native, "u_tex");

            // Mask texture
            if(this.mask != null) {
                Pixelmap.BindShaderTexture(sh, 1, this.mask.final_tex.tex_native, "u_tex_mask");
            }

            // Alpha increment value
            this.alpha_shift += 1.0f / 256.0f;
            if(this.alpha_shift > 1.0f) this.alpha_shift -= 1.0f;
            sh.setUniformf("u_shift", this.alpha_shift);

            // Render
            Layer.fbo_mesh.obj.render(sh, GL20.GL_TRIANGLES);
            fbo.end(); sh.end();
        }

        // ---------------------------------------------------------------------
        private void FxTxShift() {
            // Update time
            this.tx_shift_time = Timer.Get();

            // Render to layer final
            ShaderProgram sh = (this.mask == null) ? Layer.sh_tx_shift : 
              Layer.sh_tx_shift_mask;
            FrameBuffer fbo = this.final_tex.fbo.obj;
            sh.begin(); fbo.begin();

            // Primary texture
            Pixelmap.BindShaderTexture(sh, 0, this.primary_sn.desc.tex_native, "u_tex");

            // Mask texture
            if(this.mask != null) {
                Pixelmap.BindShaderTexture(sh, 1, this.mask.final_tex.tex_native, "u_tex_mask");
            }

            // Alpha increment value
            this.tx_shift.x += 1.0f / 256.0f; if(this.tx_shift.x > 1.0f) this.tx_shift.x -= 1.0f;
            this.tx_shift.y -= 1.0f / 128.0f; if(this.tx_shift.y < -1.0f) this.tx_shift.y += 1.0f;
            sh.setUniformf("u_shift_x", this.tx_shift.x);
            sh.setUniformf("u_shift_y", this.tx_shift.y);

            // Render
            Layer.fbo_mesh.obj.render(sh, GL20.GL_TRIANGLES);
            fbo.end(); sh.end();
        }

        // ---------------------------------------------------------------------
        private void Dispose() {
            // Textures
            if(this.blur_merge_tex != null) this.blur_merge_tex.Dispose();
            if(this.dispose_final_tex) this.final_tex.Dispose();

            // Primary snapshot
            this.primary_sn.desc.Dispose();

            // Blur snapshot
            if(this.blur_sn != null) {
                for(Snapshot s: this.blur_sn) {
                    s.desc.Dispose();
                }
            }
        }
    }

    // =========================================================================
    // Snapshot
    private static class Snapshot {
        // ---------------------------------------------------------------------
        protected TextureDesc desc;
        protected long time;
        protected int state;

        // ---------------------------------------------------------------------
        public Snapshot(boolean is_fbo, Utils.Vector2i size, int pixel_size) {
            this.desc = new TextureDesc(is_fbo, size, pixel_size);
            this.time = Timer.Get();
            this.state = Pixelmap.ST_CLEAN;
        }

        // ---------------------------------------------------------------------
        public boolean IsLocallyUpdated() {
            return (this.state == Pixelmap.ST_SET_LOCAL || 
              this.state == Pixelmap.ST_UNSET_LOCAL) ? true : false;
        }
    }

    // =========================================================================
    // TextureDesc
    private static class TextureDesc extends Obj.CommonObject {
        // ---------------------------------------------------------------------
        protected CommonObject.CommonPixmap pm;
        protected CommonObject.CommonTexture tex;
        protected CommonObject.CommonFbo fbo;
        protected Texture tex_native;

        // ---------------------------------------------------------------------
        public TextureDesc(boolean is_fbo, Utils.Vector2i size, int pixel_size) {
            super(Obj.MISC.ptr, Obj.MISC.PIXELMAP_TEX);

            // Get pixel format
            Pixmap.Format fmt = Format.Alpha;
            if(pixel_size == 8) fmt = Format.Alpha;
            else if (pixel_size == 24) fmt = Format.RGB888;
            else if (pixel_size == 32) fmt = Format.RGBA8888;
            else Utils.Assert(false, "Unsuported pixel size %d", pixel_size);

            // FBO
            if(is_fbo) {
                this.fbo = new CommonObject.CommonFbo(size.x, size.y, fmt);
                this.tex_native = this.fbo.obj.getColorBufferTexture();

            // Non FBO
            } else {
                this.pm = new CommonObject.CommonPixmap(size.x, size.y, fmt);
                this.tex = new CommonObject.CommonTexture(pm.obj);
                this.tex_native = this.tex.obj;
            }

            // Nearest filtration
            this.tex_native.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            this.tex_native.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        }

        // ---------------------------------------------------------------------
        public void Dispose() {
            if(this.fbo != null) {
                this.fbo.Dispose();
                this.fbo = null;
            } else {
                this.tex.Dispose();
                this.pm.Dispose();
            }
            this.tex_native = null;
            super.Dispose();
        }
    }

    // =========================================================================
    // Pixelmap
    public static final int ST_CLEAN = 0;
    public static final int ST_SET_LOCAL = 1;
    public static final int ST_SET_GPU = 2;
    public static final int ST_UNSET_LOCAL = 3;
    public static final int ST_UNSET_GPU = 4;

    public static final int FX_DEFAULT = 0;
    public static final int FX_BLUR = 1;
    public static final int FX_ALPHA_SHIFT = 2;
    public static final int FX_TX_SHIFT = 3;
    public static final Obj.Group FX_GROUP = new Obj.Group("pixelmap-fx", new String [] {
      "default", "blur", "alpha-shift", "tx-shift"});

    public static final int ID_BACK_PRIM = 0;
    public static final int ID_BACK_SEC_MASK = 1;
    public static final int ID_BACK_SEC = 2;
    public static final int ID_ROCK = 3;
    public static final int ID_ROCK_DETAIL = 4;
    public static final int ID_SAND = 5;
    public static final int ID_SAND_DETAIL = 6;
    public static final int ID_GOLD = 7;
    public static final int ID_GOLD_DETAIL = 8;
    public static final int ID_WATER = 9;
    public static final int ID_UI = 10;
    public static final Obj.Group ID_GROUP = new Obj.Group("pixelmap-layer", new String [] {
      "back-prim", "back-sec-mask", "back-sec", "rock", "rock-detail", "sand", 
      "sand-detail", "gold", "gold-detail", "water", "ui"});

    // -------------------------------------------------------------------------
    private Surface surface;
    private Layer[] layer;
    private Array<Layer> layer_active, layer_visible;
    private Array<Array<Layer>> layer_merge;
    private boolean is_color_changing;
    private Array<TextureDesc> surface_tex;
    private TextureDesc surface_tex_prim, surface_tex_sec;
    private ShaderProgram sh_draw_surface, sh_draw_surface_alpha;
    private Array<ShaderProgram> sh_merge_layers;
    private int tex_limit;
    private ShaderMan sh_reader;

    // -------------------------------------------------------------------------
    public Pixelmap(Surface surface, int tex_limit) {
        super(Obj.MISC.ptr, Obj.MISC.PIXELMAP);

        this.surface = surface;
        this.tex_limit = tex_limit;

        // Layer
        this.layer = new Layer[Pixelmap.ID_GROUP.GetSize()];
        this.layer_active = new Array<Layer>();
        this.layer_visible = new Array<Layer>();
        this.sh_merge_layers = new Array<ShaderProgram>();

        // Create mesh for fbo rendering
        Layer.fbo_mesh = new CommonObject.CommonMesh(true, 4, 6, 
          new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
          new VertexAttribute(Usage.TextureCoordinates, 2,ShaderProgram.TEXCOORD_ATTRIBUTE));

        Layer.fbo_mesh.obj.setVertices(new float[] { 
          -1.0f, -1.0f,       0.0f, 0.0f, 
           1.0f, -1.0f,       1.0f, 0.0f, 
          -1.0f, 1.0f,        0.0f, 1.0f, 
           1.0f, 1.0f,        1.0f, 1.0f });

        Layer.fbo_mesh.obj.setIndices(new short[] { 0, 3, 1, 0, 2, 3 });

        // Set alpha blending for textures
        Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // -------------------------------------------------------------------------
    public int GetLayerCnt() { return this.layer.length; }
    public Utils.Vector2i GetSize() { return this.surface.size; }

    // -------------------------------------------------------------------------
    public void InitLayer(LayerParam param) {
        // Layer should not be initialized twice
        Utils.Assert(this.layer[param.id] == null, 
          "Layer is already initialized :: [id=%d]", param.id);

        // Add layer to active list
        this.layer[param.id] = new Layer(Color.BLACK, this.surface.size, param);
    }

    // -------------------------------------------------------------------------
    public void FinalizeLayerParam() {
        this.layer_active.clear();
        this.layer_visible.clear();

        for(int i = 0; i < this.layer.length; i++) {
            Layer l = this.layer[i];
            if(l == null) continue;

            // Set mask
            l.mask = Utils.IsDefined(l.param.mask_id) ? this.layer[l.param.mask_id] : null;

            // Add to active list
            this.layer_active.add(l);

            // Visible list
            if(l.param.is_visible) {
                this.layer_visible.add(l);
            }
        }

        // Create primary final texture
        this.surface_tex = new Array<TextureDesc>();
        this.surface_tex_prim = new TextureDesc(true, this.surface.size, 32);
        this.surface_tex.add(this.surface_tex_prim);

        // Create secondary final texture
        if(this.layer_active.size > this.tex_limit) {
            this.surface_tex_sec = new TextureDesc(true, this.surface.size, 32);
            this.surface_tex.add(this.surface_tex_sec);
        } else {
            this.surface_tex_sec = null;
        }
    }

    // -------------------------------------------------------------------------
    public void InitShaders() {
        // Reader
        this.sh_reader = new ShaderMan();
        Logger.d(Logger.MOD_MISC, "Loading shaders ::");

        // .....................................................................
        // SHADER :: draw
        Layer.sh_draw = sh_reader.LoadShader(
          "shader/draw.frag.glsl", null,
          "shader/mvp_none.vert.glsl", null, false);

        // .....................................................................
        // SHADER :: draw
        Layer.sh_draw_mask = sh_reader.LoadShader(
          "shader/draw_mask.frag.glsl", null,
          "shader/mvp_none.vert.glsl", null, false);

        // .....................................................................
        // SHADER :: alpha_shift
        Layer.sh_alpha_shift = sh_reader.LoadShader(
          "shader/alpha_shift.frag.glsl", null,
          "shader/mvp_none.vert.glsl", null, false);

        // .....................................................................
        // SHADER :: alpha_shift_mask
        Layer.sh_alpha_shift_mask = sh_reader.LoadShader(
          "shader/alpha_shift_mask.frag.glsl", null,
          "shader/mvp_none.vert.glsl", null, false);

        // .....................................................................
        // SHADER :: tx_shift
        Layer.sh_tx_shift = sh_reader.LoadShader(
          "shader/tx_shift.frag.glsl", null,
          "shader/mvp_none.vert.glsl", null, false);

        // .....................................................................
        // SHADER :: tx_shift_mask
        Layer.sh_tx_shift_mask = sh_reader.LoadShader(
          "shader/tx_shift_mask.frag.glsl", null,
          "shader/mvp_none.vert.glsl", null, false);

        // .....................................................................
        // SHADER :: merge_blur
        Map<Integer, ShaderProgram> cnt_sh_map = new HashMap<Integer, ShaderProgram>();
        for(int i = 0; i < this.layer_active.size; i++) {
            // No blur
            Layer l = this.layer_active.get(i);
            if(l.param.fx != Pixelmap.FX_BLUR) {
                continue;
            }

            // Test whether shader is already created
            int blur_cnt = l.param.blur_alpha.length;
            l.sh_merge_blur = cnt_sh_map.get(blur_cnt);
            if(l.sh_merge_blur != null) {
                continue;
            }

            // Create iteration list
            String[] idx_seq = Utils.GetSeqList(0, blur_cnt - 1); // u_tex
            String[] idx_max = Utils.GetSeqList(0, blur_cnt - 1); // u_alpha
            String[][][] it_list = new String[][][] {
              new String[][] { idx_seq }, 
              new String[][] { idx_max },
              new String[][] { idx_seq, idx_seq } };

            // Load shader
            l.sh_merge_blur = sh_reader.LoadShader(
              "shader/merge_blur.frag.glsl", it_list,
              "shader/mvp_none.vert.glsl", null, false);

            cnt_sh_map.put(blur_cnt, l.sh_merge_blur);
        }

        // .....................................................................
        // SHADER :: merge_blur_prim
        Layer.sh_merge_blur_prim = sh_reader.LoadShader(
          "shader/merge_blur_prim.frag.glsl", null,
          "shader/mvp_none.vert.glsl", null, false);

        // .....................................................................
        // SHADER :: merge_layers
        this.sh_merge_layers.clear();

        // First layer list
        Array<Layer> layer_list = new Array<Layer>();
        this.layer_merge = new Array<Array<Layer>>();
        this.layer_merge.add(layer_list);

        // First shader is limited by 'tex_limit' layers  
        int limit = this.tex_limit;
        String[] tex_def_seq = new String[this.layer_active.size];
        String[] col_def_seq = new String[this.layer_active.size];

        // Build definition and layer lists
        for(int i = 0; i < this.layer_active.size; i++) {
            // Add to texture and color definitions
            Layer l = this.layer_active.get(i);
            tex_def_seq[i] = l.uni_tex_name;
            col_def_seq[i] = l.uni_col_name;

            // Ignore bottom layer
            if(i == 0) continue;

            // Add layer to list
            layer_list.add(l);

            // Test whether current layer list is full
            if(layer_list.size >= limit) {
                // Create new layer list
                layer_list = new Array<Layer>();
                this.layer_merge.add(layer_list);

                // Following shaders are limited by 'tex_limit - 1' layers
                limit = this.tex_limit - 1;
            }
        }

        // Create shader
        String[] merge_tex_seq, merge_col_seq, col_initial; 
        for(int i = 0; i < this.layer_merge.size; i++) {
            // Set initial color 
            Layer bottom_layer = this.layer_active.get(0);
            col_initial = new String[] { (i == 0) ? 
              bottom_layer.uni_col_name :                   // As color of bottom layer 
              String.format("texture2D(%s, v_coord.st)",    // As texture of bottom layer 
                bottom_layer.uni_tex_name) };

            // Merged texture & color names
            layer_list = this.layer_merge.get(i);
            merge_tex_seq = new String[layer_list.size];
            merge_col_seq = new String[layer_list.size];
            for(int j = 0; j < layer_list.size; j++) {
                Layer l = layer_list.get(j);
                merge_tex_seq[j] = l.uni_tex_name;
                merge_col_seq[j] = l.uni_col_name;
            }

            // Iterator list
            String[][][] it_list = new String[][][] { 
              new String[][] { tex_def_seq },                   // texture definition
              new String[][] { col_def_seq },                   // color definition
              new String[][] { col_initial },                   // initial color
              new String[][] { merge_tex_seq, merge_col_seq }}; // merge

            // Load shader
            this.sh_merge_layers.add(sh_reader.LoadShader(
              "shader/merge_layers.frag.glsl", it_list,
              "shader/mvp_none.vert.glsl", null, false));
        }

        // .....................................................................
        // SHADER :: draw_surface
        this.sh_draw_surface = sh_reader.LoadShader(
          "shader/draw.frag.glsl", null,
          "shader/mvp.vert.glsl", null, false);

        // .....................................................................
        // SHADER :: draw_surface_alpha
        this.sh_draw_surface_alpha = sh_reader.LoadShader(
          "shader/draw_alpha_mask.frag.glsl", null,
          "shader/mvp.vert.glsl", null, false);
    }

    // -------------------------------------------------------------------------
    private void SwapFinalSurfaceTex() {
        if(this.surface_tex_sec == null) return;

        TextureDesc tmp = this.surface_tex_sec;
        this.surface_tex_sec = this.surface_tex_prim;
        this.surface_tex_prim = tmp;
    }

    // -------------------------------------------------------------------------
    private void MergeLayers() {
        for(int i = 0; i < this.layer_merge.size; i++) {
            // Merger layers into one texture
            ShaderProgram sh = this.sh_merge_layers.get(i);
            FrameBuffer fbo = this.surface_tex_prim.fbo.obj;
            sh.begin(); fbo.begin();

            // Set initial color
            int tex_idx = 0;
            Layer bottom_layer = this.layer_active.get(0);
            if(i == 0) {
                // Color of botom layer
                sh.setUniform3fv(bottom_layer.uni_col_name, bottom_layer.color_buf, 0, 3);
            } else {
                // Texture of bottom layer
                Pixelmap.BindShaderTexture(sh, tex_idx++, this.surface_tex_sec.tex_native, 
                  bottom_layer.uni_tex_name);
            }

            // Set layer texture & color
            Array<Layer> layer_list = this.layer_merge.get(i);
            for(int j = 0; j < layer_list.size; j++) {
                Layer l = layer_list.get(j);
                Pixelmap.BindShaderTexture(sh, tex_idx++, l.final_tex.tex_native, l.uni_tex_name);
                sh.setUniform3fv(l.uni_col_name, l.color_buf, 0, 3);
            }

            // Render
            Layer.fbo_mesh.obj.render(sh, GL20.GL_TRIANGLES);
            fbo.end(); sh.end();

            // Do not swap surface for last shader
            if(i != this.layer_merge.size - 1) {
                this.SwapFinalSurfaceTex();
            }
        }
    }

    // -------------------------------------------------------------------------
    private void DrawSurface(Matrix4 mvp_matrix, Surface surface, int copy_cnt, 
      Texture tex, float alpha) {
        // Start shader
        ShaderProgram sh = (alpha == 0.0f) ? this.sh_draw_surface : this.sh_draw_surface_alpha;
        sh.begin();

        // Surface texture
        Pixelmap.BindShaderTexture(sh, 0, tex, "u_tex");

        // Alpha value
        if(alpha > 0.0f) {
            sh.setUniformf("u_alpha", alpha);
        }

        // Render
        for(int i = 0; i <= copy_cnt; i++) {
            surface.Render(sh, mvp_matrix, i * surface.GetSize().x, 0, 0);
        }

        // Finish shader
        sh.end();
    }

    // -------------------------------------------------------------------------
    public Color PrepareLayers() {
        Color rc = null;
        this.is_color_changing = false;
        for(int i = 0; i < this.layer_visible.size; i++) {
            Layer l = this.layer_visible.get(i);

            if(l.IsActive()) {
                // Continue fading
                Color c = l.Continue(l.start_color, l.stop_color);
                this.is_color_changing = true;

                // Update layer color buffer 
                l.color_buf[0] = c.r; l.color_buf[1] = c.g; l.color_buf[2] = c.b;

                // Return new color of background
                if(i == 0) rc = c;
            }
        }
        return rc;
    }

    // -------------------------------------------------------------------------
    public void Render(Matrix4 mvp_matrix, int copy_cnt) {
        // Clear update flag
        for(int i = 1; i < this.layer_active.size; i++) {
            this.layer_active.get(i).is_updated = false;
        }

        // Update layers
        boolean is_pixmap_updated = false;
        for(int i = 1; i < this.layer_active.size; i++) {
            Layer l = this.layer_active.get(i);

            // Handle update flags
            boolean is_mask_updated = (l.mask == null) ? false : l.mask.is_updated;
            l.is_updated = is_mask_updated | l.primary_sn.IsLocallyUpdated();

            // Send primary snapshot to GPU
            if(l.primary_sn.IsLocallyUpdated()) {
                l.SendToGpu(l.primary_sn);
                is_pixmap_updated = true;
            }

            // Default
            if(l.param.fx == Pixelmap.FX_DEFAULT) {
                if(is_mask_updated) {
                    l.FxMask();
                    is_pixmap_updated = true;
                }

            // Blur
            } else if(l.param.fx == Pixelmap.FX_BLUR) {
                // Make blur shift
                boolean is_blur_updated = false;
                if(l.IsBlurActive() && l.IsShiftBlurTime()) {
                    l.FxBlur();
                    is_pixmap_updated = true;
                    is_blur_updated = true;
                }

                // Merge blur with primary
                if(l.is_updated || is_blur_updated) {
                    l.FxBlurPrimMerge();
                }

            // Alpha-shift
            } else if(l.param.fx == Pixelmap.FX_ALPHA_SHIFT) {
                if(l.IsShiftAlphaTime()) {
                    l.FxAlphaShift();
                    is_pixmap_updated = true;
                }

            // Tx-shift
            } else if(l.param.fx == Pixelmap.FX_TX_SHIFT) {
                if(l.IsShiftTxTime()) {
                    l.FxTxShift();
                    is_pixmap_updated = true;
                }

            // Error
            } else {
                Utils.Assert(false, "Unknown Fx");
            }

            // Make layer clean if not fading
            if(!l.param.is_fading) {
                l.primary_sn.state = Pixelmap.ST_CLEAN;
                continue;
            }

            //
            // Handle fading
            //

            // Clear local changes
            if(l.primary_sn.state == Pixelmap.ST_SET_GPU) {
                l.Clear();

            // Mark layer cleaned
            } else if(l.primary_sn.state == Pixelmap.ST_UNSET_GPU) {
                l.primary_sn.state = Pixelmap.ST_CLEAN;
            }
        }

        // Merge layer if updated
        if(is_pixmap_updated || this.is_color_changing) {
            this.MergeLayers();
        }

        // Draw surface
        this.DrawSurface(mvp_matrix, this.surface, copy_cnt, this.surface_tex_prim.tex_native, 0.0f);
    }

    // -------------------------------------------------------------------------
    public void SetPixel(int id, int x, int y, float alpha) {
        Layer l = this.layer[id];
        if(l == null) return;
        l.SetPixel(x, y, alpha);
    }

    // -------------------------------------------------------------------------
    public void SetMap(int id, int x, int y, Map2d<Float> map) {
        Layer l = this.layer[id];
        if(l == null) return;
        l.SetMap(x, y, map);
    }

    // -------------------------------------------------------------------------
    public void StartLayerFading(int id, Color color, long duration) {
        Layer l = this.layer[id];
        if(l == null) return;
        l.Start(color, duration);
    }

    // -------------------------------------------------------------------------
    private static void BindShaderTexture(ShaderProgram sh, int idx, Texture tex, String name) {
        tex.bind(idx);
        sh.setUniformi(name, idx);
    }

    // -------------------------------------------------------------------------
    public void Dispose() {
        // Layers
        for(Layer l: this.layer_active) {
            l.Dispose();
        }

        // Surface textures
        for(TextureDesc t: this.surface_tex) {
            t.Dispose();
        }

        // Shader
        this.sh_reader.Dispose();       this.sh_reader = null;

        // Mesh
        this.surface.mesh.Dispose();    this.surface.mesh = null;
        Layer.fbo_mesh.Dispose();       Layer.fbo_mesh = null;
        super.Dispose();
    }
}
