/* 
 * Copyright (C) 2021 Sergio Basurto Juárez
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package gator.lib.shiro.web.env;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class GappSection {
    private String name;
    private final ArrayList<HashMap<String, String>> content = new ArrayList<>();

    /**
     * Add content to a section.
     * @param key The key to set value for.
     * @param value The data assigned to the key.
     */
    public void addContenido(String key, String value) {
            HashMap<String, String> tmpHashMap = new HashMap<>();
            tmpHashMap.put(key, value);
            content.add(tmpHashMap);
    }
    /**
     * Get the name of the section.
     * @return Section's name.
     */
    public String getName() {
            return name; 
    }
    /**
     * Get the content for section.
     * @return An array list of hash map for section's content.
     */
    public ArrayList<HashMap<String, String>> getContenido() {
            return content;
    }
}
