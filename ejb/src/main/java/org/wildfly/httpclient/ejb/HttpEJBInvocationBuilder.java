/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.httpclient.ejb;

import static org.wildfly.httpclient.ejb.EjbConstants.INVOCATION_ACCEPT;
import static org.wildfly.httpclient.ejb.EjbConstants.INVOCATION_ID;
import static org.wildfly.httpclient.ejb.EjbConstants.INVOCATION;
import static org.wildfly.httpclient.ejb.EjbConstants.SESSION_OPEN;
import static org.wildfly.httpclient.ejb.EjbConstants.EJB_CANCEL_PATH;
import static org.wildfly.httpclient.ejb.EjbConstants.EJB_EXCEPTION;
import static org.wildfly.httpclient.ejb.EjbConstants.EJB_INVOKE_PATH;
import static org.wildfly.httpclient.ejb.EjbConstants.EJB_OPEN_PATH;

import io.undertow.client.ClientRequest;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.wildfly.httpclient.common.Protocol;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


/**
 * Builder for invocations against a specific EJB, such as invocation and session open
 *
 * @author Stuart Douglas
 */
class HttpEJBInvocationBuilder {

    private String appName;
    private String moduleName;
    private String distinctName;
    private String beanName;
    private String beanId;
    private String view;
    private Method method;
    private InvocationType invocationType;
    private String invocationId;
    private int version = Protocol.LATEST;
    private boolean cancelIfRunning;

    public String getAppName() {
        return appName;
    }

    public HttpEJBInvocationBuilder setAppName(String appName) {
        this.appName = appName;
        return this;
    }

    public String getModuleName() {
        return moduleName;
    }

    public HttpEJBInvocationBuilder setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    public String getDistinctName() {
        return distinctName;
    }

    public HttpEJBInvocationBuilder setDistinctName(String distinctName) {
        this.distinctName = distinctName;
        return this;
    }

    public String getBeanName() {
        return beanName;
    }

    public HttpEJBInvocationBuilder setBeanName(String beanName) {
        this.beanName = beanName;
        return this;
    }

    public String getBeanId() {
        return beanId;
    }

    public HttpEJBInvocationBuilder setBeanId(String beanId) {
        this.beanId = beanId;
        return this;
    }

    public Method getMethod() {
        return method;
    }

    public HttpEJBInvocationBuilder setMethod(Method method) {
        this.method = method;
        return this;
    }

    public String getView() {
        return view;
    }

    public HttpEJBInvocationBuilder setView(String view) {
        this.view = view;
        return this;
    }

    public InvocationType getInvocationType() {
        return invocationType;
    }

    public HttpEJBInvocationBuilder setInvocationType(InvocationType invocationType) {
        this.invocationType = invocationType;
        return this;
    }

    public String getInvocationId() {
        return invocationId;
    }

    public HttpEJBInvocationBuilder setInvocationId(String invocationId) {
        this.invocationId = invocationId;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public HttpEJBInvocationBuilder setVersion(int version) {
        this.version = version;
        return this;
    }

    /**
     * Constructs an EJB invocation path
     *
     * @param mountPoint   The mount point of the EJB context
     * @return The request path to invoke
     */
    private String buildPath(final String mountPoint) {
        StringBuilder sb = new StringBuilder();
        buildBeanPath(mountPoint, EJB_OPEN_PATH, appName, moduleName, distinctName, beanName, sb);
        return sb.toString();
    }

    /**
     * Constructs an EJB invocation path
     *
     * @param mountPoint   The mount point of the EJB context
     * @return The request path to invoke
     */
    private String buildPath(final String mountPoint, String invocationId, boolean cancelIfRunning) {
        StringBuilder sb = new StringBuilder();
        buildBeanPath(mountPoint, EJB_CANCEL_PATH, appName, moduleName, distinctName, beanName, sb);
        sb.append("/");
        sb.append(invocationId);
        sb.append("/");
        sb.append(cancelIfRunning);
        return sb.toString();
    }

    /**
     * Constructs an EJB invocation path
     *
     * @param mountPoint   The mount point of the EJB context
     * @param beanId       The bean id
     * @return The request path to invoke
     */
    private String buildPath(final String mountPoint, final String beanId, final String view, final Method method) {
        StringBuilder sb = new StringBuilder();
        buildBeanPath(mountPoint, EJB_INVOKE_PATH, appName, moduleName, distinctName, beanName, sb);
        sb.append("/");
        if (beanId == null) {
            sb.append("-");
        } else {
            sb.append(beanId);
        }
        sb.append("/");
        sb.append(view);
        sb.append("/");
        sb.append(method.getName());
        for (final Class<?> param : method.getParameterTypes()) {
            sb.append("/");
            sb.append(encodeUrlPart(param.getName()));
        }
        return sb.toString();
    }

    private void buildBeanPath(String mountPoint, String type, String appName, String moduleName, String distinctName, String beanName, StringBuilder sb) {
        buildModulePath(mountPoint, type, appName, moduleName, distinctName, sb);
        sb.append("/");
        sb.append(encodeUrlPart(beanName));
    }

    private void buildModulePath(String mountPoint, String type, String appName, String moduleName, String distinctName, StringBuilder sb) {
        if (mountPoint != null) {
            sb.append(mountPoint);
        }
        sb.append("/ejb/v").append(version).append("/").append(type);
        appendPath(sb, appName);
        appendPath(sb, moduleName);
        appendPath(sb, distinctName);
    }

    private static void appendPath(final StringBuilder sb, final String subPath) {
        sb.append("/").append(subPath == null || subPath.isEmpty() ? "-" : encodeUrlPart(subPath));
    }

    private static String encodeUrlPart(final String part) {
        return URLEncoder.encode(part, StandardCharsets.UTF_8);
    }

    public ClientRequest createRequest(final String mountPoint) {
        ClientRequest clientRequest = new ClientRequest();
        if (invocationType == InvocationType.METHOD_INVOCATION) {
            clientRequest.setMethod(Methods.POST);
            clientRequest.setPath(buildPath(mountPoint, beanId, view, method));
            clientRequest.getRequestHeaders().add(Headers.ACCEPT, INVOCATION_ACCEPT + "," + EJB_EXCEPTION);
            if (invocationId != null) {
                clientRequest.getRequestHeaders().put(INVOCATION_ID, invocationId);
            }
            clientRequest.getRequestHeaders().put(Headers.CONTENT_TYPE, INVOCATION.toString());
        } else if (invocationType == InvocationType.STATEFUL_CREATE) {
            clientRequest.setMethod(Methods.POST);
            clientRequest.setPath(buildPath(mountPoint));
            clientRequest.getRequestHeaders().put(Headers.CONTENT_TYPE, SESSION_OPEN.toString());
            clientRequest.getRequestHeaders().add(Headers.ACCEPT, EJB_EXCEPTION.toString());
        } else if(invocationType == InvocationType.CANCEL) {
            clientRequest.setMethod(Methods.DELETE);
            clientRequest.setPath(buildPath(mountPoint, invocationId, cancelIfRunning));
        }
        return clientRequest;
    }

    public HttpEJBInvocationBuilder setCancelIfRunning(boolean cancelIfRunning) {
        this.cancelIfRunning = cancelIfRunning;
        return this;
    }

    public boolean isCancelIfRunning() {
        return cancelIfRunning;
    }


    public enum InvocationType {
        METHOD_INVOCATION,
        STATEFUL_CREATE,
        CANCEL,
    }

}
