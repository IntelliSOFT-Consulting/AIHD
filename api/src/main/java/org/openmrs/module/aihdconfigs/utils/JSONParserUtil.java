package org.openmrs.module.aihdconfigs.utils;


import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.VisitService;
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
import java.text.DateFormat;
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
        String newFileName = new Date().toString() + "_" + file.getName() + "_" + new Date().toString();
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
        String newFileName = new Date().toString() + "_" + file.getName() + "_" + new Date().toString();
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
                    System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

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
                                    System.out.println("\nCurrent Element :" + obs_node.getNodeName());
                                    String concept_id = ((Element) obs_node).getElementsByTagName("concept").item(0).getTextContent();
                                    String answer_concept_id = ((Element) obs_node).getElementsByTagName("answerConcept").item(0).getTextContent();
                                    System.out.println(concept_id);
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
        log.error("Reading JSON files...");
        List<File> directory_files = getFilesFromDir();
        log.error("Files sizes" + directory_files.size());
        for (File jsonFile : directory_files) {
            log.error("json file ", jsonFile);
            if (jsonFile.getName().endsWith(".json")) {
                try {
                    FileReader fileReader = new FileReader(jsonFile);
                    ObjectMapper mapper = new ObjectMapper();
                    ;
                    JsonNode rootNode = mapper.readTree(fileReader);
                    log.error("json root node" + rootNode.toString());
                    for (JsonNode temp : rootNode) {
                        log.error("json node node" + temp.toString() + "\n\n");
                        JsonNode obsNode = rootNode.path("obs");
                        JsonNode encounterDateNode = rootNode.path("encounterDate");
                        JsonNode patientId = rootNode.path("patient_id");
                        JsonNode encounterTypeUuid = rootNode.path("formEncounterType");
                        JsonNode encounterProviderUuid = rootNode.path("encounterProviderencounterProvider");
                        JsonNode locationUuid = rootNode.path("formUILocation");
                        log.error("obs node" + obsNode.getElements().toString() + "\n");
                        log.error("encounterDateNode node" + encounterDateNode.getTextValue() + "\n");
                        log.error("patientId node" + patientId.getTextValue() + "\n");

                        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                        try {
                            String dateString = encounterDateNode.getTextValue();
                            Date date = formatter.parse(dateString);
                            log.error("Date " + date);
                            log.error("Date format " + formatter.format(date));

                            Encounter encounter = new Encounter();
                            Patient patient = Context.getPatientService().getPatient(Integer.valueOf(patientId.getTextValue()));
                            Provider provider = Context.getProviderService().getProviderByUuid(encounterProviderUuid.getTextValue());
                            Location location = Context.getLocationService().getLocationByUuid(locationUuid.getTextValue());
                            encounter.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid(encounterTypeUuid.getTextValue()));
                            encounter.setPatient(patient);
                            encounter.setLocation(location);
                            encounter.setEncounterDatetime(date);
                            //encounter.setProvider(, provider);


                            List<JsonObs> jsonObs = mapper.readValue(
                                    obsNode.toString(),
                                    mapper.getTypeFactory().constructCollectionType(
                                            List.class, JsonObs.class));
                            log.error("obs size" + jsonObs.size());
                            for (JsonObs obs : jsonObs) {
                                if (obs != null && obs.getId() != null) {
                                    Obs observation = new Obs();
                                    observation.setLocation(location);
                                    if (!obs.getGroup_id().isEmpty()) {
                                        observation.setValueGroupId(Integer.valueOf(obs.getGroup_id()));
                                    }
                                    if (obs.getType().equals("string")) {
                                        observation.setValueText(obs.getAnswer());
                                    } else if (obs.getType().equals("numeric")) {
                                        observation.setValueNumeric(Double.valueOf(obs.getAnswer()));
                                    } else {
                                        observation.setConcept(Context.getConceptService().getConcept(Integer.valueOf(obs.getId())));
                                    }
                                    encounter.addObs(observation);
                                }
                            }
                            Context.getEncounterService().saveEncounter(encounter);

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }


                    }

                    fileReader.close();
                    moveProcessedFiles(jsonFile);

                } catch (Exception e) {
                    if (e instanceof JsonParseException) {

                    }
                    e.printStackTrace();
                }
            }
        }
    }

    private EncounterType creteEncounter(String uuid) {
        EncounterService encounterService = Context.getEncounterService();
        return encounterService.getEncounterTypeByUuid(uuid);
    }

    private void save(String encounerTypeUuid) {
        VisitService vs = Context.getVisitService();
        Visit visit = vs.getVisit(2);

        EncounterService encounterService = Context.getEncounterService();
        EncounterType encounterType = encounterService.getEncounterTypeByUuid(encounerTypeUuid);
    }

}
