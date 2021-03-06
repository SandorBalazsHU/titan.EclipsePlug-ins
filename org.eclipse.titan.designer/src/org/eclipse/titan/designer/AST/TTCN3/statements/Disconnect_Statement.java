/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.PortReference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.types.PortTypeBody;
import org.eclipse.titan.designer.AST.TTCN3.types.Port_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class Disconnect_Statement extends Statement {
	private static final String FULLNAMEPART1 = ".componentreference1";
	private static final String FULLNAMEPART2 = ".portreference1";
	private static final String FULLNAMEPART3 = ".componentreference2";
	private static final String FULLNAMEPART4 = ".portreference2";
	private static final String STATEMENT_NAME = "disconnect";

	private final Value componentReference1;
	private final PortReference portReference1;
	private final Value componentReference2;
	private final PortReference portReference2;

	public Disconnect_Statement(final Value componentReference1, final PortReference portReference1, final Value componentReference2,
			final PortReference portReference2) {
		this.componentReference1 = componentReference1;
		this.portReference1 = portReference1;
		this.componentReference2 = componentReference2;
		this.portReference2 = portReference2;

		if (componentReference1 != null) {
			componentReference1.setFullNameParent(this);
		}
		if (portReference1 != null) {
			portReference1.setFullNameParent(this);
		}
		if (componentReference2 != null) {
			componentReference2.setFullNameParent(this);
		}
		if (portReference2 != null) {
			portReference2.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_DISCONNECT;
	}

	@Override
	/** {@inheritDoc} */
	public String getStatementName() {
		return STATEMENT_NAME;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (componentReference1 == child) {
			return builder.append(FULLNAMEPART1);
		} else if (portReference1 == child) {
			return builder.append(FULLNAMEPART2);
		} else if (componentReference2 == child) {
			return builder.append(FULLNAMEPART3);
		} else if (portReference2 == child) {
			return builder.append(FULLNAMEPART4);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (componentReference1 != null) {
			componentReference1.setMyScope(scope);
		}
		if (portReference1 != null) {
			portReference1.setSubreferencesScope(scope);
		}
		if (componentReference2 != null) {
			componentReference2.setMyScope(scope);
		}
		if (portReference2 != null) {
			portReference2.setSubreferencesScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (componentReference1 != null) {
			componentReference1.setCodeSection(codeSection);
		}
		if (portReference1 != null) {
			portReference1.setCodeSection(codeSection);
		}
		if (componentReference2 != null) {
			componentReference2.setCodeSection(codeSection);
		}
		if (portReference2 != null) {
			portReference2.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		IType portType1;
		IType portType2;
		PortTypeBody body1;
		PortTypeBody body2;

		portType1 = Port_Utility.checkConnectionEndpoint(timestamp, this, componentReference1, portReference1, false);
		if (portType1 == null) {
			body1 = null;
		} else {
			body1 = ((Port_Type) portType1).getPortBody();
		}

		portType2 = Port_Utility.checkConnectionEndpoint(timestamp, this, componentReference2, portReference2, false);
		if (portType2 == null) {
			body2 = null;
		} else {
			body2 = ((Port_Type) portType2).getPortBody();
		}

		if (portType1 == null || portType2 == null || body1 == null || body2 == null) {
			lastTimeChecked = timestamp;
			return;
		}

		if (!body1.isConnectable(timestamp, body2) || (body1 != body2 && !body2.isConnectable(timestamp, body1))) {
			location.reportSemanticError(MessageFormat.format(Connect_Statement.INCONSISTENTCONNECTION, portType1.getTypename(),
					portType2.getTypename()));
			body1.reportConnectionErrors(timestamp, body2, location);
			if (body1 != body2) {
				body2.reportConnectionErrors(timestamp, body1, location);
			}
		}

		lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (componentReference1 != null) {
			componentReference1.updateSyntax(reparser, false);
			reparser.updateLocation(componentReference1.getLocation());
		}

		if (portReference1 != null) {
			portReference1.updateSyntax(reparser, false);
			reparser.updateLocation(portReference1.getLocation());
		}

		if (componentReference2 != null) {
			componentReference2.updateSyntax(reparser, false);
			reparser.updateLocation(componentReference2.getLocation());
		}

		if (portReference2 != null) {
			portReference2.updateSyntax(reparser, false);
			reparser.updateLocation(portReference2.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (componentReference1 != null) {
			componentReference1.findReferences(referenceFinder, foundIdentifiers);
		}
		if (portReference1 != null) {
			portReference1.findReferences(referenceFinder, foundIdentifiers);
		}
		if (componentReference2 != null) {
			componentReference2.findReferences(referenceFinder, foundIdentifiers);
		}
		if (portReference2 != null) {
			portReference2.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (componentReference1 != null && !componentReference1.accept(v)) {
			return false;
		}
		if (portReference1 != null && !portReference1.accept(v)) {
			return false;
		}
		if (componentReference2 != null && !componentReference2.accept(v)) {
			return false;
		}
		if (portReference2 != null && !portReference2.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode(final JavaGenData aData, final StringBuilder source) {
		final ExpressionStruct expression = new ExpressionStruct();

		expression.expression.append("TTCN_Runtime.disconnect_port(");
		componentReference1.generateCodeExpression(aData, expression, true);
		expression.expression.append(", ");
		if (componentReference1.getExpressionGovernor(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_DYNAMIC_VALUE) == null) {
			Port_Utility.generate_code_portref(aData, expression, portReference1);
		} else {
			portReference1.generateCode(aData, expression);
			expression.expression.append(".get_name()");
		}

		expression.expression.append(", ");
		componentReference2.generateCodeExpression(aData, expression, true);
		expression.expression.append(", ");
		if (componentReference2.getExpressionGovernor(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_DYNAMIC_VALUE) == null) {
			Port_Utility.generate_code_portref(aData, expression, portReference2);
		} else {
			portReference2.generateCode(aData, expression);
			expression.expression.append(".get_name()");
		}
		expression.expression.append(")");

		expression.mergeExpression(source);
	}
}
