// -----------------------------------------------------------------------------
package com.matalok.yamg;

//-----------------------------------------------------------------------------
import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.ObjectMap;
import com.matalok.yamg.Utils.Vector2i;

// -----------------------------------------------------------------------------
public class Map2d<T> {
    // =========================================================================
    // TrimContext
    public static class TrimContext {
        // ---------------------------------------------------------------------
        public int left, right, bottom, top;
        public int trim_left, trim_right, trim_bottom, trim_top;
        public int offset_x, offset_y;

        // ---------------------------------------------------------------------
        public boolean Build(int x, int y, Utils.Vector2i src_size, 
          Utils.Vector2i dst_size, boolean test_visibility) {
            return this.Build(x, y, src_size.x, src_size.y, dst_size.x, dst_size.y, 
              test_visibility);
        }

        // ---------------------------------------------------------------------
        public boolean Build(int x, int y, int src_width, int src_height, 
          int dst_width, int dst_height, boolean test_visibility) {
            // Offset
            this.offset_x = x; this.offset_y = y;

            // Convert source coordinates to destination viewport
            this.left = x; this.right = x + src_width - 1;
            this.bottom = y; this.top = y + src_height - 1;

            // Test whether source is visible in destination viewport 
            if(test_visibility && (this.right < 0 || this.top < 0 || 
               this.left >= dst_width || this.bottom >= dst_height)) {
                return false;
            }

            // Trim source parts that are not visible in destination viewport
            if(this.left < 0) this.left = 0;
            if(this.bottom < 0) this.bottom = 0;
            if(this.right >= dst_width) this.right = dst_width - 1;
            if(this.top >= dst_height) this.top = dst_height - 1;

            // Convert trimmed coordinates back to source
            this.left -= x; this.right -= x; 
            this.bottom -= y; this.top -= y;

            // Get size of trimmed part
            this.trim_left = this.left;
            this.trim_right = src_width - this.right - 1;
            this.trim_bottom = this.bottom;
            this.trim_top = src_height - this.top - 1;
            return true;
        }

        // ---------------------------------------------------------------------
        public boolean Test(int x, int y, Utils.Vector2i rc) {
            if(x >= this.left && x <= this.right && y >= this.bottom && y <= this.top) {
                rc.x = x + this.offset_x; rc.y = y + this.offset_y;
                return true;
            } else {
                return false;
            }
        }
    }

    // =========================================================================
    // WrapContext
    public static class WrapContext {
        // ---------------------------------------------------------------------
        public TrimContext [] trim_list;
        public int cnt;

        // ---------------------------------------------------------------------
        public WrapContext() {
            this.trim_list = new TrimContext[] {
              new TrimContext(), new TrimContext(), new TrimContext()};
        }

        // ---------------------------------------------------------------------
        public TrimContext GetFirst() {
            this.cnt = 0;
            return this.GetNext();
        }

        // ---------------------------------------------------------------------
        public TrimContext GetNext() {
            return this.trim_list[this.cnt++];
        }

        // ---------------------------------------------------------------------
        public void Build(int x, int y, int src_width, int src_height, 
          int dst_width, int dst_height) {
            // Get first context
            TrimContext ctx = this.GetFirst(), initial_ctx = ctx;

            // Build initial trim context
            initial_ctx.Build(x, y, src_width, src_height, dst_width, dst_height, false);

            // Return if nothing is trimmed
            if(initial_ctx.trim_left == 0 && initial_ctx.trim_right == 0) {
                return;
            }

            // Left context
            if(initial_ctx.trim_left > 0) {
                ctx = this.GetNext();
                ctx.offset_x = dst_width - initial_ctx.trim_left;
                ctx.offset_y = initial_ctx.offset_y;
                ctx.left = 0;
                ctx.right = initial_ctx.trim_left - 1;
                ctx.bottom = initial_ctx.bottom;
                ctx.top = initial_ctx.top;
            }

            // Right context
            if(initial_ctx.trim_right > 0) {
                ctx = this.GetNext();
                ctx.offset_x = -src_width + initial_ctx.trim_right;
                ctx.offset_y = initial_ctx.offset_y;
                ctx.left = src_width - initial_ctx.trim_right;
                ctx.right = src_width - 1;
                ctx.bottom = initial_ctx.bottom;
                ctx.top = initial_ctx.top;
            }
            Utils.Assert(this.cnt != 3, "fuck");
        }

        // ---------------------------------------------------------------------
        public boolean Test(int x, int y, Vector2i rc) {
            for(int i = 0; i < this.cnt; i++) {
                TrimContext ctx = this.trim_list[i];
                if(ctx.Test(x, y, rc)) {
                    return true;
                }
            }
            return false;
        }
    }

    // =========================================================================
    // Entry
    public static class Entry<T> {
        public T val;
        public int x, y;
    }

    // =========================================================================
    // Colormap
    public static class Colormap<T> extends ObjectMap<Integer, T> { }

    // =========================================================================
    // Map2d
    private static TrimContext trim_tmp = new TrimContext();
    private static WrapContext wrap_tmp = new WrapContext();

    // -------------------------------------------------------------------------
    public Utils.Vector2i size;
    public Entry<T>[][] data;
    public Entry<T>[] active;
    public int active_cnt;
    public T default_val;

    // -------------------------------------------------------------------------
    public Map2d(int width, int height, T default_val) {
        // Init map
        this.Init(width, height, default_val);

        // Fill map
        for(int y = 0; y < this.size.y; y++) {
            for(int x = 0; x < this.size.x; x++) {
                this.SetEntry(x, y, default_val);
            }
        }
    }

    // -------------------------------------------------------------------------
    public Map2d(String path, T default_val, Colormap<T> cm) {
        // Prepare map data
        Pixmap pm = Map2d.LoadPixmap(path);
        this.Init(pm.getWidth(), pm.getHeight(), default_val);
        Logger.d(Logger.MOD_MISC, "Loading 2D map :: [path=%s] [size=%d:%d]",
          path, pm.getWidth(), pm.getHeight());

        // Fill map
        for(int y = 0; y < pm.getHeight(); y++) {
            for(int x = 0; x < pm.getWidth(); x++) {
                int rgb = pm.getPixel(x, y);
                T val = (cm != null && cm.get(rgb) != null) ? cm.get(rgb) : this.default_val;
                if(val instanceof Byte) {
                    Logger.d("JJFK :: rgb=%d pm=%d", rgb, (Byte) val);
                }
                this.SetEntry(x, y, val);
            }
        }

        // Cleanup
        Map2d.ClosePixmap(pm);
        this.Finalize();
    }

    // -------------------------------------------------------------------------
    private void Init(int width, int height, T default_val) {
        this.size = new Utils.Vector2i(width, height);
        this.data = new Entry[width][height];
        this.active = new Entry[width * height];
        this.active_cnt = 0;
        this.default_val = default_val;
    }

    // -------------------------------------------------------------------------
    public void SetEntry(int x, int y, T val) {
        Utils.Assert(val != null, "Empty value [pos=%d:%d]", x , y);

        Entry<T> e = this.data[x][y] = new Entry<T>();
        e.val = val; e.x = x; e.y = y;
    }

    // -------------------------------------------------------------------------
    public void Finalize() {
        this.active_cnt = 0;
        for(int y = 0; y < this.size.y; y++) {
            for(int x = 0; x < this.size.x; x++) {
                Entry<T> e = this.data[x][y];
                if(!e.val.equals(this.default_val)) {
                    this.active[this.active_cnt++] = e;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    public static TrimContext GetTrimContext(Utils.Vector2i src_size, Utils.Vector2i dst_size, 
      int x, int y) {
        return trim_tmp.Build(x, y, src_size, dst_size, true) ? Map2d.trim_tmp : null;
    }

    // -------------------------------------------------------------------------
    public static TrimContext GetTrimContext(int src_width, int src_height, int dst_width, 
      int dst_height, int x, int y) {
        return trim_tmp.Build(x, y, src_width, src_height, dst_width, dst_height, true) ? 
          Map2d.trim_tmp : null;
    }

    // -------------------------------------------------------------------------
    public static TrimContext GetTrimContext(Map2d src, Map2d dst, int x, int y) {
        return Map2d.GetTrimContext(src.size, dst.size, x, y);
    }

    // -------------------------------------------------------------------------
    public static WrapContext GetWrapContext(int src_width, int src_height, int dst_width, 
      int dst_height, int x, int y) {
        wrap_tmp.Build(x, y, src_width, src_height, dst_width, dst_height); 
        return Map2d.wrap_tmp;
    }

    // -------------------------------------------------------------------------
    public static WrapContext GetWrapContext(Utils.Vector2i src_size, Utils.Vector2i dst_size,
      int x, int y) {
        wrap_tmp.Build(x, y, src_size.x, src_size.y, dst_size.x, dst_size.y); 
        return Map2d.wrap_tmp;
    }

    // -------------------------------------------------------------------------
    public static Pixmap LoadPixmap(String path) {
        FileHandle h_file = Gdx.files.internal(path);
        Pixmap pm = new Pixmap(h_file);
        pm.setBlending(Pixmap.Blending.None);
        return pm;
    }

    // -------------------------------------------------------------------------
    public static void ClosePixmap(Pixmap pm) {
        pm.dispose();
    }

    // -------------------------------------------------------------------------
    public static Map2d.Colormap<Float> GetGrayscaleColormap() {
        Logger.d(Logger.MOD_MISC, "Creating grayscale colormap ::");
        Map2d.Colormap<Float> cm = new Map2d.Colormap<Float>();
        for(int i = 0 ; i < 256; i++) {
            int key = Palette.Entry.RGBtoInt32(i, i, i);
            float val = (float)i / 255.0f;
            cm.put(key, val);
            if(i == 42) {
                Logger.d(Logger.MOD_MISC, "  [idx=%d] [key=%s] [val=%f]",
                  i, Utils.Int2Str(key), val);
            }
        }
        return cm;
    }

    // -------------------------------------------------------------------------
    public static Map2d.Colormap<Boolean> GetBooleanColormap() {
        Map2d.Colormap<Boolean> cm = new Map2d.Colormap<Boolean>();
        cm.put(Palette.Entry.RGBtoInt32(0, 0, 0), false);
        cm.put(Palette.Entry.RGBtoInt32(255, 255, 255), true);
        return cm;
    }

    // -------------------------------------------------------------------------
    public static Map2d.Colormap<Byte> GetRgbColormap(String path) {
        // Create palette & colormap
        Map2d.Colormap<Byte> cm = new Map2d.Colormap<Byte>();
        Palette p = new Palette(path);

        // Fill colormap with palette entries
        Iterator<Palette.Entry> it = p.entry.iterator();
        byte idx = 0;
        while(it.hasNext()) {
            Palette.Entry e = it.next();
            cm.put(e.bin32, idx++);
            Utils.Assert(idx > 0, "Palette index has wrapped");
        }
        return cm;
    }
}
