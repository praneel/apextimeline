package controllers;

import java.io.File;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import canvas.SignedRequest;

import com.anand.salesforce.log.utils.SFDCLogParser;
import com.anand.salesforce.log.operations.DatabaseOperation;
import com.anand.salesforce.log.operations.Operation;

import dto.LogFileRequest;
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
			return ok(index.render(signedRequestJson));
		}else{
			return ok(index.render(session().get("signed_request")));
			
		}
	}

	public static Result sampleTimeline() {
		return ok(sample.render());
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
				List<Operation> oprList = parser.getFlattenedDataForUI(top);
				List<DatabaseOperation> dbOprList = parser.getDatabaseOperations(top);
				ObjectMapper mapper =new ObjectMapper();
				JsonNode json = Json.toJson(oprList);
				return ok(showTimeLine.render(json,
											  mapper.defaultPrettyPrintingWriter().writeValueAsString(top),
											  dbOprList)
						);
	    	}else{
				return ok(index.render(session().get("signed_request")));
	    	}
		}
	}

}
