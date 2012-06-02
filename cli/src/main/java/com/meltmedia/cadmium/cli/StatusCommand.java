package com.meltmedia.cadmium.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.meltmedia.cadmium.core.history.HistoryEntry;
import com.meltmedia.cadmium.status.Status;


@Parameters(commandDescription = "Displays status info for a site", separators="=")
public class StatusCommand {

	@Parameter(names="--site", description="The site for which the status is desired", required=true)
	private String site;	

	private final String JERSEY_ENDPOINT = "/system/status";

	public void execute() throws ClientProtocolException, IOException {

		DefaultHttpClient client = new DefaultHttpClient();
		String url = site + JERSEY_ENDPOINT;	
		
		HttpGet get = new HttpGet(url);
		HttpResponse response = client.execute(get);
		HttpEntity entity = response.getEntity();
		
		if(entity.getContentType().getValue().equals("application/json")) {			
		
            String responseContent = EntityUtils.toString(entity);            
            Status statusObj = new Gson().fromJson(responseContent, new TypeToken<Status>() {}.getType());
            
            //TODO: print out status info!!
		}		
			
	}

}
