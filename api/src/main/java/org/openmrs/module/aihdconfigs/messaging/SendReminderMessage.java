package org.openmrs.module.aihdconfigs.messaging;


import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Patient;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.aihdconfigs.ConfigCoreUtils;
import org.openmrs.module.aihdconfigs.Dictionary;
import org.openmrs.module.aihdconfigs.Facilities;
import org.openmrs.module.aihdconfigs.calculation.ConfigCalculations;
import org.openmrs.module.aihdconfigs.calculation.ConfigEmrCalculationUtils;
import org.openmrs.module.aihdconfigs.metadata.PersonAttributeTypes;
import org.openmrs.module.aihdconfigs.metadata.PatientIdentifierTypes;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class SendReminderMessage {
    private static final int LANG_ENG = 1;
    private static final int LANG_SWA = 2;
    private static final Logger log = LoggerFactory.getLogger(SendReminderMessage.class);


    private static String getTokenFromFile() throws IOException {
        File file = new File(OpenmrsUtil.getApplicationDataDirectory() + "/token.txt");
        if (file.exists() && file.canRead()) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String key = reader.readLine();
            reader.close();
            return key;
        }
        return "";

    }

    public static void buildMessage(String phone, String message) throws IOException {
        String username = "ncd";
        String apiKey = getTokenFromFile();
        String from = "NairobiNCD";
        if (StringUtils.isNotEmpty(apiKey)) {
            AfricasTalking.initialize(username, apiKey);
            SmsService sms = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);

            try {
                List<Recipient> response = sms.send(message,from, new String[]{phone}, true);
                for (Recipient recipient : response) {
                    if(!recipient.status.equals("Success")){
                        log.error("SMS sending Failure : Number %s status %s",recipient.number,recipient.status);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static String missedAppointMentMesssage(String lang, String name, PersonAttribute facilityName) {
        if (lang.equals("english")) {
            return String.format("Dear %s, our records show that you have missed your last appointment(s) at %s. This could affect your progress and health goals. You are advised to please visit your doctor or health care provider urgently.", name, facilityName);
        } else {
            return String.format("Hujambo %s, rekodi zetu zinatuonyesha kwamba ulikosa kutembelea kituo cha afya %s kama ilivyopangwa. Hii inaweza kudhuru maendeleo na malengo yako ya kiafya. Tafadhali tembelea mhudumu wako wa afya mara moja.", name, facilityName);
        }
    }


    public static void sendReminderMssage() throws IOException {
        PatientCalculationService patientCalculationService = Context.getService(PatientCalculationService.class);
        PatientCalculationContext context = patientCalculationService.createCalculationContext();
        context.setNow(new Date());

        String message = "";
        Set<Patient> results = missedAppointments(context);
        PersonAttributeType attributeType = MetadataUtils.existing(PersonAttributeType.class, PersonAttributeTypes.TELEPHONE_NUMBER.uuid());
        PersonAttributeType langAttributeType = MetadataUtils.existing(PersonAttributeType.class, PersonAttributeTypes.USER_LANGUAGE.uuid());
        PersonAttributeType patientLocation = MetadataUtils.existing(PersonAttributeType.class, PersonAttributeTypes.PATIENT_LOCATION.uuid());

        for (Patient patient : results) {
            String name = String.format("%s", patient.getGivenName());
            PersonAttribute personAttribute = patient.getPerson().getAttribute(attributeType);
            PersonAttribute facilityName = patient.getPerson().getAttribute(patientLocation);
    
            if (langAttributeType != null) {
                PersonAttribute language = patient.getPerson().getAttribute(langAttributeType);
                
                message = missedAppointMentMesssage((language != null && StringUtils.isNotEmpty(language.getValue()) ? language.getValue() : "kiswahili"), name, facilityName);
            } else {
                message = missedAppointMentMesssage("kiswahili", name, facilityName);
            }
            if (StringUtils.isNotEmpty(convertPhoneNumber(personAttribute.getValue()))){
                buildMessage(convertPhoneNumber(personAttribute.getValue()), message);
            }
        }

    }

    private static Set<Patient> missedAppointments(PatientCalculationContext context) {
        Set<Patient> results = new HashSet<Patient>();
        CalculationResultMap lastReturnDateObss = ConfigCalculations.lastObs(Dictionary.getConcept(Dictionary.RETURN_VISIT_DATE), ConfigCoreUtils.cohort(), context);
        for (Patient patient : Context.getPatientService().getAllPatients()) {
            Date lastScheduledReturnDate = ConfigEmrCalculationUtils.datetimeObsResultForPatient(lastReturnDateObss, patient.getId());
            if (lastScheduledReturnDate != null && ConfigEmrCalculationUtils.daysSince(lastScheduledReturnDate, context) == 1 ) {
                results.add(patient);
            }
        }
        return results;
    }

    private static String convertPhoneNumber(String number) {
        String phoneNUmber = number.trim().replaceAll("\\s", "");
        if (phoneNUmber.length() == 13 && phoneNUmber.substring(0, Math.min(phoneNUmber.length(), 4)).equals("+254")) {
            return phoneNUmber;
        } else if (phoneNUmber.length() == 10 && phoneNUmber.charAt(0) == '0') {
            phoneNUmber = phoneNUmber.replaceFirst("0", "+254");
            return phoneNUmber;
        } else if (phoneNUmber.length() == 12 && phoneNUmber.substring(0, Math.min(phoneNUmber.length(), 3)).equals("254")) {
            phoneNUmber = String.format("%s%S", "+", phoneNUmber);
            return phoneNUmber;
        } else if (phoneNUmber.length() == 9) {
            phoneNUmber = String.format("%s%s", "+254", phoneNUmber);
            return phoneNUmber;
        } else {
            return "";
        }
    }

    // private static String getFacilityName(String name){
    //     PersonAttributeType attributeType = MetadataUtils.existing(PersonAttributeType.class, PersonAttributeTypes.PATIENT_LOCATION.uuid());
    //     LocationAttributeType locationAttributeType = Context.getLocationService().getLocationAttributeTypeByName("MflCode");
    //     PersonAttributeType personAttributeTypeMflForPatient = Context.getPersonService().getPersonAttributeTypeByUuid(PersonAttributeTypes.PATIENT_MFL_CODE.uuid());
    //     PatientService patientService = Context.getPatientService();
    //     AdministrationService as = Context.getAdministrationService();
    //     PatientIdentifierType pit_aihd = patientService.getPatientIdentifierTypeByUuid(PatientIdentifierTypes.AIHD_PATIENT_NUMBER.uuid());
        
    //     Patient p = patientService.getPatient((Integer) row.get(0));
    //     PersonAttribute mflCodeForPatient = p.getAttribute(personAttributeTypeMflForPatient);
    //     PersonAttribute personAttribute = p.getAttribute(attributeType);
       
    //         if(personAttribute != null && StringUtils.isNotEmpty(personAttribute.getValue())){
    //             Location location = Context.getLocationService().getLocation(personAttribute.getValue());
    //               if(location != null) {
    //                   Set<LocationAttribute> attribute = new HashSet<LocationAttribute>(location.getAttributes());
    //                   if(attribute.size() > 0){
    //                       for(LocationAttribute locationAttribute: attribute){
    //                           if(locationAttribute.getAttributeType().equals(locationAttributeType)){
    //                               UUID uuid = UUID.randomUUID();
    //                               PatientIdentifier aihdId = new PatientIdentifier();
    //                               aihdId.getLocation();
    //                           }
    //                       }
    //                   }
    //               }
    //         }

    //         else if (mflCodeForPatient != null && StringUtils.isNotEmpty(mflCodeForPatient.getValue())) {
    //             List<List<Object>> location = as.executeSQL("SELECT location_id FROM location_attribute WHERE value_reference='"+mflCodeForPatient.getValue()+"'", true);
    //             if(location.size() > 0) {
    //                 for (List<Object> loc : location) {
    //                     Location useLocation = Context.getLocationService().getLocation((Integer) loc.get(0));
    //                     if(useLocation != null) {
    //                         UUID uuid = UUID.randomUUID();
    //                         PatientIdentifier aihdId = new PatientIdentifier();
    //                         aihdId.setLocation(useLocation);
    //                         aihdId.getLocation();
    //                     }
    //                 }
    //             }

    //             }
            
     
    //     return name;
    // }
}
