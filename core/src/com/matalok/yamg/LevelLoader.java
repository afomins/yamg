// -----------------------------------------------------------------------------
package com.fomin.yamg;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.utils.XmlReader;
import com.fomin.yamg.TaskMan.Task;
import com.fomin.yamg.fallingsand.Material;
import com.fomin.yamg.game.GameMan;
import com.fomin.yamg.pixelmap.Pixelmap;

// -----------------------------------------------------------------------------
public class LevelLoader extends ServiceMan.Service implements TaskMan.ITaskListener {
    // -------------------------------------------------------------------------
    public static final int ST_READ_MANIFEST = 0;
    public static final int ST_LOAD_MAP = 1;
    public static final int ST_LOAD_BACKGROUND = 2;
    public static final int ST_LOAD_BACKGROUND_MASK = 3;
    public static final int ST_LOAD_ROCK_DETAIL = 4;
    public static final int ST_LOAD_SAND_DETAIL = 5;
    public static final int ST_LOAD_GOLD_DETAIL = 6;
    public static final int ST_CREATE_ENVIRONMENT = 7;
    public static final int ST_UPDATE_ENVIRONMENT = 8;
    public static final Obj.Group ST_GROUP = new Obj.Group("load-level",
      new String [] { "read-manifest", "load-main", "load-background", 
        "load-background-mask", "load-rock-detail", "load-sand-detail", 
        "load-gold-detail", "create-environment", "update-environment"});

    // -------------------------------------------------------------------------
    private TaskMan.ITaskListener listener;
    private int task_id;

    private String file_name, level_name, author;
    private String map_path;
    private String background_path, background_mask_path;
    private String rock_detail_path, sand_detail_path, gold_detail_path;
    private boolean is_resetting;

    private Map2d<Byte> main_map;
    private Map2d<Float> back_map, back_mask_map;
    private Map2d<Float> rock_map, sand_map, gold_map;

    public Map2d.Colormap<Float> cm_grayscale;
    public Map2d.Colormap<Byte> cm_material;

    private Material[] material_param;
    private Pixelmap.LayerParam[] layer_param;

    // -------------------------------------------------------------------------
    public LevelLoader() {
        super(Obj.SERVICE.LEVEL);
        this.task_id = Utils.ID_UNDEFINED;
    }

    // -------------------------------------------------------------------------
    // Service pointer
    public static LevelLoader p;
    protected void AcquireServicePointer() { LevelLoader.p = this; };
    protected void ReleaseServicePointer() { LevelLoader.p = null; };

    // -------------------------------------------------------------------------
    protected void OnServiceSetup(XmlReader.Element cfg) { 
        // Colormaps
        this.cm_grayscale = Map2d.GetGrayscaleColormap();
        this.cm_material = Map2d.GetRgbColormap(
          CfgReader.GetAttrib(cfg, "config:material-palette"));
        Utils.Assert(this.cm_material.size == Material.T_CNT, "Invalid colormap size");

        // Material param
        this.material_param = new Material[Material.T_CNT];
        this.UpdateMaterial(cfg);

        // Layer param
        this.layer_param = new Pixelmap.LayerParam[Pixelmap.ID_GROUP.GetSize()];
        this.UpdateLayerParam(cfg);
    }

    // -------------------------------------------------------------------------
    private void UpdateMaterial(XmlReader.Element cfg) {
        // Clear material
        for(int i = 0; i < this.material_param.length; i++) {
            this.material_param[i] = null;
        }

        // Material profile
        String profile_name = CfgReader.GetAttrib(cfg, "config:material-profile");
        cfg = CfgReader.GetChild(cfg, String.format("material:%s", profile_name));

        // Iterate material
        Logger.d(Logger.MOD_LVL, "Init material ::");
        for(int i = 0; i < cfg.getChildCount(); i++) {
            XmlReader.Element e = cfg.getChild(i);

            // Material attributes
            Material m = new Material();
            m.type = (byte)Material.T_GROUP.GetEntryIdx(CfgReader.GetAttrib(e, "id"), true);
            m.alpha = Utils.Str2Float(CfgReader.GetAttrib(e, "alpha"));
            m.is_static = Utils.Str2Bool(CfgReader.GetAttrib(e, "is_static"));
            m.density = (byte)Material.DENSITY_GROUP.GetEntryIdx(CfgReader.GetAttrib(e, "density"), true);
            m.delay = Utils.Str2Int(CfgReader.GetAttrib(e, "delay"));
            m.accel = Utils.Str2Int(CfgReader.GetAttrib(e, "accel"));
            m.min_delay = Utils.Str2Int(CfgReader.GetAttrib(e, "min-delay"));

            // Finalize and save
            m.Finalize();
            this.material_param[m.type] = m;
        }
    }

    // -------------------------------------------------------------------------
    private void UpdateLayerParam(XmlReader.Element cfg) {
        // Clear layer param
        for(int i = 0; i < this.layer_param.length; i++) {
            this.layer_param[i] = null;
        }

        // Layer profile
        String profile_name = CfgReader.GetAttrib(cfg, "config:layer-profile");
        cfg = CfgReader.GetChild(cfg, String.format("layer:%s", profile_name));

        // Iterate layers
        Logger.d(Logger.MOD_LVL, "Init layer ::");
        for(int i = 0; i < cfg.getChildCount(); i++) {
            // Ignore commented attributes
            XmlReader.Element e = cfg.getChild(i);
            if(CfgReader.IsCommented(e)) continue;

            // Common param
            Pixelmap.LayerParam param = new Pixelmap.LayerParam();
            param.id = Pixelmap.ID_GROUP.GetEntryIdx(CfgReader.GetAttrib(e, "id"), true);
            param.mask_id = Pixelmap.ID_GROUP.GetEntryIdx(CfgReader.GetAttrib(e, "mask"), false);
            param.fx = Pixelmap.FX_GROUP.GetEntryIdx(CfgReader.GetAttrib(e, "fx"), true);
            param.is_fading = Utils.Str2Bool(CfgReader.GetAttrib(e, "is_fading"));
            param.is_visible = Utils.Str2Bool(CfgReader.GetAttrib(e, "is_visible"));

            // Blur param
            if(param.fx == Pixelmap.FX_BLUR) {
                param.fx_freq = Utils.Str2Int(CfgReader.GetAttrib(e, "freq"));
                param.blur_alpha = Utils.Str2FloatArray(CfgReader.GetAttrib(e, "alpha"));

            // Alpha-shift param
            } else if(param.fx == Pixelmap.FX_ALPHA_SHIFT || param.fx == Pixelmap.FX_TX_SHIFT) {
                param.fx_freq = Utils.Str2Int(CfgReader.GetAttrib(e, "freq"));
            }

            // Save layer param
            this.layer_param[i] = param;

            // Link material with layer
            byte material_id = (byte) (Material.T_GROUP.GetEntryIdx(CfgReader.GetAttrib(e, "material"), true));
            if(material_id != Material.T_EMPTY) {
                Utils.Assert(this.material_param[material_id] != null, "Material is not initialized :: [id=%d]", material_id);
                this.material_param[material_id].LinkLayer(param.id);
            }

            // Log
            Logger.d(Logger.MOD_LVL, "  [id=%s] [mask=%s] [fx=%s] [material=%s] [is-fading=%s] [is-visible=%s] [alpha=%s]",
              Pixelmap.ID_GROUP.GetEntryName(param.id, true), 
              Pixelmap.ID_GROUP.GetEntryName(param.mask_id, false),
              Pixelmap.FX_GROUP.GetEntryName(param.fx, true),
              Material.T_GROUP.GetEntryName(material_id, true),
              Utils.Bool2Str(param.is_fading),
              Utils.Bool2Str(param.is_visible),
              (param.blur_alpha == null) ? "none" : Utils.FloatArray2Str(param.blur_alpha,  "%.1f"));
        }
    }

    // -------------------------------------------------------------------------
    public CommonTask.SleepTask LoadLevel(String name, TaskMan.ITaskListener listener) {
        // Level name
        this.is_resetting = (name.equals(this.file_name));
        this.file_name = name;

        // Log
        if(this.is_resetting) {
            Logger.d(Logger.MOD_LVL, "Resetting current level :: [name=%s]", 
              this.file_name);
        }

        // Allow only one simultaneous loading instance
        Utils.Assert(this.task_id == Utils.ID_UNDEFINED, 
          "Already loading level :: [task-id=%d]", this.task_id);

        // Dummy sleep task for each step
        CommonTask.SleepTask t = new CommonTask.SleepTask(0, this, LevelLoader.ST_GROUP.GetSize());
        this.task_id = t.GetObjId();
        this.listener = listener;
        return t;
    }

    // -------------------------------------------------------------------------
    private void HandleStep(int step) {
        // Ignore steps until ST_CREATE_ENVIRONMENT if resetting 
        if(this.is_resetting && step < LevelLoader.ST_CREATE_ENVIRONMENT) {
            // Do nothing

        // Read manifest
        } else if(step == LevelLoader.ST_READ_MANIFEST) {
            this.ReadManifest();

        // Load map
        } else if(step == LevelLoader.ST_LOAD_MAP) {
            this.main_map = new Map2d<Byte>(this.map_path, Material.T_EMPTY, this.cm_material);

        // Load background
        } else if(step == LevelLoader.ST_LOAD_BACKGROUND) {
            this.back_map = new Map2d<Float>(this.background_path, 0.0f, this.cm_grayscale);

        // Load background mask
        } else if(step == LevelLoader.ST_LOAD_BACKGROUND_MASK) {
            this.back_mask_map = new Map2d<Float>(this.background_mask_path, 0.0f, this.cm_grayscale);

        // Load rock-detail
        } else if(step == LevelLoader.ST_LOAD_ROCK_DETAIL) {
            this.rock_map = new Map2d<Float>(this.rock_detail_path, 0.0f, this.cm_grayscale);

        // Load sand detail
        } else if(step == LevelLoader.ST_LOAD_SAND_DETAIL) {
            this.sand_map = new Map2d<Float>(this.sand_detail_path, 0.0f, this.cm_grayscale);

        // Load gold detail
        } else if(step == LevelLoader.ST_LOAD_GOLD_DETAIL) {
            this.gold_map = new Map2d<Float>(this.gold_detail_path, 0.0f, this.cm_grayscale);

        // Create environment
        } else if(step == LevelLoader.ST_CREATE_ENVIRONMENT) {
            this.CreateEnvironment();

        // Create viewer
        } else if(step == LevelLoader.ST_UPDATE_ENVIRONMENT) {
            this.UpdateEnvironment();

        // Darn...
        } else {
            Utils.Assert(false, "Invalid step :: [step=%d]", step);
        }
    }

    // -------------------------------------------------------------------------
    public void OnTaskSetup(Task t) { }
    public void OnTaskRun(Task t) { }

    // -------------------------------------------------------------------------
    public void OnTaskTeardown(Task t) {
        // Called multiple times when dummy-sleep-task dies

        // Must be a valid timer task
        Utils.Assert(t.GetObjId() == this.task_id, "Invalid task :: [name=%s]", 
          t.GetObjName());

        // Log
        int step = t.GetTaskIter();
        Logger.d(Logger.MOD_LVL, "Loading level :: [step=%s] [progress=%.0f%%]", 
          LevelLoader.ST_GROUP.GetEntryName(step, true), t.GetTaskProgress() * 100f);

        // Handle step
        this.HandleStep(step);

        // Notify listener that step is over 
        this.listener.OnTaskTeardown(t);

        // Loading is over
        if(t.IsTaskLastIter()) {
            this.task_id = Utils.ID_UNDEFINED;
            this.listener = null;
        }
    }

    // -------------------------------------------------------------------------
    private void ReadManifest() {
        // Open level manifest
        String prefix = String.format("level/%s", this.file_name);
        XmlReader.Element cfg = CfgReader.Read(
          String.format("%s/manifest.xml", prefix, file_name));

        // Read manifest
        this.level_name = CfgReader.GetAttrib(cfg, "general:config:name");
        this.author = CfgReader.GetAttrib(cfg, "general:config:author");
        this.map_path = LevelLoader.GetCfgAttrib(cfg, prefix, "general:config:map");
        this.background_path = LevelLoader.GetCfgAttrib(cfg, prefix, "general:config:background");
        this.background_mask_path = LevelLoader.GetCfgAttrib(cfg, prefix, "general:config:background-mask");
        this.rock_detail_path = LevelLoader.GetCfgAttrib(cfg, prefix, "general:config:rock-detail");
        this.sand_detail_path = LevelLoader.GetCfgAttrib(cfg, prefix, "general:config:sand-detail");
        this.gold_detail_path = LevelLoader.GetCfgAttrib(cfg, prefix, "general:config:gold-detail");

        // Log
        Logger.d(Logger.MOD_LVL, "Reading manifest ::");
        Logger.d(Logger.MOD_LVL, "  [path=%s]", prefix);
        Logger.d(Logger.MOD_LVL, "  [name=%s] [author=%s]", this.level_name, this.author);
        Logger.d(Logger.MOD_LVL, "  [back=%s] [back-mask=%s]", this.background_path, this.background_mask_path);
        Logger.d(Logger.MOD_LVL, "  [rock-detail=%s]", this.rock_detail_path);
        Logger.d(Logger.MOD_LVL, "  [sand-detail=%s]", this.sand_detail_path);
        Logger.d(Logger.MOD_LVL, "  [gold-detail=%s]", this.gold_detail_path);
    }

    // -------------------------------------------------------------------------
    private void CreateEnvironment() {
        GameMan.p.CreateMap(this.main_map, this.back_map, this.back_mask_map, 
          this.rock_map, this.sand_map, this.gold_map, 
          this.layer_param, this.material_param);
    }

    // -------------------------------------------------------------------------
    private void UpdateEnvironment() {
        GameMan.p.UpdateMap(this.main_map, this.back_map, this.back_mask_map, 
          this.rock_map, this.sand_map, this.gold_map);
    }

    // -------------------------------------------------------------------------
    public boolean IsLoaded() {
        return (this.task_id == Utils.ID_UNDEFINED);
    }

    // -------------------------------------------------------------------------
    private static String GetCfgAttrib(XmlReader.Element cfg, String prefix, String name) {
        return String.format("%s/%s", prefix, CfgReader.GetAttrib(cfg, name));
    }
}
