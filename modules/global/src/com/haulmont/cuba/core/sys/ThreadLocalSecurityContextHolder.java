/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class ThreadLocalSecurityContextHolder
        extends ThreadLocal<SecurityContext>
        implements SecurityContextHolder
{
}
