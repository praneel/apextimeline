package controllers;

import java.io.File;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.anand.salesforce.log.utils.SFDCLogParser;
import com.anand.salesforce.log.operations.Operation;
import play.libs.Json;

import play.mvc.*;
import scala.util.parsing.json.JSON;

import views.html.*;

public class Application extends Controller {

	public static Result index() {
		return ok(index.render());
	}

	public static Result sampleTimeline() {
		return ok(sample.render());
	}

	@SuppressWarnings("deprecation")
	public static Result showTimeline() throws Exception {
		Http.MultipartFormData body = request().body().asMultipartFormData();
		Http.MultipartFormData.FilePart resourceFile = body.getFile("logFile");

		if( resourceFile != null )
		{
		    File file = resourceFile.getFile();
			SFDCLogParser parser = new SFDCLogParser();
			Operation top = parser.parseLogFile(file,100);
			List<Operation> oprList = parser.getFlattenedDataForUI(top);
			ObjectMapper mapper =new ObjectMapper();
			JsonNode json = Json.toJson(oprList);
			System.out.println();
			return ok(showTimeLine.render(json,
										  mapper.defaultPrettyPrintingWriter().writeValueAsString(json))
					);
		}else{
			return badRequest();
		}
	}

}
