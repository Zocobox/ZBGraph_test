/*Copyright (C) 2015 JSR Ventures GmbH */
package com.zocobox.ZBGraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;


import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.*;

/**
 * Servlet implementation class Entities
 */
//@WebServlet(description = "Manages the entities, represented as nodes in the graphDB", urlPatterns = { "/Entities" })
@Produces(MediaType.APPLICATION_JSON)
@Path("/v1/")
public class Entities extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger(Entities.class);
       
	//private String jsonCypherStatements= "{ \"statements\" [ { \"statement\" : \"match (n:$entityType) where n.ID = $startEntity return n\" }  ] }";
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Entities() {
        super();
    }

	/**
	 * Returns a specific node and all their properties. Works for all types on entities.
	 * 
	 * @Param
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * 
	 */
    @GET
    @Path("/{entityType}/{startEntity}")
	public String getEntity(
			@PathParam("entityType") String entityType,
			@PathParam("startEntity") String startEntity,
			@Context HttpServletRequest request, 
			@Context HttpServletResponse response) throws ServletException, IOException {
		
		//Check that the entityType is a valid one
		if(!isValidEntitytype(entityType)){
			return "{\"invalid request. Entity unknown.\"}";
		}
		//TODO:Get user personal cluster and corporation ID
		Integer personalCluster = 3024;
		Integer corpID = 65535;
		
		//request all information from Neo4j about the node. The query ensures that the entity is related to the personal cluster 
		// of the user, therefore he is entitles to see it
		//String jsonCypherStatementsFinal = "{ \"statements\" : [ { \"statement\" : \"match (n:" + entityType + ") where n.ID = " + startEntity + " return n\" }  ] }";
		String jsonCypherStatementsFinal = "{ \"statements\" : [ { \"statement\" : \" ";
		jsonCypherStatementsFinal += "match (subentity:" + entityType+ ")-[r*]->(cluster:Cluster)-[:RELATED_TO]->(corp:Corp)  where corp.ID=" + corpID +" and cluster.ID = " 
				+ personalCluster + " and subentity.ID ="+ startEntity + " and NONE( rel in r WHERE type(rel)=\\\"RELATED_TO\\\") return subentity, labels(subentity)";
		jsonCypherStatementsFinal +="\", \"resultDataContents\": [ \"graph\" ] ";
		jsonCypherStatementsFinal += "  }  ] }";
		log.debug("doGet: final statement = " + jsonCypherStatementsFinal);
		
		JSONObject JSONresponse = new JSONObject(sendStatementNeo4j(jsonCypherStatementsFinal));
		JSONArray nodes = JSONresponse.getJSONArray("nodes");
		
		
		//return jsonCypherStatementsFinal + "\n\n" + sendStatementNeo4j(jsonCypherStatementsFinal) + "\n\n" + nodes.toString();
		return jsonCypherStatementsFinal + "\n\n" + nodes.toString();
	}
     
    @GET
    @Path("/{entityType}")
    public String  getAllEntitiesByType(
    		@PathParam("entityType") String entityType,
    		@Context HttpServletRequest request,
    		@Context HttpServletResponse response) throws ServletException, IOException{
    	
		//Check that the entityType is a valid one
		if(!isValidEntitytype(entityType)){
			return "{\"invalid request. Entity unknown.\"}";
		}
		//TODO:Get user personal cluster and corporation ID
		Integer personalCluster = 3024;
		Integer corpID = 65535;
    	
		String jsonCypherStatementsFinal = "{ \"statements\" : [ { \"statement\" : \" ";
		jsonCypherStatementsFinal += "match (subentity:" + entityType+ ")-[r*]->(cluster:Cluster)-[:RELATED_TO]->(corp:Corp)  where corp.ID=" + corpID +" and cluster.ID = " 
				+ personalCluster + " and NONE( rel in r WHERE type(rel)=\\\"RELATED_TO\\\") return subentity, labels(subentity)";
		jsonCypherStatementsFinal +="\", \"resultDataContents\": [ \"graph\" ] ";
		jsonCypherStatementsFinal += "  }  ] }";
		log.debug("doGet: final statement = " + jsonCypherStatementsFinal);
    	
		String rawResponse = sendStatementNeo4j(jsonCypherStatementsFinal) + "\n\n";
		
		List<JSONArray> nodes = getNodesFromResponseTypeGraph(rawResponse);
		
		for (int i =0; i< nodes.size(); i++){
			rawResponse += nodes.get(i).toString() + "\n";
		 	
		}
		return jsonCypherStatementsFinal + "\n\n" +  rawResponse;

    }
    
    
    
    
    @POST
    @Path("/{entityType}/{startEntity}")
	public String createEntity(
			@PathParam("entityType") String entityType,
			@PathParam("startEntity") String startEntity,
			@Context HttpServletRequest request, 
			@Context HttpServletResponse response) throws ServletException, IOException {
		
		//Check that the entityType is a valid one
		if(!isValidEntitytype(entityType)){
			return "{\"invalid request. Entity unknown.\"}";
		}
		//TODO:Get user personal cluster and corporation ID
		Integer personalCluster = 3024;
		Integer corpID = 65535 ;
		
		//request all information from Neo4j about the node. The query ensures that the entity is related to the personal cluster 
		// of the user, therefore he is entitles to see it
		String jsonCypherStatementsFinal = "{ \"statements\" : [ { \"statement\" : \" ";
		jsonCypherStatementsFinal += "match (subentity:" + entityType+ ")-[r*]->(cluster:Cluster)-[:RELATED_TO]->(corp:Corp)  where corp.ID=" + corpID +" and cluster.ID = " 
				+ personalCluster + " and subentity.ID ="+ startEntity + " and NONE( rel in r WHERE type(rel)=\\\"RELATED_TO\\\") return subentity";
		jsonCypherStatementsFinal += " \" }, { \"statement\" : \" ";
		jsonCypherStatementsFinal += "create (n:" + entityType;
		jsonCypherStatementsFinal += " \" }  ] }";
		log.debug("doGet: final statement = " + jsonCypherStatementsFinal);
		
		return jsonCypherStatementsFinal + "\n\n" + sendStatementNeo4j(jsonCypherStatementsFinal);
	}
     
    
    
    
    
    
    
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	/**
	 * @see HttpServlet#doPut(HttpServletRequest, HttpServletResponse)
	 */
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doDelete(HttpServletRequest, HttpServletResponse)
	 */
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * 
	 * @author JSR Ventures GmbH
	 *
	 */
	public static class MyHostnameVerifier implements HostnameVerifier {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}
	
	
	/**
	 * Connects to the Zocobox instance of Neo4j, posts a Cypher statement and returns the response
	 * 
	 * @author JSR Ventures GmbH
	 * @param statementNeo4j
	 * @return JSON ron Neo4j as String
	 * @throws ServletException
	 * @throws IOException
	 */
	private String sendStatementNeo4j(String statementNeo4j ) throws ServletException, IOException{
		
		String urlstr = "http://intweb2.zocobox.com:7474/db/data/transaction/commit";
		String username = "neo4j";
		String password = "Z0c0b0x12";
		        
		String usernameAndPassword = username + ":" + password;
		String authorizationHeaderValue = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary( usernameAndPassword.getBytes() );
		
		URL url = new URL(urlstr);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		//((HttpURLConnection) connection).setHostnameVerifier(new MyHostnameVerifier()); //Only for SSL
		connection.setRequestProperty("Content-Type","application/json");
		connection.setRequestProperty("Accept","application/json");
		connection.setRequestProperty("charset", "UTF-8");
		connection.setRequestProperty("Authorization", authorizationHeaderValue);
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		
		byte[] outputBytes =  statementNeo4j.getBytes("UTF-8");
		OutputStream os = connection.getOutputStream();
		os.write(outputBytes);
		os.close();
		
		int returnCode = connection.getResponseCode();
		InputStream connectionIn = null;
		switch(returnCode){
		case 200:
		case 201:
			connectionIn = connection.getInputStream();
            break;
		default:
			connectionIn = connection.getErrorStream();
			break;
		}
        BufferedReader br = new BufferedReader(new InputStreamReader(connectionIn));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();
        
		return  sb.toString();
	}
	
	/**
	 * Extract the node list from the JSON response from Neo4j, type graph
	 * @param graphJSON
	 * @return List of nodes as JSON arrays
	 */
	public List<JSONArray> getNodesFromResponseTypeGraph(String graphJSON){
		
		JSONObject JSONresponse = new JSONObject(graphJSON);
		//JSONArray nodeArray = JSONresponse.getJSONArray("results").getJSONObject(0).getJSONArray("data").getJSONObject(2).getJSONObject("graph").getJSONArray("nodes");
		JSONArray nodeArray = JSONresponse.getJSONArray("results").getJSONObject(0).getJSONArray("data");
		List<JSONArray> nodeList = new ArrayList<JSONArray>();
		for(int i=0; i<nodeArray.length();i++){
			 nodeList.add(nodeArray.getJSONObject(i).getJSONObject("graph").getJSONArray("nodes")); 
		}
	 
		return nodeList;
	}
	
	/**
	 * Validates that the entity type (node label in Neo4j) is a valid one chcking against a 
	 * lookup array
	 * 
	 * @author JSR Ventures GmbH
	 * @param entityType, as a String to check
	 * @return if it is a valid entity or nor
	 */
	private boolean isValidEntitytype(String entityType){
		if(entityType.matches("Project|Task|Milestone")) return true;
		else return false;
		
	}
	
}
