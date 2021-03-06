/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring.scope.nodes;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.TTCN3.statements.Definition_Statement;

/**
 * A class which links together declaration {@link StatementNode}s with the same location.
 *
 * @author Viktor Varga
 */
public class MultiDeclaration {

	public final SortedSet<StatementNode> declStmts = new TreeSet<StatementNode>(new MultiDeclarationComparator());	//multi-declaration parts in the right order

	public int getSize() {
		return declStmts.size();
	}

	public StatementNode getFirstStatement() {
		return declStmts.first();
	}
	public StatementNode getLastStatement() {
		return declStmts.last();
	}
	public boolean isFirstStatement(final StatementNode toTest) {
		return declStmts.first().equals(toTest);
	}
	public boolean isLastStatement(final StatementNode toTest) {
		return declStmts.last().equals(toTest);
	}
	public boolean isIntermediateStatement(final StatementNode toTest) {
		return !isFirstStatement(toTest) && !isLastStatement(toTest);
	}
	public boolean isAllStatementsMoved() {
		final Iterator<StatementNode> it = declStmts.iterator();
		while (it.hasNext()) {
			if (!it.next().isMoved()) {
				return false;
			}
		}
		return true;
	}

	public void addDeclarationStatement(final StatementNode sn) {
		if (!declStmts.add(sn)) {
			ErrorReporter.logError("MultiDeclaration.addDeclarationStatement(): " +
					"The StatementNode is already present: " + sn);
		} else {
			//TITANDebugConsole.println("MD(" + this + "), sn added: " + sn);
		}
	}

	private class MultiDeclarationComparator implements Comparator<StatementNode> {

		@Override
		public int compare(final StatementNode arg0, final StatementNode arg1) {
			if (!(arg0.getAstNode() instanceof Definition_Statement) ||
					!(arg1.getAstNode() instanceof Definition_Statement)) {
				ErrorReporter.logError("MultiDeclarationComparator: " +
						"Invalid StatementNode ASTNode type: " + arg0.getAstNode() +
						" OR " + arg1.getAstNode());
			}
			final Definition_Statement ds0 = (Definition_Statement)arg0.getAstNode();
			final Definition_Statement ds1 = (Definition_Statement)arg1.getAstNode();
			final Location loc0 = ds0.getDefinition().getLocation();
			final Location loc1 = ds1.getDefinition().getLocation();
			final int o0 = loc0.getOffset();
			final int o1 = loc1.getOffset();
			final int result = (o0 < o1) ? -1 : ((o0 == o1) ? 0 : 1);
			if (result == 0 && ds0 != ds1) {
				ErrorReporter.logError("MultiDeclarationComparator: " +
						"Overlapping Variable declarations at: " + loc0.getOffset() + "-" + loc0.getEndOffset() + " in file: " + loc0.getFile());
			}

			return result;
		}

	}

}
