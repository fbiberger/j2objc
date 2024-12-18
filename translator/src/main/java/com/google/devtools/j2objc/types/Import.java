/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc.types;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.devtools.j2objc.Options;
import com.google.devtools.j2objc.util.ElementUtil;
import com.google.devtools.j2objc.util.NameTable;
import com.google.devtools.j2objc.util.TranslationEnvironment;
import com.google.devtools.j2objc.util.TypeUtil;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

/**
 * Description of an imported type. Imports are equal if their fully qualified
 * type names are equal.
 *
 * @author Tom Ball
 */
public class Import implements Comparable<Import> {

  private final String typeName;
  private final String importFileName;
  private final String forwardDeclaration;
  private final String javaQualifiedName;
  private final boolean isInterface;
  private final boolean hasGenerateObjectiveCGenerics;
  private final List<String> parameterNamesForObjectiveCGenerics;

  private Import(
      String typeName,
      String importFileName,
      String forwardDeclaration,
      String javaQualifiedName,
      boolean isInterface,
      boolean hasGenerateObjectiveCGenerics,
      List<String> parameterNamesForObjectiveCGenerics) {
    this.typeName = typeName;
    this.importFileName = importFileName;
    this.forwardDeclaration = forwardDeclaration;
    this.javaQualifiedName = javaQualifiedName;
    this.isInterface = isInterface;
    this.hasGenerateObjectiveCGenerics = hasGenerateObjectiveCGenerics;
    this.parameterNamesForObjectiveCGenerics = parameterNamesForObjectiveCGenerics;
  }

  public static Import newImport(TypeElement type, NameTable nameTable, Options options) {
    TypeElement mainType = type;
    while (!ElementUtil.isTopLevel(mainType)) {
      mainType = ElementUtil.getDeclaringClass(mainType);
    }

    return new Import(
        nameTable.getFullName(type),
        options.getHeaderMap().get(mainType),
        ElementUtil.getForwardDeclaration(mainType),
        ElementUtil.isIosType(mainType) ? null : ElementUtil.getQualifiedName(mainType),
        type.getKind().isInterface(),
        TypeUtil.hasGenerateObjectiveCGenerics(type),
        nameTable.getClassObjCGenericTypeNames(type.asType()));
  }

  public static Import newNativeImport(
      String typeName, String importFileName, String forwardDeclaration) {
    // For a native type that is an interface use GeneratedTypeElement.
    return new Import(
        typeName, importFileName, forwardDeclaration, null, false, false, ImmutableList.of());
  }

  /**
   * Gets the Objective-C name of the imported type.
   */
  public String getTypeName() {
    return typeName;
  }

  public boolean hasGenerateObjectiveCGenerics() {
    return hasGenerateObjectiveCGenerics;
  }

  /** Gets the Objective-C name of type's generic parameters. */
  public List<String> getParameterNamesForObjectiveCGenerics() {
    return parameterNamesForObjectiveCGenerics;
  }

  /**
   * Gets the header file to import for this type. An empty import file indicates a Foundation type
   * that doesn't require an import.
   */
  public String getImportFileName() {
    return importFileName;
  }

  /**
   * Gets the custom forward declaration for this type if not imported by file. An empty forward
   * declaration indicates a type that doesn't require a forward declaration. Note, the declaration
   * does not include a trailing semicolon.
   */
  public String getForwardDeclaration() {
    return forwardDeclaration;
  }

  /**
   * Gets the Java qualified name of the type, or null if it's an IOS type.
   */
  public String getJavaQualifiedName() {
    return javaQualifiedName;
  }

  public boolean isInterface() {
    return isInterface;
  }

  @Override
  public int compareTo(Import other) {
    return typeName.compareTo(other.typeName);
  }

  @Override
  public int hashCode() {
    return typeName.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Import other = (Import) obj;
    return typeName.equals(other.typeName);
  }

  @Override
  public String toString() {
    return typeName;
  }

  public static Set<Import> getImports(TypeMirror type, TranslationEnvironment env) {
    Set<Import> result = Sets.newLinkedHashSet();
    addImports(type, result, env);
    return result;
  }

  public static void addImports(
      TypeMirror type, Collection<Import> imports, TranslationEnvironment env) {
    if (type instanceof PointerType) {
      addImports(((PointerType) type).getPointeeType(), imports, env);
    }
    for (TypeElement objcClass : env.typeUtil().getObjcUpperBounds(type)) {
      Import newImport = newImport(objcClass, env.nameTable(), env.options());
      imports.add(newImport);
    }
    if (TypeUtil.isArray(type) && env.options().asObjCGenericDecl()) {
      // Recursion provides support for multi-dimensional arrays.
      addImports(((ArrayType) type).getComponentType(), imports, env);
    }
    if (type instanceof NativeType) {
      NativeType nativeType = (NativeType) type;
      Import nativeImport =
          newNativeImport(
              nativeType.getName(), nativeType.getHeader(), nativeType.getForwardDeclaration());
      imports.add(nativeImport);

      for (TypeMirror referencedType : nativeType.getReferencedTypes()) {
        addImports(referencedType, imports, env);
      }
      for (TypeMirror typeArgument : nativeType.getTypeArguments()) {
        addImports(typeArgument, imports, env);
      }
    }
  }
}
