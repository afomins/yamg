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

//-----------------------------------------------------------------------------
import java.io.BufferedReader;
import java.io.IOException;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

// -----------------------------------------------------------------------------
public class Palette {
    // =========================================================================
    // Map
    public static class Map {
        // ---------------------------------------------------------------------
        private Array<Palette> palette;
        private int idx;

        // ---------------------------------------------------------------------
        public Map() {
            this.palette = new Array<Palette>(true, 16);
            this.idx = -1;
        }

        // ---------------------------------------------------------------------
        public Map(String [] path) {
            this();
            for(int i = 0; i < path.length; i++) {
                this.Add(path[i]);
            }
        }

        // ---------------------------------------------------------------------
        public void Add(String path) {
            this.palette.add(new Palette(path));
        }

        // ---------------------------------------------------------------------
        public Palette Next() {
            if(++this.idx >= this.palette.size) {
                this.idx = 0;
            }
            return this.palette.get(this.idx);
        }

        // ---------------------------------------------------------------------
        public Palette First() {
            this.idx = 0;
            return this.palette.get(this.idx);
        }
    }

    // =========================================================================
    // Entry
    public static class Entry {
        // ---------------------------------------------------------------------
        public Color color;
        public byte r, g, b;
        public int bin32;
        public String name;

        // ---------------------------------------------------------------------
        public Entry() {
            this.color = new Color(); 
        }

        // ---------------------------------------------------------------------
        public Entry(int r, int g, int b, String name) {
            this(); this.Set(r, g, b, name);
        }

        // ---------------------------------------------------------------------
        public void Set(int r, int g, int b, String name) {
            this.r = (byte)r; this.g = (byte)g; this.b = (byte)b;
            this.color.set((float)r / 255.0f, (float)g / 255.0f, 
              (float)b / 255.0f, 1.0f);
            this.bin32 = Entry.RGBtoInt32(r, g, b);
            this.name = name;
        }

        // ---------------------------------------------------------------------
        public static int RGBtoInt32(int r, int g, int b) {
            return 0xff | (b << 8) | (g << 16) | (r << 24);
        }
    }

    // =========================================================================
    // Palette
    public Array<Entry> entry;

    // -------------------------------------------------------------------------
    public Palette() {
        this.entry = new Array<Entry>(true, 16);
    }

    // -------------------------------------------------------------------------
    public Palette(String path) {
        this(); this.Read(path);
    }

    // -------------------------------------------------------------------------
    private void Read(String path) {
        // Open file for reading
        Logger.d(Logger.MOD_MISC, "Reading palette :: [path=%s]", path);
        FileHandle h_file = Gdx.files.internal(path);
        BufferedReader br = h_file.reader(64);

        try {
            // Read line-by-line
            String line;
            boolean beginning_found = false;
            while ((line = br.readLine()) != null) {
                // Look for palette beginning
                if(!beginning_found) {
                    if(line.compareTo("#") == 0) {
                        beginning_found = true;
                    }
                    continue;
                }

                // Parse string - [r] [g] [b] [description]
                String [] t = line.trim().split("[\\s]+");
                Utils.Assert(t.length == 4, "Parse error [line=%s]", line);

                // Add entry
                Entry entry = new Entry(Integer.parseInt(t[0]), 
                  Integer.parseInt(t[1]), Integer.parseInt(t[2]), t[3]); 
                Logger.d(Logger.MOD_MISC, "  [id=%d:%s] [bin=%s] [rgb=%s:%s:%s]", 
                  this.entry.size, t[3], Utils.Int2Str(entry.bin32), 
                  t[0], t[1], t[2]);
                this.entry.add(entry);
            }
        }
        catch(IOException ie) {
            Utils.Assert(false, "IOException while reading");
        }
    }
}
