package beastfx.app.util;

public interface HTTPRequestHandler {

	/** return response based on request **/
	String handleRequest(String url, StringBuffer data) throws Exception;
}
