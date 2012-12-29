/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import org.apache.commons.lang.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author artamonov
 * @version $Id$
 */
@Entity(name = "sys$JmxInstance")
@Table(name = "SYS_JMX_INSTANCE")
@NamePattern("#getCaption|clusterNodeName,address,description")
@SystemLevel
public class JmxInstance extends StandardEntity {

    @Column(name = "CLUSTER_NODE_NAME", length = 255, nullable = false)
    protected String clusterNodeName;

    @Column(name = "DESCRIPTION", length = 500)
    protected String description;

    @Column(name = "ADDRESS", length = 500, nullable = false)
    protected String address;

    @Column(name = "LOGIN", length = LOGIN_FIELD_LEN, nullable = false)
    protected String login;

    @Column(name = "PASSWORD", length = 255)
    protected String password;

    public JmxInstance() {
    }

    public JmxInstance(String clusterNodeName) {
        this.clusterNodeName = clusterNodeName;
    }

    public String getClusterNodeName() {
        return clusterNodeName;
    }

    public void setClusterNodeName(String clusterNodeName) {
        this.clusterNodeName = clusterNodeName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCaption() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(description))
            sb.append(description);
        else
            sb.append(clusterNodeName);

        if (StringUtils.isNotEmpty(address))
            sb.append(" (").append(address).append(")");

        return sb.toString();
    }
}