/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.ComponentVisitor;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.TestIdManager;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.data.impl.DsContextImplementation;
import com.haulmont.cuba.gui.settings.Settings;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.AppWindow;
import com.haulmont.cuba.web.toolkit.ui.CubaTabSheet;
import com.vaadin.ui.Layout;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author abramov
 * @version $Id$
 */
public class WebTabSheet extends WebAbstractComponent<CubaTabSheet> implements TabSheet, Component.Container {

    protected boolean postInitTaskAdded;
    protected boolean componentTabChangeListenerInitialized;

    protected ComponentLoader.Context context;
    protected Map<String, Tab> tabs = new HashMap<>();

    protected Map<com.vaadin.ui.Component, ComponentDescriptor> tabMapping = new LinkedHashMap<>();
    protected Map<String, Component> componentByIds = new HashMap<>();

    protected Set<com.vaadin.ui.Component> lazyTabs = new HashSet<>();

    protected Set<TabChangeListener> listeners = new HashSet<>();

    public WebTabSheet() {
        component = createComponent();
        component.setCloseHandler(new DefaultCloseHandler());
    }

    protected CubaTabSheet createComponent() {
        return new CubaTabSheet();
    }

    @Override
    public void add(Component component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(Component component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAll() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Component> T getOwnComponent(String id) {
        return (T) componentByIds.get(id);
    }

    @Nullable
    @Override
    public <T extends Component> T getComponent(String id) {
        return ComponentsHelper.getComponent(this, id);
    }

    @Nonnull
    @Override
    public <T extends Component> T getComponentNN(String id) {
        T component = getComponent(id);
        if (component == null) {
            throw new IllegalArgumentException(String.format("Not found component with id '%s'", id));
        }
        return component;
    }

    @Override
    public Collection<Component> getOwnComponents() {
        List<Component> componentList = new ArrayList<>();
        for (ComponentDescriptor cd : tabMapping.values()) {
            componentList.add(cd.component);
        }
        return componentList;
    }

    @Override
    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    protected class Tab implements TabSheet.Tab {

        private String name;
        private Component component;
        private TabCloseHandler closeHandler;

        public Tab(String name, Component component) {
            this.name = name;
            this.component = component;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getCaption() {
            return WebTabSheet.this.component.getTab(WebComponentsHelper.unwrap(component)).getCaption();
        }

        @Override
        public void setCaption(String caption) {
            WebTabSheet.this.component.getTab(WebComponentsHelper.unwrap(component)).setCaption(caption);
        }

        @Override
        public boolean isEnabled() {
            return WebTabSheet.this.component.getTab(WebComponentsHelper.unwrap(component)).isEnabled();
        }

        @Override
        public void setEnabled(boolean enabled) {
            WebTabSheet.this.component.getTab(WebComponentsHelper.unwrap(component)).setEnabled(enabled);
        }

        @Override
        public boolean isVisible() {
            return WebTabSheet.this.component.getTab(WebComponentsHelper.unwrap(component)).isVisible();
        }

        @Override
        public void setVisible(boolean visible) {
            WebTabSheet.this.component.getTab(WebComponentsHelper.unwrap(component)).setVisible(visible);
        }

        @Override
        public boolean isClosable() {
            com.vaadin.ui.TabSheet.Tab tab = WebTabSheet.this.component.getTab(WebComponentsHelper.unwrap(component));
            return tab.isClosable();
        }

        @Override
        public void setClosable(boolean closable) {
            com.vaadin.ui.TabSheet.Tab tab = WebTabSheet.this.component.getTab(WebComponentsHelper.unwrap(component));
            tab.setClosable(closable);
        }

        @Override
        public boolean isDetachable() {
            return false;
        }

        @Override
        public void setDetachable(boolean detachable) {
        }

        @Override
        public TabCloseHandler getCloseHandler() {
            return closeHandler;
        }

        @Override
        public void setCloseHandler(TabCloseHandler tabCloseHandler) {
            this.closeHandler = tabCloseHandler;
        }

        public Component getComponent() {
            return component;
        }

        @Override
        public void setCaptionStyleName(String styleName) {
            setStyleName(styleName);
        }

        @Override
        public void setStyleName(String styleName) {
            com.vaadin.ui.TabSheet.Tab vaadinTab = WebTabSheet.this.component.getTab(WebComponentsHelper.unwrap(component));
            vaadinTab.setStyleName(styleName);
        }

        @Override
        public String getStyleName() {
            com.vaadin.ui.TabSheet.Tab vaadinTab = WebTabSheet.this.component.getTab(WebComponentsHelper.unwrap(component));
            return vaadinTab.getStyleName();
        }
    }

    @Override
    public TabSheet.Tab addTab(String name, Component childComponent) {
        if (childComponent.getParent() != null && childComponent.getParent() != this) {
            throw new IllegalStateException("Component already has parent");
        }

        final Tab tab = new Tab(name, childComponent);

        this.tabs.put(name, tab);

        final com.vaadin.ui.Component tabComponent = WebComponentsHelper.unwrap(childComponent);
        tabComponent.setSizeFull();

        tabMapping.put(tabComponent, new ComponentDescriptor(name, childComponent));
        com.vaadin.ui.TabSheet.Tab tabControl = this.component.addTab(tabComponent);

        if (getDebugId() != null) {
            this.component.setTestId(tabControl,
                    AppUI.getCurrent().getTestIdManager().getTestId(getDebugId() + "." + name));
        }
        if (AppUI.getCurrent().isTestMode()) {
            this.component.setCubaId(tabControl, name);
        }

        if (childComponent.getId() != null) {
            componentByIds.put(childComponent.getId(), childComponent);
        }

        if (frame != null) {
            if (childComponent instanceof BelongToFrame
                    && ((BelongToFrame) childComponent).getFrame() == null) {
                ((BelongToFrame) childComponent).setFrame(frame);
            } else {
                frame.registerComponent(childComponent);
            }
        }

        childComponent.setParent(this);

        return tab;
    }

    @Override
    public void setDebugId(String id) {
        super.setDebugId(id);

        String debugId = getDebugId();
        if (debugId != null) {
            TestIdManager testIdManager = AppUI.getCurrent().getTestIdManager();

            for (com.vaadin.ui.Component tabComponent : tabMapping.keySet()) {
                com.vaadin.ui.TabSheet.Tab tab = component.getTab(tabComponent);
                ComponentDescriptor componentDescriptor = tabMapping.get(tabComponent);
                String name = componentDescriptor.name;

                component.setTestId(tab, testIdManager.getTestId(debugId + "." + name));
            }
        }
    }

    @Override
    public TabSheet.Tab addLazyTab(String name,
                                   Element descriptor,
                                   ComponentLoader loader) {
        WebVBoxLayout tabContent = new WebVBoxLayout();

        Layout layout = tabContent.getComponent();
        layout.setSizeFull();

        final Tab tab = new Tab(name, tabContent);

        tabs.put(name, tab);

        final com.vaadin.ui.Component tabComponent = WebComponentsHelper.unwrap(tabContent);
        tabComponent.setSizeFull();

        tabMapping.put(tabComponent, new ComponentDescriptor(name, tabContent));
        com.vaadin.ui.TabSheet.Tab tabControl = this.component.addTab(tabComponent);
        lazyTabs.add(tabComponent);

        this.component.addSelectedTabChangeListener(new LazyTabChangeListener(tabContent, descriptor, loader));
        context = loader.getContext();

        if (!postInitTaskAdded) {
            context.addPostInitTask(new ComponentLoader.PostInitTask() {
                @Override
                public void execute(ComponentLoader.Context context, IFrame window) {
                    initComponentTabChangeListener();
                }
            });
            postInitTaskAdded = true;
        }

        if (getDebugId() != null) {
            this.component.setTestId(tabControl,
                    AppUI.getCurrent().getTestIdManager().getTestId(getDebugId() + "." + name));
        }
        if (AppUI.getCurrent().isTestMode()) {
            this.component.setCubaId(tabControl, name);
        }

        tabContent.setFrame(context.getFrame());

        return tab;
    }

    @Override
    public void removeTab(String name) {
        final Tab tab = tabs.get(name);
        if (tab == null) {
            throw new IllegalStateException(String.format("Can't find tab '%s'", name));
        }
        tabs.remove(name);

        Component childComponent = tab.getComponent();
        com.vaadin.ui.Component vComponent = WebComponentsHelper.unwrap(childComponent);
        this.component.removeComponent(vComponent);

        if (childComponent.getId() != null) {
            componentByIds.remove(childComponent.getId());
        }
        tabMapping.remove(vComponent);

        childComponent.setParent(null);
    }

    @Override
    public void removeAllTabs() {
        tabMapping.clear();
        componentByIds.clear();
        component.removeAllComponents();

        List<Tab> currentTabs = new ArrayList<>(tabs.values());
        tabs.clear();

        for (Tab tab : currentTabs) {
            Component childComponent = tab.getComponent();

            childComponent.setParent(null);
        }
    }

    @Override
    public void setFrame(IFrame frame) {
        super.setFrame(frame);

        if (frame != null) {
            for (ComponentDescriptor descriptor : tabMapping.values()) {
                Component childComponent = descriptor.getComponent();
                if (childComponent instanceof BelongToFrame
                        && ((BelongToFrame) childComponent).getFrame() == null) {
                    ((BelongToFrame) childComponent).setFrame(frame);
                }
            }
        }
    }

    @Override
    public Tab getTab() {
        final com.vaadin.ui.Component component = this.component.getSelectedTab();
        if (component == null) {
            return null;
        }

        final String name = tabMapping.get(component).getName();
        return tabs.get(name);
    }

    @Override
    public void setTab(TabSheet.Tab tab) {
        this.component.setSelectedTab(WebComponentsHelper.unwrap(((Tab) tab).getComponent()));
    }

    @Override
    public void setTab(String name) {
        Tab tab = tabs.get(name);
        if (tab == null) {
            throw new IllegalStateException(String.format("Can't find tab '%s'", name));
        }

        this.component.setSelectedTab(WebComponentsHelper.unwrap(tab.getComponent()));
    }

    @Override
    public TabSheet.Tab getTab(String name) {
        return tabs.get(name);
    }

    @Override
    public Component getTabComponent(String name) {
        Tab tab = tabs.get(name);
        return tab.getComponent();
    }

    @Override
    public Collection<TabSheet.Tab> getTabs() {
        //noinspection unchecked
        return (Collection) tabs.values();
    }

    @Override
    public void addListener(TabChangeListener listener) {
        initComponentTabChangeListener();
        listeners.add(listener);
    }

    private void initComponentTabChangeListener() {
        // init component SelectedTabChangeListener only when needed, making sure it is
        // after all lazy tabs listeners
        if (!componentTabChangeListenerInitialized) {
            component.addSelectedTabChangeListener(new com.vaadin.ui.TabSheet.SelectedTabChangeListener() {
                @Override
                public void selectedTabChange(com.vaadin.ui.TabSheet.SelectedTabChangeEvent event) {
                    // Fire GUI listener
                    fireTabChanged();
                    // Execute outstanding post init tasks after GUI listener.
                    // We suppose that context.executePostInitTasks() executes a task once and then remove it from task list.
                    if (context != null) {
                        context.executePostInitTasks();
                    }

                    Window window = com.haulmont.cuba.gui.ComponentsHelper.getWindow(WebTabSheet.this);
                    if (window != null) {
                        ((DsContextImplementation) window.getDsContext()).resumeSuspended();
                    } else {
                        LogFactory.getLog(WebTabSheet.class).warn("Please specify Frame for TabSheet");
                    }
                }
            });
            componentTabChangeListenerInitialized = true;
        }
    }

    @Override
    public void removeListener(TabChangeListener listener) {
        listeners.remove(listener);
    }

    protected void fireTabChanged() {
        for (TabChangeListener listener : listeners) {
            listener.tabChanged(getTab());
        }
    }

    protected class LazyTabChangeListener implements com.vaadin.ui.TabSheet.SelectedTabChangeListener {
        protected WebAbstractBox tabContent;
        protected Element descriptor;
        protected ComponentLoader loader;

        public LazyTabChangeListener(WebAbstractBox tabContent, Element descriptor, ComponentLoader loader) {
            this.tabContent = tabContent;
            this.descriptor = descriptor;
            this.loader = loader;
        }

        @Override
        public void selectedTabChange(com.vaadin.ui.TabSheet.SelectedTabChangeEvent event) {
            com.vaadin.ui.Component selectedTab = WebTabSheet.this.component.getSelectedTab();
            com.vaadin.ui.Component tabComponent = tabContent.getComponent();
            if (selectedTab == tabComponent && lazyTabs.remove(tabComponent)) {
                Component comp = loader.loadComponent(AppConfig.getFactory(), descriptor, null);

                tabContent.add(comp);
                com.vaadin.ui.Component impl = WebComponentsHelper.getComposition(comp);
                impl.setSizeFull();

                comp.setParent(WebTabSheet.this);

                final Window window = com.haulmont.cuba.gui.ComponentsHelper.getWindow(WebTabSheet.this);
                if (window != null) {
                    com.haulmont.cuba.gui.ComponentsHelper.walkComponents(
                            tabContent,
                            new ComponentVisitor() {
                                @Override
                                public void visit(Component component, String name) {
                                    if (component instanceof HasSettings) {
                                        Settings settings = window.getSettings();
                                        if (settings != null) {
                                            Element e = settings.get(name);
                                            ((HasSettings) component).applySettings(e);
                                        }
                                    }
                                }
                            }
                    );

                    // init debug ids after all
                    if (AppUI.getCurrent().isTestMode()) {
                        context.addPostInitTask(new ComponentLoader.PostInitTask() {
                            @Override
                            public void execute(ComponentLoader.Context context, IFrame window) {
                                AppWindow appWindow = AppUI.getCurrent().getAppWindow();
                                appWindow.getWindowManager().initDebugIds(window);
                            }
                        });
                    }
                }
            }
        }
    }

    protected static class ComponentDescriptor {
        protected Component component;

        protected String name;

        public ComponentDescriptor(String name, Component component) {
            this.name = name;
            this.component = component;
        }

        public Component getComponent() {
            return component;
        }

        public String getName() {
            return name;
        }
    }

    protected class DefaultCloseHandler implements com.vaadin.ui.TabSheet.CloseHandler {
        private static final long serialVersionUID = -6766617382191585632L;

        @Override
        public void onTabClose(com.vaadin.ui.TabSheet tabsheet, com.vaadin.ui.Component tabContent) {
            // have no other way to get tab from tab content
            for (Tab tab : tabs.values()) {
                com.vaadin.ui.Component tabComponent = WebComponentsHelper.unwrap(tab.getComponent());
                if (tabComponent == tabContent) {
                    if (tab.isClosable()) {
                        doHandleCloseTab(tab);
                        return;
                    }
                }
            }
        }

        protected void doHandleCloseTab(Tab tab) {
            if (tab.getCloseHandler() != null) {
                tab.getCloseHandler().onTabClose(tab);
            } else {
                removeTab(tab.getName());
            }
        }
    }
}