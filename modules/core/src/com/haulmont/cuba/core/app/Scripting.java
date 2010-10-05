/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 29.09.2010 12:00:07
 *
 * $Id$
 */
package com.haulmont.cuba.core.app;

import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.ScriptingProvider;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import java.util.Collections;

@ManagedBean("cuba_Scripting")
public class Scripting implements ScriptingMBean {

    private static Log log = LogFactory.getLog(Scripting.class);

    public String getRootPath() {
        return ConfigProvider.getConfig(ServerConfig.class).getServerConfDir();
    }

    public String runGroovyScript(String scriptName) {
        try {
            return ScriptingProvider.runGroovyScript(scriptName, Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            log.error("Error runGroovyScript", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }
}
