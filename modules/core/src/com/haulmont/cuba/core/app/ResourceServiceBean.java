/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.app;

import com.haulmont.cuba.core.global.Resources;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author krivopustov
 * @version $Id$
 */
@Service(ResourceService.NAME)
public class ResourceServiceBean implements ResourceService {

    @Inject
    protected Resources resources;

    @Override
    public String getResourceAsString(String name) {
        return resources.getResourceAsString(name);
    }
}
