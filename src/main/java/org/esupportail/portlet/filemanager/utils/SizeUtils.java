package org.esupportail.portlet.filemanager.utils;

public class SizeUtils {
	
	public static final long BYTES_PER_KB = 1024;
	public static final long BYTES_PER_MB = 1024 * BYTES_PER_KB;
	public static final long BYTES_PER_GB = 1024 * BYTES_PER_MB;
	public static final String SIZE_FORMAT = "%.2f";
	
	public static final String UNIT_GB = "gb";
	public static final String UNIT_MB = "mb";
	public static final String UNIT_KB = "kb";
	public static final String UNIT_B = "b";
	
	public static String getUnit(long filesize) {
		if ( filesize > BYTES_PER_GB ) return UNIT_GB;
		if ( filesize > BYTES_PER_MB ) return UNIT_MB;
		if ( filesize > BYTES_PER_KB ) return UNIT_KB;
		return UNIT_B;
	}
	
	public static float getSize(long filesize) {
		String unit = getUnit(filesize);
		if ( UNIT_GB.equals(unit) ) 
			return (float)(filesize / BYTES_PER_GB);
		if ( UNIT_MB.equals(unit) )
			return (float)(filesize / BYTES_PER_MB);
		if ( UNIT_KB.equals(unit) )
			return (float)(filesize / BYTES_PER_KB);
		return (float)filesize;
	}

}
