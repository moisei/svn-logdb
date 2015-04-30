package com.dalet.lotus;

import lotus.domino.*;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.FilenameFilter;
import java.util.Vector;

/**
 * User: Moisei Rabinovich
 * Date: 11/8/12
 * Time: 4:13 PM
 */
@SuppressWarnings("StatementWithEmptyBody")
public class LotusNotesClient implements Closeable {
    public static final String FEATURESDB_VIEWID = "F792B3C310381FAFC225632B0037986C";
    private static final Logger logger = Logger.getLogger(LotusNotesClient.class);
    public static final FilenameFilter FEAT_FILENAME_FILTER = (dir, name) -> name.toLowerCase().endsWith(".xml") && name.length() == "#1234 - AB.xml".length();
    public static final FilenameFilter BUGS_FILENAME_FILTER = (dir, name) -> name.toLowerCase().endsWith(".xml") && name.length() == "#12345 - AB.xml".length();
    private static final String NOTES_SERVER = "IL01";
    public static final String BUGS_REPLICAID = "C1256EBC002AA6A8";
    public static final String FEATURES_REPLICAID = "41256D4F0056B2C1";
    // protected static final String NOTES_USER = "tagent/IL/DALET ";
    private static final String NOTES_USER = null;
    private static final String MAIL_REPLICAID = "C22566D20037AB1D";
    private static final String DALETADDRESSBOOK_REPLICAID = "C12568B20032D381";
    //
    private Session notesSession;
    private Database bugsDb;
    private Database featDb;
    private Database mailDb;
    private Database daletAddressBookDb;
    //
    private RichTextStyle boldRichTextStyle;
    private RichTextStyle normalRichTextStyle;
    private String notesPassword;

    public Session getNotesSession() {
        return notesSession;
    }

    public LotusNotesClient(boolean createSession) throws NotesException {
        this(System.getProperty("NOTES.PASSWORD"), createSession);
    }

    public LotusNotesClient(String notesPassword) {
        this.notesPassword = notesPassword;
        this.notesSession = null;
        this.featDb = null;
        this.bugsDb = null;
    }

    public LotusNotesClient(String notesPassword, boolean createSession) throws NotesException {
        this(notesPassword);
        if (createSession) {
            createNotesSession();
        }
    }

    public void createNotesSession() throws NotesException {
        if (null != notesSession) {
            return;
        }
        NotesThread.sinitThread();
        notesSession = NotesFactory.createSession((String) null, NOTES_USER, notesPassword);
        try {
            setBoldRichTextStyle(notesSession.createRichTextStyle());
            getBoldRichTextStyle().setBold(RichTextStyle.YES);
            setNormalRichTextStyle(notesSession.createRichTextStyle());
            getNormalRichTextStyle().setBold(RichTextStyle.NO);
            bugsDb = connectDbByReplicaId(NOTES_SERVER, BUGS_REPLICAID);
            featDb = connectDbByReplicaId(NOTES_SERVER, FEATURES_REPLICAID);
            mailDb = connectDbByReplicaId(NOTES_SERVER, MAIL_REPLICAID);
            daletAddressBookDb = connectDbByReplicaId(NOTES_SERVER, DALETADDRESSBOOK_REPLICAID);
        } catch (NotesException e) {
            closeNotesSession();
            throw e;
        }
    }

    public void closeNotesSession() {
        logger.debug("entering closeNotesSession");
        if (null != notesSession) {
            notesSession = null;
            this.bugsDb = null;
            this.featDb = null;
            this.mailDb = null;
            this.daletAddressBookDb = null;
            NotesThread.stermThread();
        }
        logger.debug("finished closeNotesSession");
    }

    @Override
    public void close() {
        closeNotesSession();
    }

    private Database connectDbByReplicaId(String server, String replicaID) throws NotesException {
        DbDirectory dir = notesSession.getDbDirectory(server);
        logger.debug("+notesSession.getDbDirectory");
        Database db = dir.openDatabaseByReplicaID(replicaID);
        logger.debug("+openDatabaseByReplicaID " + replicaID);
        if (0 == db.getTitle().length()) {
            final NotesException notesException = new NotesException();
            notesException.text = "Cannot connect db by replicaID: " + replicaID;
            throw notesException;
        }
        return db;
    }

    public String getValue(Document document, String fieldName, String defaultValue) throws NotesException {
        Vector values = document.getItemValue(fieldName);
        String value;
        if (values != null && !values.isEmpty() && (values.get(0) instanceof String)) {
            value = (String) values.get(0);
        } else {
            value = defaultValue;
        }
        return value;
    }

    public Database getBugsDb() {
        return bugsDb;
    }

    public Database getFeatDb() {
        return featDb;
    }

    public Database getMailDb() {
        return mailDb;
    }

    public Database getDaletAddressBookDb() {
        return daletAddressBookDb;
    }

    public RichTextStyle getBoldRichTextStyle() {
        return boldRichTextStyle;
    }

    public void setBoldRichTextStyle(RichTextStyle boldRichTextStyle) {
        this.boldRichTextStyle = boldRichTextStyle;
    }

    public RichTextStyle getNormalRichTextStyle() {
        return normalRichTextStyle;
    }

    public void setNormalRichTextStyle(RichTextStyle normalRichTextStyle) {
        this.normalRichTextStyle = normalRichTextStyle;
    }

    @SuppressWarnings("unchecked")
    public static RichTextItem getOrCreateRichItemByName(Document document, String itemName) throws NotesException {
        Vector<Item> items = document.getItems();
        for (Item item : items) {
            if (!itemName.equals(item.getName())) {
                continue;
            }
            if (item instanceof RichTextItem) {
                return (RichTextItem) item;
            }
            logger.debug("+recreating  item");
            document.removeItem(itemName);
            return document.createRichTextItem(itemName);
        }
        return document.createRichTextItem(itemName);
    }

    public static String getStringValue(Document doc, String valueName, String defaultValue) throws NotesException {
        Vector values = doc.getItemValue(valueName);
        if ((values != null) && !values.isEmpty() && (values.get(0) instanceof String)) {
            return (String) values.get(0);
        }
        String msg;
        if ("Reference".equalsIgnoreCase(valueName)) {
            msg = "";
        } else {
            msg = " from doc with Reference: " + getStringValue(doc, "Reference", doc.getUniversalID());
        }
        logger.info("Can't extract " + valueName + msg);
        return defaultValue;
    }

    public static DocumentCollection findDocumentsByReference(Database db, String itemId) throws NotesException {
        String sFilter = "Reference='" + itemId + "'";
        return db.search(sFilter);
    }

    public static Document findOriginalDocumentByReference(Database db, String itemId) throws NotesException {
        if (itemId.trim().isEmpty()) {
            return null;
        }
        DocumentCollection docs = findDocumentsByReference(db, itemId);
        if (0 == docs.getCount()) {
            return null;
        }
        Document originalDoc = docs.getNthDocument(1);
        if (1 == docs.getCount()) {
            return originalDoc;
        }
        // conflicts exist. Assuming that original document is the one with longest $Revisions field
        int maxRevisionsSize = 0;
        for (int i = 1; i < docs.getCount(); ++i) {
            Document doc = docs.getNthDocument(i);
            Vector revisions = doc.getItemValue("$Revisions");
            if (revisions.size() > maxRevisionsSize) {
                originalDoc = doc;
                maxRevisionsSize = revisions.size();
            } else {
                // assuming this document is conflict as it has shorter $Revisions length
            }
        }
        return originalDoc;
    }

    public String buildFeatureHtmlLink(String reference) throws NotesException {
        Document doc = findOriginalDocumentByReference(featDb, reference);
        return buildFeatureHtmlLink(doc);
    }

    public static String buildFeatureHtmlLink(Document doc) throws NotesException {
        String title = getStringValue(doc, "Name", "NO-NAME");
        String reference = getStringValue(doc, "Reference", "NO-REFERENCE");
        String uniqId = getStringValue(doc, "UniqID", "NO-UniqID");
        return String.format("<a href=Notes:///%s/%s/%s>%s  %s</a>", FEATURES_REPLICAID, FEATURESDB_VIEWID, uniqId, reference, title);
    }
}
