package controllers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import canvas.SignedRequest;

import com.anand.salesforce.log.utils.SFDCLogParser;
import com.anand.salesforce.log.operations.DatabaseOperation;
import com.anand.salesforce.log.operations.Operation;
import com.anand.salesforce.log.operations.TriggerExecutionOperation;

import dto.LogFileRequest;
import dto.LogStatistics;
import play.data.Form;
import play.libs.Json;
import play.libs.F.Function;
import play.libs.WS;
import play.mvc.*;


import views.html.*;

public class Application extends Controller {

	private static final String GET_LOGS_URL_BASE= "/services/data/v29.0/";
	private static final String GET_RAW_LOG_URL = "tooling/sobjects/ApexLog/";
	private static final String GET_LOGS_QUERY = "select Id,LogUser.Name,LogUserId,LogLength,Operation,Application,Status," +
												 "DurationMilliseconds,StartTime " +
												 "from ApexLog order by SystemModstamp desc limit 100";

	public static Result index() {
		if(	!request().getHeader("X-Forwarded-Proto").equalsIgnoreCase("https"))
		{
			return redirect("https://" + request().host() + request().uri());
		}
		if(request().method().equalsIgnoreCase("POST")){
			String[] signedRequest = request().body().asFormUrlEncoded().get("signed_request");
			String yourConsumerSecret=System.getenv("CANVAS_CONSUMER_SECRET");
			String signedRequestJson = SignedRequest.verifyAndDecodeAsJson(signedRequest[0], yourConsumerSecret);
			session().put("signed_request", signedRequestJson);
			return ok(	index.render(	signedRequestJson,
					 					new Boolean(false)
							)
					);
		}else{
			return ok(index.render(	session().get("signed_request"),
									new Boolean(false)
								)
					);
			
		}
	}
	

	public static Result sampleTimeline() {
		return ok(sample.render());
	}

	public static Result logs() {
		JsonNode json = request().body().asJson();
		String sessionId = json.findPath("sessionId").getTextValue();
		String instanceUrl = json.findPath("instanceUrl").getTextValue();
		return async(play.libs.WS.url(instanceUrl+GET_LOGS_URL_BASE+"query/")
									  .setQueryParameter("q",GET_LOGS_QUERY)
									  .setHeader("Authorization", "Bearer "+sessionId)
									  .setHeader("Accept","application/json")
									  .get()
									  .map( new Function<WS.Response, Result>() {
										          public Result apply(WS.Response response) {
										        	  return ok(response.asJson());
										          }
										    }
									   )
		);
	}

	
	public static Result userInfo() {
		JsonNode json = request().body().asJson();
		String sessionId = json.findPath("sessionId").getTextValue();
		String idUrl = json.findPath("idUrl").getTextValue();
		return async(play.libs.WS.url(idUrl)
									  .setQueryParameter("q",GET_LOGS_QUERY)
									  .setHeader("Authorization", "Bearer "+sessionId)
									  .setHeader("Accept","application/json")
									  .get()
									  .map( new Function<WS.Response, Result>() {
										          public Result apply(WS.Response response) {
										        	  return ok(response.asJson());
										          }
										    }
									   )
		);
	}

	public static Result showTimelinev2(String logId) {
		final Integer minRunTime=100;
		String tokenStr = request().cookie("token").value();
		JsonNode sessionToken = Json.parse(tokenStr);
		String sessionId = sessionToken.get("access_token").getTextValue();
		String instanceUrl = sessionToken.get("instance_url").getTextValue();
		return async(play.libs.WS.url(instanceUrl+GET_LOGS_URL_BASE+GET_RAW_LOG_URL+logId+"/Body/")
				  .setHeader("Authorization", "Bearer "+sessionId)
				  .get()
				  .map( new Function<WS.Response, Result>() {
					          public Result apply(WS.Response response) throws Exception {
								SFDCLogParser parser = new SFDCLogParser();
								Operation top = parser.parseLogFile(response.getBody(),minRunTime);
								if(top!=null){
									List<Operation> oprList = parser.getFlattenedDataForUI(top);
									LogStatistics logStats = new LogStatistics();
									parser.getDatabaseOperations(top,logStats);
									ObjectMapper mapper =new ObjectMapper();
									JsonNode json = Json.toJson(oprList);
									return ok(showTimeLine.render(json,
															  mapper.defaultPrettyPrintingWriter().writeValueAsString(top),
															  logStats,
															  session().get("signed_request"))
										);
								}else{
									return ok(index.render(null,new Boolean(true)));
									
								}
					          }
					    }
				   )
		);
	}
	public static Result oauthredirect() {
		return ok(oauthredirect.render());
	}
	

	@SuppressWarnings("deprecation")
	public static Result showTimeline() throws Exception {
		
		
		Form<LogFileRequest> filledForm = Form.form(LogFileRequest.class).bindFromRequest();
		if (filledForm.hasErrors()) {
	        return badRequest();
	    } else {
	    	final LogFileRequest resource = filledForm.get();
	    	Http.MultipartFormData body = request().body().asMultipartFormData();
	    	Http.MultipartFormData.FilePart  resourceFile = body.getFile("logFile");
	    	if(resourceFile!=null){
			    File file = resourceFile.getFile();
				SFDCLogParser parser = new SFDCLogParser();
				Operation top = parser.parseLogFile(file,resource.minRunTime);
				
				if(top!=null){
					List<Operation> oprList = parser.getFlattenedDataForUI(top);
					LogStatistics logStats = new LogStatistics();
					parser.getDatabaseOperations(top,logStats);
					ObjectMapper mapper =new ObjectMapper();
					JsonNode json = Json.toJson(oprList);
					if("application/json".equalsIgnoreCase(request().getHeader("Accept")) ||
						"text/json".equalsIgnoreCase(request().getHeader("Accept"))){
						return ok(Json.toJson(top));
					}else{
						return ok(showTimeLine.render(json,
												  mapper.defaultPrettyPrintingWriter().writeValueAsString(top),
												  logStats,
												  session().get("signed_request"))
							);
					}
				}else{
					if("application/json".equalsIgnoreCase(request().getHeader("Accept")) ||
							"text/json".equalsIgnoreCase(request().getHeader("Accept"))){
						Map<String,String> resp = new HashMap<String,String>();
						resp.put("msg","Could not parse log file. Check if log file is incomplete.");
						return internalServerError(Json.toJson(resp));
						
					}else{
						return ok(index.render(null,new Boolean(true)));
					}
				}	
	    	}else{	
	    		return ok(index.render(	session().get("signed_request"),
										new Boolean(false)
								)
						);
	    	}
		}
	}

}
