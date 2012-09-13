/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.config;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.gui.NoSuchScreenException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.springframework.core.io.Resource;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericUI class holding information about all registered in <code>screens.xml</code> screens.
 *
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean(WindowConfig.NAME)
public class WindowConfig
{
    public static final String NAME = "cuba_WindowConfig";

    public static final String WINDOW_CONFIG_XML_PROP = "cuba.windowConfig";

    protected Map<String, WindowInfo> screens = new HashMap<String, WindowInfo>();

    private static Log log = LogFactory.getLog(WindowConfig.class);

    private Resources resources;

    private Metadata metadata;

    public static final Pattern ENTITY_SCREEN_PATTERN = Pattern.compile("([_A-Za-z]+\\$[A-Z][_A-Za-z0-9]*)\\..+");

    @Inject
    public WindowConfig(Resources resources, Metadata metadata) {
        this.resources = resources;
        this.metadata = metadata;

        final String configName = AppContext.getProperty(WINDOW_CONFIG_XML_PROP);

        StrTokenizer tokenizer = new StrTokenizer(configName);
        for (String location : tokenizer.getTokenArray()) {
            Resource resource = resources.getResource(location);
            if (resource.exists()) {
                InputStream stream = null;
                try {
                    stream = resource.getInputStream();
                    loadConfig(Dom4j.readDocument(stream).getRootElement());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            } else {
                log.warn("Resource " + location + " not found, ignore it");
            }
        }
    }

    public void loadConfig(Element rootElem) {
        for (Element element : (List<Element>) rootElem.elements("include")) {
            String fileName = element.attributeValue("file");
            if (!StringUtils.isBlank(fileName)) {
                String incXml = resources.getResourceAsString(fileName);
                if (incXml == null) {
                    log.warn("File " + fileName + " not found, ignore it");
                    continue;
                }
                loadConfig(Dom4j.readDocument(incXml).getRootElement());
            }
        }
        for (Element element : (List<Element>) rootElem.elements("screen")) {
            String id = element.attributeValue("id");
            if (StringUtils.isBlank(id)) {
                log.warn("Invalid window config: 'id' attribute not defined");
                continue;
            }
            WindowInfo windowInfo = new WindowInfo(id, element);
            screens.put(id, windowInfo);
        }
    }

    /**
     * Get screen information by screen ID.
     * Can be overridden for specific client type.
     * @param id screen ID as set up in <code>screen-config.xml</code>
     * @throws NoSuchScreenException if the screen with specified ID is not registered
     */
    public WindowInfo getWindowInfo(String id) {
        WindowInfo windowInfo = screens.get(id);
        if (windowInfo == null) {
            Matcher matcher = ENTITY_SCREEN_PATTERN.matcher(id);
            if (matcher.matches()) {
                MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalMetaClass(matcher.group(1));
                if (originalMetaClass != null) {
                    String originalId = new StringBuilder(id)
                            .replace(matcher.start(1), matcher.end(1), originalMetaClass.getName()).toString();
                    windowInfo = screens.get(originalId);
                }
            }
        }
        if (windowInfo == null)
            throw new NoSuchScreenException(id);
        return windowInfo;
    }

    /**
     * All registered screens
     */
    public Collection<WindowInfo> getWindows() {
        return screens.values();
    }
}
