package com.roofstone.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FileUtils {
	
	public static String readText(File file) {
		return convert(read(file));
	}
	
	public static byte[] read(File file) {
		try {
			try(FileInputStream is=new FileInputStream(file)){
				return read(is, (int) file.length(), file, (int) file.length());
			}
		} catch (IOException e) {
			throwIoException(e, file, true);
			return null;
		}
	}
	
	public static byte[] read(InputStream is, int knownSize, Object fileInfo, final int maxSize) {
		try {
			int readCounter=0;
			int currentCounter;
			int bufSize;
			byte[] buf;
			if(knownSize<=0) {
				bufSize=65536;
			}else {
				bufSize=knownSize;
			}
			if(maxSize>0 && bufSize>maxSize) {
				bufSize=maxSize;
			}
			buf=new byte[bufSize];
			currentCounter=buf.length;
			int offset=0;
			while(true) {
				currentCounter=is.read(buf, offset, currentCounter);
				if(currentCounter<=0) { // 0 possible if requested 0 bytes
					break;
				}
				readCounter+=currentCounter;
				
				if(maxSize>0 && readCounter>=maxSize) break;
				
				offset+=currentCounter;
				if(readCounter==buf.length) {
					byte[] buf1=new byte[(int) (readCounter*1.5)];
					System.arraycopy(buf, 0, buf1, 0, readCounter);
					currentCounter=buf1.length-buf.length;
					buf=buf1;
				}else {
					currentCounter=buf.length-readCounter;
				}
				if(maxSize>0) {
					currentCounter=Math.min(currentCounter, maxSize-readCounter);
				}
			}
			if(buf.length==readCounter) return buf;
			return Arrays.copyOf(buf, readCounter);
		} catch (IOException e) {
			throwIoException(e, fileInfo, true);
			return null;
		}
	}
	
	public static String convert(byte b[]) {
		Charset cs=StandardCharsets.UTF_8;
		if(cs==StandardCharsets.UTF_8) {
			int len=b.length;
			if(len>=2) {
				byte b1=b[0];
				byte b2=b[1];
				if(b1==-1 && b2==-2) {
					cs=StandardCharsets.UTF_16LE;
				}else if(b1==-2 && b2==-1) {
					cs=StandardCharsets.UTF_16BE;
				}
			}
		}
		String s=new String(b, cs);
		/*if(isRemoveBOM && s.length()>0 && s.charAt(0)==0xfffd) {
			s=s.substring(1);
		}*/
		return s;
	}
	
	@SuppressWarnings("rawtypes")
	public static String readResource(Class origin, String path) throws FileNotFoundException {
		InputStream is = origin.getResourceAsStream(path);
		if(is==null) {
			throw new FileNotFoundException(getResourcePath(origin, path));
		}
		String res=convert(read(is, -1, path, 0));
		try {
			is.close();
		} catch (IOException ignore) {
		}
		return res;
	}
	
	@SuppressWarnings("rawtypes")
	protected static String getResourcePath(Class class0, String path) {
		if(path.length()>0 && path.charAt(0)=='/') return path;
		return class0.getPackage().getName().replace('.', '/')+'/'+path;
	}
	
	protected static void throwIoException(IOException cause, Object path, boolean isRead) {
		throw new RuntimeException(TextFormatter.f("Error $1 \"$0\"", path, isRead?"reading":"writing"), cause);
	}
	
	public static void writeFile(File to, byte[] buf) {
		try {
			try(FileOutputStream os=new FileOutputStream(to)){
				os.write(buf, 0, buf.length);
			}
		} catch (IOException e) {
			throwIoException(e, to, false);
		}
	}
	
	public static void writeText(File f, String data) {
		writeFile(f, data.getBytes(StandardCharsets.UTF_8));
	}
}
