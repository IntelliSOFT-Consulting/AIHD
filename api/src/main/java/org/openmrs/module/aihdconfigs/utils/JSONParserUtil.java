package org.openmrs.module.aihdconfigs.utils;


import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.*;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class JSONParserUtil {
    private static final Logger log = LoggerFactory.getLogger(JSONParserUtil.class);

    private static List<File> getFilesFromDir() {
        List<File> directory_files = new ArrayList<File>();
        File file = new File(OpenmrsUtil.getApplicationDataDirectory() + "/data_files");
        if (!file.exists()) {
            if (!file.mkdirs())
                return null;
        }
        log.error("file path" + file.getAbsolutePath());
        if (file.listFiles() != null && file.listFiles().length > 0) {
            directory_files = Arrays.asList(file.listFiles());
        } else {
            log.error("file error" + file.getAbsolutePath());
        }
        return directory_files;
    }

    private static boolean moveProcessedFiles(File file) {
        File processed_dir = new File(OpenmrsUtil.getApplicationDataDirectory() + "/processed");
        if (!processed_dir.exists()) {
            if (!processed_dir.mkdirs())
                return false;
        }
        String newFileName = file.getName() + "_" + new Date().toString();
        newFileName = newFileName.replace(" ", "_");
        boolean renamedFile = file.renameTo(new File(processed_dir + "/" + newFileName));
        if (!renamedFile) {
            log.error("Unable to move " + file.getName() + "\n Proceeding to delete");
            if (file.delete())
                return false;
        }
        return true;
    }

    private static boolean moveUnparsabbleFiles(File file) {
        File processed_dir = new File(OpenmrsUtil.getApplicationDataDirectory() + "/failed");
        if (!processed_dir.exists()) {
            if (!processed_dir.mkdirs())
                return false;
        }
        String newFileName = file.getName() + "_" + new Date().toString();
        newFileName = newFileName.replace(" ", "_");
        boolean renamedFile = file.renameTo(new File(processed_dir + "/" + newFileName));
        if (!renamedFile) {
            log.error("Unable to move " + file.getName() + "\n Proceeding to delete");
            if (file.delete())
                return false;
        }
        return true;
    }

    public static void readXML() {
        log.error("Reading files...");
        List<File> directory_files = getFilesFromDir();
        for (File xmlFile : directory_files) {
            if (xmlFile.getName().endsWith(".xml")) {
                try {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(xmlFile);
                    doc.getDocumentElement().normalize();

                    if (doc.getDocumentElement().getNodeName().equals("form")) {
                        //Get encounters
                        NodeList encounter_nodes_list = doc.getElementsByTagName("encounter");

                        //Iterate through all encounters
                        for (int i = 0; i < encounter_nodes_list.getLength(); i++) {
                            Node encounter_node = encounter_nodes_list.item(i);
                            NodeList obs_nodelist = encounter_node.getChildNodes(); //Get all obs

                            //Iterate through all obs
                            for (int j = 0; j < obs_nodelist.getLength(); j++) {
                                Node obs_node = obs_nodelist.item(j);
                                if (obs_node.getNodeName().equals("obs")) {
                                    String concept_id = ((Element) obs_node).getElementsByTagName("concept").item(0).getTextContent();
                                    String answer_concept_id = ((Element) obs_node).getElementsByTagName("answerConcept").item(0).getTextContent();
                                    if (answer_concept_id.isEmpty())
                                        System.out.println("empty");
                                    else
                                        System.out.println(answer_concept_id);
                                }
                            }
                        }
                    }


                } catch (Exception e) {
                    if (e instanceof SAXParseException) {
                        log.error("Invalid XML file : " + xmlFile.getName());
                    }
                    e.printStackTrace();
                }
            }
        }
    }

    public static void readJSOFile() {
        log.info("Reading JSON files...");
        List<File> directory_files = getFilesFromDir();
        for (File jsonFile : directory_files) {
            if (jsonFile.getName().endsWith(".json")) {
                try {
                    FileReader fileReader = new FileReader(jsonFile);
                    ObjectMapper mapper = new ObjectMapper();
                    ;
                    JsonNode rootNode = mapper.readTree(fileReader);
                    log.error("Looping " + rootNode.size());
                    JsonNode obsNode = rootNode.path("obs");
                    JsonNode encounterDateNode = rootNode.path("encounterDate");
                    JsonNode patientId = rootNode.path("patient_id");
                    JsonNode encounterTypeUuid = rootNode.path("formEncounterType");
                    JsonNode encounterProviderUuid = rootNode.path("encounterProvider");
                    JsonNode locationUuid = rootNode.path("location_id");
                    JsonNode formName = rootNode.path("formDescription");

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = null;
                    try {
                        String dateString = encounterDateNode.getTextValue();
                        date = formatter.parse(dateString);

                        User user = Context.getUserService().getUserByUuid(encounterProviderUuid.getTextValue());
                        ProviderService service = Context.getProviderService();
                        List<Provider> provider = new ArrayList<Provider>(service.getProvidersByPerson(user.getPerson()));
                        Patient patient = Context.getPatientService().getPatient(Integer.valueOf(patientId.getTextValue()));
                        String locationName = locationUuid.getTextValue().replace("_", " ");
                        Location location = Context.getLocationService().getLocation(locationName);
                        EncounterRole encounterRole = Context.getEncounterService().getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66");
                        Form form = Context.getFormService().getFormByUuid(formName.getTextValue());

                        if (provider.size() > 0 && location != null && date != null && patient != null && form != null) {

                            Encounter encounter = new Encounter();
                            encounter.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid(encounterTypeUuid.getTextValue()));
                            encounter.setPatient(patient);
                            encounter.setLocation(location);
                            encounter.setEncounterDatetime(date);
                            encounter.setCreator(user);
                            encounter.addProvider(encounterRole, provider.get(0));
                            encounter.setForm(form);

                            List<JsonObs> jsonObs = mapper.readValue(
                                    obsNode.toString(),
                                    mapper.getTypeFactory().constructCollectionType(
                                            List.class, JsonObs.class));
                            Set<Obs> obsSet = new HashSet<Obs>();
                            for (JsonObs obs : jsonObs) {
                                if (obs != null && obs.getConcept_id() != null) {
                                    Obs observation = new Obs();
                                    Concept concept = Context.getConceptService().getConcept(obs.getConcept_id());
                                    if (concept != null) {
                                        observation.setConcept(concept);
                                    } else {
                                        continue;
                                    }
                                    observation.setLocation(location);
                                    observation.setPerson(user.getPerson());

                                    if (!obs.getDatetime().isEmpty()) {
                                        Date obsDateTime = formatter.parse(obs.getDatetime());
                                        observation.setObsDatetime(obsDateTime);
                                    } else {
                                        observation.setObsDatetime(date);
                                    }
                                    if (!obs.getGroup_id().isEmpty()) {
                                        observation.setValueGroupId(Integer.valueOf(obs.getGroup_id()));
                                    }
                                    if (obs.getType().equals("valueText")) {
                                        observation.setValueText(obs.getConcept_answer());
                                    } else if (obs.getType().equals("valueNumeric")) {
                                        observation.setValueNumeric(Double.valueOf(obs.getConcept_answer()));
                                    } else {
                                        Concept value = Context.getConceptService().getConcept(obs.getConcept_answer());
                                        observation.setValueCoded(value);
                                    }
//                                        Context.getObsService().saveObs(observation,"");
                                    obsSet.add(observation);
                                }
                            }
//                                Context.getEncounterService().saveEncounter(encounter);
                            Visit visit = new Visit();
                            visit.setStartDatetime(formatter.parse(encounterDateNode.getTextValue()));
                            visit.setPatient(patient);
                            visit.setLocation(location);
                            visit.setCreator(user);
                            VisitType visitType = Context.getVisitService().getVisitTypeByUuid("7b0f5697-27e3-40c4-8bae-f4049abfb4ed");
                            if (visitType != null) {
                                visit.setVisitType(visitType);
                            }
                            if (obsSet.size() > 0) {
                                encounter.setObs(obsSet);
                            }
                            encounter.setVisit(visit);
                            Context.getEncounterService().saveEncounter(encounter);
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (JsonParseException e) {
                        moveUnparsabbleFiles(jsonFile);
                        e.printStackTrace();
                    } catch (Exception e) {
                        moveUnparsabbleFiles(jsonFile);
                        e.printStackTrace();
                    }


                    fileReader.close();
                    moveProcessedFiles(jsonFile);

                } catch (Exception e) {
                    if (e instanceof JsonParseException) {
                        moveUnparsabbleFiles(jsonFile);
                    }
                    e.printStackTrace();
                }
            }
        }
    }

}
