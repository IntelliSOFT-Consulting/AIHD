package org.openmrs.module.aihdconfigs.tasks;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.aihdconfigs.metadata.PatientIdentifierTypes;
import org.openmrs.module.aihdconfigs.metadata.PersonAttributeTypes;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;


public class ConvertOtherPhoneNumberToIdentifierTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(ConvertPatientPhoneNumberIntoIdentifierTask.class);

    @Override
    public void execute() {
        if (!isExecuting) {
            if (log.isDebugEnabled()) {
                log.debug("Changing the other patient phone number form person attribute to patient identifier");
            }

            startExecuting();
            try {
                //do all the work here
                convertPhoneNumberIntoPatientIdentifier();
            }
            catch (Exception e) {
                log.error("Error while converting other phone number for patient into patient identifier", e);
            }
            finally {
                stopExecuting();
            }
        }
    }

    private void convertPhoneNumberIntoPatientIdentifier() {
        //logic to perform this will follow here
        PatientService patientService = Context.getPatientService();
        AdministrationService as = Context.getAdministrationService();
        PersonAttributeType otherPatientPhoneNumber= MetadataUtils.existing(PersonAttributeType.class, PersonAttributeTypes.OTHER_PATIENT_PHONE_NUMBER.uuid());
        PatientIdentifierType otherPhone = MetadataUtils.existing(PatientIdentifierType.class, PatientIdentifierTypes.OTHER_PATIENT_PHONE_NUMBER.uuid());
        List<List<Object>> patientIds_withoutOtherMobileNo = as.executeSQL("SELECT patient_id FROM patient WHERE patient_id NOT IN(select patient_id from patient_identifier where identifier_type=9)", true);
        if(patientIds_withoutOtherMobileNo.size() > 0){
            for (List<Object> row : patientIds_withoutOtherMobileNo) {
                Patient p = patientService.getPatient((Integer) row.get(0));
                PersonAttribute otherMobileNoAttr =p.getAttribute(otherPatientPhoneNumber);
                if(otherMobileNoAttr != null && StringUtils.isNotEmpty(otherMobileNoAttr.getValue())){
                    PatientIdentifier otherMobileNumberIdentifier = p.getPatientIdentifier(otherPhone);
                    if(otherMobileNumberIdentifier == null) {

                        PatientIdentifier newOtherMobileNumber = new PatientIdentifier();
                        newOtherMobileNumber.setIdentifier(otherMobileNoAttr.getValue());
                        newOtherMobileNumber.setIdentifierType(otherPhone);
                        newOtherMobileNumber.setCreator(Context.getAuthenticatedUser());
                        newOtherMobileNumber.setDateCreated(new Date());

                        p.addIdentifier(newOtherMobileNumber);
                    }

                }
            }
        }
    }
}
