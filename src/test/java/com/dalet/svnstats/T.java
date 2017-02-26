package com.dalet.svnstats;

import com.dalet.lotus.LotusNotesClient;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.NotesException;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import static com.dalet.lotus.Configuration.NOTES_PASSWORD;
import static com.dalet.lotus.LotusNotesClient.getStringValue;

/**
 * User: Moisei Rabinovich
 * Date: 2/11/14
 * Time: 10:52 AM
 */
@SuppressWarnings("StatementWithEmptyBody")
@Ignore
public class T {

    @Before
    public void setup() {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
    }

    public static void main(String[] args) throws IOException {
        LineNumberReader lnr = new LineNumberReader(new FileReader("c:\\temp\\dilc-build-out.log"));
        StringBuffer sb = new StringBuffer();
        String line;
        while (null != (line = lnr.readLine())) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String cleanLine = line.trim().toUpperCase();
            if (cleanLine.startsWith("***** ")) {
                writeToFile(sb);
                sb = new StringBuffer();
            } else {
                // continue with current buffer
            }
            sb.append(line).append("\n");
        }
        lnr.close();
        ArrayList<String> keysArray = new ArrayList<>(errorLines.keySet());
        Collections.sort(keysArray);
        for (String errorLine : keysArray) {
            System.out.println("==== " + errorLine);
            List<String> projects = errorLines.get(errorLine);
            Collections.sort(projects);
            for (String project : projects) {
                System.out.println("\t" + project.substring("***** s:\\src\\cpp\\".length(), project.length() - " *****".length()));
            }
        }

    }

    static HashMap<String, List<String>> errorLines = new HashMap<>();

    private static void writeToFile(StringBuffer sb) {
        String[] lines = sb.toString().split("\n");
        if (lines.length < 2) {
            System.err.println("block is too short: " + sb);
            return;
        }
        String firstLine = lines[0];
        //String lastBut5Line = lines[lines.length - 5];
        //String lastBut4Line = lines[lines.length - 4];
        String lastBut1Line = lines[lines.length - 2];
        String lastLine = lines[lines.length - 1];
        if (lastLine.trim().startsWith("OK")) {
            return;
        }
        String errorLine = lastLine.trim().equalsIgnoreCase("Stop.") ? lastBut1Line : lastLine;
//        errorLine = errorLine.startsWith("Copyright (C)") ? lastBut4Line : errorLine;
//        errorLine = errorLine.startsWith("Stop.") ? lastBut5Line : errorLine;
//        System.out.println(firstLine + " --> " + errorLine);
//        System.out.println(sb);
//        System.out.println("==============================================================================\n");
        errorLines.computeIfAbsent(errorLine, k -> new ArrayList<>(1000));
        errorLines.get(errorLine).add(firstLine);
    }

    @Test
    public void test2() throws Exception {
        String reference = "#69317 - LJ";
        try (LotusNotesClient lnc = new LotusNotesClient(NOTES_PASSWORD, true)) {
            Document doc = LotusNotesClient.findOriginalDocumentByReference(lnc.getBugsDb(), reference);
            //noinspection ConstantConditions
            for (Object o : doc.getItems()) {
                String valueName = o.toString();
                try {
                    System.out.println(o + " " + LotusNotesClient.getStringValue(doc, valueName, ""));
                } catch (NotesException e) {
                    //
                }
            }
        }
    }

    @org.junit.Test
    public void test1() throws Exception {
        int prefixLength = "12/08/2013 - ".length();
        TreeSet<String> historyMessages = new TreeSet<>();
        try (LotusNotesClient lnc = new LotusNotesClient(NOTES_PASSWORD, true)) {
//            Document doc = LotusNotesClient.findOriginalDocumentByReference(lnc.getBugsDb(), "#65235 - TZ");
//            System.out.println(LotusNotesClient.getStringValue(doc, "History", "DEFAULT"));
            DocumentCollection allDocuments = lnc.getBugsDb().getAllDocuments();
            int allDocumentsCount = allDocuments.getCount();
            int count = 0;
            for (Document doc = allDocuments.getFirstDocument(); null != doc && count < 100000; doc = allDocuments.getNextDocument(), count++) {
                if (0 == count % 100) {
                    System.out.println("Progress: " + count + " out of " + allDocumentsCount);
                }
                String history = getStringValue(doc, "History", "");
                if (history.isEmpty()) {
                    continue;
                }
                String[] historyMessage = history.split("\n");
                for (String token : historyMessage) {
                    if (token.length() < prefixLength) {
                        historyMessages.add(token);
                    } else {
                        historyMessages.add(token.substring(prefixLength));
                    }
                }
            }
            for (String historyMessage : historyMessages) {
                if ("UNKNOWN".equalsIgnoreCase(statusFromMessage(historyMessage))) {
                    System.out.println(historyMessage);
                }
            }
        }
    }

    private String statusFromMessage(String msg) {
        // no status change
        if (msg.startsWith("Bug Assigned  to")) {
            return "NEW";
        }
        if (msg.startsWith("Remark by")) {
            return "NEW";
        }
        if (msg.startsWith("by ")) {
            return "NEW";
        }
        if (msg.startsWith("Programmer change")) {
            return "NEW";
        }
        if (msg.startsWith("Work started by")) {
            return "NEW";
        }
        if (msg.startsWith("Tester change")) {
            return "NEW";
        }
        if (msg.startsWith("Refix started by")) {
            return "NEW";
        }
        if (msg.startsWith("Module Name updated")) {
            return "NEW";
        }
        if (msg.startsWith("Retest started by")) {
            return "NEW";
        }
        // status changed
        if (msg.startsWith("Answered by")) {
            return "NEW";
        }
        if (msg.startsWith("Rejection rejected by")) {
            return "NEW";
        }
        if (msg.startsWith("More info requested by")) {
            return "QUESTION";
        }
        if (msg.startsWith("Work endedby")) {
            return "FIXED";
        }
        if (msg.startsWith("Fixedby")) {
            return "FIXED";
        }
        if (msg.startsWith("Rejected by")) {
            return "FIXED";
        }
        if (msg.startsWith("Bug marked as archived")) {
            return "ARCHIVED";
        }
        if (msg.startsWith("Bug sent to ARCHIVE")) {
            return "ARCHIVED";
        }
        if (msg.startsWith("Case closed by")) {
            return "DONE";
        }
        if (msg.contains("says the bug is not fixed yet")) {
            return "NEW";
        }
        if (msg.startsWith("Status Changed to")) {
            return msg.substring("Status Changed to".length()).split(" ")[0];
        }
        return "UNKNOWN";
    }

    @Test
    public void test3() throws Exception {
        try (LotusNotesClient lnc = new LotusNotesClient(NOTES_PASSWORD, true)) {
//            Document doc = LotusNotesClient.findOriginalDocumentByReference(lnc.getBugsDb(), "#65235 - TZ");
//            System.out.println(LotusNotesClient.getStringValue(doc, "History", "DEFAULT"));
            DocumentCollection allDocuments = lnc.getBugsDb().getAllDocuments();
            Document doc = allDocuments.getLastDocument();
            try (PrintWriter fw = new PrintWriter(new FileWriter("bugs-features.csv"))) {
                for (int i = 0; i < 2000; doc = allDocuments.getPrevDocument()) {
                    String bugReference = doc.getItemValueString("Reference");
                    if (null != bugReference && !bugReference.isEmpty()) {
                        ++i;
                        String feature = doc.getItemValueString("Feature");
                        String featureReference = "";
                        if (!feature.isEmpty()) {
                            String[] tokens = feature.split(" ");
                            featureReference = tokens[0] + " - " + tokens[2];
                        }
                        System.out.println(bugReference);
                        fw.println(bugReference + "\t" + featureReference + "\t" + feature);
                    }
                }
            }
        }
    }


    @Test
    public void name() throws Exception {
        System.out.println(Paths.get("\\\\jopa", "popa").toFile().toString());
        System.out.println(Paths.get("popa").toString());
    }
}


