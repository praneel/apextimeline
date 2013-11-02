package controllers;

import java.io.File;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import com.anand.salesforce.log.utils.SFDCLogParser;
import com.anand.salesforce.log.operations.Operation;
import play.libs.Json;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {

	public static Result index() {
		return ok(index.render());
	}

	public static Result showTimeline() throws Exception {
		Http.MultipartFormData body = request().body().asMultipartFormData();
		Http.MultipartFormData.FilePart resourceFile = body.getFile("logFile");

		if( resourceFile != null )
		{
		    File file = resourceFile.getFile();
			SFDCLogParser parser = new SFDCLogParser();
			Operation top = parser.parseLogFile(file,100);
			List<Operation> oprList = parser.getFlattenedDataForUI(top);
			JsonNode json = Json.toJson(oprList);
			return ok(showTimeLine.render(json));
		}else{
			return badRequest();
		}
	}

}
