// -----------------------------------------------------------------------------
package com.fomin.yamg;

//-----------------------------------------------------------------------------
import java.util.HashMap;
import java.util.Map;
import com.badlogic.gdx.utils.Array;

// -----------------------------------------------------------------------------
public class Obj {
    // =========================================================================
    // SERVICE
    public static class SERVICE extends Group {
        public static final int 
          TIMER    = 0, 
          LOGGER   = 1, 
          TASK     = 2, 
          GAME     = 3, 
          LEVEL    = 4,
          USER_MAN = 5,
          UI       = 6,
          STATE    = 7,
          MEM_MAN  = 8;
        public static final String [] entry = new String [] {
          "timer", "logger", "task-man", "game-man", "level-man", "user-man", 
          "ui-man", "state-man", "mem-man"};
        public static final SERVICE ptr = new SERVICE();
        public SERVICE() { super("service", SERVICE.entry, true); }
    }

    // =========================================================================
    // TASK
    public static class TASK extends Group {
        public static final int 
          QUEUE        = 0, 
          POOL         = 1, 
          SLEEP        = 2,
          MOVE_PTP     = 3,
          MOVE_DIR     = 4,
          TWEEN        = 5,
          STOP         = 6;
        public static final String [] entry = new String [] {
          "queue", "pool", "sleep", "move-ptp", "move-dir", "tween", "stop"};
        public static final TASK ptr = new TASK();
        public TASK() { super("task", TASK.entry, true); }
    }

    // =========================================================================
    // STATE
    public static class STATE extends Group {
        public static final int 
          SHUTDOWN               = 0,
          GAME                   = 1,
          GAME_TNT               = 2,
          LOAD_LEVEL             = 3,
          MAIN_MENU_SELECT_LEVEL = 4,
          MAIN_MENU_INFO         = 5,
          MAIN_MENU_SHUTDOWN     = 6,
          MAIN_MENU_RESET        = 7;
        public static final String [] entry = new String [] {
          "shutdown", "game", "game-tnt", "load-level", 
          "mm-select-level", "mm-info", "mm-shutdown", "mm-reset"};
        public static final STATE ptr = new STATE();
        public STATE() { super("state", STATE.entry, true); }
    }

    // =========================================================================
    // UI
    public static class UI extends Group {
        public static final int 
          MAIN_MENU                 = 0,
          MAIN_MENU_BUTTON_BACK     = 1,
          MAIN_MENU_BUTTON_GAME     = 2,
          MAIN_MENU_BUTTON_INFO     = 3,
          MAIN_MENU_BUTTON_RESET    = 4,
          MAIN_MENU_BUTTON_QUIT     = 5,
          MAIN_MENU_BUTTON_MENU     = 6,
          MAIN_MENU_BUTTON_BOMB     = 7,
          MAIN_MENU_BUTTON_ACT1     = 8,
          MAIN_MENU_BUTTON_ACT2     = 9,
          MAIN_MENU_BUTTON_ACT3     = 10,
          MAIN_MENU_BUTTON_YES      = 11,
          MAIN_MENU_BUTTON_NO       = 12,
          LOAD_SCREEN               = 13,
          LOAD_SCREEN_IMAGE         = 14,
          LOAD_SCREEN_PROGRESS      = 15,
          SELECT_LEVEL_WND          = 16,
          INFO_WND                  = 17,
          CONFIRM_WND               = 18,
          LEVEL_SELECTOR            = 19,
          LEVEL_SELECTOR_PREV       = 20,
          LEVEL_SELECTOR_NEXT       = 21,
          LEVEL_SELECTOR_RUN        = 22,
          MEM_STATS_LABEL           = 23;
        public static final String [] entry = new String [] {
          "main-menu", "main-menu-back", "main-menu-game", "main-menu-info", 
          "main-menu-reset", "main-menu-quit", "main-menu-menu", "main-menu-bomb",
          "main-menu-act1", "main-menu-act2", "main-menu-act3", "main-menu-yes", 
          "main-menu-no", "load-screen", "load-screen-image", "load-screen-progress", 
          "select-lvl-wnd", "settings-wnd", "confirm-wnd", 
          "lvl-selector", "lvl-selector-prev", "lvl-selector-next", "lvl-selector-run",
          "mem-stats-label"};
        public static final UI ptr = new UI();
        public UI() { super("ui", UI.entry, true); }
    }

    // =========================================================================
    // WIDGET
    public static class WIDGET extends Group {
        public static final int 
          MENU           = 0, 
          BUTTON         = 1, 
          IMAGE          = 2, 
          PROGRESS_BAR   = 3, 
          CONTAINER      = 4,
          WINDOW         = 5,
          GRID           = 6,
          LEVEL_SELECTOR = 7,
          LABEL          = 8;
        public static final String [] entry = new String [] {
          "menu", "button", "image", "progress-bar", "container", "window", 
          "grid", "lvl-selector", "label"};
        public static final WIDGET ptr = new WIDGET();
        public WIDGET() { super("widget", WIDGET.entry, true); }
    }

    // =========================================================================
    // MISC
    public static class MISC extends Group {
        public static final int 
          TEXTURE               = 0, 
          PIXMAP                = 1,
          PIXELMAP              = 2,
          PIXELMAP_TEX          = 3,
          SHADER_MAN            = 4,
          FRAME_BUFFER          = 5,
          MESH                  = 6,
          VIEWER                = 7,
          VIEWER_MOVEMENT_PROXY = 8;
        public static final String [] entry = new String [] {
          "texture", "pixmap", "pixelmap", "pixelmap-tex", "shader-man", 
          "frame-buffer", "mesh", "viewer", "viewer-move-proxy"};
        public static final MISC ptr = new MISC();
        public MISC() { super("misc", MISC.entry, true); }
    }

    // =========================================================================
    // ICommonObject
    public interface ICommonObject {
        // ---------------------------------------------------------------------
        public int GetGroupIdx();
        public int GetEntryIdx();
        public int GetObjId();
        public String GetGroupName();
        public String GetEntryName();
        public String GetObjName();
    }

    // =========================================================================
    // CommonObject
    public static abstract class CommonObject implements ICommonObject {
        // ---------------------------------------------------------------------
        private Group group;
        private Entry entry;
        private String obj_name;
        private int obj_idx, obj_id;
        private boolean is_disposed;

        // ---------------------------------------------------------------------
        public CommonObject(Group group, int entry_idx) {
            this.group = group;
            this.entry = group.GetEntry(entry_idx);
            this.obj_idx = this.entry.Alloc();
            this.obj_id = CommonObject.BuildId(this.group.idx, this.entry.idx, this.obj_idx);
            this.obj_name = String.format("%s:%s:%d", group.name, this.entry.name, this.obj_idx);
            this.is_disposed = false;
            Logger.d(Logger.MOD_OBJ, "Creating object :: [name=%s]", this.obj_name);
        }

        // ---------------------------------------------------------------------
        public static int BuildId(int group_id, int entry_id, int idx) {
            Utils.Assert(group_id < 256, "Group-id overflow");
            Utils.Assert(entry_id < 256, "Entry-id overflow");
            Utils.Assert(idx < 65536, "Index overflow");
            return ((group_id << 0)  & 0x000000FF) |
                   ((entry_id << 8)  & 0x0000FF00) |
                   ((idx      << 16) & 0xFFFF0000);
        }

        // ---------------------------------------------------------------------
        public boolean CmpId(CommonObject obj) { return (this.obj_id == obj.obj_id); };
        public int GetGroupIdx() { return this.group.idx; }
        public int GetEntryIdx() { return this.entry.idx; }
        public int GetObjId() { return this.obj_id; }
        public String GetGroupName() { return this.group.name; }
        public String GetEntryName() { return this.entry.name; }
        public String GetObjName() { return this.obj_name; }
        public boolean IsDisposed() { return this.is_disposed; }

        // ---------------------------------------------------------------------
        public void Dispose() {
            Logger.d(Logger.MOD_OBJ, "Disposing object :: [name=%s]", this.obj_name);
            this.entry.Free();
            Utils.Assert(!this.is_disposed, "Object is already disposed :: [name=%s]", this.obj_name);
            this.is_disposed = true;
        }
    }

    // =========================================================================
    // Entry
    public static class Entry {
        // ---------------------------------------------------------------------
        private String name;
        private int idx, alloc_cnt, free_cnt;

        // ---------------------------------------------------------------------
        public Entry(int idx, String name) { this.idx = idx; this.name = name; }
        public int Alloc() { return this.alloc_cnt++; }
        public void Free() { this.free_cnt++; }
        public boolean IsClean() { return (this.free_cnt == this.alloc_cnt); }
    }

    // =========================================================================
    // Group
    public static class Group {
        // ---------------------------------------------------------------------
        private String name;
        private int idx;
        private Entry [] entry;
        private Map<String, Entry> entry_name_map;
        private Map<Integer, Entry> entry_idx_map;

        // ---------------------------------------------------------------------
        private static Array<Group> managed_list = new Array<Group>();

        // ---------------------------------------------------------------------
        public Group(String name, String [] entry) {
            this(name, entry, false);
        }

        // ---------------------------------------------------------------------
        public Group(String name, String [] entry, boolean is_managed) {
            if(is_managed) {
                this.idx = Group.managed_list.size;
                Group.managed_list.add(this);
            }

            this.name = name;
            this.entry = new Entry[entry.length];
            this.entry_name_map = new HashMap<String, Entry>();
            this.entry_idx_map = new HashMap<Integer, Entry>();

            for(int i = 0; i < entry.length; i++) {
                String entry_name = entry[i];
                Entry e = new Entry(i, entry_name);
                this.entry[i] = e;
                this.entry_name_map.put(entry_name, e);
                this.entry_idx_map.put(i, e);
            }
        }

        // ---------------------------------------------------------------------
        public int GetSize() {
            return this.entry.length;
        }

        // ---------------------------------------------------------------------
        public int GetIdx() {
            return this.idx;
        }

        // ---------------------------------------------------------------------
        public Entry GetEntry(int idx) {
            Utils.Assert(this.entry_idx_map.containsKey(idx), 
              "Invalid entry idx :: [idx=%d] [group=%s]", idx, this.name);

            return this.entry_idx_map.get(idx);
        }

        // ---------------------------------------------------------------------
        public int GetEntryIdx(String name, boolean is_strict) {
            boolean is_present = this.entry_name_map.containsKey(name);
            if(is_strict) {
                Utils.Assert(is_present, "Invalid entry name :: [name=%s] [group=%s]", 
                  name, this.name);
            }
            return (is_present) ? this.entry_name_map.get(name).idx : Utils.ID_UNDEFINED;
        }

        // ---------------------------------------------------------------------
        public String GetEntryName(int idx, boolean is_strict) {
            boolean is_present = this.entry_idx_map.containsKey(idx);
            if(is_strict) {
                Utils.Assert(is_present, "Invalid entry idx :: [idx=%d] [group=%s]", 
                  idx, this.name);
            }
            return (is_present) ? this.entry_idx_map.get(idx).name : null;
        }

        // ---------------------------------------------------------------------
        public String GetEntryName(int idx) {
            return this.GetEntryName(idx, true);
        }

        // ---------------------------------------------------------------------
        public static boolean Test() {
            boolean is_clean = true;
            Logger.d(Logger.MOD_OBJ, "Groups:");
            for(Group g: Group.managed_list) {
                Logger.d(Logger.MOD_OBJ, "  [group=%s]:", g.name);
                for(Entry e: g.entry) {
                    if(!e.IsClean()) {
                        is_clean = false;
                    }

                    Logger.d(Logger.MOD_OBJ, "    [%s]    [entry=%s] [alloc=%d] [free=%d]",
                      e.IsClean() ? "   OK  " : "* BAD *",
                      e.name, e.alloc_cnt, e.free_cnt);
                }
            }
            return is_clean;
        }
    }
}
