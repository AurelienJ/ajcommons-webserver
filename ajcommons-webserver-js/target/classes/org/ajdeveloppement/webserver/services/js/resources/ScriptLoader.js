/**
 * 
 */
var loadedScripts = new java.util.ArrayList();

/**
 * Chargement d'un script javascript
 *  
 * @param script le chemin du script
 */
function loadScript(script) {
	print("Load script: " + script);
	
	if(!loadedScripts.contains(script)) {
		load(basePath + "/" + script);
		loadedScripts.add(script);
	}
}