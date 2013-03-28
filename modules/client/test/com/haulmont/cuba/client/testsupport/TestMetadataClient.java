/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.client.testsupport;

import com.haulmont.chile.core.loader.MetadataLoader;
import com.haulmont.cuba.core.global.ExtendedEntities;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.sys.MetadataImpl;
import com.haulmont.cuba.core.sys.PersistentEntitiesMetadataLoader;

import java.util.List;

/**
 * @author krivopustov
 * @version $Id$
 */
public class TestMetadataClient extends MetadataImpl {

    private List<String> packages;

    public TestMetadataClient(List<String> packages, TestViewRepositoryClient viewRepository) {
        this.packages = packages;

        this.viewRepository = viewRepository;
        viewRepository.setMetadata(this);

        extendedEntities = new ExtendedEntities(this);
        tools = new MetadataTools(this, null, null);
    }

    @Override
    protected void initMetadata() {
        MetadataLoader persistentEntitiesMetadataLoader = new PersistentEntitiesMetadataLoader();
        for (String p : packages) {
            persistentEntitiesMetadataLoader.loadPackage(p, p);
        }
        persistentEntitiesMetadataLoader.postProcess();

        this.session = persistentEntitiesMetadataLoader.getSession();
    }
}
