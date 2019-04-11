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
import org.openmrs.module.aihdconfigs.metadata.PatientIdentifierTypes;
import org.openmrs.module.aihdconfigs.metadata.PersonAttributeTypes;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
        PatientIdentifierType pit_aihd = patientService.getPatientIdentifierTypeByUuid(PatientIdentifierTypes.AIHD_PATIENT_NUMBER.uuid());
        List<List<Object>> patientIds_withouIds = as.executeSQL("SELECT patient_id FROM patient WHERE patient_id NOT IN(select patient_id from patient_identifier where identifier_type=4)", true);

        if(patientIds_withouIds.size() > 0) {

            String prefix = "";
            String suffix = "";
            PersonAttributeType attributeType = MetadataUtils.existing(PersonAttributeType.class, PersonAttributeTypes.PATIENT_LOCATION.uuid());
            LocationAttributeType locationAttributeType = Context.getLocationService().getLocationAttributeTypeByName("MflCode");
            PersonAttributeType personAttributeTypeMflForPatient = Context.getPersonService().getPersonAttributeTypeByUuid(PersonAttributeTypes.PATIENT_MFL_CODE.uuid());
            for (List<Object> row : patientIds_withouIds) {
                Patient p = patientService.getPatient((Integer) row.get(0));
                PersonAttribute mflCodeForPatient = p.getAttribute(personAttributeTypeMflForPatient);
                PersonAttribute personAttribute = p.getAttribute(attributeType);

                if(personAttribute != null && StringUtils.isNotEmpty(personAttribute.getValue())){
                    Location location = Context.getLocationService().getLocation(personAttribute.getValue().replace("_", " "));

                      if(location != null) {
                          Set<LocationAttribute> attribute = new HashSet<LocationAttribute>(location.getAttributes());
                          if(attribute.size() > 0){
                              for(LocationAttribute locationAttribute: attribute){
                                  if(locationAttribute.getAttributeType().equals(locationAttributeType)){

                                      prefix = (String) locationAttribute.getValue();
                                      suffix = String.valueOf(identifierList_forPatientsWithoutId(patientService, prefix) + 1);
                                      String finalSuffixes = finalSuffix(suffix);
                                      UUID uuid = UUID.randomUUID();
                                      PatientIdentifier aihdId = new PatientIdentifier();
                                      aihdId.setIdentifierType(pit_aihd);
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

                else if (mflCodeForPatient != null && StringUtils.isNotEmpty(mflCodeForPatient.getValue())) {
                    List<List<Object>> location = as.executeSQL("SELECT location_id FROM location_attribute WHERE value_reference='"+mflCodeForPatient.getValue()+"'", true);
                    if(location.size() > 0) {
                        for (List<Object> loc : location) {
                            Location useLocation = Context.getLocationService().getLocation((Integer) loc.get(0));
                            if(useLocation != null) {
                                prefix = mflCodeForPatient.getValue();
                                suffix = String.valueOf(identifierList_forPatientsWithoutId(patientService, prefix) + 1);
                                String finalSuffixes = finalSuffix(suffix);
                                UUID uuid = UUID.randomUUID();
                                PatientIdentifier aihdId = new PatientIdentifier();
                                aihdId.setIdentifierType(pit_aihd);
                                aihdId.setUuid(String.valueOf(uuid));
                                aihdId.setIdentifier(prefix + "-" + finalSuffixes);
                                aihdId.setLocation(useLocation);
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

    private Integer identifierList_forPatientsWithoutId(PatientService patientService, String prefix){
        AdministrationService as = Context.getAdministrationService();
        String prefixLike = prefix+"%";
        List<List<Object>> patientIdentifierValues = as.executeSQL("SELECT patient_identifier_id FROM patient_identifier WHERE identifier_type IN (SELECT patient_identifier_type_id FROM patient_identifier_type WHERE uuid = 'b9ba3418-7108-450c-bcff-7bc1ed5c42d1') AND identifier LIKE "+"'"+prefixLike+"'", true);
        List<PatientIdentifier> finalList_without = new ArrayList<PatientIdentifier>();
        for(List<Object> row : patientIdentifierValues){
            PatientIdentifier patientIdentifier = patientService.getPatientIdentifier((Integer) row.get(0));
            if(patientIdentifier.getIdentifier() != null && patientIdentifier.getIdentifier().length() > 10 && patientIdentifier.getIdentifier().contains("-")){
                String pref = patientIdentifier.getIdentifier().split("-")[0];
                if(prefix.equals(pref)) {
                    finalList_without.add(patientIdentifier);
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
