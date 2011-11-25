package org.esupportail.portlet.stockage.utils;

import org.esupportail.commons.utils.Base64;
import org.springframework.stereotype.Service;

@Service("pathEncodingUtils")
public class Base64PathEncodingUtils extends PathEncodingUtils {

	public String encodeDir(String path) {
		if(path == null)
			return null;
		String encodedPath = path;
		encodedPath = Base64.encodeBytes(path.getBytes(), Base64.URL_SAFE);
		encodedPath = encodedPath.replaceAll("\n", "");
		encodedPath = encodedPath.replaceAll("=", "");
		return PREFIX_CODE + encodedPath;
	}
	
	public String decodeDir(String dir) {
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
}
