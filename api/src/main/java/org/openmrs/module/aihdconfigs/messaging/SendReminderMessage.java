package org.openmrs.module.aihdconfigs.messaging;


import org.json.JSONArray;
import org.json.JSONObject;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.aihdconfigs.Dictionary;
import org.openmrs.module.aihdconfigs.calculation.ConfigCalculations;
import org.openmrs.module.aihdconfigs.calculation.ConfigEmrCalculationUtils;
import org.openmrs.module.aihdconfigs.flags.MissedAppointmentCalculation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ContextNotEmptyException;
import java.util.*;

public class SendReminderMessage {
    private static final int LANG_ENG = 1;
    private static final int LANG_SWA = 2;
    private static final Logger log = LoggerFactory.getLogger(SendReminderMessage.class);


    public static void sendReminder() {
        String username = "codeAlpha";
        String apiKey = "";
        String recipients = "+254716544925,+254774562256, +254724716028";
        // And of course we want our recipients to know what we really do
        String message = "Get in there!!";
//        AfricasTalkingGateway gateway = new AfricasTalkingGateway(username, apiKey);
//
//        try {
//            JSONArray results = gateway.sendMessage(recipients, message);
//            for (int i = 0; i < results.length(); ++i) {
//                JSONObject result = results.getJSONObject(i);
//                System.out.print(result.getString("status") + ","); // status is either "Success" or "error message"
//                System.out.print(result.getString("statusCode") + ",");
//                System.out.print(result.getString("number") + ",");
//                System.out.print(result.getString("messageId") + ",");
//                System.out.println(result.getString("cost"));
//            }
//        } catch (Exception e) {
//            System.out.println("Encountered an error while sending " + e.getMessage());
//        }
        getContacts();

    }

    private String missedAppointMentMesssage(int lang, String name, String date) {
        if (lang == LANG_ENG) {
            return String.format("Dear %s , our records show that you have missed your last appointment(s). This could affect your progress and health goals. You are advised to please urgently visit your doctor within the next 24 hours. Please visit your health care provider urgently.", name);
        } else {
            return String.format("Hujambo %s, records zetu zinatuonyesha kwamba ulikosa kutembelea kituo cha afya kama ilivyopangwa. Hio inaweza kuduru afya na malengo yako ya kiafya. Tafadhali tembelea mhudumu wako wa afya mara moja.", name);
        }
    }


    private static void getContacts() {
        PatientCalculationService patientCalculationService = Context.getService(PatientCalculationService.class);
        PatientCalculationContext context = patientCalculationService.createCalculationContext();
        context.setNow(new Date());
        List<Patient> patients = Context.getPatientService().getAllPatients();
        String message = missedAppointments(patients, context);

    }

    private static  String missedAppointments(List<Patient> patients, PatientCalculationContext context) {
        List<Map<Integer, Integer>> results = new ArrayList<Map<Integer, Integer>>();
        String message = "";
        for (Patient patient : patients) {
            CalculationResultMap lastReturnDateObss = ConfigCalculations.lastObs(Dictionary.getConcept(Dictionary.RETURN_VISIT_DATE), Arrays.asList(patient.getId()), context);
            Date lastScheduledReturnDate = ConfigEmrCalculationUtils.datetimeObsResultForPatient(lastReturnDateObss, patient.getId());
            if (lastScheduledReturnDate != null && ConfigEmrCalculationUtils.daysSince(lastScheduledReturnDate, context) > 0) {
                message = "Missed Appointment";
                log.error(String.format("Missed %s on %s", patient.getId(), lastScheduledReturnDate));
                Map<Integer, Integer> map = new HashMap<Integer, Integer>();
                map.put( patient.getId(), 1);
                results.add(map);

            }
        }
        return message;
    }
}
