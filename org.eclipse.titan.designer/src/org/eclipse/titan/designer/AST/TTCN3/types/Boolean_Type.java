/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ArraySubReference;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.TypeCompatibilityInfo;
import org.eclipse.titan.designer.AST.ASN1.ASN1Type;
import org.eclipse.titan.designer.AST.ASN1.IASN1Type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.attributes.RawAST;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.types.subtypes.SubType;
import org.eclipse.titan.designer.compiler.BuildTimestamp;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * boolean type.
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class Boolean_Type extends ASN1Type {
	private static final String BOOLEAN = "boolean";
	private static final String BOOLEANVALUEEXPECTED1 = "boolean value was expected";
	private static final String BOOLEANVALUEEXPECTED2 = "BOOLEAN value was expected";
	private static final String TEMPLATENOTALLOWED = "{0} cannot be used for type `boolean''";
	private static final String LENGTHRESTRICTIONNOTALLOWED = "Length restriction is not allowed for type `boolean''";

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetype() {
		return Type_type.TYPE_BOOL;
	}

	@Override
	/** {@inheritDoc} */
	public IASN1Type newInstance() {
		return new Boolean_Type();
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetypeTtcn3() {
		if (isErroneous) {
			return Type_type.TYPE_UNDEFINED;
		}

		return getTypetype();
	}

	@Override
	/** {@inheritDoc} */
	public String getTypename() {
		return BOOLEAN;
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "boolean.gif";
	}

	@Override
	/** {@inheritDoc} */
	public boolean isCompatible(final CompilationTimeStamp timestamp, final IType otherType, final TypeCompatibilityInfo info,
			final TypeCompatibilityInfo.Chain leftChain, final TypeCompatibilityInfo.Chain rightChain) {
		check(timestamp);
		otherType.check(timestamp);
		final IType temp = otherType.getTypeRefdLast(timestamp);
		if (getIsErroneous(timestamp) || temp.getIsErroneous(timestamp)) {
			return true;
		}

		return Type_type.TYPE_BOOL.equals(temp.getTypetype());
	}

	@Override
	/** {@inheritDoc} */
	public SubType.SubType_type getSubtypeType() {
		return SubType.SubType_type.ST_BOOLEAN;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;

		initAttributes(timestamp);

		if (constraints != null) {
			constraints.check(timestamp);
		}

		checkSubtypeRestrictions(timestamp);

		if (myScope != null) {
			checkEncode(timestamp);
			checkVariants(timestamp);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkThisValue(final CompilationTimeStamp timestamp, final IValue value, final Assignment lhs, final ValueCheckingOptions valueCheckingOptions) {
		final boolean selfReference = super.checkThisValue(timestamp, value, lhs, valueCheckingOptions);

		final IValue last = value.getValueRefdLast(timestamp, valueCheckingOptions.expected_value, null);
		if (last == null || last.getIsErroneous(timestamp)) {
			return selfReference;
		}

		// already handled ones
		switch (value.getValuetype()) {
		case OMIT_VALUE:
		case REFERENCED_VALUE:
			return selfReference;
		case UNDEFINED_LOWERIDENTIFIER_VALUE:
			if (Value_type.REFERENCED_VALUE.equals(last.getValuetype())) {
				return selfReference;
			}
			break;
		default:
			break;
		}

		switch (last.getValuetype()) {
		case BOOLEAN_VALUE:
			break;
		case EXPRESSION_VALUE:
		case MACRO_VALUE:
			// already checked
			break;
		default:
			if (value.isAsn()) {
				value.getLocation().reportSemanticError(BOOLEANVALUEEXPECTED2);
			} else {
				value.getLocation().reportSemanticError(BOOLEANVALUEEXPECTED1);
			}
			value.setIsErroneous(true);
		}

		if (valueCheckingOptions.sub_check) {
			//there is no parent type to check
			if (subType != null) {
				subType.checkThisValue(timestamp, last);
			}
		}

		value.setLastTimeChecked(timestamp);

		return selfReference;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkThisTemplate(final CompilationTimeStamp timestamp, final ITTCN3Template template, final boolean isModified,
			final boolean implicitOmit, final Assignment lhs) {
		registerUsage(template);
		template.setMyGovernor(this);

		template.getLocation().reportSemanticError(MessageFormat.format(TEMPLATENOTALLOWED, template.getTemplateTypeName()));

		if (template.getLengthRestriction() != null) {
			template.getLocation().reportSemanticError(LENGTHRESTRICTIONNOTALLOWED);
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void checkCodingAttributes(final CompilationTimeStamp timestamp, final IReferenceChain refChain) {
		//check raw attributes
		if (subType != null) {
			final int restrictionLength = subType.get_length_restriction();
			if (restrictionLength != -1) {
				if (rawAttribute == null) {
					rawAttribute = new RawAST(getDefaultRawFieldLength());
				}

				rawAttribute.length_restriction = restrictionLength;
			}
		}

		checkJson(timestamp);		
		//TODO add checks for other encodings.
	}

	@Override
	/** {@inheritDoc} */
	public void checkJsonDefault() {
		final String defaultValue = jsonAttribute.default_value;
		if (!defaultValue.matches("true|false")) {
			getLocation().reportSemanticError(MessageFormat.format("Invalid {0} JSON default value", getTypename()));
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean canHaveCoding(final CompilationTimeStamp timestamp, final MessageEncoding_type coding) {
		if (coding == MessageEncoding_type.BER) {
			return hasEncoding(timestamp, MessageEncoding_type.BER, null);
		}

		switch (coding) {
		case RAW:
		case TEXT:
		case JSON:
		case XER:
			return true;
		default:
			return false;
		}
	}

	@Override
	/** {@inheritDoc} */
	public IType getFieldType(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference,
			final Expected_Value_type expectedIndex, final IReferenceChain refChain, final boolean interruptIfOptional) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (subreferences.size() <= actualSubReference) {
			return this;
		}

		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			subreference.getLocation().reportSemanticError(MessageFormat.format(ArraySubReference.INVALIDSUBREFERENCE, getTypename()));
			return null;
		case fieldSubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((FieldSubReference) subreference).getId().getDisplayName(),
							getTypename()));
			return null;
		case parameterisedSubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((ParameterisedSubReference) subreference).getId().getDisplayName(),
							getTypename()));
			return null;
		default:
			subreference.getLocation().reportSemanticError(ISubReference.INVALIDSUBREFERENCE);
			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public int getDefaultRawFieldLength() {
		return 1;
	}

	@Override
	/** {@inheritDoc} */
	public int getRawLength(final BuildTimestamp timestamp) {
		if (rawAttribute != null) {
			return rawAttribute.fieldlength;
		}

		return getDefaultRawFieldLength();
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getProposalDescription(final StringBuilder builder) {
		return builder.append(BOOLEAN);
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		if (lastTimeGenerated != null && !lastTimeGenerated.isLess(aData.getBuildTimstamp())) {
			return;
		}

		lastTimeGenerated = aData.getBuildTimstamp();

		generateCodeTypedescriptor(aData, source);
		if(needsAlias()) {
			final String ownName = getGenNameOwn();
			source.append(MessageFormat.format("\tpublic static class {0} extends {1} '{' '}'\n", ownName, getGenNameValue(aData, source)));
			source.append(MessageFormat.format("\tpublic static class {0}_template extends {1} '{' '}'\n", ownName, getGenNameTemplate(aData, source)));
		}

		if (!isAsn()) {
			if (hasDoneAttribute()) {
				generateCodeDone(aData, source);
			}
			if (subType != null) {
				subType.generateCode(aData, source);
			}
		}

		generateCodeForCodingHandlers(aData, source);
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameValue( final JavaGenData aData, final StringBuilder source ) {
		aData.addBuiltinTypeImport( "TitanBoolean" );
		return "TitanBoolean";
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameTemplate(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "TitanBoolean_template" );
		return "TitanBoolean_template";
	}

	@Override
	/** {@inheritDoc} */
	public String internalGetGenNameTypeDescriptor(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "Base_Type" );
		return "Base_Type.TitanBoolean";
	}


	@Override
	/** {@inheritDoc} */
	public String getGenNameRawDescriptor(final JavaGenData aData, final StringBuilder source) {
		if (rawAttribute == null) {
			aData.addBuiltinTypeImport( "RAW" );

			return "RAW.TitanBoolean_raw_";
		} else {
			generateCodeRawDescriptor(aData, source);

			return getGenNameOwn(aData) + "_raw_";
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameJsonDescriptor(final JavaGenData aData, final StringBuilder source) {
		if ((jsonAttribute == null || jsonAttribute.empty()) && (getOwnertype() != TypeOwner_type.OT_RECORD_OF || getParentType().getJsonAttribute() == null
				|| !getParentType().getJsonAttribute().as_map)) {
			aData.addBuiltinTypeImport( "JSON" );

			return "JSON.TitanBoolean_json_";
		} else {
			generateCodeJsonDescriptor(aData, source);

			return getGenNameOwn(aData) + "_json_";
		}
	}

}
