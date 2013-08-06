package com.pawelniewiadomski.devs.jira.webwork;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.task.TaskManager;
import com.atlassian.jira.util.AttachmentUtils;
import com.atlassian.jira.web.util.AttachmentException;
import com.atlassian.jira.web.util.WebAttachmentManager;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.atlassian.upm.license.storage.lib.PluginLicenseStoragePluginUnresolvedException;
import com.atlassian.upm.license.storage.lib.ThirdPartyPluginLicenseStorageManager;
import com.google.common.collect.LinkedListMultimap;
import com.pawelniewiadomski.devs.jira.csv.CsvData;
import com.pawelniewiadomski.devs.jira.csv.CsvReader;
import com.pawelniewiadomski.devs.jira.engine.EngineManager;
import com.pawelniewiadomski.devs.jira.servlet.LicenseServlet;
import org.apache.commons.lang.StringUtils;
import webwork.action.ServletActionContext;
import webwork.config.Configuration;
import webwork.multipart.MultiPartRequestWrapper;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

public class BulkCreateIssues extends Common {
	private final WebAttachmentManager webAttachmentManager;
    private ThirdPartyPluginLicenseStorageManager licenseManager;

    private String encoding = "UTF-8";
	private Long pid;

	public BulkCreateIssues(WebAttachmentManager webAttachmentManager,
                            ThirdPartyPluginLicenseStorageManager licenseManager) {
		this.webAttachmentManager = webAttachmentManager;
        this.licenseManager = licenseManager;
    }

    public boolean isValidLicense() {
        try {
            if (licenseManager.getLicense().isDefined())
            {
                for (PluginLicense pluginLicense : licenseManager.getLicense())
                {
                    return !pluginLicense.getError().isDefined();
                }
            }
        } catch (PluginLicenseStoragePluginUnresolvedException e) {
            // ignore it here
        }
        return false;
    }
    
    public URI getLicenseAdminUrl() throws PluginLicenseStoragePluginUnresolvedException {
        return licenseManager.isUpmLicensingAware() ? licenseManager.getPluginManagementUri()
                : LicenseServlet.getServletLocation(request.getContextPath());
    }

	public Long getMaxSize() {
		return new Long(Configuration.getString(APKeys.JIRA_ATTACHMENT_SIZE));
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public Long getPid() {
		return pid;
	}

	public void setPid(Long pid) {
		this.pid = pid;
		setSelectedProjectId(pid);
	}

	@Override
	public String doDefault() throws Exception {
		if (!canAccessSelectedProject()) {
			return "denied";
		}
		return super.doDefault();
	}

	@Override
	protected void doValidation() {
		super.doValidation();

		try {
			AttachmentUtils.checkValidTemporaryAttachmentDirectory();
		} catch (AttachmentException e) {
			addError("file", e.getMessage());
			return;
		}

		if (getMultipart() == null) {
			addError("file", getText("jbcp.BulkCreateIssues.validation.file.is.empty"));
			return;
		}

		try {
			webAttachmentManager.validateAttachmentIfExists(getMultipart(), "file", true);
		} catch (final AttachmentException e) {
			addError("file", e.getMessage());
			return;
		}

		if (StringUtils.isBlank(encoding)) {
			addError("encoding", getText("jbcp.BulkCreateIssues.validation.encoding.is.empty"));
		} else if (!Charset.isSupported(encoding)) {
			addError("encoding", getText("jbcp.BulkCreateIssues.validation.invalid.encoding"));
		}
	}

	@Nullable
	public MultiPartRequestWrapper getMultipart() {
		return ServletActionContext.getMultiPartRequest();
	}

	@Override
	protected String doExecute() throws Exception {
		if (!canAccessSelectedProject()) {
			return "denied";
		}

		final File file = getMultipart().getFile("file");
		final CsvData data;
		try {
			data = new CsvReader(file, encoding).getAllData();
		} catch(Exception e) {
			addError("file", getText("jbcp.BulkCreateIssues.cant.parse", e));
			return INPUT;
		}

		setProjectIdInSession(getSelectedProjectObject().getId());
		setDataInSession(data);

		return getRedirect("MapFields!default.jspa" + (isInlineDialogMode() ? "?decorator=dialog&inline=true" : ""));
	}
}
