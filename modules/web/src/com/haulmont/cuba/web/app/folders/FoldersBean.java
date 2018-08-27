/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.app.folders;

import com.haulmont.cuba.core.entity.AbstractSearchFolder;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowManagerProvider;
import com.haulmont.cuba.gui.WindowParams;
import com.haulmont.cuba.gui.components.Filter;
import com.haulmont.cuba.gui.components.ValuePathHelper;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.data.impl.DsContextImplementation;
import com.haulmont.cuba.security.entity.FilterEntity;
import com.haulmont.cuba.security.entity.SearchFolder;
import com.haulmont.cuba.web.App;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gorbunkov
 * @version $Id: FoldersBean.java 20882 2015-04-10 06:16:28Z gorbunkov $
 */
@ManagedBean(Folders.NAME)
public class FoldersBean implements Folders {

    private static Log log = LogFactory.getLog(FoldersBean.class);

    @Inject
    protected Messages messages;
    @Inject
    protected WindowConfig windowConfig;
    @Inject
    protected WindowManagerProvider windowManagerProvider;
    @Inject
    protected Metadata metadata;

    @Override
    public void openFolder(AbstractSearchFolder folder) {
        if (StringUtils.isBlank(folder.getFilterComponentId())) {
            log.warn("Unable to open folder: componentId is blank");
            return;
        }

        String screenId = getScreenId(folder);
        String filterComponentId = getFilterComponentId(folder);

        Map<String, Object> params = getWindowParams(folder);
        Window window = windowManagerProvider.get().openWindow(windowConfig.getWindowInfo(screenId), WindowManager.OpenType.NEW_TAB, params);

        if (filterComponentId != null) {
            Filter filterComponent = window.getComponent(filterComponentId);
            if (filterComponent != null) {
                filterComponent.setFilterEntity(createFilterEntity(folder, params));
                if (folder instanceof SearchFolder) {
                    SearchFolder searchFolder = (SearchFolder) folder;
                    if (searchFolder.getPresentation() != null) {
                        ((com.haulmont.cuba.gui.components.Component.HasPresentations) filterComponent.getApplyTo())
                                .applyPresentation(searchFolder.getPresentation().getId());
                    }
                }
            }
        }
        ((DsContextImplementation) window.getDsContext()).resumeSuspended();
    }

    protected Map<String, Object> getWindowParams(AbstractSearchFolder folder) {
        Map<String, Object> params = new HashMap<>();

        WindowParams.DISABLE_AUTO_REFRESH.set(params, true);
        WindowParams.DISABLE_RESUME_SUSPENDED.set(params, true);

        if (!StringUtils.isBlank(folder.getTabName())) {
            WindowParams.DESCRIPTION.set(params, messages.getMainMessage(folder.getTabName()));
        } else {
            WindowParams.DESCRIPTION.set(params, messages.getMainMessage(folder.getName()));
        }

        WindowParams.FOLDER_ID.set(params, folder.getId());
        return params;
    }

    protected Window openWindow(AbstractSearchFolder folder) {
        String screenId = getScreenId(folder);

        WindowConfig windowConfig = AppBeans.get(WindowConfig.NAME);
        WindowInfo windowInfo = windowConfig.getWindowInfo(screenId);

        Map<String, Object> params = getWindowParams(folder);

        return App.getInstance().getWindowManager().openWindow(windowInfo, WindowManager.OpenType.NEW_TAB, params);
    }

    protected FilterEntity createFilterEntity(AbstractSearchFolder folder, Map<String, Object> params) {
        FilterEntity filterEntity = metadata.create(FilterEntity.class);
        filterEntity.setFolder(folder);
        filterEntity.setComponentId(folder.getFilterComponentId());
        filterEntity.setName(folder.getLocName());

        filterEntity.setXml(folder.getFilterXml());
        filterEntity.setApplyDefault(BooleanUtils.isNotFalse(folder.getApplyDefault()));
        if (folder instanceof SearchFolder) {
            filterEntity.setIsSet(((SearchFolder) folder).getIsSet());
        }
        return filterEntity;
    }

    protected String getScreenId(AbstractSearchFolder folder) {
        return ValuePathHelper.parse(folder.getFilterComponentId())[0];
    }

    protected String getFilterComponentId(AbstractSearchFolder folder) {
        String[] strings = ValuePathHelper.parse(folder.getFilterComponentId());
        return strings.length > 1 ? StringUtils.join(Arrays.copyOfRange(strings, 1, strings.length), '.') : null;
    }
}
