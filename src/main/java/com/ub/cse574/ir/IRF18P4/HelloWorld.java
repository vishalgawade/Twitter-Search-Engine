package com.ub.cse574.ir.IRF18P4;

/**
* @author  Vishal Gawade and Pranav Bhagwat
* @version 2.1
* @since   2018-11-21
*/
import static spark.Spark.get;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.FacetParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.noggit.JSONUtil;

import com.detectlanguage.DetectLanguage;
import com.detectlanguage.Result;
import com.detectlanguage.errors.APIError;

import spark.ModelAndView;
import spark.Spark;
import spark.servlet.SparkApplication;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

public class HelloWorld implements SparkApplication {
	Map<String,String> corpusData= new HashMap<String,String>();
	
	public static void main(String[] args) throws SolrServerException, IOException, APIError {
		
	//	System.out.println(new HelloWorld().getCustomQueryText("i love this place"));
	//System.out.println(new HelloWorld().getCustomQueryText("Trump is winning"));
	}
	
	private Map<String,String> getResults(String text) throws SolrServerException, IOException{
		
		String urlString = "http://ec2-18-217-135-229.us-east-2.compute.amazonaws.com:8983/solr/IRF18P1";
		HttpSolrClient solr = new HttpSolrClient.Builder(urlString).build();
		solr.setParser(new XMLResponseParser());
		String originalQuery=text;
		SolrQuery query = new SolrQuery();
		//call azure api for query translation in different lang
//		text=getCustomQueryTextModified(text);
		
		//Query to retrieve the top hashtags
		query= setQueryForHashtags(query,text,originalQuery);
		QueryResponse response = solr.query(query);
		JSONArray jsonForTopHashtags=generataResponseForTopHashtags(response);
		SolrDocumentList docList = response.getResults();
		
		//Query on whole corpus to get the city with crime tweets with topic =crime
		SolrQuery crimeQuery = new SolrQuery();
		crimeQuery= setQueryForCrimeRatePerCity(crimeQuery);
		QueryResponse crimeResponse = solr.query(crimeQuery);
		JSONArray crimeJsonarray=generataResponseForCityForCrime(crimeResponse);
		
		//Query on whole corpus to get the city with crime tweets with topic =environment
		JSONArray topicVsCityJsonarray=crimeJsonarray;
		SolrQuery envQuery = new SolrQuery();
		envQuery= setQueryForEnvironmentPerCity(envQuery);
		QueryResponse envResponse = solr.query(envQuery);
		topicVsCityJsonarray=generataResponseForCityForEnvironment(envResponse,topicVsCityJsonarray);
		
		//Query on whole corpus to get the city with crime tweets with topic =politics
		SolrQuery politicsQuery = new SolrQuery();
		politicsQuery= setQueryForPoliticsPerCity(politicsQuery);
		QueryResponse politicsResponse = solr.query(politicsQuery);
		topicVsCityJsonarray=generataResponseForCityForPolitics(politicsResponse,topicVsCityJsonarray);
		
		//Query on whole corpus to get the city with crime tweets with topic =infra
		SolrQuery infraQuery = new SolrQuery();
		infraQuery= setQueryForInfraPerCity(infraQuery);
		QueryResponse infraResponse = solr.query(infraQuery);
		topicVsCityJsonarray=generataResponseForCityForInfra(infraResponse,topicVsCityJsonarray);
		
		//Query on whole corpus to get the city with crime tweets with topic =social unrest
		SolrQuery unrestQuery = new SolrQuery();
		unrestQuery= setQueryForUnrestPerCity(unrestQuery);
		QueryResponse unrestResponse = solr.query(unrestQuery);
		topicVsCityJsonarray=generataResponseForCityForSocialUnrest(unrestResponse,topicVsCityJsonarray);
		
		//Query on whole corpus to get the the device used to tweet
		SolrQuery sourceQuery = new SolrQuery();
		sourceQuery= setQueryForDeviceUsed(sourceQuery);
		QueryResponse sourceResponse = solr.query(sourceQuery);
		JSONObject sourceJsonarray=generataResponseDeviceUsed(sourceResponse);
		
		//Query on whole corpus to get the tweet age
		SolrQuery tweetAgeQuery = new SolrQuery();
		tweetAgeQuery= setQueryForTweetAge(tweetAgeQuery);
		QueryResponse tweetAgeResponse = solr.query(tweetAgeQuery);
		JSONArray tweetAgeJsonarray=generataResponseTweetAge(tweetAgeResponse);
		
		//Query on whole corpus to get the the active users
		SolrQuery activeUserQuery = new SolrQuery();
		activeUserQuery= setQueryForActiveUser(activeUserQuery);
		QueryResponse activeUserResponse = solr.query(activeUserQuery);
		JSONArray activeUserJsonarray=generataResponseForActiveUser(activeUserResponse);
		
		//Query on whole corpus to get the the language distribution of the tweets
		SolrQuery langQuery = new SolrQuery();
		langQuery= setQueryForTopLanguages(langQuery);
		QueryResponse langResponse = solr.query(langQuery);
		JSONArray jsonForTopLanguages=generataResponseForTopLanguages(langResponse);
		
		
		String returnValue = JSONUtil.toJSON(docList);
		
		Map<String,String> m=new HashMap<String,String>();
		
		//Query Dependent
		m.put("json", returnValue);
		m.put("hashtags",jsonForTopHashtags.toString());
		m.put("sentiAnal",HelloWorld.getSentimentAnalysis(docList).toString());
		
		//Whole corpus
		m.put("crimeCity",crimeJsonarray.toString());
		m.put("deviceUsed",sourceJsonarray.toString());
		m.put("activeUsers",activeUserJsonarray.toString());
		m.put("topLang",jsonForTopLanguages.toString());
		m.put("topicVsCity",topicVsCityJsonarray.toString());
		m.put("tweetAge",tweetAgeJsonarray.toString());
		return m;
	}
	

	private static JSONObject getSentimentAnalysis(SolrDocumentList docList) throws IOException
	{
		if(docList==null || docList.isEmpty())
			return new JSONObject();
		
		List lst=new ArrayList<String>();
		for(SolrDocument doc:docList)
		{
			String text = doc.getFieldValue("full_text").toString();
			lst.add(text);
		}
		
		return new SentimentAnalysis().getEvaluation(lst);
	}

	@Override
	public void init() {

		Spark.staticFiles.location("/public");
		HelloWorld h=new HelloWorld();
		//port(8070);

		get("/hello", (req, res) -> "Hello baba \n");
		Map<String,String> userLocationMap = new HashMap<String,String>();
		// do this
		get("/", (req, res) -> {
			userLocationMap.put("entryMessage", "Please enter your query here");
			return new ThymeleafTemplateEngine().render(
					new ModelAndView(userLocationMap, "homePage")
					);
		});


		
		get("/queryAccepted", (req, res) -> {
			String query = req.queryParams("query");
			userLocationMap.clear();
			Map<String,String> result=h.getResults(query);
			userLocationMap.put("result", result.get("json"));
			userLocationMap.put("hash",result.get("hashtags"));
			userLocationMap.put("query", query);
			userLocationMap.put("sentiAnal",result.get("sentiAnal"));
			userLocationMap.put("crimeCity",result.get("crimeCity"));
			userLocationMap.put("deviceUsed",result.get("deviceUsed"));
			userLocationMap.put("activeUsers",result.get("activeUsers"));
			userLocationMap.put("topLang",result.get("topLang"));
			userLocationMap.put("topicVsCity",result.get("topicVsCity"));
			userLocationMap.put("tweetAge",result.get("tweetAge"));
			corpusData = userLocationMap;
			//System.out.println(query);
			return new ThymeleafTemplateEngine().render(
					new ModelAndView(userLocationMap, "resultsPage")
					);
		});
		
		get("/resultsPageAnalysis", (req, res) -> {

			System.out.println(corpusData.size());
//			userLocationMap.clear();
//
//			userLocationMap.put("deviceUsed",corpusData.get("deviceUsed"));
//
//			userLocationMap.put("activeUsers",corpusData.get("activeUsers"));
//
//			userLocationMap.put("topLang",corpusData.get("topLang"));
//
//			userLocationMap.put("topicVsCity",corpusData.get("topicVsCity"));
//
//			userLocationMap.put("tweetAge",corpusData.get("tweetAge"));

			return new ThymeleafTemplateEngine().render(

					new ModelAndView(corpusData, "resultsPageAnalysis")

					);

		});
		
	}
	
	private String getCustomQueryText(String text){
		Map<String,String> texts=Translate.getConvertionArry(text);
		String langType=getDetectedQueryLang(text);
		if(langType=="")
			return text;
		String ret="";
		for(Entry<String, String> e:texts.entrySet()){
			//boost results of detected language
			if(e.getKey().equals(langType)){
				ret+="text_"+e.getKey()+":("+e.getValue()+")^3 OR ";
			}
			else{
				ret+="text_"+e.getKey()+":("+e.getValue()+") OR ";
			}
		}
		return (ret.substring(0, ret.lastIndexOf("OR")).trim());
	}
	
	private static SolrQuery setQueryForHashtags(SolrQuery query,String text,String plainQuery)
	{
		
		String qf="text_en text_hi text_fr text_th text_es hashtags";
		//for hashtag search
		if(plainQuery.contains("#")){
			query.set("q","hashtags:"+plainQuery.replaceAll("#", ""));
		}
		else{
			String langType=getDetectedQueryLang(plainQuery);
			//boost results of detected language
			if(!langType.equals("")){
						qf=qf.replaceFirst("text_"+langType, "text_"+langType+"^3");
			}
			query.set("defType","dismax");
			query.set("q",text);
			query.set("qf",qf);
			
		}
		
		//query.set("qf","text_en text_hi text_fr text_th text_es hashtags");
		query.setFacet(true);
		query.addFacetField("hashtags");
		query.setRows(500);
		query.setParam(FacetParams.FACET_LIMIT, "500");
		return query;
	}
	
	private static SolrQuery setQueryForDeviceUsed(SolrQuery sourceQuery) 
	{
		sourceQuery.set("q","*:*");
		sourceQuery.setFacet(true);
		sourceQuery.addFacetField("source");
		return sourceQuery;
	}
	
	private static SolrQuery setQueryForTweetAge(SolrQuery tweetAgeQuery) 
	{
		tweetAgeQuery.set("q","*:*");
		tweetAgeQuery.setFacet(true);
		tweetAgeQuery.addFacetField("user.created_at");
		return tweetAgeQuery;
	}
	
	
	
	private static SolrQuery setQueryForActiveUser(SolrQuery activeUserQuery) 
	{
		activeUserQuery.set("q","*:*");
		activeUserQuery.setFacet(true);
		activeUserQuery.addFacetField("user.screen_name");
		activeUserQuery.setRows(10);
		activeUserQuery.setParam(FacetParams.FACET_LIMIT, "10");
		return activeUserQuery;
	}
	
	private static SolrQuery setQueryForTopLanguages(SolrQuery topLangQuery) 
	{
		topLangQuery.set("q","*:*");
		topLangQuery.setFacet(true);
		topLangQuery.addFacetField("tweet_lang");
		topLangQuery.setRows(10);
		topLangQuery.setParam(FacetParams.FACET_LIMIT, "10");
		return topLangQuery;
	}
	
	
	
	private static SolrQuery setQueryForCrimeRatePerCity(SolrQuery crimeQuery)
	{
		crimeQuery.set("q","topic:crime");
		crimeQuery.setFacet(true);
		crimeQuery.addFacetField("city");
		//crimeQuery.addFacetQuery("topic:crime");
		return crimeQuery;
	}
	
	private static SolrQuery setQueryForEnvironmentPerCity(SolrQuery envQuery)
	{
		envQuery.set("q","topic:environment");
		envQuery.setFacet(true);
		envQuery.addFacetField("city");
		//envQuery.addFacetQuery("topic:environment");
		return envQuery;
	}
	
	private static SolrQuery setQueryForPoliticsPerCity(SolrQuery politicsQuery)
	{
		politicsQuery.set("q","topic:politics");
		politicsQuery.setFacet(true);
		politicsQuery.addFacetField("city");
		//politicsQuery.addFacetQuery("topic:politics");
		return politicsQuery;
	}
	
	private static SolrQuery setQueryForInfraPerCity(SolrQuery infraQuery)
	{
		infraQuery.set("q","topic:infra");
		infraQuery.setFacet(true);
		infraQuery.addFacetField("city");
		//infraQuery.addFacetQuery("topic:infra");
		return infraQuery;
	}
	
	private static SolrQuery setQueryForUnrestPerCity(SolrQuery unrestQuery)
	{
		unrestQuery.set("q","topic:social unrest");
		unrestQuery.setFacet(true);
		unrestQuery.addFacetField("city");
		//unrestQuery.addFacetQuery("topic:social unrest");
		return unrestQuery;
	}
	
	
	private static JSONArray generataResponseForTopHashtags(QueryResponse response)
	{
		List<FacetField> fflist = response.getFacetFields();
		JSONObject json ;
		JSONArray jsonForTopHashtags = new JSONArray();
		for(FacetField ff : fflist){
		    List<Count> counts = ff.getValues();
		    for(Count c : counts){
		    	json= new JSONObject();
		        String facetLabel = c.getName();
		        json.put("hashtag", facetLabel);
		        long facetCount = c.getCount();
		        json.put("count", facetCount);
		        jsonForTopHashtags.put(json);
		    }
		}
		return jsonForTopHashtags;
	}
	
	private static JSONArray generataResponseForActiveUser(QueryResponse response)
	{
		List<FacetField> fflist = response.getFacetFields();
		JSONObject json ;
		JSONArray jsonForTopActiveUsers = new JSONArray();
		for(FacetField ff : fflist){
		    List<Count> counts = ff.getValues();
		    for(Count c : counts){
		    	json= new JSONObject();
		        String facetLabel = c.getName();
		        json.put("user", facetLabel);
		        long facetCount = c.getCount();
		        json.put("count", facetCount);
		        jsonForTopActiveUsers.put(json);
		    }
		}
		return jsonForTopActiveUsers;
	}
	
	private static JSONArray generataResponseForTopLanguages(QueryResponse response)
	{
		List<FacetField> fflist = response.getFacetFields();
		JSONObject json ;
		JSONArray jsonForTopActiveUsers = new JSONArray();
		for(FacetField ff : fflist){
		    List<Count> counts = ff.getValues();
		    for(Count c : counts){
		    	json= new JSONObject();
		        String facetLabel = c.getName();
		        json.put("language", facetLabel);
		        long facetCount = c.getCount();
		        json.put("count", facetCount);
		        jsonForTopActiveUsers.put(json);
		    }
		}
		return jsonForTopActiveUsers;
	}
	
	
	private static JSONArray generataResponseForCityForCrime(QueryResponse crimeResponse)
	{
		//SolrDocumentList crimedocList = crimeResponse.getResults();
		List<FacetField> crimelist = crimeResponse.getFacetFields();
		JSONObject crimeJson ;
		JSONArray crimeJsonarray = new JSONArray();
		for(FacetField ff : crimelist){
		    
		    List<Count> counts = ff.getValues();
		    for(Count c : counts){
		    	crimeJson= new JSONObject();
		        String facetLabel = c.getName();
		        crimeJson.put("crime_city", facetLabel);
		        long facetCount = c.getCount();
		        crimeJson.put("count", facetCount);
		        crimeJsonarray.put(crimeJson);
		    }
		}
		return crimeJsonarray;
	}
	
	private static JSONArray generataResponseForCityForEnvironment(QueryResponse response,JSONArray topicVsCityJsonarray)
	{
		//SolrDocumentList crimedocList = crimeResponse.getResults();
		List<FacetField> envlist = response.getFacetFields();
		JSONObject envJson ;
		
		for(FacetField ff : envlist){
		    
		    List<Count> counts = ff.getValues();
		    for(Count c : counts){
		    	envJson= new JSONObject();
		        String facetLabel = c.getName();
		        envJson.put("env_city", facetLabel);
		        long facetCount = c.getCount();
		        envJson.put("count", facetCount);
		        topicVsCityJsonarray.put(envJson);
		    }
		}
		return topicVsCityJsonarray;
	}
	
	private static JSONArray generataResponseForCityForPolitics(QueryResponse response,JSONArray topicVsCityJsonarray)
	{
		//SolrDocumentList crimedocList = crimeResponse.getResults();
		List<FacetField> politicsList = response.getFacetFields();
		JSONObject politicsJson ;
		
		for(FacetField ff : politicsList){
		    
		    List<Count> counts = ff.getValues();
		    for(Count c : counts){
		    	politicsJson= new JSONObject();
		        String facetLabel = c.getName();
		        politicsJson.put("politics_city", facetLabel);
		        long facetCount = c.getCount();
		        politicsJson.put("count", facetCount);
		        topicVsCityJsonarray.put(politicsJson);
		    }
		}
		return topicVsCityJsonarray;
	}
	
	private static JSONArray generataResponseForCityForInfra(QueryResponse response,JSONArray topicVsCityJsonarray)
	{
		//SolrDocumentList crimedocList = crimeResponse.getResults();
		List<FacetField> infraList = response.getFacetFields();
		JSONObject infraJson ;
		
		for(FacetField ff : infraList){
		    
		    List<Count> counts = ff.getValues();
		    for(Count c : counts){
		    	infraJson= new JSONObject();
		        String facetLabel = c.getName();
		        infraJson.put("infra_city", facetLabel);
		        long facetCount = c.getCount();
		        infraJson.put("count", facetCount);
		        topicVsCityJsonarray.put(infraJson);
		    }
		}
		return topicVsCityJsonarray;
	}
	
	private static JSONArray generataResponseForCityForSocialUnrest(QueryResponse response,JSONArray topicVsCityJsonarray)
	{
		//SolrDocumentList crimedocList = crimeResponse.getResults();
		List<FacetField> unrestList = response.getFacetFields();
		JSONObject unrestJson ;
		
		for(FacetField ff : unrestList){
		    
		    List<Count> counts = ff.getValues();
		    for(Count c : counts){
		    	unrestJson= new JSONObject();
		        String facetLabel = c.getName();
		        unrestJson.put("unrest_city", facetLabel);
		        long facetCount = c.getCount();
		        unrestJson.put("count", facetCount);
		        topicVsCityJsonarray.put(unrestJson);
		    }
		}
		return topicVsCityJsonarray;
	}
	
	
	private static JSONObject generataResponseDeviceUsed(QueryResponse sourceResponse)
	{//SolrDocumentList crimedocList = crimeResponse.getResults();

		List<FacetField> sourceList = sourceResponse.getFacetFields();

		JSONObject sourceJson = new JSONObject();

		JSONArray sourceJsonarray = new JSONArray();

		for(FacetField ff : sourceList){

		    

		    List<Count> counts = ff.getValues();

		    for(Count c : counts){

//		    	sourceJson= new JSONObject();

		        String facetLabel = c.getName();

		        long facetCount = c.getCount();

		        

		        if(facetLabel.contains("Android"))

		        {

			      //  sourceJson.put("device", "Android");

			        sourceJson.put("Android", facetCount);

			        sourceJsonarray.put(sourceJson);

		        }

		        else if(facetLabel.contains("iPhone"))

		        {

		        	//sourceJson.put("device", "iPhone");

			        sourceJson.put("iPhone", facetCount);

			        sourceJsonarray.put(sourceJson);

		        }

		        else if(facetLabel.contains("Web Client"))

		        {

		        //	sourceJson.put("device", "Web Client");

			        sourceJson.put("Web_Client", facetCount);

			        sourceJsonarray.put(sourceJson);

		        }

		        else if(facetLabel.contains("iPad"))

		        {

		        //	sourceJson.put("device", "iPad");

			        sourceJson.put("iPad", facetCount);

			        sourceJsonarray.put(sourceJson);

		        }

		        else if(facetLabel.contains("Mobile Web"))

		        {

		        //	sourceJson.put("device", "Mobile Web");

			        sourceJson.put("Mobile_Web", facetCount);

			        sourceJsonarray.put(sourceJson);

		        }

		        else if(facetLabel.contains("Tweetbot for iΟS"))

		        {

		        //	sourceJson.put("device", "Tweetbot for iΟS");

			        sourceJson.put("Tweetbot_for", facetCount);

			        sourceJsonarray.put(sourceJson);

		        }

		    }

		}

		return sourceJson;

	}
	
	private static JSONArray generataResponseTweetAge(QueryResponse response)
	{
		//SolrDocumentList crimedocList = crimeResponse.getResults();
		List<FacetField> sourceList = response.getFacetFields();
		JSONObject tweetAgeJson ;
		JSONArray tweetAgeJsonarray = new JSONArray();
		for(FacetField ff : sourceList){
		    
		    List<Count> counts = ff.getValues();
		    int count2008=0,count2009=0,count2010=0,count2011=0,count2012=0,count2013=0,count2014=0,count2015=0,count2016=0,count2017=0,count2018=0;
		    ArrayList<Integer>countArray= new ArrayList<Integer>();
		    
		    for(Count c : counts){
		    	
		        String facetLabel = c.getName();
		        long facetCount = c.getCount();
		       
		        
		        if(facetLabel.contains("2008")) count2008+= (int)facetCount;
		        else if(facetLabel.contains("2009")) count2009+= (int)facetCount;
		        else if(facetLabel.contains("2010")) count2010+= (int)facetCount; 
		        else if(facetLabel.contains("2011")) count2011+= (int)facetCount;
		        else if(facetLabel.contains("2012")) count2012+= (int)facetCount;
		        else if(facetLabel.contains("2013")) count2013+= (int)facetCount;
		        else if(facetLabel.contains("2014")) count2014+= (int)facetCount;
		        else if(facetLabel.contains("2015")) count2015+= (int)facetCount;
		        else if(facetLabel.contains("2016")) count2016+= (int)facetCount;
		        else if(facetLabel.contains("2017")) count2017+= (int)facetCount;
		        else if(facetLabel.contains("2018")) count2018+= (int)facetCount;
		        
		    }
		    
	    	tweetAgeJson= new JSONObject();tweetAgeJson.put("year", "2008");tweetAgeJson.put("count", count2008);
	    	tweetAgeJsonarray.put(tweetAgeJson);
	    	
	    	tweetAgeJson= new JSONObject();tweetAgeJson.put("year", "2009");tweetAgeJson.put("count", count2009);
	    	tweetAgeJsonarray.put(tweetAgeJson);
	    	
	    	tweetAgeJson= new JSONObject();tweetAgeJson.put("year", "2010");tweetAgeJson.put("count", count2010);
	    	tweetAgeJsonarray.put(tweetAgeJson);
	    	
	    	tweetAgeJson= new JSONObject();tweetAgeJson.put("year", "2011");tweetAgeJson.put("count", count2011);
	    	tweetAgeJsonarray.put(tweetAgeJson);
	    	
	    	tweetAgeJson= new JSONObject();tweetAgeJson.put("year", "2012");tweetAgeJson.put("count", count2012);
	    	tweetAgeJsonarray.put(tweetAgeJson);
	    	
	    	tweetAgeJson= new JSONObject();tweetAgeJson.put("year", "2013");tweetAgeJson.put("count", count2013);
	    	tweetAgeJsonarray.put(tweetAgeJson);
	    	
	    	tweetAgeJson= new JSONObject();tweetAgeJson.put("year", "2014");tweetAgeJson.put("count", count2014);
	    	tweetAgeJsonarray.put(tweetAgeJson);
	    	
	    	tweetAgeJson= new JSONObject();	tweetAgeJson.put("year", "2015");tweetAgeJson.put("count", count2015);
	    	tweetAgeJsonarray.put(tweetAgeJson);

	    	tweetAgeJson= new JSONObject();tweetAgeJson.put("year", "2016");tweetAgeJson.put("count", count2016);
	    	tweetAgeJsonarray.put(tweetAgeJson);
	    	
	    	tweetAgeJson= new JSONObject();tweetAgeJson.put("year", "2017");tweetAgeJson.put("count", count2017);
	    	tweetAgeJsonarray.put(tweetAgeJson);
	    	
	    	tweetAgeJson= new JSONObject();tweetAgeJson.put("year", "2018");tweetAgeJson.put("count", count2018);
	    	tweetAgeJsonarray.put(tweetAgeJson);
		    	
		}
		return tweetAgeJsonarray;
	}
	
	private static String getDetectedQueryLang(String query){
		DetectLanguage.apiKey = "3b51578bf6ef50c27d09170cf4f48b67";
		List<Result> results;
		try {
			results = DetectLanguage.detect(query);
			Result result = results.get(0);
			return result.language;
		} catch (APIError e) {
			// TODO Auto-generated catch block
			System.err.println("Oops unable to detect text lang,check your api subscription");
			e.printStackTrace();
		}
		return "";
	}
	private static String getCustomQueryTextModified(String text){
		Map<String,String> texts=Translate.getConvertionArry(text);
		String ret="";
		for(Entry<String, String> e:texts.entrySet()){
			//boost results of detected language
				ret+="("+e.getValue()+") OR ";
		}
		return (ret.substring(0, ret.lastIndexOf("OR")).trim());
	}
	
}