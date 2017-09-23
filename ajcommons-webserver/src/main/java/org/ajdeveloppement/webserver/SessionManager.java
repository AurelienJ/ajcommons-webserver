package org.ajdeveloppement.webserver;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ajdeveloppement.commons.UncheckedException;
import org.ajdeveloppement.commons.persistence.ObjectPersistenceException;
import org.ajdeveloppement.commons.persistence.sql.QResults;
import org.ajdeveloppement.webserver.data.SessionData;
import org.ajdeveloppement.webserver.data.T_SessionData;

public class SessionManager {
	private Map<UUID, SessionData<?>> sessions = new ConcurrentHashMap<UUID, SessionData<?>>();
	
	private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
	
	private static SessionManager instance = new SessionManager();
	
	private SessionManager() {
		scheduledExecutorService.scheduleAtFixedRate(() -> cleanSessionsCache(), 30, 30, TimeUnit.MINUTES);
	}
	
	public static SessionManager getInstance() {
		return instance;
	}
	
	private void cleanSessionsCache() {
		for(Entry<UUID, SessionData<?>> entry : sessions.entrySet()) {
			if(entry.getValue().getExpirationDate().compareTo(new Date()) < 0)
				sessions.remove(entry.getKey());
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getSession(UUID sessionId) {
		if(sessionId != null) {
			T data = null;
			SessionData<?> sessionData = sessions.get(sessionId);
			if(sessionData == null)
				sessionData = QResults.from(SessionData.class)
						.where(T_SessionData.IDSESSION.equalTo(sessionId)).first();
			
			if(sessionData != null) {
				if(sessionData.getExpirationDate() == null || sessionData.getExpirationDate().compareTo(new Date()) >= 0) {
					data = (T)sessionData.getData();
					if(!sessions.containsKey(sessionId))
						sessions.put(sessionId, sessionData);
				} else {
					sessions.remove(sessionId);
					
					//Les données ont expiré
					try {
						sessionData.delete();
					} catch (ObjectPersistenceException e) {
					}
				}
			}

			
			return data;
		}
		return null;
	}
	
	public <T> void putSession(UUID sessionId, T value) {
		putSession(sessionId, false, null, value);
	}
	
	public <T> void putSession(UUID sessionId, boolean persistent, T value) {
		putSession(sessionId, persistent, null, value);
	}
	
	@SuppressWarnings("unchecked")
	public <T> void putSession(UUID sessionId, boolean persistent, Date expirationDate, T value) {
		SessionData<T> data = null;
		
		if(persistent) {
			data = QResults.from(SessionData.class)
					.where(T_SessionData.IDSESSION.equalTo(sessionId)).first();
			if(data != null)
				data.setData(value);
		}
		
		if(data == null)
			data = new SessionData<T>(sessionId, value);
		
		data.setExpirationDate(expirationDate);
		
		sessions.put(sessionId, data);
		
		if(persistent) {
			try {
				data.save();
			} catch (ObjectPersistenceException e) {
				throw new UncheckedException(e);
			}
		}
	}
}
