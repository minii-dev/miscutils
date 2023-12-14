package com.roofstone.utils;

public class Version implements Comparable<Version> {

	protected String[] v;
	protected int[] vInt;

	protected String[] splitComponents(String version) {
		return version.split("\\.");
	}

	public Version(String version) {
		v = splitComponents(version);
		int sz=v.length;
		vInt=new int[sz];
		String s;
		for(int i=0;i<sz;i++) {
			s=v[i];
			if(s.length()>0) {
				char c=s.charAt(0);
				if (c>='1' && c<='9') {// if starts with 0, assume it is string (compare "1.1 and 1.09")
					try {
						vInt[i]=Integer.parseInt(s);
						continue;
					}catch(NumberFormatException ignore) {}
				}
			}
			vInt[i]=-1; // not number
		}
	}

	@Override
	public int compareTo(Version version2) {
		if(this==version2) return 0;
		String[] v2=version2.v;
		int[] v2Int=version2.vInt;
		final int sz=v.length;
		final int sz2=v2.length;
		final int szDiff=sz-sz2;
		int minSz=szDiff>0?sz2:sz;
		int res;
		for(int i=0;i<minSz;i++) {
			int vi1=vInt[i];
			int vi2=v2Int[i];
			if(vi1!=-1 && vi2!=-1) {
				res=vi1-vi2;
			}else {
				res=v[i].compareTo(v2[i]);
			}
			if(res!=0) return res;
		}
		return szDiff;
	}
	
	public static String max(String v1, String v2) {
		return compareTo(v1, v2)>=0?v1:v2;
	}
	
	public static String min(String v1, String v2) {
		if(v1==null) return v2;
		if(v2==null) return v1;
		return compareTo(v1, v2)<0?v1:v2;
	}

	public static int compareTo(String v1, String v2) {
		if(v1==null) {
			return v2==null?0:-1;
		}
		if(v2==null) return 1;
		return new Version(v1).compareTo(new Version(v2));
	}
}
