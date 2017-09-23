/**
 * 
 */
function Database() {
		this.connectionString = "";
		this.user = "";
		this.password =  "";
}

Database.prototype.connect = function() {
		this.dbConnection = java.sql.DriverManager.getConnection(this.connectionString, this.user, this.password); 
		
		return this.dbConnection; 
	};
		
Database.prototype.executeQuery = function(sql) {
		return this.dbConnection.createStatement().executeQuery(sql);
	};
