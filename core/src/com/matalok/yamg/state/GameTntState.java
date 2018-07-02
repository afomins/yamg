// -----------------------------------------------------------------------------
package com.fomin.yamg.state;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.InputProcessor;
import com.fomin.yamg.Obj;
import com.fomin.yamg.ui.CommonWidget;

//-----------------------------------------------------------------------------
public class GameTntState extends StateMan.State {
    // -------------------------------------------------------------------------
    public GameTntState(InputProcessor [] ip_list) {
        super(Obj.STATE.GAME_TNT, ip_list);
    }

    // -------------------------------------------------------------------------
    protected void OnStateActivate( ) {
    }

    // -------------------------------------------------------------------------
    public void OnUiClick(CommonWidget w, float x, float y) {
        StateMan.p.ui_handler.OnWidgetClick(w, x, y);
    }
}
