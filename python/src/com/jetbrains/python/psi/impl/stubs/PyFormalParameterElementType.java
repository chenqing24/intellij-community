/*
 * @author max
 */
package com.jetbrains.python.psi.impl.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.PersistentStringEnumerator;
import com.intellij.util.io.StringRef;
import com.jetbrains.python.psi.PyParameter;
import com.jetbrains.python.psi.PyStubElementType;
import com.jetbrains.python.psi.impl.PyParameterImpl;
import com.jetbrains.python.psi.stubs.PyParameterStub;

import java.io.IOException;

public class PyFormalParameterElementType extends PyStubElementType<PyParameterStub, PyParameter> {
  public PyFormalParameterElementType() {
    super("FORMAL_PARAMETER");
  }

  public PyParameter createPsi(final PyParameterStub stub) {
    return new PyParameterImpl(stub);
  }

  public PyParameterStub createStub(final PyParameter psi, final StubElement parentStub) {
    return new PyParameterStubImpl(psi.getName(), psi.isPositionalContainer(), psi.isKeywordContainer(), parentStub);
  }

  public PsiElement createElement(final ASTNode node) {
    return new PyParameterImpl(node);
  }

  public void serialize(final PyParameterStub stub, final StubOutputStream dataStream)
      throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeBoolean(stub.isKeywordContainer());
    dataStream.writeBoolean(stub.isPositionalContainer());
  }

  public PyParameterStub deserialize(final StubInputStream dataStream, final StubElement parentStub) throws IOException {
    String name = StringRef.toString(dataStream.readName());
    boolean keyword = dataStream.readBoolean();
    boolean positional = dataStream.readBoolean();
    return new PyParameterStubImpl(name, positional, keyword, parentStub);
  }
}