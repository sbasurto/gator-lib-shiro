package gator.lib.shiro.authc.credential;

import gator.lib.db.ADO;
import gator.lib.db.GappSQLStatement;
import gator.lib.logs.GappLogging;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;

/**
 *
 * @author <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class GappHashedCredentialMatcher extends HashedCredentialsMatcher {
        /**
         * Logger for this object.
         */
        private final GappLogging logs = new GappLogging();
        
        /**
         * Ado object to use.
         */
        private ADO ado;

        /**
         * Configuration file.
         */
	protected String gappConfigFile;
        
        /**
         * Get the configuration file.
         * @return Configuration file name.
         */
        public String getGappConfigFile() {
		return gappConfigFile;
	}
        /**
         * Set configuration file name.
         * @param gappConfigFile Configuration file name.
         */
	public void setGappConfigFile(String gappConfigFile) {
                this.gappConfigFile = gappConfigFile;
                logs.logIt("GappHashedCredentialMatcher.setGappConfigFile", this.gappConfigFile, "shiro", "gapprealm", 0);
                ado = new ADO(this.gappConfigFile);
	}
        /**
         * Check credentials and tell if they match.
         * @param token The authentication token.
         * @param info The authentication information.
         * @return True if match, false otherwise.
         */
        @Override
        public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
                UsernamePasswordToken userPassToken = (UsernamePasswordToken) token;
                setHashIterations(getIterationsForUser(userPassToken.getUsername()));
                Object tokenHashedCredentials = hashProvidedCredentials(token, info);
                Object accountCredentials = getCredentials(info);
                return equals(tokenHashedCredentials, accountCredentials);
        }
        
        /**
         * Allows to retrieve the iterations for hashing credentials, each user has its own.
         * @param username The user to get iterations.
         * @return The number of iterations for specific user.
         */
        private int getIterationsForUser(String username) {
                        String []params = new String[1];
                        params[0] = username;
                        GappSQLStatement gappSQLStmt = new GappSQLStatement();
                        gappSQLStmt.setQuery("select usuario_hash_loops from app_usuarios where usuario_id = ?");
                        gappSQLStmt.addParam(username);
                        ado.executePreparedStmt(gappSQLStmt);
                        
                        int loops = 3000;		
                        
                        Iterator it = ado.getResult2().iterator();
                        while (it.hasNext()) {
                                HashMap<String, String> row = (HashMap<String, String>) it.next();
                                loops = Integer.parseInt(row.get("usuario_hash_loops"));                                
                        }
                        logs.logIt("GappHashedCredentialMatcher.getIterationsForUser", "loops: [" + loops + "].", "shiro", username, 0);                        			
			return loops;
	}
}
