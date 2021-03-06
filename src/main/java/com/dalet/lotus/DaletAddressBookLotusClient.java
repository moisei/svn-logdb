package com.dalet.lotus;

import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.Item;
import lotus.domino.NotesException;
import org.apache.log4j.BasicConfigurator;

import java.io.PrintStream;
import java.util.*;

import static com.dalet.lotus.Configuration.NOTES_PASSWORD;

/**
 * User: Moisei Rabinovich
 * Date: 6/19/2014
 * Time: 4:00 PM
 */
public class DaletAddressBookLotusClient {

    final static Set<String> VALID_SCRUMS = new HashSet<>(Arrays.asList(
            "scrum-managers",
            "scrumamberfin",
            "scrumautomation",
            "scrumbrio",
            "scrumbu",
            "scrumcds",
            "scrumcg",
            "scrumcore",
            "scrumddm",
            "scrumlabs",
            "scrumonecut",
            "scrumonthego",
            "scrumplatform",
            "scrumreleasevalidation",
            "scrumsports",
            "scrumux",
            "scrumweb",
            "scrumxchange"));


    public static Map<String, Set<String>> getGroupsForUsers(Set<String> groupNames, Set<String> userNames, String groupForUnknownUsers) throws NotesException {
        BasicConfigurator.configure();
        Map<String, Set<String>> groupsForUsers = new HashMap<>();
        LotusNotesClient lnc = new LotusNotesClient(true);
        try {
            DocumentCollection allDocuments = lnc.getDaletAddressBookDb().getAllDocuments();
            for (Document groupDoc = allDocuments.getFirstDocument(); null != groupDoc; groupDoc = allDocuments.getNextDocument()) {
                Set<String> groupUsers = new HashSet<>(5000);
                if (!"Group".equals(groupDoc.getItemValueString("Type"))) {
                    continue;
                }
                String groupName = groupDoc.getItemValueString("ListName");
                if (!groupNames.contains(groupName.toLowerCase())) {
                    continue;
                }
                @SuppressWarnings("unchecked") Vector<String> members = groupDoc.getItemValue("Members");
                for (String member : members) {
                    String svnName = memberToSvnName(member);
                    if (!userNames.contains(svnName)) {
                        continue;
                    }
                    groupUsers.add(svnName);
                }
                groupsForUsers.put(groupName, groupUsers);
            }
            return groupsForUsers;
        } finally {
            lnc.closeNotesSession();
        }
    }

    private static String memberToSvnName(String fullName) {
        String[] fullNameTokens = fullName.split("[=/]");
        String svnName;
        if (1 == fullNameTokens.length) {
            svnName = fullName.toLowerCase();
        } else {
            svnName = fullNameTokens[1].toLowerCase();
        }
        String[] svnNameTokens = svnName.split(" ");
        if (svnNameTokens.length != 2) {
            return svnName;
        }
        return (svnNameTokens[0].substring(0, 1) + svnNameTokens[1]).toLowerCase();
    }

    private static String nameToKey(String fullName) throws NotesException {
        String[] fullNameTokens = fullName.split("[=/]");
        if (1 == fullNameTokens.length) {
            return fullName.toLowerCase();
        } else {
            return fullNameTokens[1].toLowerCase();
        }
    }

    public void printUser(Document doc) throws NotesException {
        System.out.println("----------------------------------------------------------");
        @SuppressWarnings("unchecked") Vector<Item> items = doc.getItems();
        items.stream().filter(this::itemHasValue).sorted(this::compareItems).forEach(item -> System.out.println(item + ": " + getItemValue(item)));
//        System.out.println(doc.getItemValue("").get(0) + ": " + doc.getItemValue("InternetAddress").get(0));
    }

    public void printGroup(Document doc) throws NotesException {
        System.out.println("----------------------------------------------------------");
        System.out.println(doc.getItemValueString("ListName"));
        @SuppressWarnings("unchecked") Vector<String> members = doc.getItemValue("Members");
        members.forEach(System.out::println);
        System.out.println(members);
    }

    private boolean itemHasValue(Item item) {
        Vector itemValue = getItemValue(item);
        return (null != itemValue) && !itemValue.isEmpty();
    }

    private int compareItems(Item o1, Item o2) {
        try {
            return o1.getName().compareTo(o2.getName());
        } catch (NotesException e) {
            return 0;
        }
    }

    private Vector getItemValue(Item item) {
        try {
            return item.getValues();
        } catch (NotesException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getItemValueString(Document doc, String valueName) {
        try {
            if (null == doc) {
                return null;
            }
            return doc.getItemValueString(valueName);
        } catch (NotesException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toCamelCase(String s) {
        if (null == s) {
            return null;
        }
        String[] parts = s.split(" ");
        StringBuilder camelCaseString = new StringBuilder();
        for (String part : parts) {
            camelCaseString.append(toProperCase(part));
        }
        return camelCaseString.toString();
    }

    private static String toProperCase(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public static void printUsers(PrintStream out) throws NotesException {
        BasicConfigurator.configure();
        LotusNotesClient lnc = new LotusNotesClient(NOTES_PASSWORD, true);
        try {
            DocumentCollection allDocuments = lnc.getDaletAddressBookDb().getAllDocuments();
            System.out.println(allDocuments.getCount());
            HashMap<String, Document> users = new HashMap<>(allDocuments.getCount());
            for (Document doc = allDocuments.getFirstDocument(); null != doc; doc = allDocuments.getNextDocument()) {
                if ("Person".equals(doc.getItemValueString("Type"))) {
                    users.put(nameToKey(doc.getItemValueString("FullName")), doc);
                }
            }
            for (Document groupDoc = allDocuments.getFirstDocument(); null != groupDoc; groupDoc = allDocuments.getNextDocument()) {
                if (!"Group".equals(groupDoc.getItemValueString("Type"))) {
                    continue;
                }
                String groupName = groupDoc.getItemValueString("ListName");
                if (!VALID_SCRUMS.contains(groupName.toLowerCase())) {
                    continue;
                }
                @SuppressWarnings("unchecked") Vector<String> members = groupDoc.getItemValue("Members");
                Collections.sort(members);
                for (String user : members) {
                    Document userDoc = users.get(nameToKey(user));
                    if (null == userDoc) {
//                        System.err.println("Can't find user: " + user + ": " + members);
                        continue;
                    }
                    out.printf("%s\t%s\t%s\t%s\n",
                            groupName,
                            getItemValueString(userDoc, "InternetAddress").toLowerCase(),
                            toCamelCase(getItemValueString(userDoc, "FirstName")),
                            toCamelCase(getItemValueString(userDoc, "LastName"))
                    );
                }
            }
        } finally {
            lnc.closeNotesSession();
        }
    }



}
