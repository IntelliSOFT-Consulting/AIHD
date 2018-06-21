package org.openmrs.module.aihdconfigs.utils;


import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Visit;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
        String newFileName = new Date().toString() + "_" + file.getName();
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
                    ObjectMapper mapper = new ObjectMapper();;
                    JsonNode rootNode = mapper.readTree(fileReader);
                    log.error("json root node" + rootNode.toString());
                    for (JsonNode temp : rootNode) {
                        log.error("json node node" + temp.toString() + "\n\n");
                        JsonNode obsNode = rootNode.path("obs");
                        JsonNode encounterDate = rootNode.path("encounterDate");
                        JsonNode patientId = rootNode.path("patient_id");
                        log.error("obs node" + obsNode.toString() + "\n");
                        log.error("encounterDate node" + encounterDate.toString() + "\n");
                        log.error("patientId node" + patientId.toString() + "\n");


                        List<JsonObs> jsonObs = mapper.readValue(
                                obsNode.toString(),
                                mapper.getTypeFactory().constructCollectionType(
                                        List.class, JsonObs.class));
                        log.error("obs size" + jsonObs.size());
                        for (JsonObs obs : jsonObs) {
                            if (obs != null && obs.getId() != null) {
                                log.error("obs object" + obs.getId() + " " + obs.getGroup_id());
                                Concept concept = Context.getConceptService().getConcept(obs.getId());

                            }
                        }
                    }

                    fileReader.close();
                    moveProcessedFiles(jsonFile);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private EncounterType creteEncounter(String uuid) {
        EncounterService encounterService = Context.getEncounterService();
        return encounterService.getEncounterTypeByUuid(uuid);
    }
    private void save(String encounerTypeUuid){
        VisitService vs = Context.getVisitService();
        Visit visit = vs.getVisit(2);

        EncounterService encounterService = Context.getEncounterService();
        EncounterType encounterType =  encounterService.getEncounterTypeByUuid(encounerTypeUuid);
    }

}
