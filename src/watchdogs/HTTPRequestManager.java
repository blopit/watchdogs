package watchdogs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HTTPRequestManager extends Object {
	private String APISubscriptionKey = "c2281afa671f40c1a58dd1a39aada04a";
	private String contentType = "application/octet-stream";
	private String detectFaceUrl = "https://api.projectoxford.ai/face/v1.0/detect?returnFaceId=true&returnFaceLandmarks=false";

	
	
	public void DetectFace(String filename) {
		HttpClient httpclient = HttpClients.createDefault();

		try {
		    URIBuilder builder = new URIBuilder("https://api.projectoxford.ai/face/v1.0/detect?returnFaceId=true&returnFaceLandmarks=false");

		    URI uri = builder.build();
		    HttpPost request = new HttpPost(uri);
		    request.setHeader("Content-Type", "application/octet-stream");
		    request.setHeader("Ocp-Apim-Subscription-Key", APISubscriptionKey);
		    
		    File file = new File(filename); 
		    FileEntity reqEntity = new FileEntity(file, ContentType.APPLICATION_OCTET_STREAM);
		    request.setEntity(reqEntity);

		    HttpResponse response = (HttpResponse) httpclient.execute(request);
		    HttpEntity entity = ((org.apache.http.HttpResponse) response).getEntity();

		    if (entity != null) {
		            System.out.println(EntityUtils.toString(entity));
		            //return JsonParser.parse(EntityUtils.toString(entity));
		    }
		} catch (URISyntaxException | IOException | ParseException e) {
		        System.out.println(e.getMessage());
		}

	}
}
