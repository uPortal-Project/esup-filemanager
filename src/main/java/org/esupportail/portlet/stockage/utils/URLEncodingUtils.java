package org.esupportail.portlet.stockage.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class URLEncodingUtils {

	protected static final Log log = LogFactory.getLog(URLEncodingUtils.class);

	public static String encode(String path) {
		if(path == null)
			return null;
		String encodedPath = path;
		try {
			encodedPath = URLEncoder.encode(path, "utf-8");
		} catch (UnsupportedEncodingException e) {
			log.warn("problem encoding id ...", e);
		}
		return encodedPath;
	}
	
	public static String decodeDir(String dir) {
		if(dir == null)
			return null;
		try {
			dir = URLDecoder.decode(dir, "utf-8");
		} catch (UnsupportedEncodingException e) {
			log.error("error decoding this path : " + dir);
		}
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
}
