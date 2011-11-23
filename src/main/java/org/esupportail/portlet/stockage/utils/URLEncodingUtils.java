package org.esupportail.portlet.stockage.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.commons.utils.Base64;

public class URLEncodingUtils {

	protected static final Log log = LogFactory.getLog(URLEncodingUtils.class);

	public static String encode(String path) {
		if(path == null)
			return null;
		String encodedPath = path;
		encodedPath = Base64.encodeBytes(path.getBytes(), Base64.URL_SAFE);
		return encodedPath;
	}
	
	public static String decodeDir(String dir) {
		if(dir == null)
			return null;
		dir = new String(Base64.decode(dir, Base64.URL_SAFE));
		return dir;
	}

	public static List<String> decodeDirs(List<String> dirs) {
		if(dirs == null)
			return null;
		List<String> decodedDirs = new Vector<String>(dirs.size());
		for(String dir: dirs)
			decodedDirs.add(decodeDir(dir));
		return decodedDirs;
	}
	
	public static List<String> encodeDirs(List<String> dirs) {
		if(dirs == null)
			return null;
		List<String> encodedDirs = new Vector<String>(dirs.size());
		for(String dir: dirs)
			encodedDirs.add(encode(dir));
		return encodedDirs;
	}
}
