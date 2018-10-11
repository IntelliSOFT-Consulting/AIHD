package org.openmrs.module.aihdconfigs.utils;


import org.apache.commons.lang3.StringUtils;
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
import java.io.*;
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
        log.info("file path" + file.getAbsolutePath());
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            directory_files = Arrays.asList(files);
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

    private static boolean moveUnprocessesFile(File file, String exception) {
        File processed_dir = new File(OpenmrsUtil.getApplicationDataDirectory() + "/failed");
        if (!processed_dir.exists()) {
            if (!processed_dir.mkdirs())
                return false;
        }
        PrintWriter out = null;
        try {
            if (file.canWrite()) {
                out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                out.println(exception);
            } else {
                log.error("Cant write to file");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
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
        if (directory_files != null && directory_files.size() > 0) {
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
    }

    public static void readJSOFile() {
        List<File> directory_files = getFilesFromDir();
        if (directory_files != null && directory_files.size() > 0) {
            for (File jsonFile : directory_files) {
                if (jsonFile.getName().endsWith(".json")) {
                    try {
                        FileReader fileReader = new FileReader(jsonFile);
                        ObjectMapper mapper = new ObjectMapper();
                        ;
                        JsonNode rootNode = mapper.readTree(fileReader);
                        JsonNode obsNode = rootNode.path("obs");
                        JsonNode encounterDateNode = rootNode.path("encounterDate");
                        JsonNode patientId = rootNode.path("patient_id");
                        JsonNode encounterTypeUuid = rootNode.path("formEncounterType");
                        JsonNode encounterProviderUuid = rootNode.path("encounterProvider");
                        JsonNode locationUuid = rootNode.path("location_id");
                        JsonNode formuuid = rootNode.path("formUuid");

                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd");
                        Date date;
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
                            Form form = Context.getFormService().getFormByUuid(formuuid.getTextValue());
                            if (provider.size() > 0 && location != null && date != null && patient != null && form != null) {

                                Encounter encounter = new Encounter();
                                encounter.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid(encounterTypeUuid.getTextValue()));
                                encounter.setPatient(patient);
                                encounter.setLocation(location);
                                encounter.setEncounterDatetime(date);
                                encounter.setCreator(user);
                                encounter.addProvider(encounterRole, provider.get(0));
                                encounter.setForm(form);
                                List<JsonNode> singleObsNodes = new ArrayList<JsonNode>();
                                List<JsonNode> groupObsNodes = new ArrayList<JsonNode>();
                                for (JsonNode jsonNode : obsNode) {
                                    if (jsonNode.has("groups")) {
                                        groupObsNodes.add(jsonNode);
                                    } else {
                                        singleObsNodes.add(jsonNode);
                                    }
                                }

                                List<JsonObs> singleJsonObs = mapper.readValue(
                                        singleObsNodes.toString(),
                                        mapper.getTypeFactory().constructCollectionType(
                                                List.class, JsonObs.class));
                                Set<Obs> obsSet_single = new HashSet<Obs>();
                                Set<Obs> obsSet_group = new HashSet<Obs>();
                                Set<Obs> combined_set = new HashSet<Obs>();

                                for (JsonObs obs : singleJsonObs) {
                                    if (obs != null && obs.getConcept_id() != null) {
                                        Concept concept = Context.getConceptService().getConcept(obs.getConcept_id());
                                        Obs observation_single = new Obs();
                                        Obs grpObs = new Obs();
                                        if (concept != null) {
                                            observation_single.setConcept(concept);
                                            observation_single.setLocation(location);
                                            observation_single.setPerson(patient.getPerson());
                                            observation_single.setCreator(user);
                                            observation_single.setDateCreated(new Date());
                                            observation_single.setEncounter(encounter);

                                            if (StringUtils.isNotEmpty(obs.getDatetime())) {
                                                observation_single.setObsDatetime(formatter.parse(obs.getDatetime()));
                                            } else {
                                                observation_single.setObsDatetime(date);
                                            }
                                            if (StringUtils.isNotEmpty(obs.getComment())) {
                                                observation_single.setComment(obs.getComment());
                                            }
                                            if (obs.getType().equals("valueText")) {
                                                observation_single.setValueText(obs.getConcept_answer());
                                            } else if (obs.getType().equals("valueNumeric")) {
                                                observation_single.setValueNumeric(Double.valueOf(obs.getConcept_answer()));
                                            } else if (obs.getType().equals("valueCoded")) {
                                                Concept value = Context.getConceptService().getConcept(obs.getConcept_answer());
                                                observation_single.setValueCoded(value);
                                            } else if (obs.getType().equals("valueDatetime")) {
                                                observation_single.setValueDatetime(formatter.parse(obs.getConcept_answer()));
                                            } else if (obs.getType().equals("valueDate")) {
                                                observation_single.setValueDatetime(dateOnly.parse(obs.getConcept_answer()));
                                            }

                                            if (StringUtils.isNotEmpty(obs.getGroup_id())) {
                                                Concept concept1 = Context.getConceptService().getConcept(obs.getGroup_id());
                                                if (concept1 != null) {
                                                    grpObs.setEncounter(encounter);
                                                    grpObs.setObsDatetime(date);
                                                    grpObs.setDateCreated(new Date());
                                                    grpObs.setLocation(location);
                                                    grpObs.setPerson(patient.getPerson());
                                                    grpObs.setConcept(concept1);
                                                    grpObs.setValueCoded(null);
                                                    grpObs.addGroupMember(observation_single);
                                                }
                                            }
                                            obsSet_single.add(observation_single);

                                        }

                                    }

                                }

                                for (JsonNode jsonNode : groupObsNodes) {
                                    JsonNode parentGropsNode = jsonNode.path("groups");
                                    for (JsonNode innerGroupArrayNode : parentGropsNode) {
                                        List<JsonObs> singleGroupedJsonObs = mapper.readValue(
                                                innerGroupArrayNode.toString(),
                                                mapper.getTypeFactory().constructCollectionType(
                                                        List.class, JsonObs.class));
                                        if (singleGroupedJsonObs.size() > 0) {
                                            Concept groupConcept = Context.getConceptService().getConcept(singleGroupedJsonObs.get(0).getGroup_id());
                                            Obs grpObs = new Obs();
                                            if (groupConcept != null) {
                                                grpObs.setEncounter(encounter);
                                                grpObs.setObsDatetime(date);
                                                grpObs.setDateCreated(new Date());
                                                grpObs.setLocation(location);
                                                grpObs.setPerson(patient.getPerson());
                                                grpObs.setConcept(groupConcept);
                                                grpObs.setValueCoded(null);

                                                for (JsonObs obs : singleGroupedJsonObs) {
                                                    if (obs.getConcept_id() != null && StringUtils.isNotEmpty(obs.getGroup_id())) {
                                                        Concept concept = Context.getConceptService().getConcept(obs.getConcept_id());
                                                        Obs observation_group = new Obs();
                                                        if (concept != null) {
                                                            observation_group.setConcept(concept);
                                                            observation_group.setLocation(location);
                                                            observation_group.setPerson(patient.getPerson());
                                                            observation_group.setCreator(user);
                                                            observation_group.setDateCreated(new Date());
                                                            observation_group.setEncounter(encounter);

                                                            if (StringUtils.isNotEmpty(obs.getDatetime())) {
                                                                observation_group.setObsDatetime(formatter.parse(obs.getDatetime()));
                                                            } else {
                                                                observation_group.setObsDatetime(date);
                                                            }
                                                            if (StringUtils.isNotEmpty(obs.getComment())) {
                                                                observation_group.setComment(obs.getComment());
                                                            }
                                                            if (obs.getType().equals("valueText")) {
                                                                observation_group.setValueText(obs.getConcept_answer());
                                                            } else if (obs.getType().equals("valueNumeric")) {
                                                                observation_group.setValueNumeric(Double.valueOf(obs.getConcept_answer()));
                                                            } else if (obs.getType().equals("valueCoded")) {
                                                                Concept value = Context.getConceptService().getConcept(obs.getConcept_answer());
                                                                observation_group.setValueCoded(value);
                                                            } else if (obs.getType().equals("valueDatetime")) {
                                                                observation_group.setValueDatetime(formatter.parse(obs.getConcept_answer()));
                                                            } else if (obs.getType().equals("valueDate")) {
                                                                observation_group.setValueDatetime(dateOnly.parse(obs.getConcept_answer()));
                                                            }

                                                            if (StringUtils.isNotEmpty(obs.getGroup_id())) {
                                                                grpObs.addGroupMember(observation_group);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            obsSet_group.add(grpObs);
                                        }
                                    }
                                }
                                combined_set.addAll(obsSet_single);
                                combined_set.addAll(obsSet_group);

                                Visit visit = new Visit();
                                visit.setStartDatetime(formatter.parse(encounterDateNode.getTextValue()));
                                visit.setPatient(patient);
                                visit.setLocation(location);
                                visit.setCreator(user);
                                VisitType visitType = Context.getVisitService().getVisitTypeByUuid("7b0f5697-27e3-40c4-8bae-f4049abfb4ed");
                                if (visitType != null) {
                                    visit.setVisitType(visitType);
                                }

                                if (combined_set.size() > 0) {
                                    encounter.setObs(combined_set);
                                    encounter.setVisit(visit);
                                    Context.getEncounterService().saveEncounter(encounter);
                                }

                            } else if (provider.size() == 0) {
                                throw new Exception("The user is NOT a provider. Only providers can fill forms");
                            }
                            else if (patient == null) {
                                throw new NullPointerException("Patient object is null");
                            } else if (location == null) {
                                throw new NullPointerException("Location object is null");
                            } else if (date == null) {
                                throw new NullPointerException("Encounter date object is null");
                            } else if (form == null) {
                                throw new NullPointerException("Form object is null");
                            } else {
                                throw new NullPointerException("Null object in imputs");
                            }

                        } catch (ParseException e) {
                            fileReader.close();
                            moveUnprocessesFile(jsonFile, parseException(e));
                            e.printStackTrace();
                        } catch (JsonParseException e) {
                            fileReader.close();
                            moveUnprocessesFile(jsonFile, parseException(e));
                            e.printStackTrace();
                            parseException(e);
                        } catch (NullPointerException e) {
                            fileReader.close();
                            moveUnprocessesFile(jsonFile, parseException(e));
                            e.printStackTrace();
                        } catch (Exception e) {
                            fileReader.close();
                            moveUnprocessesFile(jsonFile, parseException(e));
                            e.printStackTrace();
                        }


                        fileReader.close();
                        moveProcessedFiles(jsonFile);

                    } catch (Exception e) {
                        moveUnprocessesFile(jsonFile, parseException(e));
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static String parseException(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        return writer.toString();
    }

}
