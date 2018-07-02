// -----------------------------------------------------------------------------
package com.matalok.yamg.fallingsand;

//-----------------------------------------------------------------------------
import com.matalok.yamg.Logger;
import com.matalok.yamg.Obj;
import com.matalok.yamg.Utils;

// -----------------------------------------------------------------------------
public class Material {
    // -------------------------------------------------------------------------
    public static final byte T_EMPTY = 0;
    public static final byte T_ROCK = 1;
    public static final byte T_SAND = 2;
    public static final byte T_GOLD = 3;
    public static final byte T_WATER = 4;
    public static final byte T_CNT = 5;
    public static final Obj.Group T_GROUP = new Obj.Group("material-type", 
      new String [] {"empty", "rock", "sand", "gold", "water"});

    public static final byte DENSITY_GASEOUS = 0;
    public static final byte DENSITY_LIQUID = 1;
    public static final byte DENSITY_LOOSE = 2;
    public static final byte DENSITY_SOLID = 3;
    public static final Obj.Group DENSITY_GROUP = new Obj.Group("materail-density", 
      new String [] {"gaseous", "liquid", "loose", "solid"});

    // -------------------------------------------------------------------------
    public byte type;
    public float alpha;
    public boolean is_static;
    public byte density;
    public long delay, min_delay, accel;
    public int linked_layer_id;

    // -------------------------------------------------------------------------
    public Material() { }

    // -------------------------------------------------------------------------
    public Material(byte type, float alpha, boolean is_static, byte density, long delay, 
      long accel, float min_delay) {
        this.type = type;
        this.is_static = is_static;
        this.density = density;
        this.alpha = alpha;
        this.delay = delay;
        this.accel = accel;
        this.min_delay = (long)min_delay * delay;
        this.linked_layer_id = Utils.ID_UNDEFINED;
    }

    // -------------------------------------------------------------------------
    public void Finalize() {
        this.min_delay = (long)(this.min_delay / 100.0f * this.delay);
        this.linked_layer_id = Utils.ID_UNDEFINED;
        this.Log();
    }

    // -------------------------------------------------------------------------
    public void Log() {
        Logger.d(Logger.MOD_LVL, "  [type=%s] [alpha=%.1f] [is-static=%s] [density=%s] [delay=%d] [min-delay=%d] [accel=%d]",
          Material.T_GROUP.GetEntryName(this.type, true),
          this.alpha, Utils.Bool2Str(this.is_static),
          Material.DENSITY_GROUP.GetEntryName(this.density, true),
          this.delay, this.min_delay, this.accel);
    }

    // -------------------------------------------------------------------------
    public void LinkLayer(int id) {
        Utils.Assert(!Utils.IsDefined(this.linked_layer_id), 
          "Layer already linked to material :: [id=%d]", this.linked_layer_id);
        this.linked_layer_id = id;
    }

    // -------------------------------------------------------------------------
    public boolean IsStatic() { return this.is_static; }
    public boolean IsEmpty() { return (this.type == Material.T_EMPTY) ? true : false; }
    public boolean IsSolid() { return (this.density == Material.DENSITY_SOLID) ? true : false; }
    public boolean IsLoose() { return (this.density == Material.DENSITY_LOOSE) ? true : false; }
    public boolean IsLiquid() { return (this.density == Material.DENSITY_LIQUID) ? true : false; }
}
