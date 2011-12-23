/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.app;

import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
@Service(ConfigStorageService.NAME)
public class ConfigStorageServiceBean implements ConfigStorageService {

    @Inject
    private ConfigStorageAPI api;

    @Override
    public Map<String, String> getDbProperties() {
        return api.getDbProperties();
    }

    @Override
    public String getDbProperty(String name) {
        return api.getDbProperty(name);
    }

    @Override
    public void setDbProperty(String name, String value) {
        api.setDbProperty(name, value);
    }
}
