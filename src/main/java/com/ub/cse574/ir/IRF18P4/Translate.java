package com.ub.cse574.ir.IRF18P4;

/**
* @author  Vishal Gawade
* @version 1.3
* @since   2018-11-25
*/

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Translate {

// **********************************************
// *** Update or verify the following values. ***
// **********************************************

// Replace the subscriptionKey string value with your valid subscription key.
    static String subscriptionKey = " ";

    static String host = "https://api.cognitive.microsofttranslator.com";
    static String path = "/translate?api-version=3.0";

    // Translate to German and French bla bla.....
    static String params = "&to=en&to=es&to=fr&to=hi&to=th";

    static String text = "Hello world!";

    public static class RequestBody {
        String Text;

        public RequestBody(String text) {
            this.Text = text;
        }
    }

    public static String Post (URL url, String content) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", content.length() + "");
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);
        connection.setRequestProperty("X-ClientTraceId", java.util.UUID.randomUUID().toString());
        connection.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        byte[] encoded_content = content.getBytes("UTF-8");
        wr.write(encoded_content, 0, encoded_content.length);
        wr.flush();
        wr.close();

        StringBuilder response = new StringBuilder ();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        return response.toString();
    }

    public static String Translate (String text) throws Exception {
        URL url = new URL (host + path + params);

        List<RequestBody> objList = new ArrayList<RequestBody>();
        objList.add(new RequestBody(text));
        String content = new Gson().toJson(objList);

        return Post(url, content);
    }

    public static String prettify(String json_text) {
        JsonParser parser = new JsonParser();
        JsonElement json = parser.parse(json_text);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);
    }
    
    public static Map<String,String> getConvertionArry(String text){
    	if(text==null || text.equalsIgnoreCase(""))
    		return null;
    	Map<String,String> ret = new HashMap<String,String>();
    	try {
            String response = Translate (text);

        	JSONArray array = new JSONArray(prettify (response));
        		array=new JSONArray(array.getJSONObject(0).get("translations").toString());
        		for(int i=0;i<array.length();i++){
        			ret.put(array.getJSONObject(i).get("to").toString(), array.getJSONObject(i).get("text").toString());
        		}
        }
        catch (Exception e) {
            System.out.println (e);
        }
    	return ret;
    }

    public static void main(String[] args) {
       /* try {
            String response = Translate ();
            System.out.println (prettify (response));
        }
        catch (Exception e) {
            System.out.println (e);
        }*/
    	/*Map<String, String> l=new Translate().getConvertionArry("i love you Rishita");
    	for(Entry<String, String> s:l.entrySet()){
    	    System.out.println(s.getKey()+":"+s.getValue());
    	}*/
    }
}