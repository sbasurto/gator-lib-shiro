package gator.lib.shiro;

import gator.lib.logs.GappLogging;
import java.util.HashMap;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.jdbc.JdbcRealm;
import org.apache.shiro.lang.util.SimpleByteSource;
import gator.lib.db.ADO;
import gator.lib.db.GappSQLStatement;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;


/**
 * This realm has all {@link JdbcRealm} capabilities. It also supports JNDI as data source and 
 * can add salt to passwords.
 */
public class GappJNDIRealm extends JdbcRealm {
        /**
         * Logger for this object.
         * @see GappLogging
         */
	private final GappLogging logs = new GappLogging();
        
        /**
         * Ado to use.
         * @see ADO
         */
	private ADO ado;
	
        /**
         * Configuration file.
         */
	protected String gappConfigFile;
	
        /**
         * Default constructor.
         */
	public GappJNDIRealm() {
	}
	
        /**
         * Get the configuration file.
         * @return Configuration file name.
         */
	public String getGappConfigFile() {
		return gappConfigFile;
	}
	/**
         * Set the configuration file.
         * @param gappConfigFile Configuration file name.
         */
	public void setGappConfigFile(String gappConfigFile) {
		this.gappConfigFile = gappConfigFile;
		logs.logIt("GappJNDIRealm.setGappConfigFile", this.gappConfigFile, "shiro", "gapprealm", 0);
		ado = new ADO(this.gappConfigFile);
	}
	/**
         * Get the information needed to authenticate subject.
         * @param token The token to use.
         * @return Authentication information as AuthenticationInfo object.
         * @see AuthenticationInfo
         * @throws AuthenticationException On exception thrown an exception.
         * @see AuthenticationException
         */
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {                
		UsernamePasswordToken userPassToken = (UsernamePasswordToken) token;
		String username = userPassToken.getUsername();
	
		if (username == null) {
			logs.logIt("GappJNDIRealm.doGetAuthenticationInfo", "User name is null", "shiro", "gapprealm", 0);
			return null;
		}
		logs.logIt("GappJNDIRealm.doGetAuthenticationInfo", "Auth for: [" + username + "].", "shiro", username, 0);
		PasswdSalt passwdSalt = getPasswordForUser(username);                
		if (passwdSalt == null) {			
			logs.logIt("GappJNDIRealm.doGetAuthenticationInfo", "No account found for user [" + username + "]", "shiro", "gapprealm", 0);
			return null;
		}
		SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(username, passwdSalt.password, getName());
		info.setCredentialsSalt(new SimpleByteSource(passwdSalt.salt));                
		return info;
	}
	/**
         * Get password salt for user.
         * @param username The user to get the salt for.
         * @return Password salt as PasswdSalt object.
         * @see PasswdSalt
         */
	private PasswdSalt getPasswordForUser(String username) {
                GappSQLStatement gappSQLStmt = new GappSQLStatement();
                gappSQLStmt.setQuery(authenticationQuery);
                gappSQLStmt.addParam(username);
		ado.executePreparedStmt(gappSQLStmt);
	
		String salt = null;
		String password = null;
		boolean hasAccount = false;
	
		Iterator it = ado.getResult2().iterator();
		while (it.hasNext()) {
			HashMap<String, String> row = (HashMap<String, String>) it.next();
			password = row.get("usuario_password");
			salt = row.get("usuario_recover_hash");
			hasAccount = true;
		}
		logs.logIt("GappJNDIRealm.getPasswordForUser", "User: [" + username + "].", "shiro", username, 0);
		logs.logIt("GappJNDIRealm.getPasswordForUser", "Has account?: [" + hasAccount + "].", "shiro", username, 0);
		if (!hasAccount)
			return null;
		
		if (ado.getNumOfRow() > 1) {
			logs.logIt("GappJNDIRealm.getPasswordForUser", "More than one user row found for user [" + username + "]. Usernames must be unique.", "shiro", username, 0);
			throw new AuthenticationException("More than one user row found for user [" + username + "]. Usernames must be unique.");
		}
		return new PasswdSalt(password, salt);		
	}
        /**
         * Get authorization information for this principals.
         * @param principals The principals for the subject.
         * @return Authorization information as AuthorizationInfo object.
         * @see AuthorizationInfo
         */
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		//null usernames are invalid
		if (principals == null) {
			throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
		}
	
		String username = (String) getAvailablePrincipal(principals);
		Set<String> roleNames = null;
		Set<String> permissions = null;
                
                roleNames = getRoleNamesForUser(username);
	
	
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
		info.setStringPermissions(permissions);
		return info;
	}
        /**
         * Get user's roles.
         * @param username The user to get roles for.
         * @return A set of strings describing the roles of the user.
         */
	protected Set<String> getRoleNamesForUser(String username) {             
		Set<String> roleNames = new LinkedHashSet<String>();
                GappSQLStatement gappSQLStmt = new GappSQLStatement();
                gappSQLStmt.setQuery(userRolesQuery);
                gappSQLStmt.addParam(username);
		ado.executePreparedStmt(gappSQLStmt);
		Iterator it = ado.getResult2().iterator();
	
		while (it.hasNext()) {                                
			HashMap<String, String> row = (HashMap<String, String>) it.next();
			roleNames.add(row.get("role"));
                        logs.logIt("GappJNDIRealm.getRoleNamesForUser", "Rol: [" + row.get("role") + "].", "shiro", username, 0);
		}
		return roleNames;
	}
}
