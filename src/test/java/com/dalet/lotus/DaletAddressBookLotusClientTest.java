package com.dalet.lotus;

import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.dalet.lotus.LotusNotesIssueReferenceUtils.extractReferencedBugs;
import static com.dalet.lotus.LotusNotesIssueReferenceUtils.extractReferencedFeatures;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: Moisei Rabinovich
 * Date: 6/19/2014
 * Time: 4:00 PM
 */
@Ignore
public class DaletAddressBookLotusClientTest {


    @Test
    public void test1() throws Exception {
        Set<String> users = new HashSet<String>() {{
            add("ctaboch");
        }};
//        Set<String> groupNames = new HashSet<String>() {{
//            add("scrumux");
//        }};
        Set<String> groupNames = DaletAddressBookLotusClient.VALID_SCRUMS;
        Map<String, Set<String>> groupsForUsers = DaletAddressBookLotusClient.getGroupsForUsers(groupNames, users, "other");
        System.out.println(groupsForUsers);
    }


    @Test
    public void testPrintUsers() throws Exception {
        DaletAddressBookLotusClient.printUsers(System.out);
    }

    @Test
    public void name() throws Exception {
//        List<String> bugs = extractReferencedBugs("jopa popa #12345 - AB kolbasa #67890 - CD halo");
//        List<String> features = extractReferencedFeatures("jopa popa #1234 - AB kolbasa #6789 - CD halo");
        assertEquals("#55345 - AB", extractReferencedBugs("#55345 - AB").get(0));
        assertEquals("#123456 - AB", extractReferencedBugs("#123456 - AB").get(0));
        assertTrue(extractReferencedBugs("#19999 - AB").isEmpty());

        assertEquals("#1234 - AB", extractReferencedFeatures("#1234 - AB").get(0));
    }
}
