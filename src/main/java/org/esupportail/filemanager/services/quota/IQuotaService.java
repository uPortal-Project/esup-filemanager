package org.esupportail.filemanager.services.quota;

import org.esupportail.filemanager.beans.Quota;

public interface IQuotaService {

	public Quota getQuota(String path
		);

	public boolean isSupportQuota(String path
		);
}
