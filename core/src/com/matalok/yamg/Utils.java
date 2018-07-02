// -----------------------------------------------------------------------------
package com.matalok.yamg;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

// -----------------------------------------------------------------------------
public class Utils {
    // =========================================================================
    // Vector2
    public static class Vector2<T> {
        public T x, y;
        public boolean Compare(Vector2<T> cmp) {
            return (this.x.equals(cmp.x) && this.y.equals(cmp.y)); 
        }
    }

    // -------------------------------------------------------------------------
    public static class Vector2i extends Utils.Vector2<Integer> {
        public Vector2i() { this.Set(0, 0); }
        public Vector2i(int x, int y) { this.Set(x, y); }
        public Vector2i(Vector2i v) { this.Set(v.x, v.y); }
        public Vector2i Set(int x, int y) { this.x = x; this.y = y; return this; }
        public Vector2i Set(Vector2i v) { this.x = v.x; this.y = v.y; return this; }
        public Vector2i Add(Vector2i v) { this.x += v.x; this.y += v.y; return this; }
        public Vector2i Sub(Vector2i v) { this.x -= v.x; this.y -= v.y; return this; }
        public Vector2i Mul(Vector2i v) { this.x *= v.x; this.y *= v.y; return this; }
        public Vector2i Mul(int v) { this.x *= v; this.y *= v; return this; }
        public Vector2i Div(Vector2i v) { this.x /= v.x; this.y /= v.y; return this; }
        public Vector2i Div(int v) { this.x /= v; this.y /= v; return this; }
    }

    // -------------------------------------------------------------------------
    public static class Vector2f extends Utils.Vector2<Float> {
        public Vector2f() { this.Set(0.0f); }
        public Vector2f(float x, float y) { this.Set(x, y); }
        public Vector2f(Vector2f v) { this.Set(v.x, v.y); }

        public Vector2f Set(float v) { this.x = v; this.y = v; return this; }
        public Vector2f Set(float x, float y) { this.x = x; this.y = y; return this; }
        public Vector2f Set(Vector2f v) { this.x = v.x; this.y = v.y; return this; }

        public Vector2f Add(float v) { this.x += v; this.y += v; return this; }
        public Vector2f Add(float x, float y) { this.x += x; this.y += y; return this; }
        public Vector2f Add(Vector2f v) { this.x += v.x; this.y += v.y; return this; }

        public Vector2f Sub(float v) { this.x -= v; this.y -= v; return this; }
        public Vector2f Sub(float x, float y) { this.x -= x; this.y -= y; return this; }
        public Vector2f Sub(Vector2f v) { this.x -= v.x; this.y -= v.y; return this; }

        public Vector2f Mul(float v) { this.x *= v; this.y *= v; return this; }
        public Vector2f Mul(float x, float y) { this.x *= x; this.y *= y; return this; }
        public Vector2f Mul(Vector2f v) { this.x *= v.x; this.y *= v.y; return this; }

        public Vector2f Div(float v) { this.x /= v; this.y /= v; return this; }
        public Vector2f Div(float x, float y) { this.x /= x; this.y /= y; return this; }
        public Vector2f Div(Vector2f v) { this.x /= v.x; this.y /= v.y; return this; }
    }

    // -------------------------------------------------------------------------
    public static class Direction2 extends Utils.Vector2f {
        // ---------------------------------------------------------------------
        public static int DIR_NONE = 0;
        public static int DIR_HORIZONTAL = 1;
        public static int DIR_VERTICAL = 2;
        public static int DIR_RANDOM = 3;

        // ---------------------------------------------------------------------
        public int type;
        public float len;

        // ---------------------------------------------------------------------
        public Direction2() { this.Update(); };
        public Direction2(float x, float y) { super(x, y); this.Update(); };
        public Direction2(Vector2f v) { super(v); this.Update(); };

        // ---------------------------------------------------------------------
        public Direction2 Set(float x, float y) {
            super.Set(x, y);
            return this;
        }

        // ---------------------------------------------------------------------
        public Direction2 Update() {
            // Zero direction
            if(this.x == 0.0f && this.y == 0.0f) {
                this.type = Direction2.DIR_NONE;
                this.len = 0.0f;
                return this;
            }

            // Type 
                 if(this.x != 0.0f && this.y == 0.0f) this.type = Direction2.DIR_HORIZONTAL;
            else if(this.x == 0.0f && this.y != 0.0f) this.type = Direction2.DIR_VERTICAL;
            else                                      this.type = Direction2.DIR_RANDOM;

            // Value
            this.len = (float)Math.sqrt(x * x + y * y);
            this.x /= this.len;
            this.y /= this.len;
            return this;
        }
    }

    // =========================================================================
    // Rect
    public abstract static class Rect<T> extends Vector2<T> {
        // ---------------------------------------------------------------------
        public T width, height;
        public T left, right, bottom, top;

        // ---------------------------------------------------------------------
        public void Set(T x, T y, T width, T height) {
            this.x = x; this.y = y; 
            this.width = width; this.height = height;
            this.Finalize();
        }

        // ---------------------------------------------------------------------
        public abstract void Finalize();
    }

    // -------------------------------------------------------------------------
    public static class Recti extends Utils.Rect<Integer> {
        // ---------------------------------------------------------------------
        public Recti() { this.Set(0, 0, 1, 1); }

        // ---------------------------------------------------------------------
        public Recti(int x, int y, int width, int height) {
            this.Set(x, y, width, height);
        }

        // ---------------------------------------------------------------------
        public void Finalize() {
            this.left = this.x; this.right = this.x + this.width - 1;
            this.bottom = this.y; this.top = this.y + this.height - 1;
        }
    };

    // =========================================================================
    // ActiveListObject
    public static class ActiveListObject {
        // ---------------------------------------------------------------------
        int active_list_idx;

        // ---------------------------------------------------------------------
        public ActiveListObject() {
            this.RemoveFromActiveList();
        }

        // ---------------------------------------------------------------------
        public boolean InActiveList() {
            return (this.active_list_idx == -1) ? false : true;
        }

        // ---------------------------------------------------------------------
        public void RemoveFromActiveList() {
            this.active_list_idx = -1;
        }
    }

    // =========================================================================
    // ActiveList
    public static class ActiveList {
        // ---------------------------------------------------------------------
        public ActiveListObject[] data;
        public int cnt;

        // ---------------------------------------------------------------------
        public ActiveList(ActiveListObject[] data) {
            this.data = data;
            this.cnt = 0;
        }

        // ---------------------------------------------------------------------
        public void Reset() {
            this.cnt = 0;
        }

        // ---------------------------------------------------------------------
        public void Append(ActiveListObject obj) {
            obj.active_list_idx = this.cnt;
            this.data[this.cnt++] = obj;
        }
    }

    // =========================================================================
    // ActiveDoubleList
    public static class ActiveDoubleList {
        // ---------------------------------------------------------------------
        public ActiveList primary, secondary;
        public ActiveList[] array;

        // ---------------------------------------------------------------------
        public ActiveDoubleList(ActiveList primary, ActiveList secondary) {
            this.array = new ActiveList[2];
            this.array[0] = this.primary = primary;
            this.array[1] = this.secondary = secondary;
        }

        // ---------------------------------------------------------------------
        public void Swap() {
            // Swap primary <--> secondary 
            ActiveList tmp = this.primary;
            this.primary = this.secondary;
            this.secondary = tmp;

            // Reset secondary
            this.secondary.Reset();
        }
    }

    // -------------------------------------------------------------------------
    public static final int ID_UNDEFINED = -1;
    public static boolean IsDefined(int id) { return (id != Utils.ID_UNDEFINED); }

    // =========================================================================
    // Tmp objects
    public static Vector3 v3_tmp = new Vector3();
    public static Vector2i v2i_tmp = new Vector2i();
    public static Vector2f v2f_tmp = new Vector2f();
    public static String version = "0.4";
    public static boolean is_desktop;

    // =========================================================================
    // Conversion
    public static String Bool2Str(boolean val) {
        return val ? "true" : "false";
    }

    // -------------------------------------------------------------------------
    public static Boolean Str2Bool(String str) {
        return str.toLowerCase().equals("true") | str.toLowerCase().equals("t");
    }

    // -------------------------------------------------------------------------
    public static int Str2Int(String str) {
        return Integer.parseInt(str); 
    }

    // -------------------------------------------------------------------------
    public static String Int2Str(int val) {
        return java.lang.Integer.toHexString(val);
    }

    // -------------------------------------------------------------------------
    public static float Str2Float(String str) {
        return Float.parseFloat(str);
    }

    // -------------------------------------------------------------------------
    public static String FloatArray2Str(float[] val, String fmt) {
        String str = new String();
        for(int i = 0; i < val.length; i++) {
            str += String.format(fmt, val[i]);

            if(i != val.length - 1) {
                str += ", ";
            }
        }
        return str;
    }

    // -------------------------------------------------------------------------
    public static float[] Str2FloatArray(String str) {
        String[] split = str.split("[,][, ]");
        float[] arr = (split.length == 1) ? null : new float[split.length];
        for(int i = 0; i < split.length; i++) {
            arr[i] = Utils.Str2Float(split[i]);
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    public static Color Str2Color(String str) {
        String[] split = str.split(" ");
        Utils.Assert(split.length == 4, "Not a color format :: [str=%s]", str);
        return new Color(
          (float)(Utils.Str2Int(split[0]) / 255.0),
          (float)(Utils.Str2Int(split[1]) / 255.0),
          (float)(Utils.Str2Int(split[2]) / 255.0),
          (float)(Utils.Str2Int(split[3]) / 255.0));
    }

    // =========================================================================
    // Misc
    public static int GetGlVersion() {
             if(Gdx.gl20 != null) return 2;
//        else if(Gdx.gl10 != null) return 1;
        else                      return Utils.ID_UNDEFINED;
    }

    // -------------------------------------------------------------------------
    public static void Assert(boolean expr, String fmt, Object... args) {
        if(expr) return;
        Gdx.app.error("ASSERT", String.format(fmt, args));
        int ass = 0, ert = 100500 / ass;
    }

    // -------------------------------------------------------------------------
    public static String[] GetSeqList(int start, int end) {
        // Create list
        int cnt = end - start + 1;
        String[] list = new String[cnt];

        // Fill list
        for(int i = 0; i < cnt; i++) {
            list[i] = String.format("%d", i + start);
        }
        return list;
    }

    // -------------------------------------------------------------------------
    public static int GetBitCnt(int mask) {
        int cnt = 0;
        for(int i = 0; i < 32; i++) {
            if(((1 << i) & mask) != 0) cnt++;
        }
        return cnt;
    }
}
