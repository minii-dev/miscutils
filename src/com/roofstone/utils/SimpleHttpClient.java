package com.roofstone.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class SimpleHttpClient  {

	private HttpURLConnection httpClient;
	private int responseCode;
	private String result;

	private String url;
	private String httpMethod = "POST";	
	private boolean isWantRedirectedUrl;



	private int connectTimeoutMillis=-1;
	private int readTimeoutMillis=-1;
	private ILog log;

	public SimpleHttpClient(String url, ILog log) {
		this(url, false, log);
	}

	public SimpleHttpClient(String url, boolean isAutoAddUrlTrailingSlash, ILog log) {
		if(url!=null) {
			if(isAutoAddUrlTrailingSlash) {
				url=ensureSlash(url);
			}
		}
		this.url = url;
		this.log = log;
	}

	public static String ensureSlash(String s) {
		if(!s.endsWith("/")) {
			return s+'/';
		}
		return s;
	}

	public void setWantRedirectedUrl(boolean isWantRedirectedUrl) {
		this.isWantRedirectedUrl = isWantRedirectedUrl;
		if(httpClient!=null) {
			httpClient.setInstanceFollowRedirects(isWantRedirectedUrl);
		}
	}

	protected boolean isDebug() {
		return log.isDebug();
	}

	protected void copyTo(SimpleHttpClient to) {
		to.url=url;
		to.httpMethod=httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	protected void onInitRequest(HttpData data) {
		if(data!=null) {
			String loginData = data.getLoginData();
			if(loginData!=null) {
				setRequestProperty("Authorization", loginData);
			}
		}
	}


	public void setRequestProperty(String name, String value) {
		httpClient.setRequestProperty(name, value);
	}

	public String sendPost(String data) {
		return sendPost(data==null?null:new HttpData(data));
	}

	protected String tryAuthenticate(HttpData data) {
		return null;
	}

	public String read() {
		return sendPost((HttpData)null);
	}

	public String sendPost(HttpData data) {
		String res = sendPostSingle(data);
		if(responseCode==401) {
			String res1=tryAuthenticate(data);
			if(res1!=null) {
				res=res1;
			}
		}
		return res;
	}

	protected String doAuth(HttpData data) {
		return sendPostSingle(data);
	}

	protected void afterAuth(HttpData data, String result) {
	}

	public void setConnectTimeoutMillis(int connectTimeoutMillis) {
		this.connectTimeoutMillis = connectTimeoutMillis;
	}
	
	public void setReadTimeoutMillis(int readTimeoutMillis) {
		this.readTimeoutMillis = readTimeoutMillis;
	}


	public void open() {
		responseCode=0;
		try {
			httpClient = (HttpURLConnection) new URL(url).openConnection();
			if(connectTimeoutMillis>=0) {
				httpClient.setConnectTimeout(connectTimeoutMillis);
			}
			if(readTimeoutMillis>=0) {
				httpClient.setReadTimeout(readTimeoutMillis);
			}
			if(isWantRedirectedUrl) {
				httpClient.setInstanceFollowRedirects(false);
			}
		} catch (Exception e) {
			throw new HttpException(TextFormatter.f("Cannot connect to $0", url), e);
		}
	}

	protected String sendPostSingle(HttpData data) {
		open();// must reinit because different read/write mode can be already set

		if(data!=null) {
			try {
				final String stringData=data.string;
				final int outDataLength=stringData!=null?stringData.length():0;
				String cType=data.contentType;
				if(cType==null) {
					if(outDataLength>3) {
						char c=stringData.charAt(0);
						if(c=='[' || c=='{') {
							cType="application/json";
						}else if(stringData.startsWith("<?xml")) {
							cType="application/xml";
						}else if(stringData.startsWith("<html")) {
							cType="text/html";
						}else {
							cType="text/plain";
						}
					}
				}
				String charSetName=data.httpCharset;
				httpClient.setDoOutput(true);
				httpClient.setRequestMethod(httpMethod);
				if(charSetName!=null) {
					String charSetString="charset="+charSetName;
					if(cType!=null) {
						cType=cType+';'+charSetString;
					}else cType=charSetString;
				}
				httpClient.setRequestProperty("Accept-Charset", "UTF-8"); // because read in UTF8 anyway
				{
					if(cType!=null) {
						httpClient.addRequestProperty("Content-Type", cType);
						if(stringData!=null || "POST".equals(httpMethod)) {
							httpClient.addRequestProperty("Content-Length", Integer.toString(outDataLength));
						}
					}
				}

				onInitRequest(data);

				if(stringData!=null) {
					try(OutputStream os = httpClient.getOutputStream()){
						try(BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(os, Charset.forName(charSetName)))){

							if(stringData!=null) {
								wr.write(stringData);
								wr.write("\r\n");
							}
							wr.flush();
						}
					}
				}

			}catch(Exception e) {
				throw new HttpException(TextFormatter.f("Cannot post to $0", url), e);
			}
		}else {
			onInitRequest(data);
		}
		try {
			responseCode = httpClient.getResponseCode();
			result=data!=null && data.isSkipReading?null:readString();
			if(isDebug()) {
				log.debug(new Txt(351, "http-rc: ($0) $1", responseCode, result));
				if(log.is(ILog.TRACE)) {
					Map<String, List<String>> hf = httpClient.getHeaderFields();
					if(hf!=null) {
						for(Entry<String, List<String>> hfe: hf.entrySet()) {
							List<String> lst=hfe.getValue();
							if(lst!=null) {
								int size = lst.size();
								if (size>0) {
									String val=lst.get(0);
									for(int i=1;i<size;i++) {
										val=val+", "+lst.get(i);
									}
									log.log(ILog.TRACE, new Txt(352, "http-hdr: $0 = $1", hfe.getKey(), ':', val));
								}
							}
						}
					}
				}
			}
			return result;
		}catch(Exception e) {
			throw new HttpException(TextFormatter.f("Cannot read data from $0", url), e);
		}
	}

	public void proceedRedirects() throws IOException {
		int redirectMaxCount=5;
		while(true) {
			int rc = httpClient.getResponseCode();
			if(rc==HttpURLConnection.HTTP_MOVED_PERM || rc==HttpURLConnection.HTTP_MOVED_TEMP || rc==HttpURLConnection.HTTP_SEE_OTHER) {
				String location = httpClient.getHeaderField("location");
				if(location!=null) {
					if(location.indexOf("://")<0) {
						location=httpClient.getURL().toString()+location;
					}
					httpClient.disconnect();
					httpClient = (HttpURLConnection) new URL(location).openConnection();
					if(connectTimeoutMillis>=0) {
						httpClient.setConnectTimeout(connectTimeoutMillis);
					}
					httpClient.setInstanceFollowRedirects(false);
				}
				if(redirectMaxCount--<0) {
					throw new IOException("Too many redirects");
				}
			}else {
				break;
			}
		}
	}

	public URL getRealUrl() {
		return httpClient.getURL();
	}

	public String beginSimpleDownload() throws IOException {
		if(httpClient==null) {
			setWantRedirectedUrl(true);
			open();
			HttpData hd=new HttpData(null);
			hd.setSkipReading(true);
			sendPost(hd);
			proceedRedirects();
		}
		String fileName=getFilename();
		if(fileName==null) {
			fileName=getLastUrlPart();
		}
		return fileName;
	}

	public String getLastUrlPart() {
		String url=httpClient==null?this.url:httpClient.getURL().toString();
		int e=url.indexOf('?');
		if(e<0) e=url.length();
		int b=url.lastIndexOf('/', e)+1;
		String name=url.substring(b, e);
		return name;
	}

	public String getHeader(String name) {
		List<String> list = getHeaderValues(name);
		return list==null || list.size()==0?null:list.get(list.size()-1);
	}

	public List<String> getHeaderValues(String name) {
		if(name!=null) {
			Map<String, List<String>> map = httpClient.getHeaderFields();
			if(map!=null) {
				List<String> val=map.get(name);
				if(val!=null) return val;
				for(Entry<String, List<String>> e: map.entrySet()) {
					if(name.equalsIgnoreCase(e.getKey())) return e.getValue();
				}
			}
		}
		return null;
	}

	private static final Pattern contentDispositionFilename=Pattern.compile("filename(\\*)?=(?:(['\"])(.*)\\2|([^\\s;]*))");
	private static final Pattern contentDispositionModDate=Pattern.compile("modification-date=(['\"])(.*)\\1");
	private static final SimpleDateFormat dateFormatRFC822 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	public static String getRFC822DateTime(long timestamp) {
		synchronized (dateFormatRFC822) {	
			return dateFormatRFC822.format(timestamp);
		}
	}

	public static long parseRFC822DateTime(String date) {
		synchronized (dateFormatRFC822) {	
			try {
				Date d=dateFormatRFC822.parse(date);
				return d.getTime();
			} catch (ParseException ignore) {
			}	
		}
		return 0;
	}

	public String getFilename() {
		String s=httpClient.getHeaderField("Content-Disposition");
		if(s!=null) {
			Matcher m = contentDispositionFilename.matcher(s);
			if(m.find()) {
				String nameString=m.group(3);
				if(nameString==null) nameString=m.group(4);
				if("*".equals(m.group(1))){
					int p=nameString.indexOf("''");
					Charset cs=null;
					if(p>=0) {
						String charsetName=nameString.substring(0, p);
						nameString=nameString.substring(p+2);
						try {
							cs=Charset.forName(charsetName);
						}catch (Exception ignore) {
							ignore.printStackTrace();
						}
					}
					nameString=URLDecoder.decode(nameString, cs==null?StandardCharsets.UTF_8:cs);
				}
				return nameString;
			}
		}
		return null;
	}

	public long getFileModificationDate() {
		String s=httpClient.getHeaderField("Content-Disposition");
		if(s!=null) {
			Matcher m=contentDispositionModDate.matcher(s);
			if(m.find()) {
				String date=m.group(2);
				return parseRFC822DateTime(date);
			}
		}
		return 0;
	}

	public Map<String, List<String>> getAllHeaders() {
		return httpClient.getHeaderFields();
	}

	public byte[] readBytes() throws IOException{
		return m_readBytes();
	}

	private byte[] m_readBytes() throws IOException{
		if(httpClient==null) {
			open();
		}
		InputStream is = httpClient.getErrorStream();
		if(is==null) is=httpClient.getInputStream();
		String enc=getHeader("Content-Encoding");
		if("gzip".equalsIgnoreCase(enc)) {
			is=new GZIPInputStream(is);
		}
		try {
			return FileUtils.read(is, 16384, null, 0);
		}finally {
			is.close();
		}
	}

	public long getContentSize() {
		String len_s;
		len_s=httpClient.getHeaderField("Content-Range");
		if(len_s!=null) {
			int p=len_s.indexOf('/');
			if(p>=0) {
				len_s=len_s.substring(p+1);
				try {
					return Long.parseLong(len_s);
				}catch(Exception ignore) {
				}
			}
		}
		len_s=httpClient.getHeaderField("Content-Length");
		if(len_s!=null) {
			try {
				return Long.parseLong(len_s);
			}catch(Exception ignore) {
			}
		}
		return 0L;
	}

	protected String convert(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private String readString() throws IOException {
		return convert(m_readBytes());
	}

	public int rc() {
		return responseCode;
	}

	public void throwOnRc() {
		HttpException rcException = getRcException();
		if(rcException!=null) throw rcException;
	}

	public HttpException getRcException() {
		if(responseCode<200 || responseCode>299) {
			if(responseCode==401) {
				return new HttpException("Authentication error");
			}
			return new HttpException(TextFormatter.f("HTTP error $0 data from $1: $2", responseCode, url, result));
		}
		return null;
	}

	public static Map<String, String> parseParametersSingle(String urlParameters){
		Map<String, String> parameters=new LinkedHashMap<>();
		String[] keyValueArray = urlParameters.split("&");
		for (String keyValue : keyValueArray) {
			String[] a = keyValue.split("=", 2);
			String key = a[0];
			String value = a.length > 1 ? a[1] : "";
			key = URLDecoder.decode(key, StandardCharsets.UTF_8);
			value = URLDecoder.decode(value, StandardCharsets.UTF_8);
			parameters.put(key, value);
		}
		return parameters;
	}

}
