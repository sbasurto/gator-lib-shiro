package gator.lib.shiro.session.test;

import com.google.gson.JsonObject;
import gator.lib.shiro.sessions.GappSessionDAO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;

public final class GappSessionDAOSelfCheck {

    private GappSessionDAOSelfCheck() {
    }

    public static void main(String[] args) {
        Map<String, String> database = new ConcurrentHashMap<>();
        FakeDAO firstNode = new FakeDAO(database);
        SimpleSession created = new SimpleSession("127.0.0.1");
        created.setAttribute("user", "admin");
        String sessionId = (String) firstNode.create(created);

        assert database.containsKey(sessionId);
        assert firstNode.readSession(sessionId) == created;

        FakeDAO restartedNode = new FakeDAO(database);
        assert "admin".equals(restartedNode.readSession(sessionId).getAttribute("user"));

        restartedNode.killSession(sessionId);
        assert database.isEmpty();
        try {
            restartedNode.readSession(sessionId);
            throw new AssertionError("Killed session was restored");
        } catch (UnknownSessionException expected) {
            // Expected.
        }

        firstNode.create(new SimpleSession());
        firstNode.create(new SimpleSession());
        firstNode.killAllSessions();
        assert database.isEmpty();
        assert firstNode.getActiveSessions().isEmpty();
    }

    private static final class FakeDAO extends GappSessionDAO {
        private final Map<String, String> database;

        private FakeDAO(Map<String, String> database) {
            this.database = database;
            setCacheManager(new MemoryConstrainedCacheManager());
        }

        @Override
        protected JsonObject execute(String action, JsonObject request) {
            JsonObject response = new JsonObject();
            response.addProperty("ok", true);
            String sessionId = request.has("sessionId") ? request.get("sessionId").getAsString() : null;
            switch (action) {
                case "alta", "cambio" -> database.put(sessionId, request.get("sessionObject").getAsString());
                case "consulta" -> {
                    String stored = database.get(sessionId);
                    if (stored == null) {
                        response.add("sessionObject", null);
                    } else {
                        response.addProperty("sessionObject", stored);
                    }
                }
                case "baja" -> database.remove(sessionId);
                case "baja_todas" -> database.clear();
                case "limpiar" -> { }
                default -> throw new AssertionError("Unexpected action: " + action);
            }
            return response;
        }
    }
}
