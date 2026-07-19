package gator.lib.shiro.filters;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gator.lib.db.GappSQLStatement;
import gator.lib.db.helpers.GappDBHelper;
import gator.lib.db.helpers.GappResponses;
import gator.lib.logs.GappLogging;
import gator.lib.net.cookies.CookieMonster;
import gator.lib.net.cookies.GappCookie;
import gator.lib.shiro.sessions.GappBodega;
import gator.lib.shiro.sessions.GappCuenta;
import gator.lib.shiro.sessions.GappSessionData;
import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;


/**
 *
 * @author sbas
 */
public class GappFilter implements Filter {
        /**
         * The logger class
         */
	private final GappLogging logs = new GappLogging();
        
        /**
         * Database helper.
         */
	private GappDBHelper dbhelper;
        
        /**
         * Responses that will be returned by database.
         */
        private GappResponses responses;
        
        /**
         * Session.
         */
        private Session session;
        
        
        private final Gson gson = new Gson();
	/**
	 * Just override the method to comply.
	 * @param filterConfig The filter configuration.
	 * @throws ServletException A Servlet exception will be thrown if something goes wrong.
	 */
	 @Override
	 public void init(FilterConfig filterConfig) throws ServletException {
	 }
         /**
          * Actually filter request.
          * @param req The servlet request.
          * @param res The servlet response.
          * @param filterChain The chain filter to use.
          */
	 @Override
	 public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) {
		try{                        
                        Subject currentUser = SecurityUtils.getSubject();
                        logs.logIt("GappFilter.doFilter", "Usuario: " + currentUser.getPrincipal(), "shiro", "gapprealm", 0);
                        logs.logIt("GappFilter.doFilter", "Ya se identificó: " + currentUser.isAuthenticated(), "shiro", "gapprealm", 0);
                        
                        if(currentUser.getSession(false) == null) {                                
                                session = currentUser.getSession(true);
                                logs.logIt("GappFilter.doFilter", "La sesión ha sido creada", "shiro", "gapprealm", 0);
                        } else {
                                logs.logIt("GappFilter.doFilter", "La sesión existe", "shiro", "gapprealm", 0);
                                session = currentUser.getSession(false);
                        }
                        if(currentUser.isAuthenticated()) {                                                     
                                logs.logIt("GappFilter.doFilter", "Es usuario autenticado", "shiro", "gapprealm", 0);
                                logs.logIt("GappFilter.doFilter", "La sesión ha sido iniciada?: " + session.getAttribute("init"), "shiro", "gapprealm", 0);
                                if(session.getAttribute("init") == null) {
                                        logs.logIt("GappFilter.doFilter", "Inicializando sesión", "shiro", "gapprealm", 0);
                                        logs.logIt("GappFilter.doFilter", "Req:" + req.getParameter("file"), "shiro", "gapprealm", 0);
                                        logs.logIt("GappFilter.doFilter", "Sess:" + session.getAttribute("gappConfigFile"), "shiro", "gapprealm", 0);
                                        String file = req.getParameter("file") == null?(String)session.getAttribute("gappConfigFile"):(String)req.getParameter("file");
                                        if(file == null) file = req.getServletContext().getInitParameter("gappConfigFile");
                                        String sessionDataStr = initGapp(file, (String)currentUser.getPrincipal());                                        
                                        GappSessionData sessionData = gson.fromJson(sessionDataStr, GappSessionData.class);
                                        logs.logIt("GappFilter.doFilter", "DL Sess:" + sessionData.getDebugLevel(), "shiro", "gapprealm", 0);
                                        sessionData.setDebugLevel(sessionData.getDebugLevel());
                                        session.setTimeout(sessionData.getSessionTimeout());
                                        session.setAttribute("sessionData", sessionDataStr);
                                        logs.logIt("GappFilter.doFilter", "Final:" + file, "shiro", "gapprealm", 0);
                                        session.setAttribute("gappConfigFile", file);                                        
                                        session.setAttribute("init", true);
                                }
                                if(req.getParameter("cuentaId") != null && req.getParameter("bodegaId") != null) {
                                        GappSessionData sessionData = gson.fromJson((String) session.getAttribute("sessionData"), GappSessionData.class);
                                        GappCuenta selectedAccount = sessionData.getAccounts().findById(req.getParameter("cuentaId"));
                                        if (selectedAccount == null || selectedAccount.findWarehouseById(req.getParameter("bodegaId")) == null) {
                                                throw new ServletException("La cuenta y bodega seleccionadas no pertenecen al usuario");
                                        }
                                        session.setAttribute("cuenta_id", req.getParameter("cuentaId"));
                                        session.setAttribute("bodega_id", req.getParameter("bodegaId"));
                                }
                                logs.logIt("GappFilter.doFilter", "Req-ini:" + req.getParameter("file"), "shiro", "gapprealm", 0);
                                logs.logIt("GappFilter.doFilter", "Sess-ini:" + session.getAttribute("gappConfigFile"), "shiro", "gapprealm", 0);
                        } else {
                                Session session = currentUser.getSession(false);
                                logs.logIt("GappFilter.doFilter", "pre iniciada: " + session.getAttribute("pre.init"), "shiro", "gapprealm", 0);
                                if(session.getAttribute("pre.init") == null) {
                                        GappSessionData sessionData = new GappSessionData();
                                        logs.logIt("GappFilter.doFilter", "Pre-inicializando sesión", "shiro", "gapprealm", 0);
                                        logs.logIt("GappFilter.doFilter", "Req:" + req.getParameter("file"), "shiro", "gapprealm", 0);
                                        logs.logIt("GappFilter.doFilter", "ReqToUse:" + req.getParameter("indexToUse"), "shiro", "gapprealm", 0);
                                        logs.logIt("GappFilter.doFilter", "Sess:" + session.getAttribute("gappConfigFile"), "shiro", "gapprealm", 0);
                                        String file = req.getParameter("file") == null?(String)session.getAttribute("gappConfigFile"):(String)req.getParameter("file");
                                        file = file == null?req.getParameter("indexToUse"):file;
                                        sessionData.setLanguage("es");
                                        GappCuenta account = new GappCuenta();
                                        GappBodega bodega = new GappBodega();
                                        account.setCuentaId("0");
                                        bodega.setBodegaId("0");
                                        account.setCuentaNombre("anonymous");
                                        bodega.setBodegaNombre("anonymous");
                                        account.setCuentaSistema(req.getParameter("systemId"));
                                        account.addBodega(bodega);
                                        sessionData.addAccounts(account);
                                        session.setAttribute("pre.init", true);
                                        String sessionDataStr = gson.toJson(sessionData);
                                        session.setAttribute("sessionData", sessionDataStr);
                                        if(file != null) {
                                                session.setAttribute("gappConfigFile", file);
                                        } else {
                                                file = req.getServletContext().getInitParameter("gappConfigFile");
                                                session.setAttribute("gappConfigFile", file);
                                        }
                                }
                        }
                        CookieMonster monster = new CookieMonster();
                        GappCookie gappCookie;
                        HttpServletRequest httpReq = (HttpServletRequest) req;
                        if (currentUser.isAuthenticated()
                                && !Boolean.TRUE.equals(session.getAttribute("login.challenge.verified"))
                                && !isChallengeAllowed(httpReq)) {
                                ((HttpServletResponse) res).sendRedirect(httpReq.getContextPath() + "/forms2/choose_server2.jsp");
                                return;
                        }
                        String manageRedirect = session.getAttribute("manageredirect") == null?"0":(String) session.getAttribute("manageredirect");
                        logs.logIt("GappFilter.doFilter", "I will manage redirect: " + manageRedirect, "shiro", "gapprealm", 0);
                        if(httpReq.getHeader("Cookie") != null && manageRedirect.equals("1")) {
                            logs.logIt("GappFilter.doFilter", "Eating cookie jar at filter yumi, yumi!", "shiro", "gapprealm", 0);
                            gappCookie = monster.eatCookie(httpReq.getHeader("Cookie"), "softgator.id");
                            String file = req.getParameter("file") == null?(String)session.getAttribute("gappConfigFile"):(String)req.getParameter("file");
                            session.setAttribute("manageredirect", "0");
                            if(isThereAnURI(file, gappCookie.getId())) {
                                httpReq.getServletContext().getRequestDispatcher("/forms2/manage_redirect.jsp").forward(req, res);
                            }
                        }                        
                        res.setCharacterEncoding("UTF-8");
			filterChain.doFilter(req, res);
		}catch(ServletException | IOException | NullPointerException e){
                        logs.logIt("GappFilter.doFilter", logs.getStackTraceString(e), "shiro", "gapprealm", 0);
		}
	}
	@Override
	public void destroy() {
	}// </editor-fold>
        private String initGapp(String gappConfigFile, String usuario) {   
                dbhelper = new GappDBHelper(gappConfigFile);
                JsonObject jsonObj = new JsonObject();
                jsonObj.addProperty("usuario", usuario);
                GappSQLStatement gappSQLStmt = new GappSQLStatement();
                gappSQLStmt.setStoreProcedure("app_fn_get_session_data");
                gappSQLStmt.addParam(gson.toJson(jsonObj));
                String jsonProcess = dbhelper.executeStore(gappSQLStmt);
                responses = gson.fromJson(jsonProcess, GappResponses.class);
                responses.getResponses().get(0).getResponse().get("servidor");
                String informacionDeSesion = gson.toJson(responses.getResponses().get(0).getResponse().get("session_data"));
                return informacionDeSesion;
        }
        private boolean isThereAnURI(String gappConfigFile, String cookieId) {               
                JsonObject jsonObj = new JsonObject();
                dbhelper = new GappDBHelper(gappConfigFile);
                jsonObj.addProperty("cookieId", cookieId);
                jsonObj.addProperty("atributo", "last_uri");
                GappSQLStatement gappSQLStmt = new GappSQLStatement();
                gappSQLStmt.setStoreProcedure("app_fn_get_cookie_attr");
                gappSQLStmt.addParam(gson.toJson(jsonObj));
                String respuesta = dbhelper.executeStore(gappSQLStmt);                
                return !respuesta.equals("");
        }
        private boolean isChallengeAllowed(HttpServletRequest request) {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                return path.equals("/forms2/choose_server2.jsp") || path.equals("/index.jsp") || path.equals("/")
                        || path.equals("/GenJS") || path.equals("/GetJS") || path.equals("/GenCSS") || path.equals("/GetCSS")
                        || path.equals("/GappDoTranslate") || path.equals("/salida.jsp")
                        || path.matches(".*\\.(css|js|png|jpg|jpeg|gif|svg|ico|woff2?|ttf)$");
        }
}
