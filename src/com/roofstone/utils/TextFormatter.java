package com.roofstone.utils;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class TextFormatter {

	private static final char PREFIX = '$';
	private static java.text.SimpleDateFormat f_date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static String f(String template, Object ... objects) {

		if(template!=null && objects!=null) {
			final int ocount=objects.length;
			final boolean[] objUsed=new boolean[ocount];
			final int l=template.length();
			if(ocount>0 && l>0) {
				Object o;
				StringBuilder sb=new StringBuilder(l+ocount*15+20);
				int p=0;
				int pp=0;
				int idx;
				char c;
				do {
					p=template.indexOf(PREFIX, p);
					if(p==-1) break;
					sb.append(template.substring(pp, p));
					p++;
					if(p==l) break;
					c=template.charAt(p++);
					if(c<'0' || c>'9') {
						sb.append(PREFIX).append(c);
					}else {
						idx=c-'0';
						if(p<l) {
							c=template.charAt(p);
							if(c>='0' && c<='9') {
								idx=idx*10+c-'0';
								p++;
								if(p<l)c=template.charAt(p);
							}
						}
						if(idx<ocount) {
							o=objects[idx];
							objUsed[idx]=true;
						}else o="{null}";

						if(p<l) {
							if(c=='{') {
								int e=template.indexOf('}', p);
								if(e>0) {
									if(o==null) {
										int defBegin=template.indexOf(',', p);
										if(defBegin>0 && defBegin<e) {
											o=template.substring(defBegin+1, e);
										}
									}

									p=e+1;
								}
							}
						}

						appendValue(o, sb);
					}
					pp=p;
				}while(p<l);
				if(pp<l) {
					sb.append(template.substring(pp));
				}
				boolean notFoundPrinted=false;
				for(int i=0;i<ocount;i++) {
					if(!objUsed[i]) {
						// add object passed but not found in template
						o=objects[i];
						if(!notFoundPrinted) {
							sb.append(" [");
						}else sb.append(", ");
						appendValue(o, sb);
						notFoundPrinted=true;
					}
				}
				if(notFoundPrinted) sb.append(']');
				return sb.toString();
			}
		}
		return template;
	}
	private static int MAX_PARAMETER_ELEMENT_COUNT=50;
	private static int MAX_STRING_LENGTH=100000;
	private static int MAX_PARAMETER_LENGTH=(int) (MAX_STRING_LENGTH*0.8);
	private static int MAX_PARAMETER_RECURSION_LEVEL=2; // at what level to stop digging and print class name only

	/**
	 * Appends parameter value (of any class). Should ignore errors and, possibly, limit too long values (like lists, maps and so on)  
	 * @param value
	 * @param sb
	 */
	public static void appendValue(Object value, StringBuilder sb) {
		try {
			appendValue(value, sb, sb.length(), 0);
		}catch(Exception ignore) {
			sb.append("<toString() error>");
		}
	}

	/**
	 * Appends parameter value (of any class). Should, possibly, limit too long values (like lists, maps and so on)  
	 * @param value
	 * @param sb
	 * @param start - starting length of sb (to limit length)
	 * @param level - level>0 when appendParameter is called recursively (for instance, processing map or array). Used to limit recursive level
	 */
	@SuppressWarnings("rawtypes")
	protected static void appendValue(Object value, StringBuilder sb, final int start, int level) {
		if(sb.length()-start>MAX_STRING_LENGTH) return;
		if(value instanceof CharSequence) {
			int sz=((CharSequence) value).length();
			if(sz>MAX_PARAMETER_LENGTH) {
				sb.append((CharSequence) value, 0, MAX_PARAMETER_LENGTH);
				sb.append("...size:").append(sz);
			}else sb.append(value);
			return;
		}
		if(value instanceof Number || value instanceof Character || value==null) {
			sb.append(value);
			return;
		}

		if(value instanceof Date) {
			synchronized(f_date) {
				sb.append(f_date.format(value));
			}
			return;
		}

		if(level>MAX_PARAMETER_RECURSION_LEVEL || (sb.length()-start)>MAX_PARAMETER_LENGTH) {
			sb.append('<').append(value.getClass().getName()).append('>');
			return;
		}
		level++;

		boolean isMap=value instanceof Map;
		if(isMap) {
			value=((Map<?, ?>)value).entrySet();
		}
		if(value instanceof Iterable) {
			Iterator<?> it = ((Iterable<?>) value).iterator();
			sb.append(isMap?'{':'[');
			for(int i=0;i<MAX_PARAMETER_ELEMENT_COUNT && it.hasNext();i++){
				if(i>0) sb.append(", ");
				Object val = it.next();
				appendValue(val, sb, start, val instanceof Map.Entry?level-1:level);
			}
			if(it.hasNext()) {
				sb.append(",...");
			}
			sb.append(isMap?'}':']');
			return;
		}
		if(value instanceof Map.Entry) {
			appendValue(((Map.Entry) value).getKey(), sb, start, level);
			sb.append('=');
			appendValue(((Map.Entry) value).getValue(), sb, start, level);
			return;
		}
		if(value.getClass().isArray()) {
			sb.append('[');
			int sz=Array.getLength(value);
			if(sz>MAX_PARAMETER_ELEMENT_COUNT) {
				sb.append("SIZE:").append(sz).append(' ');
				sz=MAX_PARAMETER_ELEMENT_COUNT;
			}
			for(int i=0;i<sz;i++) {
				if(i>0) sb.append(", ");
				Object val=Array.get(value, i);
				if(val instanceof Byte) {
					int vInt=(byte)val&0xff;
					val=Integer.toString(vInt, 16);
					if(vInt<16) {
						val="0"+val;
					}
				}
				appendValue(val, sb, start, level);
			}
			sb.append(']');
			return;
		}
		appendValue(value.toString(), sb, start, level); // append as String
	}
	
	public static String replace(String text, Map<String, String> map) {
		int p=0;
		int b;
		int e;
		StringBuilder sb=new StringBuilder(text.length()+64);
		while(true) {
			b=text.indexOf("$(", p);
			if(b<0) break;
			e=text.indexOf(")", b);
			if(e<0) break;
			
			sb.append(text, p, b);
			sb.append(map.get(text.substring(b+2, e)));
			p=e+1;
		}
		int len=text.length();
		sb.append(text, p, len);
		return sb.toString();
	}

}
