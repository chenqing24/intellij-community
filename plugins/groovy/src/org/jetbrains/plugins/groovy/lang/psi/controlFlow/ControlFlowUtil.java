/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
package org.jetbrains.plugins.groovy.lang.psi.controlFlow;

import com.intellij.openapi.diagnostic.Logger;
import gnu.trove.TIntHashSet;
import gnu.trove.TObjectIntHashMap;

import java.util.*;

/**
 * @author ven
 */
public class ControlFlowUtil {
  private static final Logger LOG = Logger.getInstance("org.jetbrains.plugins.groovy.lang.psi.controlFlow.ControlFlowUtil");

  public static int[] postorder(Instruction[] flow) {
    int[] result = new int[flow.length];
    boolean[] visited = new boolean[flow.length];
    for (int i = 0; i < result.length; i++) visited[i] = false;

    int N = flow.length;
    N = doVisitForPostorder(flow[0], N, result, visited);

    LOG.assertTrue(N == 0);
    return result;
  }

  private static int doVisitForPostorder(Instruction curr, int currN, int[] postorder, boolean[] visited) {
    visited[curr.num()] = true;
    for (Instruction succ : curr.allSucc()) {
      if (!visited[succ.num()]) {
        currN = doVisitForPostorder(succ, currN, postorder, visited);
      }
    }
    postorder[curr.num()] = --currN;
    return currN;
  }

  public static ReadWriteVariableInstruction[] getReadsWithoutPriorWrites(Instruction[] flow) {
    List<ReadWriteVariableInstruction> result = new ArrayList<ReadWriteVariableInstruction>();
    TObjectIntHashMap<String> namesIndex = buildNamesIndex(flow);

    ArrayList<TIntHashSet> definitelyAssigned = new ArrayList<TIntHashSet>();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < flow.length; i++) definitelyAssigned.add(null);

    int[] postorder = postorder(flow);
    int[] revpostorder = revpostorder(postorder);

    findReadsBeforeWrites(flow, definitelyAssigned, result, namesIndex, postorder, revpostorder);

    return result.toArray(new ReadWriteVariableInstruction[result.size()]);
  }

  private static int[] revpostorder(int[] postorder) {
    int[] result = new int[postorder.length];
    for (int i = 0; i < postorder.length; i++) {
      result[postorder[i]] = i;
    }

    return result;
  }

  private static TObjectIntHashMap<String> buildNamesIndex(Instruction[] flow) {
    TObjectIntHashMap<String> namesIndex = new TObjectIntHashMap<String>();
    int idx = 0;
    for (Instruction instruction : flow) {
      if (instruction instanceof ReadWriteVariableInstruction) {
        String name = ((ReadWriteVariableInstruction) instruction).getVariableName();
        if (!namesIndex.contains(name)) {
          namesIndex.put(name, idx++);
        }
      }
    }
    return namesIndex;
  }

  private static void findReadsBeforeWrites(Instruction[] flow, ArrayList<TIntHashSet> definitelyAssigned,
                                            List<ReadWriteVariableInstruction> result,
                                            TObjectIntHashMap<String> namesIndex,
                                            int[] postorder,
                                            int[] revpostorder) {

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < flow.length; i++) {
      int j = revpostorder[i];
      Instruction curr = flow[j];
      if (curr instanceof ReadWriteVariableInstruction) {
        ReadWriteVariableInstruction readWriteInsn = (ReadWriteVariableInstruction) curr;
        int idx = namesIndex.get(readWriteInsn.getVariableName());
        TIntHashSet vars = definitelyAssigned.get(j);
        if (!readWriteInsn.isWrite()) {
          if (vars == null || !vars.contains(idx)) {
            result.add(readWriteInsn);
          }
        } else {
          if (vars == null) {
            vars = new TIntHashSet();
            definitelyAssigned.add(j, vars);
          }
          vars.add(idx);
        }
      }

      for (Instruction succ : curr.allSucc()) {
        if (postorder[succ.num()] > postorder[curr.num()]) {
          TIntHashSet currDefinitelyAssigned = definitelyAssigned.get(curr.num());
          TIntHashSet succDefinitelyAssigned = definitelyAssigned.get(succ.num());
          if (currDefinitelyAssigned != null) {
            int[] currArray = currDefinitelyAssigned.toArray();
            if (succDefinitelyAssigned == null) {
              succDefinitelyAssigned = new TIntHashSet();
              succDefinitelyAssigned.addAll(currArray);
              definitelyAssigned.add(succ.num(), succDefinitelyAssigned);
            } else {
              succDefinitelyAssigned.retainAll(currArray);
            }
          } else {
            if (succDefinitelyAssigned != null) {
              succDefinitelyAssigned.clear();
            } else {
              succDefinitelyAssigned = new TIntHashSet();
              definitelyAssigned.add(succ.num(), succDefinitelyAssigned);
            }
          }
        }
      }

    }
  }
}
