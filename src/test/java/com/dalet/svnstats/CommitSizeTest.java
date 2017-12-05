package com.dalet.svnstats;

import org.hsqldb.lib.StopWatch;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dalet.svnstats.CommitSizeTestData.HUGE_REVS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("SameParameterValue")
public class CommitSizeTest {

    final SVNURL url = SVNURL.parseURIEncoded("svn://gfn-svn:3692");

    public CommitSizeTest() throws SVNException {
    }


    @Test
    public void name() throws Exception {

//        generateDiff(200000, url, String.format("%d.diff", 200000));
//        generateDiff(200001, url, String.format("%d.diff", 200001));

        int fromRev = 150000;
        int toRev = 276473;
        for (int i = 5; i < 25; i++) {
            generateDiffs(url, fromRev, toRev, i);
        }
    }

    private void generateDiffs(SVNURL url, int fromRev, int toRev, int threadsCount) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        StopWatch sw1 = new StopWatch(true);
        List<Future<Long>> futures = new ArrayList<>();
        for (int i = fromRev; i < toRev; ++i) {
            final int rev = i;
            Future<Long> submit = executorService.submit(() -> {
                StopWatch sw = new StopWatch();
                sw.start();
                try {
                    generateDiff(rev, url, String.format("diffs/%d.diff", rev));
                } catch (SVNException | FileNotFoundException e) {
                    e.printStackTrace();
                }
                sw.stop();
                return sw.elapsedTime();
            });
            futures.add(submit);
        }
        long sumTime = 0;
        for (Future<Long> future : futures) {
            Long elapsedTime = future.get();
            sumTime += elapsedTime;
        }
        sw1.stop();
        System.out.printf("Thread: %02d, Total time: %d Sum times: %d Ratio: %.2f%n", threadsCount, sw1.elapsedTime(), sumTime, (double) sumTime / sw1.elapsedTime());
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
    }

//    public static void gzipFile(String src, String trg) throws IOException {
//        byte[] buffer = new byte[64*1024];
//        try (GZIPOutputStream gzipOuputStream = new GZIPOutputStream(new FileOutputStream(trg)); FileInputStream fileInput = new FileInputStream(src)) {
//            int readBytesCnt;
//            while ((readBytesCnt = fileInput.read(buffer)) > 0) {
//                gzipOuputStream.write(buffer, 0, readBytesCnt);
//            }
//            gzipOuputStream.finish();
//        }
//    }


    @Test
    public void tst1() throws Exception {
        int rev = 228769;
        String filename = rev + ".diff";
        try {
            Files.delete(Paths.get(filename));
        } catch (IOException ignore) {
        }
        generateDiff(rev, url, filename);
    }

    public static void generateDiff(long rev, SVNURL repositoryUrl, String outputFileName) throws SVNException, IOException {
        Path outfile = Paths.get(outputFileName);
        if (Files.isRegularFile(outfile)) {
            return;
        }
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        try {
            DefaultSVNDiffGenerator diffGenerator = new DefaultSVNDiffGenerator();
            SVNDiffOptions diffOptions = new SVNDiffOptions(true, true, true);
            diffGenerator.setDiffOptions(diffOptions);
            diffGenerator.setDiffCopied(false);
            diffGenerator.setDiffDeleted(false);
            diffGenerator.setDiffUnversioned(false);
            diffGenerator.setDiffCopied(false);
            diffGenerator.setForcedBinaryDiff(false);
            diffGenerator.setDiffAdded(false);
            final SvnDiff diff = svnOperationFactory.createDiff();
            diff.setDiffGenerator(diffGenerator);
            diff.setSource(SvnTarget.fromURL(repositoryUrl), SVNRevision.create(rev - 1), SVNRevision.create(rev));
            diff.setDiffOptions(diffOptions);
            diff.setNoDiffDeleted(true);
            diff.setShowCopiesAsAdds(false);
            try (PrintStream out = new PrintStream(new FileOutputStream(outputFileName))) {
                diff.setOutput(out);
                diff.run();
            }
        } finally {
            svnOperationFactory.dispose();
        }
    }


    @Test
    public void tst2() throws Exception {
        for (int rev = 0; rev < 1000; rev++) {
            System.out.print(String.format("\rHandled diff for revision %6d", rev));
            System.out.flush();
            Thread.sleep(1);
        }
    }

    @Test
    public void test3() throws Exception {
        DiffsGenerator dg = new DiffsGenerator(SVNURL.parseURIEncoded("svn://gfn-svn:3692"), 1, "c:/temp/difftst");
        dg.generateDiffs(113502, 113502);
    }

    private void checkNeedsDiff(List<Long> list) throws SVNException {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        SvnLog log = svnOperationFactory.createLog();
        log.setSingleTarget(SvnTarget.fromURL(url));
        log.setUseMergeHistory(false);
        log.setDiscoverChangedPaths(true);
        list.stream().limit(10).map(SVNRevision::create).forEach(r -> log.addRange(SvnRevisionRange.create(r, r)));
        // log.addRange(SvnRevisionRange.create(SVNRevision.create(113500), SVNRevision.create(113510)));
        // log.addRange(SvnRevisionRange.create(SVNRevision.create(113503), SVNRevision.create(113503)));
        List<SVNLogEntry> logEntries = new ArrayList<>();
        log.run(logEntries);
        logEntries.forEach(logEntry -> {
            System.out.println(logEntry.getRevision() + ": " + DiffsGenerator.needsGenerateDiff(logEntry));
            logEntry.getChangedPaths().values().forEach(v -> System.out.println("\t" + v));
        });
    }

    @Test
    public void test4() throws Exception {
        List<Long> list = Arrays.stream(HUGE_REVS).boxed().collect(Collectors.toList());
//        Collections.reverse(list);
        checkNeedsDiff(list);
    }

    @Test
    public void test7() throws Exception {
        checkNeedsDiff(Collections.singletonList(228474L));
//        checkNeedsDiff(Collections.singletonList(123471L));
//        checkNeedsDiff(Collections.singletonList(278191L));
//        checkNeedsDiff(Collections.singletonList(85708L));
//        checkNeedsDiff(Collections.singletonList(112523L));
    }

    private static final String[] SKIP_SUF = new String[]{
            ".log",
            ".xml",
            ".pdf",
            ".doc",
            ".ini",
            ".jar",
            ".class",
            ".exe",
            ".dll",
            ".ocx",
            ".lib"
    };
    private static final String SKIP_SUF_PATTERN_STR = Arrays.stream(SKIP_SUF).map(Pattern::quote).collect(Collectors.joining("|"));
    private static final Pattern SKIP_SUF_PATTERN = Pattern.compile(String.format("(?i).*(%s)$", SKIP_SUF_PATTERN_STR));

    @Test
    public void test5() throws Exception {
        assertTrue(SKIP_SUF_PATTERN.matcher("file.xml").matches());
        assertTrue(SKIP_SUF_PATTERN.matcher("fIle.OcX").matches());
        assertTrue(SKIP_SUF_PATTERN.matcher("file.test.jar").matches());
        assertTrue(SKIP_SUF_PATTERN.matcher("/full/path/tofile.exe").matches());
        assertFalse(SKIP_SUF_PATTERN.matcher("file.xml.1").matches());
        assertFalse(SKIP_SUF_PATTERN.matcher("fIle.OcX2").matches());
        assertFalse(SKIP_SUF_PATTERN.matcher("file.test.thejar").matches());
        assertFalse(SKIP_SUF_PATTERN.matcher("/full/path/tofile.exe/").matches());
    }

}
