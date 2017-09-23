package org.ajdeveloppement.webserver.data;

import javax.annotation.Generated;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import org.ajdeveloppement.commons.persistence.sql.QResults;
import org.ajdeveloppement.commons.persistence.sql.QField;
import org.ajdeveloppement.commons.persistence.ObjectPersistenceException;

@Generated(value="Generated by ajcommons")
@SuppressWarnings({"nls","javadoc"})
public class T_Resources {
	public static final String TABLE_NAME = "AJWEBSERVER.Resources";

	/**
	 * Object Binder for Path field of table AJWEBSERVER.Resources
	 */
	public static final QField<java.lang.String> PATH = new QField<>(TABLE_NAME,"Path");

	/**
	 * Object Binder for Length field of table AJWEBSERVER.Resources
	 */
	public static final QField<java.lang.Long> LENGTH = new QField<>(TABLE_NAME,"Length");

	/**
	 * Object Binder for CreatedDate field of table AJWEBSERVER.Resources
	 */
	public static final QField<java.util.Date> CREATEDDATE = new QField<>(TABLE_NAME,"CreatedDate");

	/**
	 * Object Binder for LastModifiedDate field of table AJWEBSERVER.Resources
	 */
	public static final QField<java.util.Date> LASTMODIFIEDDATE = new QField<>(TABLE_NAME,"LastModifiedDate");

	/**
	 * Return all instance in database as QResults iterator
	 */
	public static QResults<Resources, Void> all() {
		return QResults.from(Resources.class);
	}

	public static Resources getInstanceWithPrimaryKey(java.lang.String path) {
		return QResults.from(Resources.class).where(PATH.equalTo(path)).first();
	}

	public static Map<String, Object> getPrimaryKeyMap(ResultSet rs) throws SQLException, ObjectPersistenceException {
		return getPrimaryKeyMap(getPrimaryKeyValues(rs));
	}

	public static Map<String, Object> getPrimaryKeyMap(Object... pkValues) {
		if(pkValues == null || pkValues.length != 1)
			return null;

		Map<String, Object> persistenceInformations = new HashMap<String, Object>();
		persistenceInformations.put("Path",pkValues[0]);
		return persistenceInformations;
	}

	public static Object[] getPrimaryKeyValues(ResultSet rs) throws SQLException, ObjectPersistenceException {
		Object[] pkValues = new Object[] {
			PATH.getValue(rs)
		};
		return pkValues;
	}
}