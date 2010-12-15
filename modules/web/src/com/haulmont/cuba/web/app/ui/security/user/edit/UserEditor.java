/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 05.02.2009 13:35:20
 * $Id$
 */
package com.haulmont.cuba.web.app.ui.security.user.edit;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.config.PermissionConfig;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.security.app.UserSessionService;
import com.haulmont.cuba.security.entity.*;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.app.ui.security.user.NameBuilderListener;
import com.haulmont.cuba.web.app.ui.security.role.edit.PermissionsLookup;
import com.haulmont.cuba.web.gui.components.WebLookupField;
import com.haulmont.cuba.web.gui.components.WebPopupButton;
import com.haulmont.cuba.web.gui.components.WebTextField;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.List;

public class UserEditor extends AbstractEditor {

    private Datasource<User> userDs;
    private Table rolesTable;
    private Table substTable;
    protected TextField passwField;
    protected TextField confirmPasswField;
    protected LookupField languageLookup;
    protected PopupButton popupButton;

    public UserEditor(Window frame) {
        super(frame);
    }                                                                                                  

    protected void init(Map<String, Object> params) {

        userDs = getDsContext().get("user");
        userDs.addListener(new NameBuilderListener((FieldGroup) getComponent("fields")));

        rolesTable = getComponent("roles");
        rolesTable.addAction(new AddRoleAction());
        rolesTable.addAction(new EditRoleAction());
        TableActionsHelper rolesTableActions = new TableActionsHelper(this, rolesTable);
        rolesTableActions.createRemoveAction(false);

        substTable = getComponent("subst");
        substTable.addAction(new AddSubstitutedAction());
        substTable.addAction(new EditSubstitutedAction());
        TableActionsHelper substTableActions = new TableActionsHelper(this, substTable);
        substTableActions.createRemoveAction(false);


        setPermissionsShowAction(rolesTable, "show-screens", "sec$Target.screenPermissions.lookup", PermissionType.SCREEN);
        setPermissionsShowAction(rolesTable, "show-entities", "sec$Target.entityPermissions.lookup", PermissionType.ENTITY_OP);
        setPermissionsShowAction(rolesTable, "show-properties", "sec$Target.propertyPermissions.lookup", PermissionType.ENTITY_ATTR);
        setPermissionsShowAction(rolesTable, "show-specific", "sec$Target.specificPermissions.lookup", PermissionType.SPECIFIC);

        initCustomFields();

        getDsContext().addListener(
                new DsContext.CommitListener() {
                    public void beforeCommit(CommitContext<Entity> context) {
                    }

                    public void afterCommit(CommitContext<Entity> context, Map<Entity, Entity> result) {
                        UserSession us = UserSessionClient.getUserSession();
                        for (Map.Entry<Entity, Entity> entry : result.entrySet()) {
                            if (entry.getKey().equals(us.getUser())) {
                                us.setUser((User) entry.getValue());
                            }
                            if (entry.getKey().equals(us.getSubstitutedUser())) {
                                us.setSubstitutedUser((User) entry.getValue());
                            }
                        }
                    }
                }
        );
    }

    @Override
    public void setItem(Entity item) {
        super.setItem(item);
        if (PersistenceHelper.isNew(item)) {
            addDefaultRoles();

            languageLookup.setValue(UserSessionClient.getUserSession().getLocale().getLanguage());
        }
    }

    private void addDefaultRoles() {
        CollectionDatasource<UserRole, UUID> ds = rolesTable.getDatasource();
        LoadContext ctx = new LoadContext(Role.class);
        ctx.setQueryString("select r from sec$Role r where r.defaultRole = true");
        List<Role> defaultRoles = getDsContext().getDataService().loadList(ctx);

        for (Role role : defaultRoles) {
            final MetaClass metaClass = ds.getMetaClass();
            UserRole userRole = ds.getDataService().newInstance(metaClass);
            userRole.setRole(role);
            userRole.setUser(userDs.getItem());
            ds.addItem(userRole);
        }
    }

    private void initCustomFields() {
        final FieldGroup fields = getComponent("fields");

        FieldGroup.Field f = fields.getField("permissionsLookupField");
        fields.addCustomField(f, new FieldGroup.CustomFieldGenerator() {
            public Component generateField(Datasource datasource, Object propertyId) {
                popupButton = new WebPopupButton();
                popupButton.setCaption(getMessage("permissions"));
                popupButton.addAction(new PermissionLookupAction("screens",getMessage("screens"),"show-screens"));
                popupButton.addAction(new PermissionLookupAction("entities",getMessage("entities"),"show-entities"));
                popupButton.addAction(new PermissionLookupAction("properties",getMessage("properties"),"show-properties"));
                popupButton.addAction(new PermissionLookupAction("specific",getMessage("specific"),"show-specific"));

                return popupButton;
//                final LookupField lookupField = new WebLookupField();
//
//                java.util.Map<String, Object> optionsMap = new HashMap<String, Object>();
//
//                optionsMap.put(getMessage("screens"), "show-screens");
//                optionsMap.put(getMessage("entities"), "show-entities");
//                optionsMap.put(getMessage("properties"), "show-properties");
//                optionsMap.put(getMessage("specific"), "show-specific");
//
//                lookupField.setOptionsMap(optionsMap);
//
//                lookupField.addListener(new ValueListener() {
//                    public void valueChanged(Object source, String property, Object prevValue, Object value) {
//                        if (value == null) return;
//                        rolesTable.getAction((String)value).actionPerform(rolesTable);
//                        lookupField.setValue(null);
//                    }
//                });
//                return lookupField;
            }
        });

        f = fields.getField("passw");
        if (f != null) {
            fields.addCustomField(f, new FieldGroup.CustomFieldGenerator() {
                public Component generateField(Datasource datasource, Object propertyId) {
                    passwField = new WebTextField();
                    passwField.setRequiredMessage(getMessage("passwMsg"));
                    passwField.setSecret(true);
                    passwField.setRequired(!ConfigProvider.getConfig(WebConfig.class).getUseActiveDirectory());
                    return passwField;
                }
            });
        }

        f = fields.getField("confirmPassw");
        if (f != null) {
            fields.addCustomField(f, new FieldGroup.CustomFieldGenerator() {
                public Component generateField(Datasource datasource, Object propertyId) {
                    confirmPasswField = new WebTextField();
                    confirmPasswField.setSecret(true);
                    confirmPasswField.setRequiredMessage(getMessage("confirmPasswMsg"));
                    confirmPasswField.setRequired(!ConfigProvider.getConfig(WebConfig.class).getUseActiveDirectory());
                    return confirmPasswField;
                }
            });
        }

        f = fields.getField("language");
        fields.addCustomField(f, new FieldGroup.CustomFieldGenerator() {
            public Component generateField(Datasource datasource, Object propertyId) {
                languageLookup = new WebLookupField();

                Map<String, Locale> locales = ConfigProvider.getConfig(WebConfig.class).getAvailableLocales();
                TreeMap<String, Object> options = new TreeMap<String, Object>();
                for (Map.Entry<String, Locale> entry : locales.entrySet()) {
                    options.put(entry.getKey(), entry.getValue().getLanguage());
                }
                languageLookup.setOptionsMap(options);

                return languageLookup;
            }
        });
    }


    private void setPermissionsShowAction(ActionsHolder actionsHolder, String actionName, final String lookupAlias, final PermissionType permissionType) {
        actionsHolder.addAction(new AbstractAction(actionName) {
            public void actionPerform(Component component) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("showAccessOptions", false);
                final PermissionsLookup permissionsLookup = openLookup(lookupAlias, null, WindowManager.OpenType.THIS_TAB, params);
                permissionsLookup.setLookupHandler(new Lookup.Handler() {
                    public void handleLookup(Collection items) {
                        if(items.size()==0)
                            return;
                        StringBuilder sb = new StringBuilder();
                        UserSessionService uss = ServiceLocator.lookup(UserSessionService.JNDI_NAME);
                        for (Object item : items) {
                            if (item == null) continue;
                            PermissionConfig.Target target = (PermissionConfig.Target)item;
                            Integer permissionValue =  uss.getPermissionValue(userDs.getItem(), permissionType, target.getValue());
                            String permissionStringValue = "";
                            if (permissionType == PermissionType.ENTITY_ATTR) {
                                if (permissionValue == null) permissionValue = 2;
                                permissionStringValue = EntityAttrAccess.fromId(permissionValue).toString();
                            } else {
                                if (permissionValue == null) permissionValue = 1;
                                permissionStringValue = (permissionValue == 1) ? "ALLOW" : "DENY";
                            }
                            sb.append(getMessage("permissionOn") +" "+ target.getValue()+" ("+target.getCaption()+")"+ " - ")
                                    .append(getMessage(permissionStringValue)).append("\n");
                        }
                        if (sb.length() == 0) {
                            showNotification("Please, ensure you've selected target attributes", NotificationType.WARNING);
                        } else
                            openWindow("sec$Permission.show", WindowManager.OpenType.DIALOG,
                                    Collections.<String,Object>singletonMap("message",sb.toString()));
                            //showNotification(sb.toString(), NotificationType.HUMANIZED);
                        if(popupButton != null)
                            popupButton.setPopupVisible(false);
                    }
                });
            }
        });
    }

    private boolean _commit() {
        DatasourceImplementation rolesDs = (DatasourceImplementation) rolesTable.getDatasource();
        if (rolesTable.getDatasource().isModified()) {
            CommitContext ctx = new CommitContext(Collections.emptyList(), rolesDs.getItemsToDelete());
            getDsContext().getDataService().commit(ctx);

            ArrayList modifiedRoles = new ArrayList(rolesDs.getItemsToCreate());
            modifiedRoles.addAll(rolesDs.getItemsToUpdate());
            rolesDs.commited(Collections.<Entity, Entity>emptyMap());
            for (Object userRole : modifiedRoles) {
                rolesDs.modified((Entity) userRole);
            }
        }

        boolean isNew = PersistenceHelper.isNew(userDs.getItem());
        if (isNew) {
            String passw = passwField.getValue();
            String confPassw = confirmPasswField.getValue();
            if (ObjectUtils.equals(passw, confPassw)) {
                if (StringUtils.isEmpty(passw))
                    userDs.getItem().setPassword(null);
                else
                    userDs.getItem().setPassword(DigestUtils.md5Hex(passw));
                return true;
            } else {
                showNotification(getMessage("passwordsDoNotMatch"), NotificationType.WARNING);
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean commit() {
        return _commit() && super.commit();
    }

    @Override
    public void commitAndClose() {
        if (_commit()) {
            super.commitAndClose();
        }
    }

    private class AddRoleAction extends AbstractAction {

        public AddRoleAction() {
            super("add");
        }

        public void actionPerform(Component component) {
            Map<String, Object> lookupParams = Collections.<String, Object>singletonMap("windowOpener", "sec$User.edit");
            final CollectionDatasource<UserRole, UUID> ds = rolesTable.getDatasource();
            openLookup("sec$Role.browse", new Lookup.Handler() {
                public void handleLookup(Collection items) {
                    Collection<String> existingRoleNames = getExistingRoleNames();
                    for (Object item : items) {
                        Role role = (Role)item;
                        if (existingRoleNames.contains(role.getName())) continue;

                        final MetaClass metaClass = ds.getMetaClass();
                        UserRole userRole = ds.getDataService().newInstance(metaClass);
                        userRole.setRole(role);
                        userRole.setUser(userDs.getItem());

                        ds.addItem(userRole);
                        existingRoleNames.add(role.getName());
                    }
                }

                private Collection<String> getExistingRoleNames() {
                    User user = userDs.getItem();
                    Collection<String> existingRoleNames = new HashSet<String>();
                    if (user.getUserRoles() != null) { 
                        for (UserRole userRole : user.getUserRoles()) {
                            if (userRole.getRole() != null)
                                existingRoleNames.add(userRole.getRole().getName());
                        }
                    }
                    return existingRoleNames;
                }

            }, WindowManager.OpenType.THIS_TAB, lookupParams);
        }
    }

    private class EditRoleAction extends AbstractAction {

        public EditRoleAction() {
            super("edit");
        }

        public void actionPerform(Component component) {
            Map<String, Object> lookupParams = Collections.<String, Object>singletonMap("windowOpener", "sec$User.edit");
            final CollectionDatasource<UserRole, UUID> ds = rolesTable.getDatasource();
            if (ds.getItem() == null) return;
            Window window = openEditor("sec$Role.edit", ds.getItem().getRole(), WindowManager.OpenType.THIS_TAB);
            window.addListener(new CloseListener() {
                public void windowClosed(String actionId) {
                    if (Window.COMMIT_ACTION_ID.equals(actionId)) {
                        ds.refresh();
                    }
                }
            });
        }

        @Override
        public String getCaption() {
            return getMessage("actions.Edit");
        }
    }

    private class AddSubstitutedAction extends AbstractAction {
        public AddSubstitutedAction() {
            super("add");
        }

        public void actionPerform(Component component) {
            final UserSubstitution substitution = EntityFactory.create(UserSubstitution.class);
            substitution.setUser(userDs.getItem());

            final CollectionDatasource<UserSubstitution, UUID> usDs = getDsContext().get("substitutions");

            Map<String, Object> params = new HashMap();

            if (!usDs.getItemIds().isEmpty()) {
                List<UUID> list = new ArrayList();
                for (UUID usId : usDs.getItemIds()) {
                    list.add(usDs.getItem(usId).getSubstitutedUser().getId());
                }
                params.put("existingIds", list);
            }

            getDialogParams().setWidth(450);

            openEditor("sec$UserSubstitution.edit", substitution,
                    WindowManager.OpenType.DIALOG, params, usDs);
        }
    }

    private class EditSubstitutedAction extends AbstractAction {
        public EditSubstitutedAction() {
            super("edit");
        }

        public void actionPerform(Component component) {
            final CollectionDatasource<UserSubstitution, UUID> usDs = substTable.getDatasource();

            getDialogParams().setWidth(450);

            if (usDs.getItem() != null)
                openEditor("sec$UserSubstitution.edit", usDs.getItem(),
                        WindowManager.OpenType.DIALOG, usDs);
        }
    }

    private class PermissionLookupAction extends AbstractAction{

        private String caption;
        private String screen;

        private PermissionLookupAction(String id, String caption, String screen) {
            super(id);
            this.caption = caption;
            this.screen = screen;
        }

        public void actionPerform(Component component) {
            rolesTable.getAction(screen).actionPerform(rolesTable);
        }

        @Override
        public String getCaption() {
            return caption;
        }
    }
}
