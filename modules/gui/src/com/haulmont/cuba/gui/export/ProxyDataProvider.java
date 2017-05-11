/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.export;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author artamonov
 */
public class ProxyDataProvider implements ExportDataProvider {

    private boolean closed = false;

    private ExportDataProvider dataProvider = null;

    public ProxyDataProvider(ExportDataProvider dataProvider) throws ClosedDataProviderException {
        this.dataProvider = dataProvider;
        InputStream dataInputStream = dataProvider.provide();
        try {
            dataInputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream provide() throws ResourceException, ClosedDataProviderException {
        if (closed)
            throw new ClosedDataProviderException();

        return dataProvider.provide();
    }

    @Override
    public void close() {
        if (!closed) {
            if (dataProvider != null)
                dataProvider.close();
            closed = true;
        }
    }
}