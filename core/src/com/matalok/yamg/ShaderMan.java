// -----------------------------------------------------------------------------
package com.matalok.yamg;

// -----------------------------------------------------------------------------
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;

// -----------------------------------------------------------------------------
public class ShaderMan extends Obj.CommonObject {
    // =========================================================================
    // ShaderObject
    private static class FileDesc {
        // ---------------------------------------------------------------------
        public FileHandle h_file;
        public String body;

        // ---------------------------------------------------------------------
        public FileDesc(String path) {
            this.h_file = Gdx.files.internal(path);
            this.body = new String();
        }
    }

    // =========================================================================
    // ShaderMan
    private static final int KW_IMPORT = 0;
    private static final int KW_ITERATE_LINE = 1;
    private static final String[] KEYWORDS = new String[] {"import", "iterate-line"};
    private static final String KEYWORD_PATTERN = new String("(\\s*)(\\<)(.*)(\\>)");
    private static final int KEYWORD_BODY_IDX = 3;

    // -------------------------------------------------------------------------
    private Map<String, FileDesc> files;
    private Array<ShaderProgram> sh_list;
    private Pattern kw_pattern;

    // -------------------------------------------------------------------------
    public ShaderMan() {
        super(Obj.MISC.ptr, Obj.MISC.SHADER_MAN);
        this.files = new HashMap<String, FileDesc>();
        this.sh_list = new Array<ShaderProgram>();
        this.kw_pattern = Pattern.compile(ShaderMan.KEYWORD_PATTERN);
    }

    // -------------------------------------------------------------------------
    public String Load(String path, String[][][] it_list) {
        FileDesc fd = this.Open(path, it_list);
        return fd.body;
    }

    // -------------------------------------------------------------------------
    private FileDesc Open(String path, String[][][] it_list) {
        // Return constant shader if already opened 
        FileDesc fd = null;
        if(it_list == null) {
            fd = this.files.get(path);
            if(fd != null) return fd;
        }

        // Create file descriptor
        fd = new FileDesc(path);

        // Add constant shader to map
        if(it_list == null) {
            this.files.put(path, fd);
        }

        // Create new file descriptor and read file
        BufferedReader br = fd.h_file.reader(64);
        try {
            // Read line-by-line
            String line;
            boolean do_iterate_line = false;
            int it_cnt = 0;
            while ((line = br.readLine()) != null) {
                // Handle keyword
                Matcher m = this.kw_pattern.matcher(line);
                if(m.find()) {
                    // Parse keyword line
                    line = m.group(ShaderMan.KEYWORD_BODY_IDX);
                    String[] list = line.split("[\\s]+");

                    // Get keyword
                    String kw_name = list[0];
                    int id = 0; for(; id < ShaderMan.KEYWORDS.length; id++) {
                        if(kw_name.compareTo(ShaderMan.KEYWORDS[id]) == 0) break;
                    }

                    // Keyword as comment
                    line = "// " + line;

                    // Import
                    if(id == ShaderMan.KW_IMPORT) {
                        Utils.Assert(list.length == 2, "Invalid argument count [cnt=%d]", 
                          list.length);
                        line += "\n" + this.Open(list[1], null).body;

                    // Iterate line
                    } else if(id == ShaderMan.KW_ITERATE_LINE) {
                        if(it_list != null) {
                            Utils.Assert(list.length == 1, "Invalid argument count [cnt=%d]", 
                              list.length);
                            do_iterate_line = true;
                        }

                    // Error
                    } else {
                        Utils.Assert(false, "Unknown keyword [name=%s] [id=%d]", 
                          kw_name, id);
                    }

                // Iterate line
                } else if(do_iterate_line) {
                    Utils.Assert(it_cnt < it_list.length, "Iteration value overflow");
                    line = this.IterateLine(line, it_list[it_cnt++]);
                    do_iterate_line = false;
                }

                // Add line to body 
                fd.body += line;
                if(!line.endsWith("\n")) {
                    fd.body += "\n";
                }
            }
        }
        catch(IOException ie) {
            Utils.Assert(false, "IOException while reading");
        }

        return fd;
    }

    // -------------------------------------------------------------------------
    private String IterateLine(String line, String[][] var_list) {
        // Split line
        String[] list = line.split("@");
        int var_cnt = list.length - 1;
        Utils.Assert(var_cnt == var_list.length, "Invalid variable count list [%d!=%d]", 
          var_cnt, var_list.length);

        // Init body
        String body = new String();

        // Fill body
        int copy_cnt = var_list[0].length;
        for(int icopy = 0; icopy < copy_cnt; icopy++) {
            int ivar = 0; for(; ivar < var_cnt; ivar++) {
                body += list[ivar] + var_list[ivar][icopy];
            }
            body += list[ivar] + "\n";
        }
        return body;
    }

    // -------------------------------------------------------------------------
    public ShaderProgram LoadShader(String frag_path, String[][][] frag_it_list, 
      String vert_path, String[][][] vert_it_list, boolean do_log) {
        // Get shader body
        String frag_body = this.Load(frag_path, frag_it_list);
        String vert_body = this.Load(vert_path, vert_it_list);

        // Compile shader
        ShaderProgram sh = new ShaderProgram(
          this.Load(vert_path, vert_it_list),
          this.Load(frag_path, frag_it_list));

        // Log
        if(do_log || !sh.isCompiled()) {
            Logger.d(Logger.MOD_SH, "Loading fragment shader [path=%s]\n%s", frag_path, frag_body);
            Logger.d(Logger.MOD_SH, "Loading vertex shader [path=%s]\n%s", vert_path, vert_body);
        }

        // Validate compilation status
        Utils.Assert(sh.isCompiled(), "Shader failure :: [log=%s]", sh.getLog());
        Logger.d(Logger.MOD_SH, "  [frag=%s] [vert=%s]", frag_path, vert_path);
        this.sh_list.add(sh);
        return sh;
    }

    // -------------------------------------------------------------------------
    public void Dispose() {
        for(ShaderProgram sh: this.sh_list) {
            sh.dispose();
        }
        super.Dispose();
    }
}
