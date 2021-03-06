/*-
 * ========================LICENSE_START=================================
 * Smooks Commons
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 * 
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 * 
 * ======================================================================
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * ======================================================================
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.javabean.pojogen;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import org.smooks.assertion.AssertArgument;
import org.smooks.io.StreamUtils;
import org.smooks.util.FreeMarkerTemplate;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.*;

/**
 * Java POJO model.
 * @author bardl
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public class JClass {

    private final String uniqueId;
    private final String packageName;
    private final String className;
    private final Set<JType> rawImports = new LinkedHashSet<JType>();
    private final Set<JType> implementTypes = new LinkedHashSet<JType>();
    private final Set<JType> extendTypes = new LinkedHashSet<JType>();
    private final Set<JType> annotationTypes = new LinkedHashSet<JType>();
    private Class<?> skeletonClass;
    private final List<JNamedType> properties = new ArrayList<JNamedType>();
    private final List<JMethod> constructors = new ArrayList<JMethod>();
    private final List<JMethod> methods = new ArrayList<JMethod>();
    private boolean fluentSetters = true;
    private boolean serializable = false;
    private boolean finalized = false;

    private static final FreeMarkerTemplate template;

    static {
        try {
            template = new FreeMarkerTemplate(StreamUtils.readStreamAsString(JClass.class.getResourceAsStream("JavaClass.ftl"), "UTF-8"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load JavaClass.ftl FreeMarker template.", e);
        }
    }

    public JClass(String packageName, String className) {
        this(packageName, className, UUID.randomUUID().toString());
    }

    public JClass(String packageName, String className, String uniqueId) {
        AssertArgument.isNotNull(packageName, "packageName");
        AssertArgument.isNotNull(className, "className");
        AssertArgument.isNotNull(uniqueId, "uniqueId");
        this.packageName = packageName;
        this.className = className;
        this.uniqueId = uniqueId;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Set<JType> getRawImports() {
        return rawImports;
    }

    public Set<JType> getImplementTypes() {
        return implementTypes;
    }

    public Set<JType> getExtendTypes() {
        return extendTypes;
    }

    public Set<JType> getAnnotationTypes() {
        return annotationTypes;
    }

    public void setFluentSetters(boolean fluentSetters) {
        this.fluentSetters = fluentSetters;
    }

    public Class<?> getSkeletonClass() {
        if(skeletonClass == null) {
            String skeletonClassName = packageName + "." + className;

            try {
                skeletonClass = Thread.currentThread().getContextClassLoader().loadClass(skeletonClassName);
            } catch (ClassNotFoundException e) {
                ClassPool pool = new ClassPool(true);
                CtClass cc = pool.makeClass(skeletonClassName);

                try {
                    skeletonClass = cc.toClass();
                } catch (CannotCompileException ee) {
                    throw new IllegalStateException("Unable to create runtime skeleton class for class '" + skeletonClassName + "'.", ee);
                } finally {
                    cc.detach();
                }
            }
        }
        
        return skeletonClass;
    }

    public JClass setSerializable() {
        this.serializable = true;
        implementTypes.add(new JType(Serializable.class));
        return this;
    }

    public boolean isSerializable() {
        return serializable;
    }

    public void addProperty(JNamedType property) {
        AssertArgument.isNotNull(property, "property");
        assertPropertyUndefined(property);

        properties.add(property);
    }

    public JClass addBeanProperty(JNamedType property) {
        addProperty(property);

        String propertyName = property.getName();
        String capitalizedPropertyName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

        // Add property getter method...
        JMethod getterMethod = new JMethod(property.getType(), "get" + capitalizedPropertyName);
        getterMethod.appendToBody("return " + property.getName() + ";");
        methods.add(getterMethod);

        // Add property setter method...
        if(fluentSetters) {
            JMethod setterMethod = new JMethod(new JType(getSkeletonClass()), "set" + capitalizedPropertyName);
            setterMethod.addParameter(property);
            setterMethod.appendToBody("this." + property.getName() + " = " + property.getName() + ";  return this;");
            methods.add(setterMethod);
        } else {
            JMethod setterMethod = new JMethod("set" + capitalizedPropertyName);
            setterMethod.addParameter(property);
            setterMethod.appendToBody("this." + property.getName() + " = " + property.getName() + ";");
            methods.add(setterMethod);
        }

        return this;
    }

    public List<JNamedType> getProperties() {
        return properties;
    }

    public List<JMethod> getConstructors() {
        return constructors;
    }

    public List<JMethod> getMethods() {
        return methods;
    }

    public JMethod getDefaultConstructor() {
        for(JMethod constructor : constructors) {
            if(constructor.getParameters().isEmpty()) {
                return constructor;
            }
        }

        JMethod constructor = new JMethod(getClassName());
        constructors.add(constructor);

        return constructor;
    }

    public Set<Class<?>> getImports() {
        Set<Class<?>> importSet = new LinkedHashSet<Class<?>>();

        addImports(importSet, implementTypes);
        addImports(importSet, extendTypes);
        addImports(importSet, annotationTypes);
        for(JNamedType property : properties) {
            property.getType().addImports(importSet, new String[] {"java.lang", packageName});
        }
        addMethodImportData(constructors, importSet);
        addMethodImportData(methods, importSet);
        addImports(importSet, rawImports);

        return importSet;
    }

    private void addImports(Set<Class<?>> importSet, Collection<JType> types) {
        for(JType property : types) {
            property.addImports(importSet, new String[] {"java.lang", packageName});
        }
    }

    private void addMethodImportData(List<JMethod> methodList, Set<Class<?>> importSet) {
        for(JMethod method : methodList) {
            method.getReturnType().addImports(importSet, new String[] {"java.lang", packageName});
            for(JNamedType param : method.getParameters()) {
                param.getType().addImports(importSet, new String[] {"java.lang", packageName});
            }
            for(JType exception : method.getExceptions()) {
                exception.addImports(importSet, new String[] {"java.lang", packageName});
            }
        }
    }

    public String getImplementsDecl() {
        return PojoGenUtil.getTypeDecl("implements", implementTypes);
    }

    public String getExtendsDecl() {
        return PojoGenUtil.getTypeDecl("extends", extendTypes);
    }

    public void writeClass(Writer writer) throws IOException {
        Map<String, JClass> contextObj = new HashMap<String, JClass>();

        contextObj.put("class", this);
        writer.write(template.apply(contextObj));

        // Finalize all the methods... allowing them to be GC'd...
        finalizeMethods(constructors);
        finalizeMethods(methods);
        finalized = true;
    }

    public boolean isFinalized() {
        return finalized;
    }

    private void finalizeMethods(List<JMethod> methodList) {
        for(JMethod method : methodList) {
            method.finalizeMethod();
        }
    }

    private void assertPropertyUndefined(JNamedType property) {
        if(hasProperty(property.getName())) {
            throw new IllegalArgumentException("Property '" + property.getName() + "' already defined.");
        }
    }

    public boolean hasProperty(String propertyName) {
        for(JNamedType definedProperty : properties) {
            if(definedProperty.getName().equals(propertyName)) {
                return true;
            }
        }

        return false;
    }
}
