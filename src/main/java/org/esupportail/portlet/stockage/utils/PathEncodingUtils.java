package org.esupportail.portlet.stockage.utils;

import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.commons.utils.Base64;

/**
 * This Class provides encode/decode methods for path string
 * so that we have a two-way converter between a path and a string/id
 * using only alphanumeric char, '_' char and '-' char.
 * This id can be used with jquery, like id of html tag, etc.
 * Moreover this id desn't need specific encoding characters for accents 
 * (because we don't use accents).
 */
public class PathEncodingUtils {

	protected static final Log log = LogFactory.getLog(PathEncodingUtils.class);

	protected static final String PREFIX_CODE = "path_";
	
	public static String encode(String path) {
		if(path == null)
			return null;
		String encodedPath = path;
		encodedPath = Base64.encodeBytes(path.getBytes(), Base64.URL_SAFE);
		encodedPath = encodedPath.replaceAll("=", "");
		return PREFIX_CODE + encodedPath;
	}
	
	public static String decodeDir(String dir) {
		if(dir == null || "".equals(dir))
			return null;
		dir = dir.substring(PREFIX_CODE.length());
		int nb_equals_to_add = 4 - dir.length() % 4;
		if(nb_equals_to_add == 1)
			dir = dir + "=";
		if(nb_equals_to_add == 2)
			dir = dir + "==";
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
