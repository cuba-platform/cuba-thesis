/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 04.12.2008 12:02:22
 *
 * $Id$
 */
package com.haulmont.cuba.web;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.global.MetadataProvider;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.config.MenuConfig;
import com.haulmont.cuba.gui.config.MenuItem;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.app.UserSettingHelper;
import com.haulmont.cuba.web.log.LogWindow;
import com.haulmont.cuba.web.sys.ActiveDirectoryHelper;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.*;
import org.dom4j.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main application window.
 * <p>
 * Specific application should inherit from this class and create appropriate
 * instance in {@link com.haulmont.cuba.web.App#createAppWindow()} method
 */
public class AppWindow extends Window {

    /**
     * Main window mode. See {@link #TABBED}, {@link #SINGLE}
     */
    public enum Mode {
        /**
         * If the main window is in TABBED mode, it creates the Tabsheet inside
         * and opens screens with {@link com.haulmont.cuba.gui.WindowManager.OpenType#NEW_TAB} as tabs.
         */
        TABBED,

        /**
         * In SINGLE mode each new screen opened with {@link com.haulmont.cuba.gui.WindowManager.OpenType#NEW_TAB}
         * opening type will replace the current screen.
         */
        SINGLE
    }

    protected Connection connection;

    protected MenuBar menuBar;
    protected TabSheet tabSheet;

    protected Mode mode;

    /** Very root layout of the window. Contains all other layouts */
    protected VerticalLayout rootLayout;

    /** Title layout. Topmost by default */
    protected Layout titleLayout;

    /** Layout containing the menu bar. Next to title layout by default */
    protected HorizontalLayout menuBarLayout;

    /** Empty layout, below menu bar layout by default*/
    protected HorizontalLayout emptyLayout;

    /** Layout containing application screens */
    protected VerticalLayout mainLayout;

    public AppWindow(Connection connection) {
        super();

        this.connection = connection;
        setCaption(getAppCaption());

        mode = UserSettingHelper.loadAppWindowMode();

        rootLayout = createLayout();
        initLayout();
        setLayout(rootLayout);
        setTheme("saneco");
        postInitLayout();
    }

    /**
     * Current mode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Creates root and enclosed layouts.
     * <br>Can be overridden in descendant to create an app-specific root layout
     */
    protected VerticalLayout createLayout() {
        final VerticalLayout layout = new VerticalLayout();

        layout.setMargin(false);
        layout.setSpacing(false);
        layout.setSizeFull();

        titleLayout = createTitleLayout();
        layout.addComponent(titleLayout);

        menuBarLayout = createMenuBarLayout();

        layout.addComponent(menuBarLayout);

        emptyLayout = new HorizontalLayout();
        emptyLayout.setMargin(false);
        emptyLayout.setSpacing(false);
        emptyLayout.setSizeFull();

        layout.addComponent(emptyLayout);

        mainLayout = new VerticalLayout();
        mainLayout.setMargin(true);
        mainLayout.setSpacing(true);
        mainLayout.setSizeFull();

        if (Mode.TABBED.equals(mode)) {
            tabSheet = new TabSheet();
            tabSheet.setSizeFull();

            mainLayout.addComponent(tabSheet);
            mainLayout.setExpandRatio(tabSheet, 1);
        }

        layout.addComponent(mainLayout);
        layout.setExpandRatio(mainLayout, 1);

        return layout;
    }

    /**
     * Can be overridden in descendant to create an app-specific caption
     */
    protected String getAppCaption() {
        return MessageProvider.getMessage(getClass(), "application.caption", Locale.getDefault());
    }

    /**
     * Enclosed Tabsheet
     * @return the tabsheet in TABBED mode, null in SINGLE mode
     */
    public TabSheet getTabSheet() {
        return tabSheet;
    }

    public MenuBar getMenuBar() {
        return menuBar;
    }

    /** See {@link #rootLayout} */
    public VerticalLayout getRootLayout() {
        return rootLayout;
    }

    /** See {@link #titleLayout} */
    public Layout getTitleLayout() {
        return titleLayout;
    }

    /** See {@link #menuBarLayout} */
    public HorizontalLayout getMenuBarLayout() {
        return menuBarLayout;
    }

    /** See {@link #emptyLayout} */
    public HorizontalLayout getEmptyLayout() {
        return emptyLayout;
    }

    /** See {@link #mainLayout} */
    public VerticalLayout getMainLayout() {
        return mainLayout;
    }

    /**
     * Can be overridden in descendant to init an app-specific layout
     */
    protected void initLayout() {
    }

    /**
     * Can be overridden in descendant to init an app-specific layout
     */
    protected void postInitLayout() {
    }

    /**
     * Can be overridden in descendant to create an app-specific menu bar layout
     */
    protected HorizontalLayout createMenuBarLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(false);
        layout.setSpacing(false);
        menuBar = createMenuBar();
        layout.addComponent(menuBar);

        return layout;
    }

    /**
     * Can be overridden in descendant to create an app-specific menu bar
     */
    protected MenuBar createMenuBar() {
        menuBar = new MenuBar();

        final MenuConfig menuConfig = AppConfig.getInstance().getMenuConfig();
        List<MenuItem> rootItems = menuConfig.getRootItems();
        for (MenuItem menuItem : rootItems) {
            createMenuBarItem(menuBar, menuItem);
        }

        return menuBar;
    }

    /**
     * Can be overridden in descendant to create an app-specific title layout
     */
    protected Layout createTitleLayout() {
        HorizontalLayout titleLayout = new HorizontalLayout();

        titleLayout.setWidth(100, Sizeable.UNITS_PERCENTAGE);
        titleLayout.setHeight(20, Sizeable.UNITS_PIXELS); // TODO (abramov) This is a bit tricky

        titleLayout.setMargin(false);
        titleLayout.setSpacing(false);

        Label logoLabel = new Label(MessageProvider.getMessage(getClass(), "logoLabel"));
        logoLabel.setStyleName("logo");//new style for logo
        Label loggedInLabel = new Label(String.format(MessageProvider.getMessage(getClass(), "loggedInLabel"),
                connection.getSession().getUser().getName()));
        loggedInLabel.setStyleName("logo");
        Button logoutBtn = new Button(MessageProvider.getMessage(getClass(), "logoutBtn"),
                new Button.ClickListener() {
                    public void buttonClick(Button.ClickEvent event) {
                        connection.logout();
                        String url = ActiveDirectoryHelper.useActiveDirectory() ? "login" : "";
                        open(new ExternalResource(App.getInstance().getURL() + url));
                    }
                }
        );
        logoutBtn.setStyleName("title");

        Button viewLogBtn = new Button(MessageProvider.getMessage(getClass(), "viewLogBtn"),
                new Button.ClickListener()
                {
                    public void buttonClick(Button.ClickEvent event) {
                        LogWindow logWindow = new LogWindow();
                        addWindow(logWindow);
                    }
                }
        );
        viewLogBtn.setStyleName("title");

        logoLabel.setSizeFull();

        titleLayout.addComponent(logoLabel);
        titleLayout.setExpandRatio(logoLabel, 2);

        titleLayout.addComponent(loggedInLabel);
        titleLayout.setExpandRatio(loggedInLabel, 1);
        
        titleLayout.addComponent(logoutBtn);
        titleLayout.addComponent(viewLogBtn);

        return titleLayout;
    }

    private void createMenuBarItem(MenuBar menuBar, MenuItem item) {
        if (!connection.isConnected()) return;

        final UserSession session = connection.getSession();
        if (item.isPermitted(session)) {
            MenuBar.MenuItem menuItem = menuBar.addItem(item.getCaption(), null);

            if (!item.getChildren().isEmpty()) {
                for (final MenuItem childItem : item.getChildren()) {
                    createMenuItem(menuItem, childItem);
                }
            }
            if (!menuItem.hasChildren()) {
                menuBar.removeItem(menuItem);
            }
        }
    }

    private void createMenuItem(MenuBar.MenuItem menuItem, MenuItem item) {
        if (!item.isPermitted(connection.getSession()))
            return;

        menuItem.addItem(item.getCaption(), createMenuBarCommand(item));

        if (!item.getChildren().isEmpty()) {
            for (final MenuItem childItem : item.getChildren()) {
                createMenuItem(menuItem, childItem);
            }
        }
    }

    private MenuBar.Command createMenuBarCommand(final MenuItem item) {
        return new MenuBar.Command() {
            public void menuSelected(MenuBar.MenuItem selectedItem) {
                String caption = item.getCaption();

                Map<String, Object> params = new HashMap<String, Object>();
                Element descriptor = item.getDescriptor();
                for (Element element : Dom4j.elements(descriptor, "param")) {
                    params.put(element.attributeValue("name"), element.attributeValue("value"));
                }
                params.put("caption", caption);

                final com.haulmont.cuba.gui.config.WindowConfig windowConfig = AppConfig.getInstance().getWindowConfig();
                WindowInfo windowInfo = windowConfig.getWindowInfo(item.getId());

                final String id = windowInfo.getId();
                if (id.endsWith(".create") || id.endsWith(".edit")) {
                    final String[] strings = id.split("[.]");
                    if (strings.length != 2) throw new UnsupportedOperationException();

                    final String metaClassName = strings[0];
                    final MetaClass metaClass = MetadataProvider.getSession().getClass(metaClassName);
                    if (metaClass == null) throw new IllegalStateException(String.format("Can't find metaClass %s", metaClassName));

                    Object newItem;
                    try {
                        newItem = metaClass.createInstance();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }

                    App.getInstance().getWindowManager().openEditor(
                            windowInfo,
                            newItem,
                            WindowManager.OpenType.NEW_TAB,
                            params
                    );
                } else {
                    App.getInstance().getWindowManager().openWindow(
                            windowInfo,
                            WindowManager.OpenType.NEW_TAB,
                            params
                    );
                }
            }
        };
    }
}
