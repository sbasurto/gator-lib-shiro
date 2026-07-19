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
package gator.lib.shiro;

import gator.lib.logs.GappLogging;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;


/**
 *
 * @author <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class GappFormAuthenticationFilter extends FormAuthenticationFilter {
        private final GappLogging logs = new GappLogging();
        /**
         * Configuration file.
         */
        protected String gappConfigFile;
        /**
         * Set the failure attribute if authentication fails.
         * @param request The servlet request.
         * @param ae The exception thrown.
         */
	protected void setFailureAttribute(ServletRequest request, Exception ae) {
                logs.logIt("GappFormAuthenticationFilter", logs.getStackTraceString(ae), "shiro", "anonymous", 0);		
		request.setAttribute(getFailureKeyAttribute(), ae.getMessage());
	}
        /**
         * Get the configuration file name.
         * @return Configuration file name.
         */
        public String getGappConfigFile() {
		return gappConfigFile;
	}
        /**
         * Allows to set the configuration file name.
         * @param gappConfigFile The name for the configuration file.
         */
	public void setGappConfigFile(String gappConfigFile) {
                this.gappConfigFile = gappConfigFile;
                logs.logIt("GappFormAuthenticationFilter.setGappConfigFile", this.gappConfigFile, "shiro", "gapprealm", 0);
	}
        /**
         * On success redirect to original page or the default login page defined in shiro ini file.
         * @param token The authentication token.
         * @param subject Subject that was authenticated.
         * @param request Servlet Request.
         * @param response Servlet Response.
         * @return True on success, false otherwise.
         * @throws Exception Any exception that could happens.
         */
        @Override
        protected boolean onLoginSuccess(AuthenticationToken token, Subject subject,
                                     ServletRequest request, ServletResponse response) throws Exception {
                request.setAttribute("gappConfigFile", this.getGappConfigFile());
                subject.getSession(true).setAttribute("gappConfigFile", this.getGappConfigFile());
                subject.getSession().setAttribute("manageredirect", "1");
                subject.getSession().setAttribute("login.challenge.verified", false);
                subject.getSession().removeAttribute("login.challenge.hash");
                subject.getSession().removeAttribute("login.challenge.expires");
                subject.getSession().removeAttribute("login.challenge.attempts");
                subject.getSession().removeAttribute("login.challenge.sent");
                subject.getSession().removeAttribute("login.challenge.token");
                issueSuccessRedirect(request, response);
                //we handled the success redirect directly, prevent the chain from continuing:
                return false;
        }
}
