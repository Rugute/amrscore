package org.openmrs.module.amrscore.api.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Concept;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.FormService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FormsManager {
	
	public static String retrieveFormsForVisit(String patientUuid) {
       // ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode formList = JsonNodeFactory.instance.arrayNode();
        ObjectNode allFormsObj = JsonNodeFactory.instance.objectNode();
        ObjectNode encObj = JsonNodeFactory.instance.objectNode();
        ObjectNode formObj = JsonNodeFactory.instance.objectNode();

        List<Form> forms = new ArrayList<>();
        PatientService patientService = Context.getPatientService();
        FormService formService = Context.getFormService();

        // Retrieve the patient based on UUID
        Patient patient = patientService.getPatientByUuid(patientUuid);
       // System.out.println("Patients Information "+ patient.getUuid());
        // If patient with the provided UUID is found
        if (patient != null) {

            List<Visit> activeVisits = Context.getVisitService().getActiveVisitsByPatient(patient);
            if (!activeVisits.isEmpty()) {
                Visit patientVisit = activeVisits.get(0);

                // Get the encounters associated with the patient
                System.out.println("Patients Information " + patient.getUuid());
                // List<Form> allForms = formService.getFormByUuid("a1b8fe75-b405-4654-a6d9-7297ae51e95b");//getAllForms();
               // List<Form> allForms = formService.getForms().getPublishedForms();//getAllForms();

                List<String> formUUIDs = Arrays.asList(
                        "2f58e03b-72d0-4b6c-a72a-5936defcdd60",
                        "f35a2c77-916e-4130-b5cd-fbc684ac50bf",
                        "7a16a346-0991-48ac-b339-aacd463a7845",
                        "edb264d4-91fc-4d16-bb34-a413eef2b865",
                        "b1a2c42c-310b-4190-bac1-c17e68f9b906",
                        "6c82c396-45dc-476b-abc4-84c2b71992c5",
                        "e54e2200-7a2d-4e6b-baaf-46d3b6d66502",
                        "3b688533-0de1-4c3e-b75a-c5b865080d80"
                );

                // Fetch each form by UUID and add it to the forms list
                for (String uuid : formUUIDs) {
                    Form frm = formService.getFormByUuid(uuid);
                    if (frm != null) {
                         encObj.put("uuid", frm.getEncounterType().getUuid());
                         encObj.put("display", frm.getEncounterType().getName());
                        formObj.put("uuid", frm.getUuid());
                        formObj.put("encounterType", encObj);
                        formObj.put("name", frm.getName());
                        String nname=frm.getName().replace("AMPATH POC","");
                        formObj.put("display", frm.getName());
                        //formObj.put("display", nname);
                        formObj.put("version", frm.getVersion());
                        formObj.put("published", frm.getPublished());
                        formObj.put("retired", frm.getRetired());
                        formObj.put("formCategory", "available");
                        formList.add(formObj);
                       // forms.add(form);
                        System.out.println("Fetched form: " + frm.getName());
                    } else {
                        System.out.println("Form with UUID " + uuid + " not found.");
                    }
                }

               // System.out.println("Forms Sizes  " + allForms.size());
           /* for (Form form : allForms) {
                // Check if the form is associated with any visit of the patient
              //  if (form.getEncounterType() != null && form.getEncounterType().getUuid() != null) {
                    // Fetch forms associated with the patient
                   // Concept concept =form.getEncounterType().getUuid();
                    List<Form> formsForPatient = formService.getPublishedForms();
                    if (formsForPatient != null && !formsForPatient.isEmpty()) {
                        forms.addAll(formsForPatient);
                    }
               // }
            }*/
                // formObj.put("formCategory", "available");
                // formList.add(formObj);
              /*  for (Form frm : allForms) {
                    // encObj.put("uuid", frm.getEncounterType().getUuid());
                    // encObj.put("display", frm.getEncounterType().getName());
                    formObj.put("uuid", frm.getUuid());
                    // formObj.put("encounterType", encObj);
                    formObj.put("name", frm.getName());
                    formObj.put("display", frm.getName());
                    formObj.put("version", frm.getVersion());
                    formObj.put("published", frm.getPublished());
                    formObj.put("retired", frm.getRetired());
                    formObj.put("formCategory", "available");
                    formList.add(formObj);
                    // You can access more properties of the form as needed
                } */
                allFormsObj.put("results", formList);
            }
        }else{

            allFormsObj.put("results", formList);
        }
       // allFormsObj.put("results", formList);
        return allFormsObj.toString();
        // Convert forms to JSON
       /* try {
            return objectMapper.writeValueAsString(forms);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error converting forms to JSON";
        } */
    }
	
	public static List<Form> retrieveFormsForVisitT(String patientUuid) {
        List<Form> forms = new ArrayList<>();
        PatientService patientService = Context.getPatientService();
        FormService formService = Context.getFormService();

        // Retrieve the patient based on UUID
        Patient patient = patientService.getPatientByUuid(patientUuid);

        // If patient with the provided UUID is found
        if (patient != null) {
            // Get the encounters associated with the patient
            List<Form> allForms = formService.getAllForms();
            /*for (Form form : allForms) {
                // Check if the form is associated with any visit of the patient
                if (form.getEncounterType() != null && form.getEncounterType().getUuid() != null) {
                    //List<Form> formsForPatient = formService.getFormsContainingConcept(form.getEncounterType().getUuid());
                    List<Form> formsForPatient = formService.getAllForms(true);

                    for (Form f : formsForPatient) {
                        forms.add(f);
                    }
                }
            }*/
            for (Form form : allForms) {
                System.out.println("Form Name: " + form.getName());
                System.out.println("Form UUID: " + form.getUuid());
                // You can access more properties of the form as needed
            }
        }

        return forms;
    }
}
