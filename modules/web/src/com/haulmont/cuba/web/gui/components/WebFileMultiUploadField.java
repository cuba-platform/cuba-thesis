/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.components.FileMultiUploadField;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.theme.ThemeConstants;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.WebWindowManager;
import com.haulmont.cuba.web.toolkit.FileUploadTypesHelper;
import com.haulmont.cuba.web.toolkit.VersionedThemeResource;
import com.haulmont.cuba.web.toolkit.ui.CubaFileUpload;
import com.haulmont.cuba.web.toolkit.ui.CubaMultiUpload;
import com.haulmont.cuba.web.toolkit.ui.UploadComponent;
import com.vaadin.server.Page;
import com.vaadin.server.WebBrowser;
import com.vaadin.ui.Component;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

public class WebFileMultiUploadField extends WebAbstractComponent<UploadComponent> implements FileMultiUploadField {

    private static final int BYTES_IN_MEGABYTE = 1048576;

    protected Log log = LogFactory.getLog(getClass());

    protected FileUploadingAPI fileUploading;

    protected final List<UploadListener> listeners = new LinkedList<>();
    protected final Map<UUID, String> files = new HashMap<>();

    protected UUID tempFileId;
    protected String icon;
    protected String accept;

    protected DropZone dropZone;
    protected String dropZonePrompt;

    public WebFileMultiUploadField() {
        fileUploading = AppBeans.get(FileUploadingAPI.NAME);

        WebBrowser webBrowser = Page.getCurrent().getWebBrowser();
        if ((webBrowser.isIE() && !webBrowser.isEdge()) && webBrowser.getBrowserMajorVersion() < 10) {
            initOldComponent();
        } else {
            initComponent();
        }
    }

    protected void initOldComponent() {
        CubaMultiUpload impl = createOldComponent();

        ThemeConstants theme = App.getInstance().getThemeConstants();

        String width = theme.get("cuba.web.WebFileMultiUploadField.upload.width");
        String height = theme.get("cuba.web.WebFileMultiUploadField.upload.height");

        impl.setWidth(width);
        impl.setHeight(height);

        int buttonTextLeft = theme.getInt("cuba.web.WebFileMultiUploadField.buttonText.left");
        int buttonTextTop = theme.getInt("cuba.web.WebFileMultiUploadField.buttonText.top");

        impl.setButtonTextLeft(buttonTextLeft);
        impl.setButtonTextTop(buttonTextTop);

        impl.setButtonWidth(Integer.parseInt(width.replace("px", "")));
        impl.setButtonHeight(Integer.parseInt(height.replace("px", "")));

        Messages messages = AppBeans.get(Messages.NAME);
        impl.setCaption(messages.getMessage(AppConfig.getMessagesPack(), "multiupload.submit"));

        Configuration configuration = AppBeans.get(Configuration.NAME);
        impl.setFileSizeLimitMB(configuration.getConfig(ClientConfig.class).getMaxUploadSizeMb());

        WebConfig webConfig = configuration.getConfig(WebConfig.class);
        if (!webConfig.getUseFontIcons()) {
            impl.setButtonImage(new VersionedThemeResource("components/multiupload/images/multiupload-button.png"));
        } else {
            impl.setButtonImage(new VersionedThemeResource("components/multiupload/images/multiupload-button-font-icon.png"));
        }

        impl.setButtonStyles(theme.get("cuba.web.WebFileMultiUploadField.button.style"));
        impl.setButtonDisabledStyles(theme.get("cuba.web.WebFileMultiUploadField.button.disabled.style"));

        impl.setBootstrapFailureHandler(new CubaMultiUpload.BootstrapFailureHandler() {
            @Override
            public void loadWebResourcesFailed() {
                Messages messages = AppBeans.get(Messages.NAME);
                String resourcesLoadFailed = messages.getMessage(WebFileMultiUploadField.class, "multiupload.resources.notLoaded");
                WebWindowManager wm = App.getInstance().getWindowManager();
                wm.showNotification(resourcesLoadFailed, IFrame.NotificationType.ERROR);
            }

            @Override
            public void flashNotInstalled() {
                Messages messages = AppBeans.get(Messages.NAME);
                String swfNotSupported = messages.getMessage(WebFileMultiUploadField.class, "multiupload.resources.swfNotSupported");
                WebWindowManager wm = App.getInstance().getWindowManager();
                wm.showNotification(swfNotSupported, IFrame.NotificationType.ERROR);
            }
        });

        impl.setReceiver(new CubaMultiUpload.Receiver() {
            @Override
            public OutputStream receiveUpload(String filename, String mimeType) {
                FileOutputStream outputStream;
                try {
                    FileUploadingAPI.FileInfo fileInfo = fileUploading.createFile();
                    tempFileId = fileInfo.getId();
                    File tmpFile = fileInfo.getFile();
                    outputStream = new FileOutputStream(tmpFile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return outputStream;
            }
        });

        setExpandable(false);

        impl.addUploadListener(new CubaMultiUpload.UploadListener() {
            @Override
            public void fileUploadStart(String fileName) {
                for (UploadListener listener : listeners)
                    listener.fileUploadStart(fileName);
            }

            @Override
            public void fileUploaded(String fileName) {
                files.put(tempFileId, fileName);

                for (UploadListener listener : listeners)
                    listener.fileUploaded(fileName);
            }

            @Override
            public void queueUploadComplete() {
                for (UploadListener listener : listeners)
                    listener.queueUploadComplete();
            }

            @Override
            public void errorNotify(String fileName, String message, CubaMultiUpload.UploadErrorType errorCode) {
                log.warn(String.format("Error while uploading file '%s' with code '%s': %s", fileName, errorCode.getId(), message));

                Messages messages = AppBeans.get(Messages.NAME);
                WebWindowManager wm = App.getInstance().getWindowManager();
                switch (errorCode) {
                    case QUEUE_LIMIT_EXCEEDED:
                        wm.showNotification(messages.getMessage(WebFileMultiUploadField.class, "multiupload.queueLimitExceed"),
                                IFrame.NotificationType.WARNING);
                        break;
                    case FILE_EXCEEDS_SIZE_LIMIT:
                        Configuration configuration = AppBeans.get(Configuration.NAME);
                        ClientConfig clientConfig = configuration.getConfig(ClientConfig.class);
                        final int maxUploadSizeMb = clientConfig.getMaxUploadSizeMb();

                        wm.showNotification(messages.formatMessage(WebFileMultiUploadField.class, "multiupload.filesizeLimitExceed", fileName, maxUploadSizeMb),
                                IFrame.NotificationType.WARNING);
                        break;
                    case SECURITY_ERROR:
                        wm.showNotification(messages.getMessage(WebFileMultiUploadField.class, "multiupload.securityError"),
                                IFrame.NotificationType.WARNING);
                        break;
                    case ZERO_BYTE_FILE:
                        wm.showNotification(messages.formatMessage(WebFileMultiUploadField.class, "multiupload.zerobyteFile", fileName),
                                IFrame.NotificationType.WARNING);
                        break;
                    default:
                        boolean handled = false;
                        for (UploadListener listener : listeners)
                            handled = handled | listener.uploadError(fileName);
                        if (!handled) {
                            String uploadError = messages.formatMessage(WebFileMultiUploadField.class, "multiupload.uploadError", fileName);
                            wm.showNotification(uploadError, IFrame.NotificationType.ERROR);
                        }
                        break;

                }
            }
        });
        impl.setDescription(null);

        component = impl;
    }

    protected void initComponent() {
        CubaFileUpload impl = createComponent();
        impl.setMultiSelect(true);

        Messages messages = AppBeans.get(Messages.NAME);
        impl.setProgressWindowCaption(messages.getMainMessage("upload.uploadingProgressTitle"));
        impl.setUnableToUploadFileMessage(messages.getMainMessage("upload.unableToUploadFile"));
        impl.setCancelButtonCaption(messages.getMainMessage("upload.cancel"));
        impl.setCaption(messages.getMainMessage("upload.submit"));
        impl.setDropZonePrompt(messages.getMainMessage("upload.dropZonePrompt"));
        impl.setDescription(null);

        Configuration configuration = AppBeans.get(Configuration.NAME);
        final long maxUploadSizeMb = configuration.getConfig(ClientConfig.class).getMaxUploadSizeMb();
        final long maxSizeBytes = maxUploadSizeMb * BYTES_IN_MEGABYTE;

        impl.setFileSizeLimit(maxSizeBytes);

        impl.setReceiver(new CubaFileUpload.Receiver() {
            @Override
            public OutputStream receiveUpload(String fileName, String MIMEType) {
                FileOutputStream outputStream;
                try {
                    FileUploadingAPI.FileInfo fileInfo = fileUploading.createFile();
                    tempFileId = fileInfo.getId();
                    File tmpFile = fileInfo.getFile();
                    outputStream = new FileOutputStream(tmpFile);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to receive file", e);
                }
                return outputStream;
            }
        });
        impl.addStartedListener(new CubaFileUpload.StartedListener() {
            @Override
            public void uploadStarted(CubaFileUpload.StartedEvent event) {
                for (FileMultiUploadField.UploadListener listener : listeners) {
                    listener.fileUploadStart(event.getFileName());
                }
            }
        });
        impl.addQueueUploadFinishedListener(new CubaFileUpload.QueueFinishedListener() {
            @Override
            public void queueUploadFinished(CubaFileUpload.QueueFinishedEvent event) {
                for (FileMultiUploadField.UploadListener listener : listeners) {
                    listener.queueUploadComplete();
                }
            }
        });
        impl.addSucceededListener(new CubaFileUpload.SucceededListener() {
            @Override
            public void uploadSucceeded(CubaFileUpload.SucceededEvent event) {
                files.put(tempFileId, event.getFileName());

                for (FileMultiUploadField.UploadListener listener : listeners) {
                    listener.fileUploaded(event.getFileName());
                }
            }
        });
        impl.addFailedListener(new CubaFileUpload.FailedListener() {
            @Override
            public void uploadFailed(CubaFileUpload.FailedEvent event) {
                try {
                    // close and remove temp file
                    fileUploading.deleteFile(tempFileId);
                    tempFileId = null;
                } catch (Exception e) {
                    if (e instanceof FileStorageException) {
                        FileStorageException fse = (FileStorageException) e;
                        if (fse.getType() != FileStorageException.Type.FILE_NOT_FOUND) {
                            log.warn(String.format("Could not remove temp file %s after broken uploading", tempFileId));
                        }
                    }
                    log.warn(String.format("Error while delete temp file %s", tempFileId));
                }

                for (UploadListener listener : listeners) {
                    listener.uploadError(event.getFileName());
                }
            }
        });
        impl.addFileSizeLimitExceededListener(new CubaFileUpload.FileSizeLimitExceededListener() {
            @Override
            public void fileSizeLimitExceeded(CubaFileUpload.FileSizeLimitExceededEvent e) {
                Messages messages = AppBeans.get(Messages.NAME);
                String warningMsg = messages.formatMainMessage("upload.fileTooBig.message", e.getFileName(), maxUploadSizeMb);
                getFrame().showNotification(warningMsg, IFrame.NotificationType.WARNING);
            }
        });

        component = impl;
    }

    protected CubaFileUpload createComponent() {
        return new CubaFileUpload();
    }

    protected CubaMultiUpload createOldComponent() {
        return new CubaMultiUpload();
    }

    @Override
    public String getCaption() {
        return component.getCaption();
    }

    @Override
    public void setCaption(String caption) {
        component.setCaption(caption);
    }

    @Override
    public String getDescription() {
        return component.getDescription();
    }

    @Override
    public void setDescription(String description) {
        component.setDescription(description);
    }

    @Deprecated
    public int getButtonWidth() {
        return ((CubaMultiUpload) component).getButtonWidth();
    }

    @Deprecated
    public void setButtonWidth(int buttonWidth) {
        ((CubaMultiUpload) component).setButtonWidth(buttonWidth);
    }

    @Deprecated
    public void setButtonHeight(int buttonHeight) {
        ((CubaMultiUpload) component).setButtonHeight(buttonHeight);
    }

    @Deprecated
    public int getButtonHeight() {
        return ((CubaMultiUpload) component).getButtonHeight();
    }

    @Deprecated
    public String getFileTypesMask() {
        return ((CubaMultiUpload) component).getFileTypesMask();
    }

    @Deprecated
    public void setFileTypesMask(String fileTypesMask) {
        ((CubaMultiUpload) component).setFileTypesMask(fileTypesMask);
    }

    @Deprecated
    public String getFileTypesDescription() {
        return ((CubaMultiUpload) component).getFileTypesDescription();
    }

    @Deprecated
    public void setFileTypesDescription(String fileTypesDescription) {
        ((CubaMultiUpload) component).setFileTypesDescription(fileTypesDescription);
    }

    @Deprecated
    public double getFileSizeLimitMB() {
        return ((CubaMultiUpload) component).getFileSizeLimitMB();
    }

    @Deprecated
    public void setFileSizeLimitMB(double filesizeLimit) {
        ((CubaMultiUpload) component).setFileSizeLimitMB(filesizeLimit);
    }

    @Deprecated
    public double getQueueUploadLimitMB() {
        return ((CubaMultiUpload) component).getQueueUploadLimitMB();
    }

    @Deprecated
    public void setQueueUploadLimitMB(double queueUploadLimit) {
        ((CubaMultiUpload) component).setQueueUploadLimitMB(queueUploadLimit);
    }

    @Deprecated
    public int getQueueSizeLimit() {
        return ((CubaMultiUpload) component).getQueueSizeLimit();
    }

    @Deprecated
    public void setQueueSizeLimit(int queueSizeLimit) {
        ((CubaMultiUpload) component).setQueueSizeLimit(queueSizeLimit);
    }

    @Override
    public void addListener(UploadListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    @Override
    public void removeListener(UploadListener listener) {
        listeners.remove(listener);
    }

    /**
     * Get uploads map
     *
     * @return Map (UUID - Id of file in FileUploadService, String - FileName )
     */
    @Override
    public Map<UUID, String> getUploadsMap() {
        return Collections.unmodifiableMap(files);
    }

    @Override
    public void clearUploads() {
        files.clear();
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;

        if (component instanceof CubaFileUpload) {
            if (!StringUtils.isEmpty(icon)) {
                component.setIcon(WebComponentsHelper.getIcon(icon));
            } else {
                component.setIcon(null);
            }
        }
    }

    @Override
    public String getAccept() {
        return accept;
    }

    @Override
    public void setAccept(String accept) {
        if (!StringUtils.equals(accept, getAccept())) {
            this.accept = accept;
            component.setAccept(component instanceof CubaMultiUpload
                    ? FileUploadTypesHelper.convertSeparator(accept, ";")
                    : FileUploadTypesHelper.convertToMIME(accept));
        }
    }

    @Override
    public void setDropZone(DropZone dropZone) {
        this.dropZone = dropZone;

        if (component instanceof CubaFileUpload) {
            if (dropZone == null) {
                ((CubaFileUpload) component).setDropZone(null);
            } else {
                com.haulmont.cuba.gui.components.Component target = dropZone.getTarget();
                if (target instanceof Window.Wrapper) {
                    target = ((Window.Wrapper) target).getWrappedWindow();
                }

                Component vComponent = WebComponentsHelper.unwrap(target);
                ((CubaFileUpload) this.component).setDropZone(vComponent);
            }
        }
    }

    @Override
    public DropZone getDropZone() {
        return dropZone;
    }

    @Override
    public void setDropZonePrompt(String dropZonePrompt) {
        this.dropZonePrompt = dropZonePrompt;

        if (component instanceof CubaFileUpload) {
            ((CubaFileUpload) component).setDropZonePrompt(dropZonePrompt);
        }
    }

    @Override
    public String getDropZonePrompt() {
        return dropZonePrompt;
    }
}