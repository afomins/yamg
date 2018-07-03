/*
 * YAMG - Yet Another Mining Game
 * Copyright (C) 2013 Alex Fomins
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

// -----------------------------------------------------------------------------
package com.matalok.yamg;

// -----------------------------------------------------------------------------
import java.io.IOException;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.XmlReader;

// -----------------------------------------------------------------------------
public class CfgReader {
    // -------------------------------------------------------------------------
    public static XmlReader.Element Read(String path) {
        try {
            Logger.d(Logger.MOD_CFG, "Read config :: [path=%s]", path);
            XmlReader xmlReader = new XmlReader();
            return xmlReader.parse(Gdx.files.internal(path));

//        } catch (IOException e) {
//            Utils.Assert(false, "IOException while reading");

        } catch (Exception e) {
            Utils.Assert(false, "Exception while reading"); 
        }
        return null;
    }

    // -------------------------------------------------------------------------
    private static XmlReader.Element GetChild(XmlReader.Element el, String[] tokens, 
      int start_idx, int end_idx) {
        if(end_idx == -1) end_idx = tokens.length - 1;
        for(int i = start_idx; i <= end_idx; i++) {
            el = el.getChildByName(tokens[i]);
            Utils.Assert(el != null, "Invalid token name :: [token-name=%s]", tokens[i]);
        }
        return el; 
    }

    // -------------------------------------------------------------------------
    public static XmlReader.Element GetChild(XmlReader.Element el, String name) {
        String[] tokens = name.split(":");
        return CfgReader.GetChild(el, tokens, 0, -1); 
    }

    // -------------------------------------------------------------------------
    public static String GetAttrib(XmlReader.Element el, String name) {
        String[] tokens = name.split(":");
        try { 
            if(tokens.length > 1) {
                el = CfgReader.GetChild(el, tokens, 0, tokens.length - 2);
            }
        }
        catch(Exception e) { 
            Utils.Assert(false, "No Attribute :: [node-name=%s] [attrib-name=%s]",
              el.getName(), name);
        }
        return el.getAttribute(tokens[tokens.length - 1]);
    }

    // -------------------------------------------------------------------------
    public static boolean IsCommented(XmlReader.Element el) {
        return !el.getName().toLowerCase().equals("ok");
    }
}
