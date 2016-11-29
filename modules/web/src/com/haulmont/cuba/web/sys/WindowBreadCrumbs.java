/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.sys;

import com.haulmont.cuba.gui.TestIdManager;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.mainwindow.AppWorkArea;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.cuba.web.gui.components.mainwindow.WebAppWorkArea;
import com.haulmont.cuba.web.toolkit.ui.CubaButton;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.themes.BaseTheme;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class WindowBreadCrumbs extends CssLayout {
    protected static final String BREADCRUMBS_VISIBLE_WRAP_STYLE = "cuba-breadcrumbs-visible";

    protected boolean visibleExplicitly = true;

    public interface Listener {
        void windowClick(Window window);
    }

    protected boolean tabbedMode;

    protected LinkedList<Window> windows = new LinkedList<>();

    protected Layout logoLayout;
    protected Layout linksLayout;
    protected Button closeBtn;

    protected Map<Button, Window> btn2win = new HashMap<>();

    protected List<Listener> listeners = new ArrayList<>();

    public WindowBreadCrumbs(WebAppWorkArea workArea) {
        setWidth(100, Unit.PERCENTAGE);
        setHeight(-1, Unit.PIXELS);
        setStyleName("cuba-headline-container");

        tabbedMode = workArea.getMode() == AppWorkArea.Mode.TABBED;

        if (tabbedMode) {
            super.setVisible(false);
        }

        addAttachListener(new AttachListener() {
            @Override
            public void attach(AttachEvent event) {
                adjustParentStyles();
            }
        });

        logoLayout = createLogoLayout();

        linksLayout = createLinksLayout();
        linksLayout.setSizeUndefined();

        if (!tabbedMode) {
            closeBtn = new CubaButton("", new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    final Window window = getCurrentWindow();
                    window.close(Window.CLOSE_ACTION_ID);
                }
            });
            closeBtn.setIcon(WebComponentsHelper.getIcon("icons/close.png"));
            closeBtn.setStyleName("cuba-closetab-button");
        }

        AppUI ui = AppUI.getCurrent();
        if (ui.isTestMode()) {
            TestIdManager testIdManager = ui.getTestIdManager();
            linksLayout.setId(testIdManager.getTestId("breadCrumbs"));
            linksLayout.setCubaId("breadCrumbs");

            if (closeBtn != null) {
                closeBtn.setId(testIdManager.reserveId("closeBtn"));
                closeBtn.setCubaId("closeBtn");
            }
        }

        Layout enclosingLayout = createEnclosingLayout();
        enclosingLayout.addComponent(linksLayout);

        addComponent(logoLayout);
        addComponent(enclosingLayout);

        if (closeBtn != null) {
            addComponent(closeBtn);
        }
    }

    protected Layout createEnclosingLayout() {
        Layout enclosingLayout = new CssLayout();
        enclosingLayout.setStyleName("cuba-breadcrumbs-container");
        return enclosingLayout;
    }

    protected Layout createLinksLayout() {
        CssLayout linksLayout = new CssLayout();
        linksLayout.setStyleName("cuba-breadcrumbs");
        return linksLayout;
    }

    protected Layout createLogoLayout() {
        CssLayout logoLayout = new CssLayout();
        logoLayout.setStyleName("cuba-breadcrumbs-logo");
        return logoLayout;
    }

    public Window getCurrentWindow() {
        if (windows.isEmpty())
            return null;
        else
            return windows.getLast();
    }

    public void addWindow(Window window) {
        windows.add(window);
        update();
        if (windows.size() > 1 && tabbedMode)
            super.setVisible(visibleExplicitly);

        if (getParent() != null) {
            adjustParentStyles();
        }
    }

    public void removeWindow() {
        if (!windows.isEmpty()) {
            windows.removeLast();
            update();
        }
        if (windows.size() <= 1 && tabbedMode)
            super.setVisible(false);

        if (getParent() != null) {
            adjustParentStyles();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        this.visibleExplicitly = visible;

        super.setVisible(isVisible() && visibleExplicitly);

        if (getParent() != null) {
            adjustParentStyles();
        }
    }

    protected void adjustParentStyles() {
        if (isVisible()) {
            getParent().addStyleName(BREADCRUMBS_VISIBLE_WRAP_STYLE);
        } else {
            getParent().removeStyleName(BREADCRUMBS_VISIBLE_WRAP_STYLE);
        }
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    protected void fireListeners(Window window) {
        for (Listener listener : listeners) {
            listener.windowClick(window);
        }
    }

    public void update() {
        AppUI ui = AppUI.getCurrent();
        boolean isTestMode = ui.isTestMode();

        linksLayout.removeAllComponents();
        btn2win.clear();
        for (Iterator<Window> it = windows.iterator(); it.hasNext(); ) {
            Window window = it.next();
            Button button = new CubaButton(StringUtils.trimToEmpty(window.getCaption()), new BtnClickListener());
            button.setSizeUndefined();
            button.setStyleName(BaseTheme.BUTTON_LINK);
            button.setTabIndex(-1);

            if (isTestMode) {
                button.setCubaId("breadCrubms_Button_" + window.getId());
                button.setId(ui.getTestIdManager().getTestId("breadCrubms_Button_" + window.getId()));
            }

            btn2win.put(button, window);

            if (it.hasNext()) {
                linksLayout.addComponent(button);

                Label separatorLab = new Label("&nbsp;&gt;&nbsp;");
                separatorLab.setStyleName("cuba-breadcrumbs-separator");
                separatorLab.setSizeUndefined();
                separatorLab.setContentMode(ContentMode.HTML);
                linksLayout.addComponent(separatorLab);
            } else {
                Label captionLabel = new Label(window.getCaption());
                captionLabel.setStyleName("cuba-breadcrumbs-win-caption");
                captionLabel.setSizeUndefined();
                linksLayout.addComponent(captionLabel);
            }
        }
    }

    public class BtnClickListener implements Button.ClickListener {
        @Override
        public void buttonClick(Button.ClickEvent event) {
            Window win = btn2win.get(event.getButton());
            if (win != null)
                fireListeners(win);
        }
    }
}