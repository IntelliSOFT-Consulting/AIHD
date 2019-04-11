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

public class ConvertNationalIdFromPersonAttributeToPatientIdentifier extends AbstractTask {


    private static final Logger log = LoggerFactory.getLogger(ConvertNationalIdFromPersonAttributeToPatientIdentifier.class);

    @Override
    public void execute() {
        if (!isExecuting) {
            if (log.isDebugEnabled()) {
                log.debug("Changing the patient national ID form person attribute to patient identifier");
            }

            startExecuting();
            try {
                //do all the work here
                convertNationalIdIntoPatientIdentifier();
            } catch (Exception e) {
                log.error("Error while converting patient national ID for patient into patient identifier", e);
            } finally {
                stopExecuting();
            }
        }
    }

    private void convertNationalIdIntoPatientIdentifier() {
        PatientService patientService = Context.getPatientService();
        AdministrationService as = Context.getAdministrationService();
        PersonAttributeType nationalId= MetadataUtils.existing(PersonAttributeType.class, PersonAttributeTypes.NATIONAL_ID_NUMBER.uuid());
        PatientIdentifierType nationalIdPit = MetadataUtils.existing(PatientIdentifierType.class, PatientIdentifierTypes.NATIONAL_ID_NUMBER.uuid());
        List<List<Object>> patientIds_withoutNationalId = as.executeSQL("SELECT patient_id FROM patient WHERE patient_id NOT IN(select patient_id from patient_identifier where identifier_type=7)", true);
        if(patientIds_withoutNationalId.size() > 0) {
            for (List<Object> nRow : patientIds_withoutNationalId) {
                Patient patient = patientService.getPatient((Integer) nRow.get(0));
                PersonAttribute nationalIdNoAttr =patient.getAttribute(nationalId);
                if(nationalIdNoAttr != null && StringUtils.isNotEmpty(nationalIdNoAttr.getValue())){
                    PatientIdentifier nationalIdNumberIdentifier = patient.getPatientIdentifier(nationalIdPit);
                    if(nationalIdNumberIdentifier == null) {

                        PatientIdentifier nId = new PatientIdentifier();
                        nId.setIdentifier(nationalIdNoAttr.getValue());
                        nId.setIdentifierType(nationalIdPit);
                        nId.setCreator(Context.getAuthenticatedUser());
                        nId.setDateCreated(new Date());

                        patient.addIdentifier(nId);
                    }

                }
            }
        }
    }
}
