package watchdogs;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

public class HTTPRequestManager extends Object {
	private String APISubscriptionKey = "c2281afa671f40c1a58dd1a39aada04a";
	private String contentType = "application/octet-stream";
	private String detectFaceUrl = "https://api.projectoxford.ai/face/v1.0/detect?returnFaceId=true&returnFaceLandmarks=false";

	public HTTPRequestManager(String APISubscriptionKey, String contentType) {
		this.APISubscriptionKey = APISubscriptionKey;
		this.contentType = contentType;
	}

	public void DetectFace(String imagebody) {
		Future<HttpResponse<JsonNode>> detectfacePostRequest = Unirest.post(detectFaceUrl)
				.header("Content-Type", contentType).header("Ocp-Apim-Subscription-Key", APISubscriptionKey)
				.body(imagebody).asJsonAsync(new Callback<JsonNode>() {
					public void failed(UnirestException e) {
						System.out.println("The request has failed");
					}

					public void completed(HttpResponse<JsonNode> response) {
						int code = response.getStatus();
						System.out.println(code);
						Map<String, List<String>> headers = response.getHeaders();
						JsonNode body = response.getBody();
						InputStream rawBody = response.getRawBody();
					}

					public void cancelled() {
						System.out.println("The request has been cancelled");
					}

				});

	}
}
