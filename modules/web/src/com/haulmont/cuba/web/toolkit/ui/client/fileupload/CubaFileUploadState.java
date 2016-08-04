/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.fileupload;

import com.vaadin.shared.Connector;
import com.vaadin.shared.annotations.NoLayout;
import com.vaadin.shared.ui.TabIndexState;

public class CubaFileUploadState extends TabIndexState {
    {
        primaryStyleName = "cuba-fileupload";
    }

    @NoLayout
    public String iconAltText = null;

    @NoLayout
    public boolean multiSelect = false;

    // permitted mime types, comma separated
    @NoLayout
    public String accept = null;

    @NoLayout
    public String progressWindowCaption;

    @NoLayout
    public String cancelButtonCaption;

    @NoLayout
    public String unableToUploadFileMessage;

    @NoLayout
    public long fileSizeLimit;

    @NoLayout
    public Connector dropZone;

    @NoLayout
    public String dropZonePrompt;
}