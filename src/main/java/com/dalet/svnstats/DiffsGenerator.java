package com.dalet.svnstats;

import org.hsqldb.lib.StopWatch;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiffsGenerator implements Closeable {

    private final int threadsCount;
    private SVNURL url;
    private String diffsDir;
    private final ExecutorService executorService;
    private final CompletionService<Long> completionService;
    private final StopWatch globalStopWatch;
    private long submittedDiffsCount = 0;
    private long skippedDiffsCount;
    private long expectedDiffsCount;

    public DiffsGenerator(SVNURL url, int threadsCount, final String diffsDir) throws IOException {
        this.url = url;
        this.diffsDir = diffsDir;
        this.threadsCount = threadsCount;
        executorService = Executors.newFixedThreadPool(this.threadsCount);
        completionService = new ExecutorCompletionService<>(executorService);
        Files.createDirectories(Paths.get(diffsDir));
        globalStopWatch = new StopWatch(true);
    }

    public void generateDiffs(long fromRev, long toRev) throws Exception {
        System.out.printf("Generating diffs in dir %s, for revs: %d - %d, in %d threads%n", diffsDir, fromRev, toRev, threadsCount);
        for (long rev = fromRev; rev <= toRev; ++rev) {
            submit(rev);
        }
        StopWatch swGlobal = new StopWatch(true);
        swGlobal.stop();
        waitForTermination(8, TimeUnit.HOURS);
    }

    public static void generateDiff(long rev, SVNURL repositoryUrl, String outputFileName) throws SVNException, IOException {
        Path outfile = Paths.get(outputFileName);
        if (Files.isRegularFile(outfile)) {
            return;
        }
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        try {
            DefaultSVNDiffGenerator diffGenerator = new DefaultSVNDiffGenerator();
            diffGenerator.setDiffOptions(new SVNDiffOptions(true, true, true));
            diffGenerator.setDiffCopied(false);
            diffGenerator.setDiffDeleted(false);
            diffGenerator.setDiffAdded(true);
            diffGenerator.setDiffUnversioned(false);
            diffGenerator.setForcedBinaryDiff(false);
            final SvnDiff diff = svnOperationFactory.createDiff();
            diff.setDiffGenerator(diffGenerator);
            diff.setSource(SvnTarget.fromURL(repositoryUrl), SVNRevision.create(rev - 1), SVNRevision.create(rev));
            diff.setShowCopiesAsAdds(false);
            try (PrintStream out = new PrintStream(new FileOutputStream(outputFileName))) {
                diff.setOutput(out);
                diff.run();
            }
        } finally {
            svnOperationFactory.dispose();
        }
    }

    public void submitIfNeeded(SVNLogEntry svnLogEntry) throws Exception {
        if (!needsGenerateDiff(svnLogEntry)) {
            completionService.submit(() -> backupObsoleteDiffFile(svnLogEntry.getRevision()));
            skippedDiffsCount++;
        } else {
            submit(svnLogEntry.getRevision());
        }
        if (0 == (submittedDiffsCount % 100)) {
            System.out.printf("\rHandled %6d revisions out of %6d. skipped: %6d", submittedDiffsCount, expectedDiffsCount, skippedDiffsCount);
            System.out.flush();
        }
    }

    public void submit(long rev) throws Exception {
        if (executorService.isShutdown()) {
            throw new Exception("Can't handle more diffs. Waiting for termination");
        }
        completionService.submit(() -> measureGenerateDiff(rev));
        submittedDiffsCount++;
    }

    private Long measureGenerateDiff(long rev) throws SVNException, IOException {
        StopWatch sw = new StopWatch(true);
        generateDiff(rev, url, diffFilenameByRev(rev));
        sw.stop();
        return sw.elapsedTime();
    }

    private String diffFilenameByRev(long rev) {
        return String.format("%s/%d.diff", diffsDir, rev);
    }

    public void waitForTermination(int units, TimeUnit timeUnit) throws InterruptedException {
        executorService.shutdown();
        System.out.println();
        long sumTime = 0;
        for (int handled = 0; handled < submittedDiffsCount; handled++) {
            if (0 == (handled % 100)) {
                System.out.printf("\rGenerated diff for %6d revisions out of %6d", handled, submittedDiffsCount);
                System.out.flush();
            }
            try {
                Long elapsedTime = completionService.take().get();
                sumTime += elapsedTime;
            } catch (ExecutionException e) {
                System.err.println("Failed to handle diff");
                if (null != e.getCause()) {
                    e.getCause().printStackTrace();
                } else {
                    e.printStackTrace();
                }
            }
        }
        System.out.println();
        executorService.awaitTermination(units, timeUnit);
        System.out.printf("Thread: %02d, Total time: %d Sum times: %d Ratio: %.2f%n", threadsCount, globalStopWatch.elapsedTime(), sumTime, (double) sumTime / globalStopWatch.elapsedTime());
    }

    private long backupObsoleteDiffFile(long rev) {
        String filename = diffFilenameByRev(rev);
        Path file = Paths.get(filename);
        if (!Files.exists(file)) {
            return 0;
        }
        String newFilename = filename + ".obsolete";
        Path newFile = Paths.get(newFilename);
        if (Files.exists(newFile)) {
            newFilename = newFilename + System.currentTimeMillis();
            newFile = Paths.get(newFilename);
        }
        try {
            Files.move(file, newFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignore) {
            System.err.println("Can't rename file " + " to " + newFile + ": " + ignore.getMessage());
        }
        return 0;
    }


    private static final String CASE_INSENSITIVE = "(?i)";

    private static String[] SKIP_PREF = new String[]{
            "/amberfin/import-from-amberfin-repository",
            "/branches/tools",
            "/branches/utils",
            "/tags",
            "/vendor"
    };

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
    private static final String SKIP_PREF_PATTERN_STR = Arrays.stream(SKIP_PREF).map(Pattern::quote).collect(Collectors.joining("|"));
    private static final Pattern SKIP_PREF_PATTERN = Pattern.compile(String.format("%s.*(%s)$", CASE_INSENSITIVE, SKIP_PREF_PATTERN_STR));
    private static final Predicate<SVNLogEntryPath> MATCH_SKIP_PREF_PREDICATE = p -> SKIP_PREF_PATTERN.matcher(p.getPath()).matches();

    private static final String SKIP_SUF_PATTERN_STR = Arrays.stream(SKIP_SUF).map(Pattern::quote).collect(Collectors.joining("|"));
    private static final Pattern SKIP_SUF_PATTERN = Pattern.compile(String.format("%s.*(%s)$", CASE_INSENSITIVE, SKIP_SUF_PATTERN_STR));
    private static final Predicate<SVNLogEntryPath> MATCH_SKIP_SUF_PREDICATE = p -> SKIP_SUF_PATTERN.matcher(p.getPath()).matches();

    private static final Predicate<SVNLogEntryPath> FILE_CHANGED_PREDICATE = p -> SVNNodeKind.FILE.equals(p.getKind()) && ('D' != p.getType());

    private static final Predicate<SVNLogEntryPath> COPY_DIR_PREDICATE = p -> SVNNodeKind.DIR.equals(p.getKind()) && !isNullOrEmpty(p.getCopyPath());

    static boolean needsGenerateDiff(SVNLogEntry svnLogEntry) {
        Collection<SVNLogEntryPath> changedPaths = svnLogEntry.getChangedPaths().values();
        return
                changedPaths.stream()
                        .noneMatch(COPY_DIR_PREDICATE)
                        &&
                        changedPaths.stream()
                                .filter(MATCH_SKIP_PREF_PREDICATE.negate())
                                .filter(MATCH_SKIP_SUF_PREDICATE.negate())
                                .anyMatch(FILE_CHANGED_PREDICATE);
    }

    private static boolean isNullOrEmpty(String str) {
        return null == str || str.isEmpty();
    }

    @Override
    public void close() {
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
    }

    public void setExpectedDiffsCount(long expectedDiffsCount) {
        this.expectedDiffsCount = expectedDiffsCount;
    }
}
