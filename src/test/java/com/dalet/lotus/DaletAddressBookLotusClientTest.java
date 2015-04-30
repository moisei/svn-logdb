package com.dalet.lotus;

import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        Set<String> groupNames = new HashSet<String>() {{
            add("scrumux");
        }};
        groupNames = DaletAddressBookLotusClient.VALID_SCRUMS;
        Map<String, Set<String>> groupsForUsers = DaletAddressBookLotusClient.getGroupsForUsers(groupNames, users, "other");
        System.out.println(groupsForUsers);
    }
}
