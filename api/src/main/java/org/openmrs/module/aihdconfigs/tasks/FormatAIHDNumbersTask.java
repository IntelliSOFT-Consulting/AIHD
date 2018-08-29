package org.openmrs.module.aihdconfigs.tasks;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FormatAIHDNumbersTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(FormatAIHDNumbersTask.class);

    @Override
    public void execute() {
        if (!isExecuting) {
            if (log.isDebugEnabled()) {
                log.debug("Formatting patients numbers to match location and auto generated numbers");
            }

            startExecuting();
            try {
                //do all the work here
                formatAihdPatientNumbers();
            }
            catch (Exception e) {
                log.error("Error while formatting AIHD number for  for patient", e);
            }
            finally {
                stopExecuting();
            }
        }
    }

    private void formatAihdPatientNumbers(){
        PatientService patientService = Context.getPatientService();
        AdministrationService as = Context.getAdministrationService();
        PatientIdentifierType pit = patientService.getPatientIdentifierTypeByUuid("b9ba3418-7108-450c-bcff-7bc1ed5c42d1");
        List<List<Object>> patientIds = as.executeSQL("SELECT patient_id FROM patient_identifier WHERE identifier_type IN (SELECT patient_identifier_type_id FROM patient_identifier_type WHERE uuid = 'b9ba3418-7108-450c-bcff-7bc1ed5c42d1')", true);
        if(patientIds.size() > 0){
            String prefix = "";
            String suffix = "";
            for (List<Object> row : patientIds) {
                Patient p = patientService.getPatient((Integer) row.get(0));
                PatientIdentifier identifiers= p.getPatientIdentifier(pit);
                if(identifiers != null && identifiers.getLocation() != null) {
                    if(identifiers.getIdentifier().endsWith("-")) {
                        //get that patient and update their identifier
                        prefix = identifiers.getIdentifier().split("-")[0];
                        suffix = String.valueOf(identifierList(pit, patientService, identifiers, prefix) + 1);
                         if(suffix.length() == 1){
                            suffix ="0000"+suffix;
                        }
                        else if(suffix.length() == 2){
                            suffix ="000"+suffix;
                        }
                        else if(suffix.length() == 3){
                            suffix ="00"+suffix;
                        }
                        else if(suffix.length() == 4){
                            suffix ="0"+suffix;
                        }
                        identifiers.setIdentifier(prefix+"-"+suffix);
                    }
                }
            }
        }

    }

    private Integer identifierList(PatientIdentifierType pit, PatientService patientService, PatientIdentifier identifiers, String prefix){
        List<PatientIdentifier> allIdentifiers = patientService.getPatientIdentifiers(null, Arrays.asList(pit), Arrays.asList(identifiers.getLocation()), null, true);
        List<PatientIdentifier> finalList = new ArrayList<PatientIdentifier>();
        for(PatientIdentifier patientIdentifier:allIdentifiers){
            if(patientIdentifier.getIdentifier() != null && patientIdentifier.getIdentifier().length() > 10 && patientIdentifier.getIdentifier().contains("-")){
                //get the first part of the identifier
                String pref = patientIdentifier.getIdentifier().split("-")[0];
                if(pref.equals(prefix)) {
                    finalList.add(patientIdentifier);
                }
            }
        }

        return finalList.size();
    }

}
