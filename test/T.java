import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * User: Moisei Rabinovich
 * Date: 2/11/14
 * Time: 10:52 AM
 */
public class T {
    public static void main(String[] args) throws IOException {
        LineNumberReader lnr = new LineNumberReader(new FileReader("c:\\temp\\dilc-build-out.log"));
        StringBuffer sb = new StringBuffer();
        String line;
        while (null != (line = lnr.readLine())) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String cleanLine = line.trim().toUpperCase();
            //noinspection StatementWithEmptyBody
            if (cleanLine.startsWith("***** ")) {
                writeToFile(sb);
                sb = new StringBuffer();
            } else {
                // continue with current buffer
            }
            sb.append(line).append("\n");
        }
        lnr.close();
        ArrayList<String> keysArray = new ArrayList<String>(errorLines.keySet());
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

    static HashMap<String, List<String>> errorLines = new HashMap<String, List<String>>();

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
        if (null == errorLines.get(errorLine)) {
            errorLines.put(errorLine, new ArrayList<String>(1000));
        }
        errorLines.get(errorLine).add(firstLine);
    }
}
