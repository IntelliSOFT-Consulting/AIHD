package org.openmrs.module.aihdconfigs.tasks;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.aihdconfigs.metadata.PersonAttributeTypes;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        List<List<Object>> patientIds_withIds = as.executeSQL("SELECT patient_id FROM patient_identifier WHERE identifier_type IN (SELECT patient_identifier_type_id FROM patient_identifier_type WHERE uuid = 'b9ba3418-7108-450c-bcff-7bc1ed5c42d1')", true);
        List<List<Object>> patientIds_withouIds = as.executeSQL("SELECT patient_id FROM patient_identifier WHERE patient_id NOT IN (SELECT patient_id FROM patient_identifier p INNER JOIN patient_identifier_type pt ON (p.identifier_type = pt.patient_identifier_type_id AND pt.uuid = 'b9ba3418-7108-450c-bcff-7bc1ed5c42d1'))", true);
        if(patientIds_withIds.size() > 0){
            String prefix = "";
            String suffix = "";
            for (List<Object> row : patientIds_withIds) {
                Patient p = patientService.getPatient((Integer) row.get(0));
                PatientIdentifier identifiers= p.getPatientIdentifier(pit);
                if(identifiers != null && identifiers.getLocation() != null) {
                    if(identifiers.getIdentifier().endsWith("-")) {
                        //get that patient and update their identifier
                        prefix = identifiers.getIdentifier().split("-")[0];
                        suffix = String.valueOf(identifierList_forPatientsWithId(pit, patientService, identifiers, prefix) + 1);
                        String finalSuffixes = finalSuffix(suffix);

                        identifiers.setIdentifier(prefix+"-"+finalSuffixes);
                    }
                }
            }
        }
        if(patientIds_withouIds.size() > 0) {
            String prefix = "";
            String suffix = "";
            PersonAttributeType attributeType = MetadataUtils.existing(PersonAttributeType.class, PersonAttributeTypes.PATIENT_LOCATION.uuid());
            LocationAttributeType locationAttributeType = Context.getLocationService().getLocationAttributeTypeByName("MflCode");
            for (List<Object> row : patientIds_withouIds) {
                Patient p = patientService.getPatient((Integer) row.get(0));
                PersonAttribute personAttribute = p.getAttribute(attributeType);
                if(personAttribute != null && StringUtils.isNotEmpty(personAttribute.getValue())){
                    Location location = Context.getLocationService().getLocation(personAttribute.getValue().replace("_", " "));
                      if(location != null) {
                          Set<LocationAttribute> attribute = new HashSet<LocationAttribute>(location.getAttributes());
                          if(attribute.size() > 0){
                              for(LocationAttribute locationAttribute: attribute){
                                  if(locationAttribute.getAttributeType().equals(locationAttributeType)){

                                      prefix = (String) locationAttribute.getValue();
                                      suffix = String.valueOf(identifierList_forPatientsWithoutId(pit, patientService, prefix) + 1);
                                      String finalSuffixes = finalSuffix(suffix);
                                      UUID uuid = UUID.randomUUID();
                                      PatientIdentifier aihdId = new PatientIdentifier();
                                      aihdId.setIdentifierType(pit);
                                      aihdId.setUuid(String.valueOf(uuid));
                                      aihdId.setIdentifier(prefix+"-"+finalSuffixes);
                                      aihdId.setLocation(location);
                                      aihdId.setPreferred(true);
                                      aihdId.setCreator(Context.getAuthenticatedUser());
                                      aihdId.setDateCreated(new Date());

                                      //
                                      p.addIdentifier(aihdId);

                                  }
                              }
                          }
                      }
                }



            }
        }

    }

    private Integer identifierList_forPatientsWithId(PatientIdentifierType pit, PatientService patientService, PatientIdentifier identifiers, String prefix){
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

    private Integer identifierList_forPatientsWithoutId(PatientIdentifierType pit, PatientService patientService, String prefix){
        List<PatientIdentifier> allIdentifiersWithAihds = patientService.getPatientIdentifiers(null, Arrays.asList(pit), null, null, true);
        List<PatientIdentifier> finalList_without = new ArrayList<PatientIdentifier>();
        for(PatientIdentifier patientIdentifiers:allIdentifiersWithAihds){
            if(patientIdentifiers.getIdentifier() != null && patientIdentifiers.getIdentifier().length() > 10 && patientIdentifiers.getIdentifier().contains("-")){
                //get the first part of the identifier
                String pref = patientIdentifiers.getIdentifier().split("-")[0];
                if(pref.equals(prefix)) {
                    finalList_without.add(patientIdentifiers);
                }
            }
        }

        return finalList_without.size();
    }

    private String finalSuffix(String suffix){
        String results = "";
        if(suffix.length() == 1){
            results ="0000"+suffix;
        }
        else if(suffix.length() == 2){
            results ="000"+suffix;
        }
        else if(suffix.length() == 3){
            results ="00"+suffix;
        }
        else if(suffix.length() == 4){
            results ="0"+suffix;
        }

        return results;
    }

}
