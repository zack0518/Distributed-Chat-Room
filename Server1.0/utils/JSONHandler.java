package utils;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Function: To convert the JSONstring and Object to each other.
 * */
public class JSONHandler {
	public static Map<String, Object> Json2Map(String json){
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);
			return result;
		} catch (JsonParseException e) {
			System.err.println("(ERROR) Parsing JSON String error. String: "
					+ json);
			return null;
		} catch (JsonMappingException e) {
			System.err.println("(ERROR) Parsing JSON String error. String: " + json);
			return null;
		} catch (IOException e) {
			System.err.println("(ERROR) Parsing JSON String error. String: " + json);
			return null;
		}
	}
	
	public static String Map2Json(Map<String, Object> map) {
		try {
			String result = new ObjectMapper().writeValueAsString(map);
			return result;
		}
		catch(Exception e) {
			System.err.println("(ERROR) Converting Object to JSON error.");
			return "";
		}
	}
}
