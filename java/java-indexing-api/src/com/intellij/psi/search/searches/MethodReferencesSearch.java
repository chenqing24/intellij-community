// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search.searches;

import com.intellij.openapi.application.DumbAwareSearchParameters;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author max
 */
public class MethodReferencesSearch extends ExtensibleQueryFactory<PsiReference, MethodReferencesSearch.SearchParameters> {
  public static final ExtensionPointName<QueryExecutor> EP_NAME = ExtensionPointName.create("com.intellij.methodReferencesSearch");
  public static final MethodReferencesSearch INSTANCE = new MethodReferencesSearch();

  public static class SearchParameters implements DumbAwareSearchParameters {
    private final PsiMethod myMethod;
    private final Project myProject;
    private final SearchScope myScope;
    private volatile SearchScope myEffectiveScope;
    private final boolean myStrictSignatureSearch;
    private final SearchRequestCollector myOptimizer;
    private final boolean isSharedOptimizer;

    public SearchParameters(@NotNull PsiMethod method, @NotNull SearchScope scope, boolean strictSignatureSearch, @Nullable SearchRequestCollector optimizer) {
      myMethod = method;
      myScope = scope;
      myStrictSignatureSearch = strictSignatureSearch;
      isSharedOptimizer = optimizer != null;
      myOptimizer = optimizer != null ? optimizer : new SearchRequestCollector(new SearchSession());
      myProject = PsiUtilCore.getProjectInReadAction(method);
    }

    public SearchParameters(@NotNull PsiMethod method, @NotNull SearchScope scope, final boolean strict) {
      this(method, scope, strict, null);
    }

    @Override
    public boolean isQueryValid() {
      return myMethod.isValid();
    }

    @Override
    @NotNull
    public Project getProject() {
      return myProject;
    }

    @NotNull
    public PsiMethod getMethod() {
      return myMethod;
    }

    public boolean isStrictSignatureSearch() {
      return myStrictSignatureSearch;
    }

    public SearchRequestCollector getOptimizer() {
      return myOptimizer;
    }

    /**
     * @return the user-visible search scope, most often "Project Files" or "Project and Libraries".
     * Searchers most likely need to use {@link #getEffectiveSearchScope()}.
     */
    public SearchScope getScopeDeterminedByUser() {
      return myScope;
    }


    /**
     * @return Same as {@link #getScopeDeterminedByUser()}. Searchers most likely need to use {@link #getEffectiveSearchScope()}.
     */
    @Deprecated
    @NotNull
    public SearchScope getScope() {
      return getScopeDeterminedByUser();
    }

    @NotNull
    public SearchScope getEffectiveSearchScope () {
      SearchScope scope = myEffectiveScope;
      if (scope == null) {
        myEffectiveScope = scope = myScope.intersectWith(PsiSearchHelper.getInstance(myMethod.getProject()).getUseScope(myMethod));
      }
      return scope;
    }
  }

  private MethodReferencesSearch() {}

  public static Query<PsiReference> search(@NotNull PsiMethod method, SearchScope scope, final boolean strictSignatureSearch) {
    return search(new SearchParameters(method, scope, strictSignatureSearch));
  }

  public static void searchOptimized(final PsiMethod method, SearchScope scope, final boolean strictSignatureSearch,
                                     @NotNull SearchRequestCollector collector, final Processor<? super PsiReference> processor) {
    searchOptimized(method, scope, strictSignatureSearch, collector, false, (psiReference, collector1) -> processor.process(psiReference));
  }

  public static void searchOptimized(final PsiMethod method, SearchScope scope, final boolean strictSignatureSearch,
                                     SearchRequestCollector collector, final boolean inReadAction,
                                     PairProcessor<PsiReference, SearchRequestCollector> processor) {
    final SearchRequestCollector nested = new SearchRequestCollector(collector.getSearchSession());
    collector.searchQuery(new QuerySearchRequest(search(new SearchParameters(method, scope, strictSignatureSearch, nested)), nested,
                                                 inReadAction, processor));
  }

  public static Query<PsiReference> search(final SearchParameters parameters) {
    final Query<PsiReference> result = INSTANCE.createQuery(parameters);
    if (parameters.isSharedOptimizer) {
      return uniqueResults(result);
    }

    final SearchRequestCollector requests = parameters.getOptimizer();

    Project project = PsiUtilCore.getProjectInReadAction(parameters.getMethod());
    return uniqueResults(new MergeQuery<>(result, new SearchRequestQuery(project, requests)));
  }

  public static Query<PsiReference> search(final PsiMethod method, final boolean strictSignatureSearch) {
    return search(method, GlobalSearchScope.allScope(PsiUtilCore.getProjectInReadAction(method)), strictSignatureSearch);
  }

  public static Query<PsiReference> search(final PsiMethod method) {
    return search(method, true);
  }

  private static UniqueResultsQuery<PsiReference, ReferenceDescriptor> uniqueResults(@NotNull Query<PsiReference> composite) {
    return new UniqueResultsQuery<>(composite, ContainerUtil.canonicalStrategy(), ReferenceDescriptor.MAPPER);
  }
}