// -----------------------------------------------------------------------------
package com.matalok.yamg;

//-----------------------------------------------------------------------------
import java.util.HashSet;
import java.util.Set;
import com.badlogic.gdx.utils.XmlReader;

// -----------------------------------------------------------------------------
public class UserMan extends ServiceMan.Service {
    // -------------------------------------------------------------------------
    private String name;
    private Set<String> open_level_name;

    // -------------------------------------------------------------------------
    public UserMan() {
        super(Obj.SERVICE.USER_MAN);
    }

    // -------------------------------------------------------------------------
    // Service pointer
    public static UserMan p;
    protected void AcquireServicePointer() { UserMan.p = this; };
    protected void ReleaseServicePointer() { UserMan.p = null; };

    // -------------------------------------------------------------------------
    protected void OnServiceSetup(XmlReader.Element cfg) {
        // Read config
        cfg = CfgReader.Read(CfgReader.GetAttrib(cfg, "config:path"));
        this.name = CfgReader.GetAttrib(cfg, "config:name");

        // Prepare container 
        this.open_level_name = new HashSet<String>();
        cfg = CfgReader.GetChild(cfg, "open-levels");

        // Read open levels
        for(int i = 0; i < cfg.getChildCount(); i++) {
            XmlReader.Element e = cfg.getChild(i);
            if(CfgReader.IsCommented(e)) continue;
            this.open_level_name.add(CfgReader.GetAttrib(e, "name"));
        }
    }

    // -------------------------------------------------------------------------
    public boolean IsOpen(String name) { return this.open_level_name.contains(name); }
    public String GetName() { return this.name; }
}
