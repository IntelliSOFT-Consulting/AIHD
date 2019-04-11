package org.openmrs.module.aihdconfigs.tasks;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.*;
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

public class ConvertPatientPhoneNumberIntoIdentifierTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(ConvertPatientPhoneNumberIntoIdentifierTask.class);

    @Override
    public void execute() {
        if (!isExecuting) {
            if (log.isDebugEnabled()) {
                log.debug("Changing the patient phone number form person attribute to patient identifier");
            }

            startExecuting();
            try {
                //do all the work here
                convertPhoneNumberIntoPatientIdentifier();
            }
            catch (Exception e) {
                log.error("Error while converting phone number for patient into patient identifier", e);
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
        PersonAttributeType mobileNumberAlternative= MetadataUtils.existing(PersonAttributeType.class, PersonAttributeTypes.ALTERNATIVE_PATIENT_PHONE_NUMBER.uuid());
        PatientIdentifierType altPhone = MetadataUtils.existing(PatientIdentifierType.class, PatientIdentifierTypes.ALTERNATIVE_PHONE_NUMBER.uuid());
        List<List<Object>> patientIds_withoutAlternativeMobileNo = as.executeSQL("SELECT patient_id FROM patient WHERE patient_id NOT IN(select patient_id from patient_identifier where identifier_type=8)", true);
        if(patientIds_withoutAlternativeMobileNo.size() > 0){
            for (List<Object> row : patientIds_withoutAlternativeMobileNo) {
                Patient p = patientService.getPatient((Integer) row.get(0));
                PersonAttribute alternativeMobileNoAttr =p.getAttribute(mobileNumberAlternative);
                if(alternativeMobileNoAttr != null && StringUtils.isNotEmpty(alternativeMobileNoAttr.getValue())){
                    PatientIdentifier alternativeMobileNumberIdentifier = p.getPatientIdentifier(altPhone);
                    if(alternativeMobileNumberIdentifier == null) {

                        PatientIdentifier newMobileNumber = new PatientIdentifier();
                        newMobileNumber.setIdentifier(alternativeMobileNoAttr.getValue());
                        newMobileNumber.setIdentifierType(altPhone);
                        newMobileNumber.setCreator(Context.getAuthenticatedUser());
                        newMobileNumber.setDateCreated(new Date());

                        //
                        p.addIdentifier(newMobileNumber);
                    }

                }
            }
        }
    }
}
