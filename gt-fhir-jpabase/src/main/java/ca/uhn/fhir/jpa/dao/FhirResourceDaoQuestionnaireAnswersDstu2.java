package ca.uhn.fhir.jpa.dao;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2015 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.OperationOutcome;
import ca.uhn.fhir.model.dstu2.resource.Questionnaire;
import ca.uhn.fhir.model.dstu2.resource.QuestionnaireAnswers;
import ca.uhn.fhir.model.dstu2.resource.ValueSet;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;

public class FhirResourceDaoQuestionnaireAnswersDstu2 extends FhirResourceDaoDstu2<QuestionnaireAnswers> {

	private FhirContext myRefImplCtx = FhirContext.forDstu2Hl7Org();
	
	//@Override
	//FIXME
	protected void validateResourceForStorage(IResource theResource) {
		//super.validateResourceForStorage(theResource);

		QuestionnaireAnswers qa = (QuestionnaireAnswers) theResource;
		if (qa.getQuestionnaire().getReference().isEmpty()) {
			return;
		}

		FhirValidator val = myRefImplCtx.newValidator();
		val.setValidateAgainstStandardSchema(false);
		val.setValidateAgainstStandardSchematron(false);

//		FhirQuestionnaireAnswersValidator module = new FhirQuestionnaireAnswersValidator();
//		module.setResourceLoader(new JpaResourceLoader());
//		val.registerValidatorModule(module);

		ValidationResult result = val.validateWithResult(myRefImplCtx.newJsonParser().parseResource(getBaseDao().getContext().newJsonParser().encodeResourceToString(qa)));
		if (!result.isSuccessful()) {
			IBaseOperationOutcome oo = getBaseDao().getContext().newJsonParser().parseResource(OperationOutcome.class, myRefImplCtx.newJsonParser().encodeResourceToString(result.toOperationOutcome()));
			//throw new UnprocessableEntityException(getBaseDao().getContext(), oo);
		}
	}

	public class JpaResourceLoader {

		//@Override
		public <T extends IBaseResource> T load(Class<T> theType, IIdType theId) throws ResourceNotFoundException {
			/*
			 * The QuestionnaireAnswers validator uses RI structures, so for now we need
			 * to convert between that and HAPI structures. This is a bit hackish, but
			 * hopefully it will go away at some point.
			 */
			if ("ValueSet".equals(theType.getSimpleName())) {
				IFhirResourceDao<ValueSet> dao = getBaseDao().getDao(ValueSet.class);
				ValueSet in = dao.read(theId);
				String encoded = getBaseDao().getContext().newJsonParser().encodeResourceToString(in);
				
				// TODO: this is temporary until structures-dstu2 catches up to structures-hl7org.dstu2
				encoded = encoded.replace("\"define\"", "\"codeSystem\"");
				
				return myRefImplCtx.newJsonParser().parseResource(theType, encoded);
			} else if ("Questionnaire".equals(theType.getSimpleName())) {
				IFhirResourceDao<Questionnaire> dao = getBaseDao().getDao(Questionnaire.class);
				Questionnaire vs = dao.read(theId);
				return myRefImplCtx.newJsonParser().parseResource(theType, getBaseDao().getContext().newJsonParser().encodeResourceToString(vs));
			} else {
				// Should not happen, validator will only ask for these two
				throw new IllegalStateException("Unexpected request to load resource of type " + theType);
			}

		}

	}

}
