/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.theme.impl;

import com.haulmont.cuba.desktop.DesktopResources;
import com.haulmont.cuba.desktop.theme.ComponentDecorator;
import com.haulmont.cuba.desktop.theme.DesktopTheme;
import net.miginfocom.layout.PlatformDefaults;
import net.miginfocom.layout.UnitValue;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.*;

/**
 * @author Alexander Budarov
 * @version $Id$
 */
public class DesktopThemeImpl implements DesktopTheme {

    private String name;

    private String lookAndFeel;

    private Map<String, Object> uiDefaults;

    private Map<String, List<DesktopStyle>> styles;

    private DesktopResources resources;

    private Log log = LogFactory.getLog(DesktopThemeImpl.class);

    /**
     * we can control margin & spacing sizes with help of {@link net.miginfocom.layout.PlatformDefaults} class.
     */
    private Integer marginSize;

    private Integer spacingSize;

    public DesktopThemeImpl() {
        this.uiDefaults = new HashMap<>();
        this.styles = new HashMap<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public DesktopResources getResources() {
        return resources;
    }

    public void setResources(DesktopResources resources) {
        this.resources = resources;
    }

    public Map<String, Object> getUiDefaults() {
        return uiDefaults;
    }

    public Map<String, List<DesktopStyle>> getStyles() {
        return styles;
    }

    public String getLookAndFeel() {
        return lookAndFeel;
    }

    public void setLookAndFeel(String lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void init() {
        JDialog.setDefaultLookAndFeelDecorated(true);
        JFrame.setDefaultLookAndFeelDecorated(true);

        try {
            UIManager.setLookAndFeel(lookAndFeel);
            initUIDefaults();
            initButtonsKeyBinding();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        if (marginSize != null) {
            UnitValue marginValue = new UnitValue(marginSize);
            PlatformDefaults.setPanelInsets(marginValue, marginValue, marginValue, marginValue);
        }

        if (spacingSize != null) {
            UnitValue spacingValue = new UnitValue(spacingSize);
            PlatformDefaults.setGridCellGap(spacingValue, spacingValue);
        }
    }

    protected void initButtonsKeyBinding() {
        InputMap im = (InputMap) UIManager.get("Button.focusInputMap");
        im.put(KeyStroke.getKeyStroke("ENTER"), "pressed");
        im.put(KeyStroke.getKeyStroke("released ENTER"), "released");
    }

    protected void initUIDefaults() {
        for (String propertyName : uiDefaults.keySet()) {
            UIManager.getLookAndFeelDefaults().put(propertyName, uiDefaults.get(propertyName));
        }
    }

    @Override
    public void applyStyle(Object component, String styleName) {
        applyStyle(component, styleName, null);
    }

    @Override
    public void applyStyle(Object component, String styleNameString, Set<String> state) {
        // split string into individual style names
        StrTokenizer tokenizer = new StrTokenizer(styleNameString);
        String[] styleNames = tokenizer.getTokenArray();
        for (String styleName : styleNames) {
            applyStyleName(component, state, styleName);
        }
    }

    private void applyStyleName(Object component, Set<String> state, String styleName) {
        DesktopStyle style = findStyle(component.getClass(), styleName);
        if (style == null) {
            if (log.isDebugEnabled()) {
                log.debug("Can not find style " + styleName + " for component " + component);
            }
            return;
        }

        for (ComponentDecorator decorator : style.getDecorators()) {
            try {
                decorator.decorate(component, state);
            } catch (Exception e) {
                log.error("Error applying decorator " + decorator + " to " + component, e);
            }
        }
    }

    @Nullable
    private DesktopStyle findStyle(Class componentClass, String styleName) {
        List<DesktopStyle> stylesByName = styles.get(styleName);
        if (stylesByName == null) {
            return null;
        }
        for (DesktopStyle desktopStyle : stylesByName) {
            if (desktopStyle.isSupported(componentClass)) {
                return desktopStyle;
            }
        }
        return null;
    }

    public void setStyles(Map<String, List<DesktopStyle>> styles) {
        this.styles = styles;
    }

    /**
     * Add style to theme. Adding any subsequent style with the same name will override existing styles.
     *
     * @param style style to add
     */
    public void addStyle(DesktopStyle style) {
        List<DesktopStyle> list = styles.get(style.getName());
        if (list != null) {
            list.add(0, style);
        } else {
            list = new ArrayList<>();
            list.add(style);
            styles.put(style.getName(), list);
        }
    }

    public void setMarginSize(Integer marginSize) {
        this.marginSize = marginSize;
    }

    public void setSpacingSize(Integer spacingSize) {
        this.spacingSize = spacingSize;
    }
}
