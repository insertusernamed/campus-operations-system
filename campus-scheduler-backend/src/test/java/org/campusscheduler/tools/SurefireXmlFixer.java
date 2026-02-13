package org.campusscheduler.tools;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Fixes Surefire's generated XML summary attributes when JUnit Platform nested tests
 * end up with &lt;testsuite tests="0"&gt; despite having &lt;testcase&gt; entries.
 *
 * <p>This is a reporting-only fix: it does not change which tests are executed.
 */
public final class SurefireXmlFixer {
    private static final Pattern INT_ATTR =
            Pattern.compile("\\b([a-zA-Z]+)=\"(\\d+)\"");

    private record Counts(int tests, int failures, int errors, int skipped) {}

    public static void main(String[] args) throws Exception {
        Path reportsDir = Paths.get(args.length > 0 ? args[0] : "target/surefire-reports");
        if (!Files.isDirectory(reportsDir)) {
            System.out.println("SurefireXmlFixer: no surefire reports dir at " + reportsDir);
            return;
        }

        List<Path> reportFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(reportsDir, "TEST-*.xml")) {
            for (Path path : stream) {
                reportFiles.add(path);
            }
        }
        Collections.sort(reportFiles);

        int updated = 0;
        int sumTestsAttrBefore = 0;
        int sumTestcasesAfter = 0;

        for (Path file : reportFiles) {
            String xml = Files.readString(file, StandardCharsets.UTF_8);

            String header = extractTestsuiteHeader(xml);
            sumTestsAttrBefore += readIntAttr(header, "tests");

            Counts counts = parseCounts(file);
            sumTestcasesAfter += counts.tests;

            String fixedXml = fixTestsuiteHeader(xml, counts);
            if (!fixedXml.equals(xml)) {
                Files.writeString(file, fixedXml, StandardCharsets.UTF_8);
                updated++;
            }
        }

        System.out.println(
                "SurefireXmlFixer: reports="
                        + reportFiles.size()
                        + ", updated="
                        + updated
                        + ", testsuite_tests_before="
                        + sumTestsAttrBefore
                        + ", testsuite_tests_after="
                        + sumTestcasesAfter);
    }

    private static String extractTestsuiteHeader(String xml) {
        int start = xml.indexOf("<testsuite");
        if (start < 0) {
            return "";
        }
        int end = xml.indexOf('>', start);
        if (end < 0) {
            return "";
        }
        return xml.substring(start, end + 1);
    }

    private static int readIntAttr(String header, String name) {
        Matcher m = INT_ATTR.matcher(header);
        while (m.find()) {
            if (name.equals(m.group(1))) {
                return Integer.parseInt(m.group(2));
            }
        }
        return 0;
    }

    private static Counts parseCounts(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Best-effort hardening; this parses local files produced by the build.
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        try (InputStream in = Files.newInputStream(file)) {
            doc = builder.parse(in);
        }
        Element root = doc.getDocumentElement();

        int tests = root.getElementsByTagName("testcase").getLength();
        int failures = root.getElementsByTagName("failure").getLength();
        int errors = root.getElementsByTagName("error").getLength();
        int skipped = root.getElementsByTagName("skipped").getLength();
        return new Counts(tests, failures, errors, skipped);
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String name, boolean value) {
        try {
            factory.setFeature(name, value);
        } catch (Exception ignored) {
            // Feature not supported by this XML implementation.
        }
    }

    private static String fixTestsuiteHeader(String xml, Counts counts) {
        int start = xml.indexOf("<testsuite");
        if (start < 0) {
            return xml;
        }
        int end = xml.indexOf('>', start);
        if (end < 0) {
            return xml;
        }

        String header = xml.substring(start, end + 1);
        String fixedHeader = header;
        fixedHeader = replaceOrInsertAttr(fixedHeader, "tests", counts.tests);
        fixedHeader = replaceOrInsertAttr(fixedHeader, "failures", counts.failures);
        fixedHeader = replaceOrInsertAttr(fixedHeader, "errors", counts.errors);
        fixedHeader = replaceOrInsertAttr(fixedHeader, "skipped", counts.skipped);

        if (fixedHeader.equals(header)) {
            return xml;
        }
        return xml.substring(0, start) + fixedHeader + xml.substring(end + 1);
    }

    private static String replaceOrInsertAttr(String header, String name, int value) {
        Pattern p = Pattern.compile("\\b" + Pattern.quote(name) + "=\"[^\"]*\"");
        Matcher m = p.matcher(header);
        if (m.find()) {
            return m.replaceFirst(name + "=\"" + value + "\"");
        }

        // Fallback: insert attribute before the closing '>'.
        if (header.endsWith(">")) {
            return header.substring(0, header.length() - 1) + " " + name + "=\"" + value + "\">";
        }
        return header;
    }
}

