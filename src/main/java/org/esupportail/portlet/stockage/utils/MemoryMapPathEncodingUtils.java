package org.esupportail.portlet.stockage.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

//@Service("pathEncodingUtils")
public class MemoryMapPathEncodingUtils extends PathEncodingUtils {

	Map<String, String> idsPathes = new HashMap<String, String>();
	
	public String encodeDir(String path) {
		String encPath = PREFIX_CODE + path.hashCode();
		if(!idsPathes.containsKey(encPath))
			idsPathes.put(encPath, path);
		return encPath;
	}
	
	public String decodeDir(String encPath) {
		if(encPath == null || "".equals(encPath))
			return null;
		String path = idsPathes.get(encPath);
		Assert.notNull(path);
		return path;
	}
}
