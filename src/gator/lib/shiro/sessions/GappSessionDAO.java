/*
 * Copyright (C) 2017 Sergio Basurto Juárez
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package gator.lib.shiro.sessions;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gator.lib.db.GappSQLStatement;
import gator.lib.db.helpers.GappDBHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;

/**
 * Keeps active sessions in Shiro's cache and persists them in PostgreSQL.
 */
public class GappSessionDAO extends EnterpriseCacheSessionDAO {

    private static final Gson GSON = new Gson();
    private String gappConfigFile;

    public String getGappConfigFile() {
        return gappConfigFile;
    }

    public void setGappConfigFile(String gappConfigFile) {
        this.gappConfigFile = gappConfigFile;
    }

    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = super.doCreate(session);
        persist("alta", session);
        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        JsonObject request = new JsonObject();
        request.addProperty("sessionId", String.valueOf(sessionId));
        JsonObject response = execute("consulta", request);
        if (!response.has("sessionObject") || response.get("sessionObject").isJsonNull()) {
            return null;
        }
        return deserialize(response.get("sessionObject").getAsString());
    }

    @Override
    protected void doUpdate(Session session) {
        if (session.getId() == null) {
            throw new UnknownSessionException("Cannot update a session without an identifier");
        }
        persist("cambio", session);
    }

    @Override
    protected void doDelete(Session session) {
        JsonObject request = new JsonObject();
        request.addProperty("sessionId", String.valueOf(session.getId()));
        execute("baja", request);
    }

    /** Removes one session from the in-memory cache and PostgreSQL. */
    public void killSession(Serializable sessionId) {
        SimpleSession session = new SimpleSession();
        session.setId(sessionId);
        delete(session);
    }

    /** Removes every session from the in-memory cache and PostgreSQL. */
    public void killAllSessions() {
        Cache<Serializable, Session> cache = getActiveSessionsCache();
        if (cache != null) {
            cache.clear();
        }
        execute("baja_todas", new JsonObject());
    }

    @Override
    public Collection<Session> getActiveSessions() {
        execute("limpiar", new JsonObject());
        return super.getActiveSessions();
    }

    private void persist(String action, Session session) {
        Date lastAccess = session.getLastAccessTime();
        long expiresAt = Math.addExact(lastAccess.getTime(), session.getTimeout());
        JsonObject request = new JsonObject();
        request.addProperty("sessionId", String.valueOf(session.getId()));
        request.addProperty("sessionObject", serialize(session));
        request.addProperty("sessionHost", session.getHost());
        request.addProperty("sessionStartedAt", session.getStartTimestamp().getTime());
        request.addProperty("sessionLastAccessAt", lastAccess.getTime());
        request.addProperty("sessionExpiresAt", expiresAt);
        execute(action, request);
    }

    /** Kept protected so persistence can be verified without a live database. */
    protected JsonObject execute(String action, JsonObject request) {
        if (gappConfigFile == null || gappConfigFile.isBlank()) {
            throw new SessionException("gappConfigFile is required for persistent sessions");
        }
        request.addProperty("accion", action);
        GappSQLStatement statement = new GappSQLStatement();
        statement.setStoreProcedure("app_fn_admon_session");
        statement.addParam(GSON.toJson(request));
        String result = new GappDBHelper(gappConfigFile).executeStore(statement);
        try {
            JsonObject response = JsonParser.parseString(result).getAsJsonObject();
            if (!response.has("ok") || !response.get("ok").getAsBoolean()) {
                throw new SessionException("Database rejected the session operation");
            }
            return response;
        } catch (RuntimeException exception) {
            if (exception instanceof SessionException sessionException) {
                throw sessionException;
            }
            throw new SessionException("Invalid response from app_fn_admon_session", exception);
        }
    }

    private static String serialize(Session session) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(session);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new SessionException("Unable to persist session", exception);
        }
    }

    private static Session deserialize(String serialized) {
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(serialized)))) {
            input.setObjectInputFilter(GappSessionDAO::filterSerializedClass);
            Object object = input.readObject();
            if (object instanceof Session session) {
                return session;
            }
            throw new IOException("Stored object is not a Shiro session");
        } catch (IOException | ClassNotFoundException | IllegalArgumentException exception) {
            throw new SessionException("Unable to restore persisted session", exception);
        }
    }

    private static ObjectInputFilter.Status filterSerializedClass(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 64 || info.references() > 100_000 || info.streamBytes() > 5_000_000
                || info.arrayLength() > 100_000) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (type.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        String name = type.getName();
        return name.startsWith("java.lang.") || name.startsWith("java.util.")
                || name.startsWith("org.apache.shiro.") || name.startsWith("gator.")
                ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
    }
}
