package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

@RestController
public class GoogleAPIController {

	private static final String APPLICATION_NAME = "GmailAlexa";
	private static HttpTransport httpTransport;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static com.google.api.services.gmail.Gmail client;

	GoogleClientSecrets clientSecrets;
	GoogleAuthorizationCodeFlow flow;
	Credential credential;
	TokenResponse tokenResponse;

	private String clientId = "503934729045-ui7u57op13q6no7ehh4n5hd8j1q79omn.apps.googleusercontent.com";

	private String clientSecret = "lzCts0oJusH1lfCE3Ogg6_NL";

	private String redirectUri = "http://localhost:8080/login/gmailCallback";

	private String refreshToken = "";
	
	@RequestMapping(value = "/login/gmail", method = RequestMethod.GET)
	public RedirectView googleConnectionStatus(HttpServletRequest request) throws Exception {
		return new RedirectView(authorize());
	}

	@RequestMapping(value = "/login/gmailCallback", method = RequestMethod.GET, params = "code")
	public String oauth2Callback2(@RequestParam(value = "code") String code) throws IOException {
		this.googleAuth(code);
		this.refreshToken = tokenResponse.getRefreshToken();
		return tokenResponse.getAccessToken() + " $$ " + tokenResponse.getRefreshToken();
	}

	@GetMapping(value = "/google/query")
	public String googleMailQuery() {

		JSONObject json = new JSONObject();
		JSONArray arr = new JSONArray();

		try {
			String userId = "me";
			String query = "subject:'gmap'";
			ListMessagesResponse MsgResponse = client.users().messages().list(userId).setQ(query).execute();

			List<Message> messages = new ArrayList<>();

			System.out.println("message length:" + MsgResponse.getMessages().size());

			for (Message msg : MsgResponse.getMessages()) {

				messages.add(msg);

				Message message = client.users().messages().get(userId, msg.getId()).execute();
				System.out.println("snippet :" + message.getSnippet());

				arr.put(message.getSnippet());

				/*
				 * if (MsgResponse.getNextPageToken() != null) { String pageToken =
				 * MsgResponse.getNextPageToken(); MsgResponse =
				 * client.users().messages().list(userId).setQ(query).
				 * setPageToken(pageToken).execute(); } else { break; }
				 */
			}
			json.put("response", arr);

			for (Message msg : messages) {

				System.out.println("msg: " + msg.toPrettyString());
			}

		} catch (Exception e) {

			System.out.println("exception cached ");
			e.printStackTrace();
		}

		return json.toString();

	}

	@RequestMapping(value = "/login/gmailRefresh", method = RequestMethod.GET)
	@ResponseBody
	public String oauth2Refresh() throws IOException {

		/*TokenResponse response = new GoogleRefreshTokenRequest(httpTransport, JSON_FACTORY,
				refreshToken, clientId, clientSecret).setGrantType("refresh_token").execute();
		*/

		boolean response = credential.refreshToken();
		
		if (response) {
			return "Success ::: " + credential.getRefreshToken();
			
		} else {
			return "Failure";	
		}
		
	}

	private String authorize() throws Exception {
		final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
		ClassPathResource resource = new ClassPathResource("client_secret.json");
		InputStream in = resource.getInputStream();
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
		Collection<String> scopes = Collections.singleton(GmailScopes.GMAIL_READONLY);

		File file = new File("googleauth");
		DataStoreFactory dataStore = new FileDataStoreFactory(file);

		httpTransport = GoogleNetHttpTransport.newTrustedTransport();

		flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, scopes)
				.setAccessType("offline").setDataStoreFactory(dataStore).setApprovalPrompt("auto").build();
		AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri);

		System.out.println("gmail authorizationUrl ->" + authorizationUrl);
		return authorizationUrl.build();

	}

	public void googleAuth(String code) throws IOException {

		tokenResponse = flow.newTokenRequest(code).setGrantType("authorization_code")
				.setRedirectUri(redirectUri)
				.execute();

		System.out.println(tokenResponse.toString());
		System.out.println(tokenResponse.getAccessToken());
		System.out.println(tokenResponse.getRefreshToken());

		ClassPathResource resource = new ClassPathResource("client_secret.json");
		InputStream in = resource.getInputStream();
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
		
	    // Create the OAuth2 credential.
	    credential = new GoogleCredential.Builder()
	        .setTransport(httpTransport)
	        .setJsonFactory(JSON_FACTORY)
	        .setClientSecrets(clientSecrets) 
	        .build();

	    // Set authorized credentials.
	    credential.setFromTokenResponse(tokenResponse);
		
		client = new com.google.api.services.gmail.Gmail.Builder(httpTransport, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME).build();

		System.out.println("Refresh Token : " + credential.getRefreshToken());
	}

}
