// -----------------------------------------------------------------------------
package com.matalok.yamg.game;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.XmlReader;
import com.matalok.yamg.CfgReader;
import com.matalok.yamg.CommonTask;
import com.matalok.yamg.LevelLoader;
import com.matalok.yamg.Logger;
import com.matalok.yamg.Map2d;
import com.matalok.yamg.Obj;
import com.matalok.yamg.Palette;
import com.matalok.yamg.ServiceMan;
import com.matalok.yamg.TaskMan;
import com.matalok.yamg.Utils;
import com.matalok.yamg.TaskMan.Task;
import com.matalok.yamg.fallingsand.FallingSand;
import com.matalok.yamg.fallingsand.Material;
import com.matalok.yamg.game.GameInput.GestureDetectorEx;
import com.matalok.yamg.game.GameInput.GestureListenerEx;
import com.matalok.yamg.pixelmap.Pixelmap;
import com.matalok.yamg.pixelmap.Surface;
import com.matalok.yamg.ui.UiMan;

// -----------------------------------------------------------------------------
public class GameMan extends ServiceMan.Service implements TaskMan.ITaskListener {
    // =========================================================================
    // ViewerMovementProxy
    public static class ViewerMovementProxy extends Obj.CommonObject 
      implements CommonTask.IMoveTaskObject {
        // ---------------------------------------------------------------------
        private Utils.Vector2f cur_pos;

        // ---------------------------------------------------------------------
        public ViewerMovementProxy() {
            super(Obj.MISC.ptr, Obj.MISC.VIEWER_MOVEMENT_PROXY);
            this.cur_pos = new Utils.Vector2f();
        }

        // ---------------------------------------------------------------------
        public Utils.Vector2f GetMovePos() {
            return this.cur_pos;
        }

        // ---------------------------------------------------------------------
        public void SetMoveRelPos(float x, float y) {
            Utils.Assert(false, "Blah!!!");
        }

        // ---------------------------------------------------------------------
        public void SetMoveAbsPos(float x, float y) {
            // Move viewer
            float dx = x - this.cur_pos.x;
            float dy = y - this.cur_pos.y;
            GameMan.p.viewer.Move(-dx, -dy, 0.0f, true);

            // Mouse position
            this.cur_pos.Set(x, y);
        }

        // ---------------------------------------------------------------------
        public void Start(Utils.Vector2f pos) {
            this.cur_pos.Set(pos);
        }
    }

    // =========================================================================
    // ViewerMovementProxy
    public static class ViewerZoomProxy {
        // -------------------------------------------------------------------------
        private float orig_dist, cur_dist, orig_zoom_factor;
        private Utils.Vector2i orig_size;
        private boolean is_active;

        // -------------------------------------------------------------------------
        public ViewerZoomProxy() {
            this.orig_size = new Utils.Vector2i();
        }

        // -------------------------------------------------------------------------
        public void Update(float orig_dist, float cur_dist, float cur_zoom_factor, 
          Utils.Vector2i cur_size) {
            // Update original & current distance
            this.orig_dist = orig_dist;
            this.cur_dist = cur_dist;

            // Save zoom-factor when idle
            if(!this.is_active) {
                this.orig_zoom_factor = cur_zoom_factor;
                this.orig_size.Set(cur_size.x, cur_size.y);
                this.is_active = true;
            }
        }

        // -------------------------------------------------------------------------
        public boolean Finalize(boolean is_touched) {
            if(this.is_active) {
                this.is_active = is_touched;
                return true;
            }
            return this.is_active;
        }

        // -------------------------------------------------------------------------
        public float GetValue() {
            if(this.orig_dist == 0.0f) {
                return 0.1f;
            }

            this.cur_dist = this.orig_dist + (this.cur_dist - this.orig_dist);
            return this.orig_zoom_factor * (this.cur_dist / this.orig_dist);
        }

        // -------------------------------------------------------------------------
        public Utils.Vector2i GetMoveValue(Utils.Vector2i cur_size) {
            return Utils.v2i_tmp.Set(cur_size).Sub(this.orig_size).Div(2);
        }
    }

    // =========================================================================
    // MouseViewer
    private static class MouseViewer {
        // ---------------------------------------------------------------------
        private SoftMovement soft_move;
        private ViewerMovementProxy move_proxy;
        private ViewerZoomProxy zoom_proxy;

        private long stop_durtion;
        private boolean do_stop;
        private Utils.Direction2 stop_velocity;

        protected Utils.Vector2i pos;
        protected Utils.Vector2f posf;

        // ---------------------------------------------------------------------
        public MouseViewer(float speed) {
            this.pos = new Utils.Vector2i();
            this.posf = new Utils.Vector2f();
            this.move_proxy = new ViewerMovementProxy();
            this.zoom_proxy = new ViewerZoomProxy();
            this.soft_move = new SoftMovement(new Utils.Direction2(), -1.0f, speed, false);
            this.stop_velocity = new Utils.Direction2();
        }

        // ---------------------------------------------------------------------
        protected void Run() {
            this.RunMovementHandler();
            this.RunZoomHandler();
        }

        // ---------------------------------------------------------------------
        private void RunMovementHandler() {
            // Handle stop and convert velocity pix/sec -> pix/msec
            if(this.do_stop) {
                this.soft_move.Stop(this.stop_durtion,   
                  this.stop_velocity.len / 2000.0f, GameMan.p.viewer);

                // Reset velocity
                this.stop_velocity.len = 0.0f;
                this.do_stop = false;
            }

            // Update destination
            this.soft_move.UpdatePtpDestination(this.UpdateMousePos());
        }

        // ---------------------------------------------------------------------
        private void RunZoomHandler() {
            if(this.zoom_proxy.Finalize((Utils.is_desktop) ? false : Gdx.input.isTouched())) {
                GameMan.p.viewer.ZoomSet(this.zoom_proxy.GetValue());
            }
        }

        // ---------------------------------------------------------------------
        protected void StopMovement(long duration) {
            this.StopMovement(duration, 0, 0);
        }

        // ---------------------------------------------------------------------
        protected void StopMovement(long duration, float velocity_x, float velocity_y) {
            // Set stop flag 
            this.do_stop = true;
            this.stop_durtion = duration;

            // Update stop velocity
            if(velocity_x != 0.0 && velocity_y != 0.0f) {
                this.stop_velocity.Set(velocity_x, velocity_y);
                this.stop_velocity.Update();
            }
        }

        // ---------------------------------------------------------------------
        protected void StartMovement() {
            this.move_proxy.Start(this.UpdateMousePos());
            this.soft_move.Start(this.move_proxy, true);
        }

        // ---------------------------------------------------------------------
        private Utils.Vector2f UpdateMousePos() {
            this.pos.Set(Gdx.input.getX(), Gdx.input.getY());
            return this.posf.Set((float)this.pos.x, (float)this.pos.y);
        }

        // ---------------------------------------------------------------------
        protected void ResetVerticalDir() {
            this.soft_move.ResetDirection(false, true);
        }

        // ---------------------------------------------------------------------
        protected void UpdateZoom(float orig_dist, float cur_dist, float cur_zoom_factor, 
          Utils.Vector2i cur_size) {
            this.zoom_proxy.Update(orig_dist, cur_dist, cur_zoom_factor, cur_size);
        }
    }

    // =========================================================================
    // GameMan
    protected Surface surface;
    protected Viewer viewer;
    protected Pixelmap pm;
    protected FallingSand fs;
    private MouseViewer mv;

    private boolean is_gap_mhandler_enabled;
    private SoftMovement top_gap_move, bottom_gap_move, menu_move;

    private Palette.Map palette_map;
    private long palette_update_duration;

    protected float default_zoom;

    private GestureDetectorEx gesture_detector;
    private String startup_level_name;
    protected Map2d<Float> explosion_mesh;

    private int selected_object;

    // -------------------------------------------------------------------------
    public GameMan() {
        super(Obj.SERVICE.GAME);
    }

    // -------------------------------------------------------------------------
    // Service pointer
    public static GameMan p;
    protected void AcquireServicePointer() { GameMan.p = this; };
    protected void ReleaseServicePointer() { GameMan.p = null; };

    // -------------------------------------------------------------------------
    public InputProcessor GetIp() {
        return this.gesture_detector;
    }

    // -------------------------------------------------------------------------
    protected void OnServiceSetup(XmlReader.Element cfg) {
        // Palette
        this.palette_update_duration = Utils.Str2Int(
          CfgReader.GetAttrib(cfg, "config:palette-update-duration"));
        this.palette_map = new Palette.Map(new String[] {
          "palette/palette-002.gpl", 
          "palette/palette-001.gpl"});

        // Move handlers
        this.menu_move = new SoftMovement(new Utils.Direction2(1.0f, 0.0f), 1.0f, 0.05f, true);
        this.top_gap_move = new SoftMovement(new Utils.Direction2(0.0f, 1.0f), 1.0f, 0.3f, true);
        this.bottom_gap_move = new SoftMovement(new Utils.Direction2(0.0f, -1.0f), 1.0f, 0.3f, true);
        this.EnableGapHandler();

        // Mouse viewer
        this.mv = new MouseViewer(0.8f);

        // Explosion mesh
        this.explosion_mesh = new Map2d<Float>(
          CfgReader.GetAttrib(cfg, "config:explosion-mesh"), 
          0.0f, LevelLoader.p.cm_grayscale);

        // Input
        this.gesture_detector = new GestureDetectorEx(new GestureListenerEx());

        // Startup level
        this.startup_level_name = CfgReader.GetAttrib(cfg, "config:startup-level");
    }

    // -------------------------------------------------------------------------
    protected void OnServiceRun() {
        // Not ready yet
        if(!this.IsReady()) return;

        // Handle movement
        this.mv.Run();

        // Gap handler
        this.HandleGap();

        // Update falling-sand
        this.fs.FinalizeBlockMaterial();
        this.fs.FinalizeBlockActivity();

        // Apply camera
        this.viewer.Apply(this.pm.PrepareLayers());
        this.pm.Render(this.viewer.GetMatrixVP(), this.viewer.GetCloneCnt());

        // Mouse position
        Utils.Vector2i mouse_pos = this.viewer.GetSelectedPixel(
          this.mv.pos.x, this.mv.pos.y);

        // Draw cursor
        this.pm.SetMap(Pixelmap.ID_UI, mouse_pos.x - this.explosion_mesh.size.x / 2, 
          mouse_pos.y - this.explosion_mesh.size.y / 2, this.explosion_mesh);
    }

    // -------------------------------------------------------------------------
    protected void OnServiceTimer() { 
        // Not ready yet
        if(!this.IsReady()) return;

        float top_offset = -this.viewer.GetRelPos().y;
        float bottom_offset = -(top_offset + this.surface.GetVisibleSize().y - this.viewer.GetHeight());

        Logger.d(Logger.MOD_LVL, "Game-man :: [viewer-pos=%.1f:%.1f] [top=%.1f] [bottom=%.1f]", 
          this.viewer.GetMovePos().x, this.viewer.GetMovePos().y, top_offset, bottom_offset);
    }

    // -------------------------------------------------------------------------
    private void UpdatePalette() {
        Logger.d(Logger.MOD_LVL, "Update palette ::");

        // Select next palette from palette-map and start fading
        Palette cur_palette = this.palette_map.First();
        for(int i = 0; i < cur_palette.entry.size; i++) {
            // Get color from palette
            Palette.Entry entry = cur_palette.entry.get(i);
            Utils.Assert(entry.color != null, "Empty palette");

            // Get layer index 
            int id = Pixelmap.ID_GROUP.GetEntryIdx(entry.name, true);

            // Start fading
            this.pm.StartLayerFading(id, entry.color, this.palette_update_duration);
            Logger.d(Logger.MOD_LVL, "  [layer=%d:%s] [color=%s]", id, entry.name, 
              Utils.Int2Str(entry.bin32));
        }
    }

    // -------------------------------------------------------------------------
    private void ResetViewer() {
        int h = UiMan.p.screen_size.y;

        this.viewer.ZoomSet(this.default_zoom);
        this.viewer.Set(
          this.viewer.init_pos.x, 
          this.viewer.init_pos.y + (this.surface.GetVisibleSize().y - h) / 2.0f, 
          this.viewer.init_pos.z, true);
    }

    // -------------------------------------------------------------------------
    public void CreateMap(Map2d<Byte> main_map, Map2d<Float> back_map, Map2d<Float> back_mask_map, 
      Map2d<Float> rock_map, Map2d<Float> sand_map, Map2d<Float> gold_map, 
      Pixelmap.LayerParam [] layer_param, Material [] material_param) {

        // All maps should have same size
        Utils.Assert(main_map.size.Compare(back_map.size), "Map/background size mismatch");
        Utils.Assert(main_map.size.Compare(back_mask_map.size), "Map/background-mask size mismatch");
        Utils.Assert(main_map.size.Compare(rock_map.size), "Map/rock-pattern size mismatch");
        Utils.Assert(main_map.size.Compare(sand_map.size), "Map/sand-pattern size mismatch");
        Utils.Assert(main_map.size.Compare(gold_map.size), "Map/gold-pattern size mismatch");

        // Test whether environment needs to be re-create
        if(this.surface != null) {
            if(this.surface.GetSize().Compare(main_map.size)) {
                Logger.d(Logger.MOD_LVL, "Environment is OK, skipping this step");

                // Default viewer position
                this.ResetViewer();

                // Stop falling sand
                this.fs.Stop();
                return;
            }

            // Stop mouse handler 
            this.mv.StopMovement(0);

            // Stop all viewer movement handlers
            this.menu_move.Stop(0, 0.0f, null);
            this.top_gap_move.Stop(0, 0.0f, null);
            this.bottom_gap_move.Stop(0, 0.0f, null);

            // Dispose old resources 
            Logger.d(Logger.MOD_LVL, "Disposing old resources");
            this.pm.Dispose();
            this.viewer.Dispose();
        }

        // Create surface/viewer/pixelmap/fallingsand
        int w = UiMan.p.screen_size.x;
        int h = UiMan.p.screen_size.y;
        this.default_zoom = (float)h / (float)main_map.size.y * 1.02f;
        Logger.d(Logger.MOD_LVL, "Creating level environment level :: [screen-size=%d:%d] [map-size=%d:%d] [zoom=%.2f]", 
          w, h, main_map.size.x, main_map.size.y, this.default_zoom);

        // Surface
        this.surface = new Surface.Flat(false, false, main_map.size, 1.0f, 1.0f, 0.0f);

        // Viewer
        this.viewer = new Viewer.Orthographic(
          this.surface,
          new Vector3(w / 2.0f, h / 2.0f, -50.0f), 
          new Vector3(0.0f, 0.0f, 1.0f), 
          new Vector3(0.0f, -1.0f, 0.0f), 
          new Vector3(0.0f, 1.0f, 0.0f));

        this.ResetViewer();

        // Pixelmap
        this.pm = new Pixelmap(this.surface, 8);
        for(Pixelmap.LayerParam p: layer_param) {
            if(p != null) this.pm.InitLayer(p);
        }
        this.pm.FinalizeLayerParam();
        this.pm.InitShaders();

        // Falling sand
        this.fs = new FallingSand(this.pm, material_param, 0.2f);
    }

    // -------------------------------------------------------------------------
    public void UpdateMap(Map2d<Byte> main_map, Map2d<Float> back_map, Map2d<Float> back_mask_map, 
      Map2d<Float> rock_map, Map2d<Float> sand_map, Map2d<Float> gold_map) {
        // Update pixelmap
        this.pm.SetMap(Pixelmap.ID_BACK_SEC, 0, 0, back_map);
        this.pm.SetMap(Pixelmap.ID_BACK_SEC_MASK, 0, 0, back_mask_map);
        this.pm.SetMap(Pixelmap.ID_ROCK_DETAIL, 0, 0, rock_map);
        this.pm.SetMap(Pixelmap.ID_SAND_DETAIL, 0, 0, sand_map);
        this.pm.SetMap(Pixelmap.ID_GOLD_DETAIL, 0, 0, gold_map);

        // Update fallingsand
        this.fs.UpdateBlockMaterial(main_map);

        // Start palette update
        this.UpdatePalette();
    }

    // -------------------------------------------------------------------------
    public boolean IsReady() {
        return (LevelLoader.p.IsLoaded() && this.surface != null);
    }

    // -------------------------------------------------------------------------
    public String GetStartupLevelName() {
        return this.startup_level_name;
    }

    // -------------------------------------------------------------------------
    public void EnableGapHandler() {
        this.is_gap_mhandler_enabled = true;
    }

    // -------------------------------------------------------------------------
    public void DisableGapHandler() {
        this.is_gap_mhandler_enabled = false;
        this.top_gap_move.Stop(0, 0.0f, null);
        this.bottom_gap_move.Stop(0, 0.0f, null);
    }

    // -------------------------------------------------------------------------
    public void HandleGap() {
        if(!this.is_gap_mhandler_enabled) return;

        // Calculate gap
        float top_gap = -this.viewer.GetRelPos().y;
        float bottom_gap = -(top_gap + this.surface.GetVisibleSize().y - this.viewer.GetHeight());
        boolean is_top_gap = (top_gap > 0.0f), is_bottom_gap = (bottom_gap > 0.0f);

        // Correct top gap
        int pre_st_top = this.top_gap_move.GetState();
        if(is_top_gap && !is_bottom_gap) this.top_gap_move.Start(this.viewer, false);
        else                             this.top_gap_move.Stop(0, 0.0f, null);

        // Correct bottom gap
        int pre_st_bottom = this.bottom_gap_move.GetState();
        if(!is_top_gap && is_bottom_gap) this.bottom_gap_move.Start(this.viewer, false);
        else                             this.bottom_gap_move.Stop(0, 0.0f, null);

        // Reset vertical movement when gap correction was stopped
        int post_st_top = this.top_gap_move.GetState(), 
          post_st_bottom = this.bottom_gap_move.GetState();

        if((pre_st_top != SoftMovement.ST_IDLE && post_st_top == SoftMovement.ST_IDLE) ||
          (pre_st_bottom != SoftMovement.ST_IDLE && post_st_bottom == SoftMovement.ST_IDLE)) {
            this.mv.ResetVerticalDir();
        }
    }

    // -------------------------------------------------------------------------
    public void StartMenuMovement() {
        this.menu_move.Start(this.viewer, false);
    }

    // -------------------------------------------------------------------------
    public void StopMenuMovement() {
        this.menu_move.Stop(UiMan.p.wnd_scroll.GetDuration(), 0.0f, null);
    }

    // -------------------------------------------------------------------------
    public void StartMouseMovement() {
        this.mv.StartMovement();
        this.DisableGapHandler();
    }

    // -------------------------------------------------------------------------
    public void StopMouseMovement(float velocity_x, float velocity_y) {
        this.mv.StopMovement(1000, velocity_x, velocity_y);
        this.EnableGapHandler();
    }

    // -------------------------------------------------------------------------
    public void UpdateZoom(float orig_dist, float cur_dist, float cur_zoom_factor) {
        this.mv.UpdateZoom(orig_dist, cur_dist, cur_zoom_factor, 
          this.surface.GetVisibleSize());
    }

    // -------------------------------------------------------------------------
    public void Dispose() {
        if(this.pm != null) this.pm.Dispose();
        if(this.viewer != null) this.viewer.Dispose();
        if(this.mv.move_proxy != null) this.mv.move_proxy.Dispose();
        super.Dispose();
    }

    // -------------------------------------------------------------------------
    // ITaskListener
    public void OnTaskSetup(Task t) { }
    public void OnTaskRun(Task t) { }
    public void OnTaskTeardown(Task t) { }
}
