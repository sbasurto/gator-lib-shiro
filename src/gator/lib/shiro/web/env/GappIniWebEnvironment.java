/*
 * Copyright (C) 2017 Sergio Basurto Juárez
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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gator.lib.db.GappSQLStatement;
import gator.lib.logs.GappLogging;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.config.Ini;
import org.apache.shiro.util.CollectionUtils;
import org.apache.shiro.web.env.IniWebEnvironment;
import jakarta.servlet.ServletContext;
import gator.lib.db.helpers.GappDBHelper;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class GappIniWebEnvironment extends IniWebEnvironment {
	private GappDBHelper dbHelper;
	private final GappLogging log = new GappLogging();
	/**
	 * Loads configuration {@link Ini} from {@link #getConfigLocations()} if set, otherwise falling back
	 * to the {@link #getDefaultConfigLocations()}. Finally any Ini objects will be merged with the value returned
	 * from {@link #getFrameworkIni()}
	 * @return Ini configuration to be used by this Environment.
	 * @since 1.4
	 */
        @Override
	protected Ini parseConfig() {
		Ini ini = getIni();
		String[] configLocations = getConfigLocations();

		if (!CollectionUtils.isEmpty(ini) && configLocations != null && configLocations.length > 0) {                        
			log.logIt(IniWebEnvironment.class.getSimpleName(), "Explicit INI instance has been provided, but configuration locations have also been specified.  The {} implementation does not currently support multiple Ini config, but this may be supported in the future. Only the INI instance will be used for configuration.", "shiro", "parseconfig", 9);
		}
		if (CollectionUtils.isEmpty(ini)) {
			log.logIt(IniWebEnvironment.class.getSimpleName(), "Checking for database configuration.", "shiro", "parseconfig", 200);
			ini = getDatabaseIni();
		}

		if (CollectionUtils.isEmpty(ini)) {
			log.logIt(IniWebEnvironment.class.getSimpleName(), "Checking any specified config locations.", "shiro", "parseconfig", 200);
			ini = getSpecifiedIni(configLocations);
		}

		if (CollectionUtils.isEmpty(ini)) {
			log.logIt(IniWebEnvironment.class.getSimpleName(), "No INI instance or config locations specified.  Trying default config locations.", "shiro", "parseconfig", 200);
			ini = getDefaultIni();
		}

		// Allow for integrations to provide default that will be merged other configuration.
		// to retain backwards compatibility this must be a different method then 'getDefaultIni()'
		ini = mergeIni(getFrameworkIni(), ini);
		Ini.Section main = ini.getSection("main");
		if (main != null && "org.apache.shiro.cache.ehcache.EhCacheManager".equals(main.get("cacheManager"))) {
			main.put("cacheManager", "org.apache.shiro.cache.MemoryConstrainedCacheManager");
		}
		if (main != null && "org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO".equals(main.get("sessionDAO"))) {
			main.put("sessionDAO", "gator.lib.shiro.sessions.GappSessionDAO");
		}
		if (main != null && "gator.lib.shiro.sessions.GappSessionDAO".equals(main.get("sessionDAO"))
				&& !main.containsKey("sessionDAO.gappConfigFile")) {
			String gappConfigFile = getServletContext().getInitParameter("gappConfigFile");
			if (gappConfigFile != null && !gappConfigFile.isBlank()) {
				main.put("sessionDAO.gappConfigFile", gappConfigFile);
			}
		}

		if (CollectionUtils.isEmpty(ini)) {
			String msg = "Shiro INI configuration was either not found or discovered to be empty/unconfigured.";
			throw new ConfigurationException(msg);
		}
		if (Boolean.getBoolean("gator.shiro.logConfig")) {
			logEffectiveConfig(ini);
		}
		return ini;            
        }

	private void logEffectiveConfig(Ini ini) {
		for (Map.Entry<String, Ini.Section> section : ini.entrySet()) {
			for (Map.Entry<String, String> property : section.getValue().entrySet()) {
				String normalizedKey = property.getKey().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
				boolean sensitive = normalizedKey.contains("password") || normalizedKey.contains("passwd")
						|| normalizedKey.contains("secret") || normalizedKey.contains("token")
						|| normalizedKey.contains("cipherkey") || normalizedKey.contains("privatekey");
				String value = sensitive ? "[REDACTED]" : property.getValue();
				log.logIt(IniWebEnvironment.class.getSimpleName(),
						"Shiro INI [" + section.getKey() + "] " + property.getKey() + "=" + value,
						"shiro", "parseconfig", 0);
			}
		}
	}
        /**
         * Get the SHIRO init from database. 
         * @return The SHIRO INI got from database.
         */
	protected Ini getDatabaseIni() {
		ServletContext servletContext = getServletContext();
		String gappConfigFile = servletContext.getInitParameter("gappConfigFile");
		String shiroConfigName = servletContext.getInitParameter("shiroConfigName");
		JsonObject jsonObj = new JsonObject();
                Gson gson = new Gson();
		dbHelper = new GappDBHelper(gappConfigFile);
		jsonObj.addProperty("gappConfigFile", gappConfigFile);
		jsonObj.addProperty("shiroConfigName", shiroConfigName);
		GappSQLStatement gappSQLStmt = new GappSQLStatement();                        
                gappSQLStmt.setStoreProcedure("app_fn_get_shiro_ini");                        
                gappSQLStmt.addParam(gson.toJson(jsonObj));                        
                String jsonShiroIni = dbHelper.executeStore(gappSQLStmt);
		gson = new Gson();
		GappSections sections = gson.fromJson(jsonShiroIni, GappSections.class);
                Ini ini = new Ini();                
                int numberOfSections = sections.getSections().size();
                log.logIt(IniWebEnvironment.class.getSimpleName(), "Section count:" + numberOfSections, "shiro", "parseconfig", 0);
                for(GappSection section: sections.getSections()) {                                         
                        log.logIt(IniWebEnvironment.class.getSimpleName(), "Section name:" + section.getName(), "shiro", "parseconfig", 0);
                        log.logIt(IniWebEnvironment.class.getSimpleName(), "Section content:" + section.getContenido().size(), "shiro", "parseconfig", 0);
                        for(HashMap<String,String> hashMap: section.getContenido()){                            
                            for (Map.Entry<String, String> map : hashMap.entrySet()) {
                                    ini.setSectionProperty(section.getName(), (String) map.getKey(), (String) map.getValue());
                            }
                        }                        
                }
                if(numberOfSections <= 0){
                        return null;
                } else {
                        return ini;
                }
	}
}
