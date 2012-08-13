package com.meltmedia.cadmium.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class BasicFileServlet
  extends HttpServlet
{

  public static final String GET_METHOD = "GET";
  public static final String HEAD_METHOD = "HEAD";
  public static final String ETAG_HEADER = "ETag";
  public static final String IF_MATCH_HEADER = "If-Match";
  public static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";
  public static final String IF_UNMODIFIED_SINCE_HEADER = "If-Unmodified-Since";
  public static final String IF_NONE_MATCH_HEADER = "If-None-Match";
  public static final String LAST_MODIFIED_HEADER = "Last-Modified";
  public static final String RANGE_HEADER = "Range";
  public static final String IF_RANGE_HEADER = "If-Range";
  public static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";
  public static final String CONTENT_TYPE_HEADER = "Content-Type";
  public static final String TEXT_HTML_TYPE = "text/html";
  public static final String LOCATION_HEADER = "Location";
  public static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
  public static final String ACCEPT_RANGES_HEADER = "Accept-Ranges";
  public static final String CONTENT_RANGE_HEADER = "Content-Range";
  public static final String CONTENT_LENGTH_HEADER = "Content-Length";
  public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
  
  protected File contentDir;
  protected Long lastUpdated = System.currentTimeMillis();
  
  
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    
    setBasePath(config.getInitParameter("basePath"));
    
  }
  
  protected void setLastUpdated(long lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  protected void setBasePath(String basePath) throws ServletException {
    if(basePath == null) {
      throw new ServletException("Please set the base path in init paramater \"basePath\".");
    } else {
      File contentDir = new File(basePath);
      if(!contentDir.exists()) {
        throw new ServletException("The basePath \""+basePath+"\" does not exist on the file system.");
      } else if(!contentDir.isDirectory()) {
        throw new ServletException("The basePath \""+basePath+"\" exists and is not a directory.");
      } else if(!contentDir.canRead()) {
        throw new ServletException("The basePath\""+basePath+"\" cannot be read.");
      }
      this.contentDir = contentDir;
    }
  }
  
  protected String getBasePath() {
    return contentDir.toString();
  }
  
  
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    processRequest(new FileRequestContext(req, resp, true));
  }
  
  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    processRequest(new FileRequestContext(req, resp, false));
  }
  
  public void processRequest( FileRequestContext context )
    throws ServletException, IOException {
    
    // Find the file to serve in the file system.  This may redirect for welcome files or send 404 responses.
    if(locateFileToServe(context)) return;
    
    // Handle any conditional headers that may be present.
    if(handleConditions(context)) return;
    
    // Sets the content type header.
    resolveContentType(context);
    
    // Sets compress if Accept-Encoding allows for gzip or identity
    if(checkAccepts(context)) return;
    
    context.range = context.request.getHeader(RANGE_HEADER);
    context.inRangeETag = context.request.getHeader(IF_RANGE_HEADER);
    if( context.inRangeETag == null || !context.inRangeETag.matches("\\A(.*|W\\\\|\\\")") ) {
      context.inRangeETag = null;
      try {
        context.inRangeDate = context.request.getDateHeader(IF_RANGE_HEADER);
      }
      catch( IllegalArgumentException iae ) {
        // ignore per spec.
      }
    }
    
    if( context.range != null ) {
      if( !( context.inRangeETag != null && validateStrong(context.inRangeETag, context.eTag ) ||
             context.inRangeDate != -1 && context.inRangeDate >= lastUpdated ) ) {
        context.range = null;
        context.inRangeETag = null;
        context.inRangeDate = -1;
      }
    }
    
    if( context.sendEntity ) {
      try {
        context.response.setStatus(HttpServletResponse.SC_OK);
        context.response.setContentType(context.contentType);
        context.in = new FileInputStream(context.file);
        context.out = context.response.getOutputStream();
        if( context.compress ) context.out = new GZIPOutputStream(context.out);
        IOUtils.copy(context.in, context.out);
      }
      finally {
        IOUtils.closeQuietly(context.in);
        IOUtils.closeQuietly(context.out);
      } 
    }
  }
  
  /**
   * Checks the accepts headers and makes sure that we can fulfill the request.
   * @param context 
   * @return
   * @throws IOException 
   */
  private boolean checkAccepts(FileRequestContext context) throws IOException {
    if (!canAccept(context.request.getHeader("Accept"), context.contentType)) {
      notAcceptable(context);
      return true;
    }
    
    if (!canAccept(context.request.getHeader("Accept-Encoding"), "gzip", "identity")) {
      notAcceptable(context);
      return true;
    }
    
    if (canAccept(context.request.getHeader("Accept-Encoding"), "gzip")) {
      context.compress = true;
    }
    return false;
  }

  /**
   * Locates the file to serve.  Returns true if locating the file caused the request to be handled.
   * @param context
   * @return
   * @throws IOException
   */
  public boolean locateFileToServe( FileRequestContext context ) throws IOException {
    context.file = new File( contentDir, context.path);
    
    // if the path is not on the file system, send a 404.
    if( !context.file.exists() ) {
      context.response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return true;    
    }
    
    // redirect welcome files if needed.
    if( handleWelcomeRedirect(context) ) return true;
    
    // if the requested file is a directory, try to find the welcome file.
    if( context.file.isDirectory() ) {
      context.file = new File(context.file, "index.html");
    }
    
    // if the file does not exist, then terminate with a 404.
    if( !context.file.exists() ) {
      context.response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return true;
    }
    
    return false;
  }
  
  public boolean handleConditions( FileRequestContext context ) throws IOException {
    // check the conditions
    context.eTag = context.path+"_"+lastUpdated;
    
    context.ifMatch = context.request.getHeader(IF_MATCH_HEADER);
    if( context.ifMatch != null && validateStrong(context.ifMatch, context.eTag) ) {
      context.response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
      return true;
    }
    
    context.ifNoneMatch = context.request.getHeader(IF_NONE_MATCH_HEADER);
    if( context.ifNoneMatch != null && !validateStrong(context.ifNoneMatch, context.eTag)) {
      context.response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      context.response.setHeader(ETAG_HEADER, context.eTag);
      context.response.setDateHeader(LAST_MODIFIED_HEADER, lastUpdated);
      return true;
    }
    
    context.ifModifiedSince = context.request.getDateHeader(IF_MODIFIED_SINCE_HEADER);
    if( context.ifModifiedSince != -1 && context.ifModifiedSince >= lastUpdated ) {
      context.response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      context.response.setHeader(ETAG_HEADER, context.eTag);
      context.response.setDateHeader(LAST_MODIFIED_HEADER, lastUpdated);
      return true;
    }
    
    context.ifUnmodifiedSince = context.request.getDateHeader(IF_UNMODIFIED_SINCE_HEADER);
    if( context.ifUnmodifiedSince != -1 && context.ifUnmodifiedSince < lastUpdated ) {
      context.response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
      return true;
    }
    
    return false;

  }
  
  public static Pattern etagPattern = null;
  public static Pattern unescapePattern = null;
  static {
    try {
      etagPattern = Pattern.compile( "(W/)?\"((?:[^\"\\\\]*|\\\\.)*)\"\\s*(?:,\\s*)?.*");
      unescapePattern = Pattern.compile("\\\\(.)");
    }
    catch( PatternSyntaxException pse ) {
      pse.printStackTrace();
    }
  }
  
  /**
   * Parses an Accept header value and checks to see if the type is acceptable.
   * @param headerValue The value of the header.
   * @param type The token that we need in order to be acceptable.
   * @return
   */
  public static boolean canAccept(String headerValue, String... type) {
    if(headerValue == null || type == null) {
      return true;
    } else {
      String availableTypes[] = headerValue.split(",");
      for(String availableType : availableTypes) {
        String typeParams[] = availableType.split(";");
        double qValue = 1.0d;
        if(typeParams.length > 0) {
          for(int i=1; i<typeParams.length; i++) {
            if(typeParams[i].trim().startsWith("q=")) {
              String qString = typeParams[i].substring(2).trim();
              if(qString.matches("\\\\A\\\\d+(\\.\\\\d*){0,1}\\\\Z")){
                qValue = Double.parseDouble(qString);
                break;
              } 
            }
          }
        }
        boolean matches = false;
        if(typeParams[0].equals("*") || typeParams[0].equals("*/*")) {
          matches = true;
        } else {
          matches = hasMatch(typeParams, type);
        }
        if(qValue != 0 && matches) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check to see if a Accept header accept part matches any of the given types.
   * @param typeParams
   * @param type
   * @return
   */
  private static boolean hasMatch(String[] typeParams, String... type) {
    boolean matches = false;
    for(String t : type) {
      for(String typeParam : typeParams) {
        if(typeParam.contains("/")) {
          String typePart = typeParam.replace("*", "");
          if(t.startsWith(typePart) || t.endsWith(typePart)) {
            matches = true;
            break;
          }
        } else if(t.equals(typeParam)) {
          matches = true;
          break;
        }
      }
      if(matches) {
        break;
      }
    }
    return matches;
  }
  
  /**
   * Sends an error on the response for status code 406 NOT ACCEPTABLE.
   * @param context
   * @throws IOException
   */
  public static void notAcceptable(FileRequestContext context) throws IOException {
    context.response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
  }
  
  public static List<String> parseETagList( String value ) {
    List<String> etags = new ArrayList<String>();
    value = value.trim();
    if( "*".equals(value) ) {
      etags.add(value);
    }
    else {
      Matcher etagMatcher = etagPattern.matcher(value);
      while( etagMatcher.lookingAt() ) {
        etags.add(unescapePattern.matcher(etagMatcher.group(2)).replaceAll("$1"));
        etagMatcher.region(etagMatcher.start()+etagMatcher.group().length(), etagMatcher.end());
      }
    }
    return etags;
  }
  
  public static boolean validateStrong( String condition, String eTag ) {
    List<String> parsed = parseETagList(condition);
    if( parsed.size() == 1 && "*".equals(parsed.get(0)) ) return true;
    else return parsed.contains(eTag);
  }
  
  /**
   * Forces requests for index files to not use the file name.
   * 
   * @param context
   * @return true if the request was handled, false otherwise.
   * @throws IOException 
   */
  public boolean handleWelcomeRedirect( FileRequestContext context ) throws IOException {
    if( context.file.isFile() && context.file.getName().equals("index.html")) {
      resolveContentType(context);
      String location = context.path.replaceFirst("/index.html\\Z", "");
      if( location.isEmpty() ) location = "/";
      if( context.request.getQueryString() != null ) location = location + "?" + context.request.getQueryString();
      sendPermanentRedirect(context, location);
      return true;
    }
    return false;
  }

  /**
   * Looks up the mime type based on file extension and if found sets it on the FileRequestContext.
   * 
   * @param context
   */
  public void resolveContentType(FileRequestContext context) {
    String contentType = lookupMimeType(context.path);
    if(contentType != null) {
      context.contentType = contentType;
      if(contentType.equals("text/html")) {
        context.contentType += ";charset=utf-8";
      }
    }
  }
  
  public static void sendPermanentRedirect( FileRequestContext context, String location ) throws IOException {
    context.response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    context.response.setContentType(context.contentType);
    context.response.setHeader(LOCATION_HEADER, location);
    context.response.getOutputStream().close();
  }
  
  public String lookupMimeType( String path ) {
    return getServletContext().getMimeType(path);
  }
  
  public static class FileRequestContext
  {
    public String range;
    public long inRangeDate;
    public String inRangeETag;
    public long ifUnmodifiedSince;
    public long ifModifiedSince;
    public String ifNoneMatch;
    public String eTag;
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    File file = null;
    InputStream in = null;
    OutputStream out = null;
    boolean compress = false;
    String path = null;
    private boolean sendEntity = true;
    String contentType = DEFAULT_CONTENT_TYPE;
    public String ifMatch;
    
    public FileRequestContext( HttpServletRequest request, HttpServletResponse response, boolean sendEntity ) {
      this.request = request;
      this.response = response;
      this.sendEntity = sendEntity;
      this.path = request.getPathInfo();
    }
  }
}
