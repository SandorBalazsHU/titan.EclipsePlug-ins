/******************************************************************************
 * Copyright (c) 2000-2015 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ArraySubReference;
import org.eclipse.titan.designer.AST.BridgingNamedNode;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.ASN1.types.ASN1_Sequence_Type;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.types.CompField;
import org.eclipse.titan.designer.AST.TTCN3.types.Signature_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Sequence_Type;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class Sequence_Value extends Value {
	private static final String TOOMANYELEMENTS = "Too many elements in value list notation for type `{0}'': {1} was expected instead of {2}";
	private static final String ALLARENOTUSED = "All elements of value list notation for type `{0}'' are not used symbols (`-'')";
	private static final String NONEXISTENTFIELD = "Reference to non-existent record field `{0}'' in type `{1}''";

	private final NamedValues values;

	private Value convertedValue;

	public Sequence_Value(final NamedValues values) {
		this.values = values;

		if (values != null) {
			values.setFullNameParent(this);
		}
	}

	/**
	 * function used to convert a value written without naming the fields into a template where all field names are provided.
	 * 
	 * @param timestamp the timestamp of the actual build cycle
	 * @param value the value to be converted
	 * */
	public static Sequence_Value convert(final CompilationTimeStamp timestamp, final SequenceOf_Value value) {
		if (value.getMyGovernor() == null) {
			Sequence_Value target = new Sequence_Value(null);
			target.copyGeneralProperties(value);
			return target;
		}

		IType t = value.getMyGovernor().getTypeRefdLast(timestamp);
		int nofComponents = 0;
		switch (t.getTypetype()) {
		case TYPE_TTCN3_SEQUENCE:
			nofComponents = ((TTCN3_Sequence_Type) t).getNofComponents();
			break;
		case TYPE_ASN1_SEQUENCE:
			nofComponents = ((ASN1_Sequence_Type) t).getNofComponents(timestamp);
			break;
		case TYPE_SIGNATURE:
			nofComponents = ((Signature_Type) t).getNofParameters();
			break;
		default:{
			Sequence_Value target = new Sequence_Value(null);
			target.copyGeneralProperties(value);
			return target;
		}
		}

		Values oldValues = value.getValues();
		int nofValues = oldValues.getNofValues();
		if (nofValues > nofComponents) {
			value.getLocation().reportSemanticError(MessageFormat.format(TOOMANYELEMENTS, t.getTypename(), nofComponents, nofValues));
			value.setIsErroneous(true);
		}

		int upperLimit;
		boolean allNotUsed;
		if (nofValues <= nofComponents) {
			upperLimit = nofValues;
			allNotUsed = true;
		} else {
			upperLimit = nofComponents;
			allNotUsed = false;
		}

		NamedValues values = new NamedValues();
		NamedValue namedValue;
		Identifier identifier;
		for (int i = 0; i < upperLimit; i++) {
			IValue v = oldValues.getValueByIndex(i);
			if (!Value_type.NOTUSED_VALUE.equals(v.getValuetype())) {
				allNotUsed = false;
				switch (t.getTypetype()) {
				case TYPE_TTCN3_SEQUENCE:
					identifier = ((TTCN3_Sequence_Type) t).getComponentIdentifierByIndex(i);
					break;
				case TYPE_ASN1_SEQUENCE:
					identifier = ((ASN1_Sequence_Type) t).getComponentIdentifierByIndex(i);
					break;
				case TYPE_SIGNATURE:
					identifier = ((Signature_Type) t).getParameterIdentifierByIndex(i);
					break;
				default:{
					Sequence_Value target = new Sequence_Value(null);
					target.copyGeneralProperties(value);
					return target;
				}
				}

				namedValue = new NamedValue(identifier, v);
				namedValue.setLocation(v.getLocation());
				values.addNamedValue(namedValue);
			}
		}

		

		if (allNotUsed && nofValues > 0) {
			value.getLocation().reportSemanticWarning(MessageFormat.format(ALLARENOTUSED, t.getTypename()));
		}
		
		Sequence_Value target = new Sequence_Value(values);
		target.copyGeneralProperties(value);
		
		return target;
	}

	@Override
	public Value_type getValuetype() {
		return Value_type.SEQUENCE_VALUE;
	}

	@Override
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("{");
		boolean isAsn1 = isAsn();
		for (int i = 0; i < values.getSize(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			NamedValue namedValue = values.getNamedValueByIndex(i);
			builder.append(namedValue.getName().getDisplayName());
			if (isAsn1) {
				builder.append(' ');
			} else {
				builder.append(" := ");
			}
			builder.append(namedValue.getValue().createStringRepresentation());
		}
		builder.append('}');

		return builder.toString();
	}

	@Override
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		return Type_type.TYPE_UNDEFINED;
	}

	@Override
	public IValue getReferencedSubValue(final CompilationTimeStamp timestamp, final Reference reference,
			final int actualSubReference, final IReferenceChain refChain) {
		List<ISubReference> subreferences = reference.getSubreferences();
		if (getIsErroneous(timestamp) || subreferences.size() <= actualSubReference) {
			return this;
		}

		if (convertedValue != null && convertedValue != this) {
			IValue temp = convertedValue.getReferencedSubValue(timestamp, reference, actualSubReference, refChain);
			if (temp != null && temp.getIsErroneous(timestamp)) {
				setIsErroneous(true);
			}

			return temp;
		}

		IType type = myGovernor.getTypeRefdLast(timestamp);
		if (type.getIsErroneous(timestamp)) {
			return null;
		}

		ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			subreference.getLocation().reportSemanticError(MessageFormat.format(ArraySubReference.INVALIDVALUESUBREFERENCE, type.getTypename()));
			return null;
		case fieldSubReference:
			Identifier fieldId = ((FieldSubReference) subreference).getId();
			switch (type.getTypetype()) {
			case TYPE_TTCN3_SEQUENCE:
				if (!((TTCN3_Sequence_Type) type).hasComponentWithName(fieldId.getName())) {
					subreference.getLocation().reportSemanticError(MessageFormat.format(NONEXISTENTFIELD, fieldId.getDisplayName(), type.getTypename()));
					return null;
				}
				break;
			case TYPE_ASN1_SEQUENCE:
				if (!((ASN1_Sequence_Type) type).hasComponentWithName(fieldId)) {
					subreference.getLocation().reportSemanticError(MessageFormat.format(NONEXISTENTFIELD, fieldId.getDisplayName(), type.getTypename()));
					return null;
				}
				break;
			default:
				return null;
			}

			if (values.hasNamedValueWithName(fieldId)) {
				return values.getNamedValueByName(fieldId).getValue().getReferencedSubValue(timestamp, reference, actualSubReference + 1, refChain);
			}

			if (Type_type.TYPE_TTCN3_SEQUENCE.equals(type.getTypetype())) {
				if (!reference.getUsedInIsbound()) {
					subreference.getLocation().reportSemanticError(MessageFormat.format("Reference to unbound record field `{0}''", fieldId.getDisplayName()));
				}
				return null;
			}

			CompField compField = ((ASN1_Sequence_Type) type).getComponentByName(fieldId);
			if (compField.isOptional()) {
				//create an explicit omit value
				Value result = new Omit_Value();

				BridgingNamedNode bridge = new BridgingNamedNode(this, "." + fieldId.getDisplayName());
				result.setFullNameParent(bridge);

				result.setMyScope(getMyScope());

				return result;
			} else if (compField.hasDefault()) {
				return compField.getDefault().getReferencedSubValue(timestamp, reference, actualSubReference + 1, refChain);
			}

			return null;
		case parameterisedSubReference:
			subreference.getLocation().reportSemanticError(ParameterisedSubReference.INVALIDVALUESUBREFERENCE);
			return null;
		default:
			subreference.getLocation().reportSemanticError(ISubReference.INVALIDSUBREFERENCE);
			return null;
		}
	}

	@Override
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (values == null) {
			return true;
		}

		for (int i = 0; i < values.getSize(); i++) {
			IValue temp = values.getNamedValueByIndex(i).getValue();
			if (temp == null || temp.isUnfoldable(timestamp, expectedValue, referenceChain)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Adds the provided named value to the list of named values in this sequence.
	 * <p>
	 * Right now is only used to add implicit omit elements.
	 *
	 * @param value the named value to add.
	 * */
	public void addNamedValue(final NamedValue value) {
		if (value != null) {
			values.addNamedValue(value);
			value.setMyScope(myScope);
		}
	}

	/**
	 * Remove all named values that were not parsed,
	 * but generated during previous semantic checks.
	 * */
	public void removeGeneratedValues() {
		if (values != null) {
			values.removeGeneratedValues();
		}
	}

	protected NamedValues getValues() {
		return values;
	}

	public int getNofComponents() {
		if (values == null) {
			return 0;
		}

		return values.getSize();
	}

	public NamedValue getSeqValueByIndex(final int index) {
		if (values == null) {
			return null;
		}

		return values.getNamedValueByIndex(index);
	}

	public boolean hasComponentWithName(final Identifier name) {
		if (values == null) {
			return false;
		}

		return values.hasNamedValueWithName(name);
	}

	public NamedValue getComponentByName(final Identifier name) {
		if (values == null) {
			return null;
		}

		return values.getNamedValueByName(name);
	}

	@Override
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (values != null) {
			values.setMyScope(scope);
		}
	}

	@Override
	public Value setValuetype(final CompilationTimeStamp timestamp, final Value_type newType) {
		switch (newType) {
		case SET_VALUE:
			convertedValue =  new Set_Value(this);
			break;
		case CHOICE_VALUE:
			convertedValue = new Choice_Value(timestamp, this);
			break;
		case ANYTYPE_VALUE:
			convertedValue = new Anytype_Value(timestamp, this);
			break;
		case REAL_VALUE:
			convertedValue = new Real_Value(timestamp, this);
			break;
		default:
			convertedValue = super.setValuetype(timestamp, newType);
			break;
		}

		return convertedValue;
	}

	/**
	 * Checks the uniqueness of the sequence value.
	 *
	 * @param timestamp the timestamp of the actual build cycle
	 * */
	public void checkUniqueness(final CompilationTimeStamp timestamp) {
		if (values == null) {
			return;
		}

		values.checkUniqueness(timestamp);
	}

	@Override
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (referenceChain.add(this)) {
			IValue temp;
			for (int i = 0, size = values.getSize(); i < size; i++) {
				temp = values.getNamedValueByIndex(i).getValue();
				if (temp != null) {
					referenceChain.markState();
					temp.checkRecursions(timestamp, referenceChain);
					referenceChain.previousState();
				}
			}
		}
	}

	@Override
	public IValue getValueRefdLast(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (convertedValue == null || convertedValue.getIsErroneous(timestamp)) {
			return this;
		}

		final CompilationTimeStamp lastTimestamp = convertedValue.getLastTimeChecked();
		if (lastTimestamp == null || lastTimestamp.isLess(timestamp)) {
			return this;
		}

		return convertedValue.getValueRefdLast(timestamp, expectedValue, referenceChain);
	}

	@Override
	public boolean checkEquality(final CompilationTimeStamp timestamp, final IValue other) {
		if (convertedValue != null && convertedValue != this) {
			return convertedValue.checkEquality(timestamp, other);
		}

		IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		IValue last = other.getValueRefdLast(timestamp, referenceChain);
		referenceChain.release();

		if (!Value_type.SEQUENCE_VALUE.equals(last.getValuetype())) {
			return false;
		}

		if (myGovernor == null) {
			return false;
		}

		IType leftGovernor = myGovernor.getTypeRefdLast(timestamp);

		Sequence_Value otherSequence = (Sequence_Value) last;
		if (values.getSize() != otherSequence.values.getSize()) {
			return false;
		}

		int nofComps = 0;
		switch (leftGovernor.getTypetype()) {
		case TYPE_TTCN3_SEQUENCE:
			nofComps = ((TTCN3_Sequence_Type) leftGovernor).getNofComponents();
			break;
		case TYPE_ASN1_SEQUENCE:
			nofComps = ((ASN1_Sequence_Type) leftGovernor).getNofComponents(timestamp);
			break;
		default:
			return false;
		}

		CompField compField = null;
		for (int i = 0; i < nofComps; i++) {
			switch (leftGovernor.getTypetype()) {
			case TYPE_TTCN3_SEQUENCE:
				compField = ((TTCN3_Sequence_Type) leftGovernor).getComponentByIndex(i);
				break;
			case TYPE_ASN1_SEQUENCE:
				compField = ((ASN1_Sequence_Type) leftGovernor).getComponentByIndex(i);
				break;
			default:
				return false;
			}
			Identifier fieldName = compField.getIdentifier();

			if (hasComponentWithName(fieldName)) {
				IValue leftValue = getComponentByName(fieldName).getValue();
				if (otherSequence.hasComponentWithName(fieldName)) {
					IValue otherValue = otherSequence.getComponentByName(fieldName).getValue();
					if ((Value_type.OMIT_VALUE.equals(leftValue.getValuetype()) && !Value_type.OMIT_VALUE.equals(otherValue.getValuetype()))
							|| (!Value_type.OMIT_VALUE.equals(leftValue.getValuetype()) && Value_type.OMIT_VALUE.equals(otherValue.getValuetype()))) {
						return false;
					}

					if (!leftValue.checkEquality(timestamp, otherValue)) {
						return false;
					}
				} else {
					if (compField.hasDefault()) {
						if (!leftValue.checkEquality(timestamp, compField.getDefault())) {
							return false;
						}
					} else {
						if (!Value_type.OMIT_VALUE.equals(leftValue.getValuetype())) {
							return false;
						}
					}
				}
			} else {
				if (otherSequence.hasComponentWithName(fieldName)) {
					IValue otherValue = otherSequence.getComponentByName(fieldName).getValue();
					if (compField.hasDefault()) {
						if (Value_type.OMIT_VALUE.equals(otherValue.getValuetype())) {
							return false;
						}

						if (!compField.getDefault().checkEquality(timestamp, otherValue)) {
							return false;
						}
					}
				}
			}
		}

		return true;
	}

	@Override
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (values != null) {
			values.updateSyntax(reparser, false);
		}
	}

	@Override
	public boolean evaluateIsvalue(final boolean fromSequence) {
		if (values == null) {
			return true;
		}

		for (int i = 0, size = values.getSize(); i < size; i++) {
			if (!values.getNamedValueByIndex(i).getValue().evaluateIsvalue(true)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean evaluateIsbound(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference) {
		List<ISubReference> subreferences = reference.getSubreferences();
		if (getIsErroneous(timestamp) || subreferences.size() <= actualSubReference) {
			return true;
		}

		if (convertedValue != null && convertedValue != this) {
			return convertedValue.evaluateIsbound(timestamp, reference, actualSubReference);
		}

		IType type = myGovernor.getTypeRefdLast(timestamp);
		if (type.getIsErroneous(timestamp)) {
			return false;
		}

		ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			return false;
		case fieldSubReference:
			Identifier fieldId = ((FieldSubReference) subreference).getId();
			switch (type.getTypetype()) {
			case TYPE_TTCN3_SEQUENCE:
				if (!((TTCN3_Sequence_Type) type).hasComponentWithName(fieldId.getName())) {
					return false;
				}
				break;
			case TYPE_ASN1_SEQUENCE:
				if (!((ASN1_Sequence_Type) type).hasComponentWithName(fieldId)) {
					return false;
				}
				break;
			default:
				return false;
			}

			if (values.hasNamedValueWithName(fieldId)) {
				// we can move on with the check
				return values.getNamedValueByName(fieldId).getValue().evaluateIsbound(timestamp, reference, actualSubReference + 1);
			}

			if (Type_type.TYPE_TTCN3_SEQUENCE.equals(type.getTypetype())) {
				return false;
			}

			CompField compField = ((ASN1_Sequence_Type) type).getComponentByName(fieldId);
			if (compField.isOptional()) {
				//create an explicit omit value
				Value result = new Omit_Value();

				BridgingNamedNode bridge = new BridgingNamedNode(this, "." + fieldId.getDisplayName());
				result.setFullNameParent(bridge);

				result.setMyScope(getMyScope());

				return result.evaluateIsbound(timestamp, reference, actualSubReference + 1);
			} else if (compField.hasDefault()) {
				return compField.getDefault().evaluateIsbound(timestamp, reference, actualSubReference + 1);
			}

			return false;
		case parameterisedSubReference:
			return false;
		default:
			return false;
		}
	}

	@Override
	public boolean evaluateIspresent(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference) {
		List<ISubReference> subreferences = reference.getSubreferences();
		if (getIsErroneous(timestamp) || subreferences.size() <= actualSubReference) {
			return true;
		}

		if (convertedValue != null && convertedValue != this) {
			return convertedValue.evaluateIsbound(timestamp, reference, actualSubReference);
		}

		IType type = myGovernor.getTypeRefdLast(timestamp);
		if (type.getIsErroneous(timestamp)) {
			return false;
		}

		ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			return false;
		case fieldSubReference:
			Identifier fieldId = ((FieldSubReference) subreference).getId();
			switch (type.getTypetype()) {
			case TYPE_TTCN3_SEQUENCE:
				if (!((TTCN3_Sequence_Type) type).hasComponentWithName(fieldId.getName())) {
					return false;
				}
				break;
			case TYPE_ASN1_SEQUENCE:
				if (!((ASN1_Sequence_Type) type).hasComponentWithName(fieldId)) {
					return false;
				}
				break;
			default:
				return false;
			}

			if (values.hasNamedValueWithName(fieldId)) {
				// we can move on with the check
				return values.getNamedValueByName(fieldId).getValue().evaluateIspresent(timestamp, reference, actualSubReference + 1);
			}

			if (Type_type.TYPE_TTCN3_SEQUENCE.equals(type.getTypetype())) {
				return false;
			}

			CompField compField = ((ASN1_Sequence_Type) type).getComponentByName(fieldId);
			if (compField.isOptional()) {
				//create an explicit omit value
				Value result = new Omit_Value();

				BridgingNamedNode bridge = new BridgingNamedNode(this, "." + fieldId.getDisplayName());
				result.setFullNameParent(bridge);

				result.setMyScope(getMyScope());

				return result.evaluateIspresent(timestamp, reference, actualSubReference + 1);
			} else if (compField.hasDefault()) {
				return compField.getDefault().evaluateIspresent(timestamp, reference, actualSubReference + 1);
			}

			return false;
		case parameterisedSubReference:
			return false;
		default:
			return false;
		}
	}

	@Override
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (values == null) {
			return;
		}

		if (referenceFinder.assignment.getAssignmentType() == Assignment_type.A_TYPE && referenceFinder.fieldId != null && myGovernor != null) {
			// check if this is the type and field we are searching for
			IType governorLast = myGovernor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
			if (referenceFinder.type == governorLast) {
				NamedValue nv = values.getNamedValueByName(referenceFinder.fieldId);
				if (nv != null) {
					foundIdentifiers.add(new Hit(nv.getName()));
				}
			}
		}
		values.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	protected boolean memberAccept(final ASTVisitor v) {
		if (values!=null && !values.accept(v)) {
			return false;
		}
		return true;
	}
}
