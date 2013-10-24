package org.esupportail.portlet.filemanager.beans;

import org.esupportail.portlet.filemanager.utils.SizeUtils;

public class Quota {

	private long usedBytes;
	private long maxBytes;
	
	public Quota(long usedBytes, long maxBytes) {
		this.usedBytes = usedBytes;
		this.maxBytes = maxBytes;
	}

	/**
	 * @return the usedBytes
	 */
	public long getUsedBytes() {
		return usedBytes;
	}


	/**
	 * @return the maxBytes
	 */
	public long getMaxBytes() {
		return maxBytes;
	}

	public float getUsage() {
		return (float)( usedBytes*100l / maxBytes );
	}
	
	public float getUsedSize() {
		return SizeUtils.getSize(usedBytes);
	}
	
	public float getMaxSize() {
		return SizeUtils.getSize(maxBytes);
	}
	
	public String getUsedUnit() {
		return SizeUtils.getUnit(usedBytes);
	}
	
	public String getMaxUnit() {
		return SizeUtils.getUnit(maxBytes);
			
	}
}
