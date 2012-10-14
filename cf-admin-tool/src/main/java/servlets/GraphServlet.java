package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.examples.AdminTool;
import ch.ethz.inf.vs.californium.util.Properties;

public class GraphServlet extends HttpServlet{
	
	private AdminTool main;
	String psURI;
	boolean hasPS;
	
	
	public GraphServlet(){
		main = AdminTool.getInstance();
		hasPS=false;
		if(Properties.std.containsKey("PS_ADDRESS")){
			String psHost = Properties.std.getStr("PS_ADDRESS");
			psURI = "coap://"+psHost+"/persistingservice/tasks";
			hasPS=true;
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	
		PrintWriter out = response.getWriter();
	     
		response.setContentType("text/html");
	    response.setHeader("Cache-control", "no-cache, no-store");
	    response.setHeader("Pragma", "no-cache");
	    response.setHeader("Expires", "-1");
	 
	    String id = "";
		String[] ids = request.getParameterValues("id");
				
		 
		if(ids!=null && ids.length == 1 ){
			id=ids[0];
		}
		if(!hasPS){
			out.write("Year,Value\n2012/10/10-13:04:36,22\n2012/10/10-13:09:01,23\n2012/10/10-13:11:53,23");
			out.flush();
			out.close();
			return;
		}
		GETRequest graphRequest = new GETRequest();
		if(!graphRequest.setURI(psURI+"/"+id+"/history/since")){
			out.write("No Persisting Service Available");
			return;
		}
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH,-1);
		
		graphRequest.setOption(new Option("date="+dateFormat.format(cal.getTime())+"&withdate=true",OptionNumberRegistry.URI_QUERY));
		graphRequest.enableResponseQueue(true);
		graphRequest.execute();
		Response graphResponse = null;
		try {
			graphResponse=graphRequest.receiveResponse();
		} catch (InterruptedException e) {
			
		}
		if(graphResponse!=null){
			if(graphResponse.getCode()==CodeRegistry.RESP_CONTENT){
				String dataset = "";
				BufferedReader bufReader = new BufferedReader(new StringReader(graphResponse.getPayloadString()));
				String line=null;
				while( (line=bufReader.readLine()) != null )
				{
					String date = line.substring(line.indexOf(";")+1);
					String value = line.substring(0, line.indexOf(";"));
					if(value.matches("-?\\d+(\\.\\d+)?")){ //remove non numeric entries
						dataset += "\n"+date+","+value+dataset;
					}
				}
				 dataset = "Date,"+id.substring(id.lastIndexOf("/")) +dataset;
				
				out.print(dataset);
			}
			if(graphResponse.getCode()==CodeRegistry.RESP_NOT_FOUND){
				out.print("Resource not in Persisting Service, no history data available");
			}
		}
		out.flush();
		out.close();
		
	}
}