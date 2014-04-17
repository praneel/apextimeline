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
import play.api.mvc.MultipartFormData;
import play.data.Form;
import play.libs.Json;

import play.mvc.*;
import scala.util.parsing.json.JSON;
import sun.rmi.log.ReliableLog.LogFile;

import views.html.*;

public class Application extends Controller {

	public static Result index() {
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

	public static Result showTimelinev2() {
		return ok(showTimeLinev2.render(null,null,null,null));
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
	    	LogFileRequest resource = filledForm.get();
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
