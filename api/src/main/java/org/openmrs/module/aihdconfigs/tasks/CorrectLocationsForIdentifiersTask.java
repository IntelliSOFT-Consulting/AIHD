package org.openmrs.module.aihdconfigs.tasks;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.aihdconfigs.metadata.PersonAttributeTypes;
import org.openmrs.module.aihdconfigs.utils.JSONParserUtil;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CorrectLocationsForIdentifiersTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(CorrectLocationsForIdentifiersTask.class);

    @Override
    public void execute() {
        if (!isExecuting) {
            if (log.isDebugEnabled()) {
                log.debug("Xform provides only the first location for identifiers, we need to use logged in location instead");
            }

            startExecuting();
            try {
                //do all the work here
                correctPatientIdentifierLocationsAtRegistration();
            } catch (Exception e) {
                log.error("Error while Correcting and updating the identifier location", e);
            } finally {
                stopExecuting();
            }
        }
    }

    private void correctPatientIdentifierLocationsAtRegistration(){
        PatientService patientService = Context.getPatientService();
        AdministrationService as = Context.getAdministrationService();
        List<Location> allLocations = Context.getLocationService().getAllLocations();
        PatientIdentifierType pit = patientService.getPatientIdentifierTypeByUuid("b9ba3418-7108-450c-bcff-7bc1ed5c42d1");
        List<List<Object>> patientIds_withIds = as.executeSQL("SELECT patient_id FROM patient_identifier WHERE identifier_type IN (SELECT patient_identifier_type_id FROM patient_identifier_type WHERE uuid = 'b9ba3418-7108-450c-bcff-7bc1ed5c42d1')", true);
        if(patientIds_withIds.size() > 0){
            String mfl = "";
            Location wantedLocation = null;
            for (List<Object> row : patientIds_withIds) {
                Patient p = patientService.getPatient((Integer) row.get(0));
                PatientIdentifier identifiers= p.getPatientIdentifier(pit);
                if(identifiers != null && StringUtils.isNotEmpty(identifiers.getIdentifier())){
                    String value =identifiers.getIdentifier();
                    if(value.contains("-") && value.length() > 10){
                        mfl = value.split("-")[0];
                        //now find location that has the same mfl code as mfl
                        for(Location location:allLocations) {
                            Set<LocationAttribute> allAttributes = new HashSet<LocationAttribute>(location.getAttributes());
                            for(LocationAttribute locationAttribute: allAttributes){
                                if(locationAttribute.getValue().toString().equals(mfl)){
                                    wantedLocation = locationAttribute.getLocation();
                                    if(identifiers.getLocation() == null ||  !identifiers.getLocation().equals(wantedLocation)) {
                                        identifiers.setLocation(wantedLocation);
                                        break;
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}
