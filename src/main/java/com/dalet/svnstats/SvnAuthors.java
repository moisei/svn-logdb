package com.dalet.svnstats;

import java.util.HashMap;

/**
 * User: Moisei Rabinovich
 * Date: 2/20/14
 * Time: 3:21 PM
 */
public class SvnAuthors {
    public static final HashMap<String, String> AUTHORS_GROUP = new HashMap<>();

    static {
        AUTHORS_GROUP.put("abelulu", "video");
        AUTHORS_GROUP.put("adavidovich", "core");
        AUTHORS_GROUP.put("aeitan", "ux");
        AUTHORS_GROUP.put("afixler", "ux");
        AUTHORS_GROUP.put("agershman", "labs");
        AUTHORS_GROUP.put("agroisman", "labs");
        AUTHORS_GROUP.put("ahershkovitz", "labs");
        AUTHORS_GROUP.put("aisrael", "ux");
        AUTHORS_GROUP.put("alevi", "labs");
        AUTHORS_GROUP.put("amagaud", "video");
        AUTHORS_GROUP.put("aportnoy", "auto");
        AUTHORS_GROUP.put("asaveliev", "xchange");
        AUTHORS_GROUP.put("asoudarikov", "ux");
        AUTHORS_GROUP.put("avatury", "ux");
        AUTHORS_GROUP.put("avolynsky", "core");
        AUTHORS_GROUP.put("bbar", "labs");
        AUTHORS_GROUP.put("caucagne", "sport");
        AUTHORS_GROUP.put("crustichelli", "brio");
        AUTHORS_GROUP.put("cshay", "web");
        AUTHORS_GROUP.put("ctaboch", "ux");
        AUTHORS_GROUP.put("dbenknaan", "video");
        AUTHORS_GROUP.put("dliahovitsky", "labs");
        AUTHORS_GROUP.put("dmarciano", "main");
        AUTHORS_GROUP.put("dshultz", "main");
        AUTHORS_GROUP.put("dsoker", "labs");
        AUTHORS_GROUP.put("dzusman", "media");
        AUTHORS_GROUP.put("ecohen", "web");
        AUTHORS_GROUP.put("eelkin", "labs");
        AUTHORS_GROUP.put("eguillaume", "ddm");
        AUTHORS_GROUP.put("ekrasner", "core");
        AUTHORS_GROUP.put("elevi", "media");
        AUTHORS_GROUP.put("eliss", "media");
        AUTHORS_GROUP.put("esinger", "labs");
        AUTHORS_GROUP.put("etomer", "web");
        AUTHORS_GROUP.put("fpegeot", "ddm");
        AUTHORS_GROUP.put("gbonariva", "sport");
        AUTHORS_GROUP.put("gesterin", "labs");
        AUTHORS_GROUP.put("ggoldman", "labs");
        AUTHORS_GROUP.put("gkoren", "labs");
        AUTHORS_GROUP.put("glev", "core");
        AUTHORS_GROUP.put("gshamay", "cds");
        AUTHORS_GROUP.put("gwolfer", "web");
        AUTHORS_GROUP.put("habravanel", "web");
        AUTHORS_GROUP.put("hmeltzr", "media");
        AUTHORS_GROUP.put("idalkiun", "video");
        AUTHORS_GROUP.put("imedvetsky", "video");
        AUTHORS_GROUP.put("jkrein", "core");
        AUTHORS_GROUP.put("kturbovich", "cds");
        AUTHORS_GROUP.put("lgavriel", "web");
        AUTHORS_GROUP.put("lglik", "main");
        AUTHORS_GROUP.put("lvidrak", "main");
        AUTHORS_GROUP.put("mandrijasevic", "core");
        AUTHORS_GROUP.put("mcorniquet", "cds");
        AUTHORS_GROUP.put("melhadad", "labs");
        AUTHORS_GROUP.put("mnir", "web");
        AUTHORS_GROUP.put("mpshater", "main");
        AUTHORS_GROUP.put("mrabinovitch", "labs");
        AUTHORS_GROUP.put("mshmaiovitch", "ux");
        AUTHORS_GROUP.put("mzanoletti", "cg");
        AUTHORS_GROUP.put("namir", "labs");
        AUTHORS_GROUP.put("nazaria", "labs");
        AUTHORS_GROUP.put("nbouget", "labs");
        AUTHORS_GROUP.put("ndaka", "media");
        AUTHORS_GROUP.put("nelhadad", "web");
        AUTHORS_GROUP.put("nlusternik", "xchange");
        AUTHORS_GROUP.put("nschwartz", "labs");
        AUTHORS_GROUP.put("obenita", "auto");
        AUTHORS_GROUP.put("obroitman", "core");
        AUTHORS_GROUP.put("ocohen", "video");
        AUTHORS_GROUP.put("ohasbani", "labs");
        AUTHORS_GROUP.put("olevinger", "labs");
        AUTHORS_GROUP.put("omeisels", "core");
        AUTHORS_GROUP.put("oronen", "media");
        AUTHORS_GROUP.put("osabag", "other");
        AUTHORS_GROUP.put("ozeevi", "ddm");
        AUTHORS_GROUP.put("pgeminiani", "cg");
        AUTHORS_GROUP.put("rbelinsky", "main");
        AUTHORS_GROUP.put("rgamliel", "labs");
        AUTHORS_GROUP.put("rielpo", "brio");
        AUTHORS_GROUP.put("rkimhi", "web");
        AUTHORS_GROUP.put("rlegrand", "media");
        AUTHORS_GROUP.put("rliberman", "cds");
        AUTHORS_GROUP.put("rpinku", "other");
        AUTHORS_GROUP.put("sfridman", "media");
        AUTHORS_GROUP.put("sguez", "core");
        AUTHORS_GROUP.put("shourmann", "labs");
        AUTHORS_GROUP.put("sjordanov", "main");
        AUTHORS_GROUP.put("skaravaev", "ux");
        AUTHORS_GROUP.put("skorolev", "sport");
        AUTHORS_GROUP.put("smalka", "labs");
        AUTHORS_GROUP.put("smanor", "labs");
        AUTHORS_GROUP.put("srabinovitch", "xchange");
        AUTHORS_GROUP.put("sshkeer", "media");
        AUTHORS_GROUP.put("sshorsher", "labs");
        AUTHORS_GROUP.put("staub", "other");
        AUTHORS_GROUP.put("szeharia", "video");
        AUTHORS_GROUP.put("tbabamuratov", "ux");
        AUTHORS_GROUP.put("tpinhas", "labs");
        AUTHORS_GROUP.put("viva1", "core");
        AUTHORS_GROUP.put("vmintz", "media");
        AUTHORS_GROUP.put("vvajarov", "main");
        AUTHORS_GROUP.put("ydagan", "main");
        AUTHORS_GROUP.put("ykrief", "core");
        AUTHORS_GROUP.put("zorenbakh", "main");
    }

    public static String getTeam(String author) {
        author = author.toLowerCase();
        AUTHORS_GROUP.putIfAbsent(author, "other");
        return AUTHORS_GROUP.get(author);
    }
}
