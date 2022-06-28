package com.striker25;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.striker25.models.Configuration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.striker25.Constants.FILE_SEPARATOR;

public class CsvTools {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {

        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // read json
        String jsonStr = readFile();
        List<Configuration> configurations = mapper.readValue(jsonStr, new TypeReference<>() {
        });

        configurations
                .forEach(configuration -> {

                    Reader in = getReader(configuration);

                    Iterable<CSVRecord> iterable = getCsvRecords(in);

                    List<CSVRecord> records = StreamSupport
                            .stream(iterable.spliterator(), false)
                            .collect(Collectors.toList());

                    List<String> headers = getCsvHeaders(records);
                    List<String> updatedHeaders = getUpdatedCsvHeaders(headers, configuration);
                    List<String> updatedHeadersWithoutDeletedColumns = new ArrayList<>(List.copyOf(updatedHeaders));

                    // delete duplicate columns
                    updatedHeadersWithoutDeletedColumns.removeAll(configuration.getColumnsToDelete());

                    // corrected csv headers
                    try {
                        Reader inCorrected = getReader(configuration);

                        String[] updatedHeadersArray = updatedHeaders.toArray(String[]::new);
                        String[] updatedHeadersWithoutDeletedArray = updatedHeadersWithoutDeletedColumns.toArray(String[]::new);

                        Iterable<CSVRecord> iterableCorrectCsv = CSVFormat.DEFAULT
                                .builder()
                                .setSkipHeaderRecord(true)
                                .setAllowDuplicateHeaderNames(true)
                                .setHeader(updatedHeadersArray)
                                .build()
                                .parse(inCorrected);


                        writeCsvFile(configuration, updatedHeadersWithoutDeletedColumns, updatedHeadersWithoutDeletedArray, iterableCorrectCsv);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                });
    }

    private static List<String> getUpdatedCsvHeaders(List<String> headers, Configuration configuration) {
        return headers.stream().map(header -> {
            final String[] result = {null};

            configuration.getHeadersToRename().forEach(headerRename -> {
                if (header.equals(headerRename.getSource())) {
                    result[0] = headerRename.getTarget();
                }
            });

            return result[0] != null ? result[0] : header;
        }).collect(Collectors.toList());
    }

    private static List<String> getCsvHeaders(List<CSVRecord> records) {

        if (records.isEmpty()) {
            throw new IllegalStateException("File doesn't have any header");
        }

        List<String> headers = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            if (i == 0) { // headers

                CSVRecord csvRecord = records.get(i);
                for (String header : csvRecord) {
                    headers.add(header);
                }
            }
        }

        return headers;
    }

    private static Iterable<CSVRecord> getCsvRecords(Reader in) {
        try {
            return CSVFormat.DEFAULT
                    .builder()
                    .setSkipHeaderRecord(false)
                    .setAllowDuplicateHeaderNames(true)
                    .build()
                    .parse(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("Not able to read the csv file");
    }

    private static Reader getReader(Configuration configuration) {
        try {
            return new FileReader(configuration.getCsvFullPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException(String.format("File %s was not found", configuration.getCsvFullPath()));
    }

    private static String getOutDirectoryPathStr(Configuration configuration) {

        String filePath = Paths.get(configuration.getCsvFullPath()).getParent().toString() + FILE_SEPARATOR;

        return filePath + "out";
    }

    private static Path getCsvOutPath(Configuration configuration) {
        String filePath = Paths.get(configuration.getCsvFullPath()).getParent().toString() + FILE_SEPARATOR;
        String fileName = configuration.getCsvFullPath().replace(filePath, "");

        return Paths.get(getOutDirectoryPathStr(configuration) + FILE_SEPARATOR + fileName);
    }

    private static String readFile() throws IOException {
        return Files.readString(Paths.get(Constants.CONFIGURATION_JSON_FILE_PATH));
    }

    private static void writeCsvFile(Configuration configuration, List<String> updatedHeadersWithoutDeletedColumns, String[] updatedHeadersWithoutDeletedArray, Iterable<CSVRecord> iterableCorrectCsv) {
        String outDirectoryPathStr = getOutDirectoryPathStr(configuration);
        File outDirectory = new File(outDirectoryPathStr);

        boolean directoryWasCreated = false;
        if (!outDirectory.exists()) {
            directoryWasCreated = outDirectory.mkdir();
        }

        if (outDirectory.exists() || directoryWasCreated) {
            try (
                    BufferedWriter writer = Files.newBufferedWriter(getCsvOutPath(configuration));

                    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                            .builder()
                            .setHeader(updatedHeadersWithoutDeletedArray)
                            .build())
            ) {

                for (CSVRecord csvRecord : iterableCorrectCsv) {
                    List<String> row = new ArrayList<>();
                    updatedHeadersWithoutDeletedColumns.forEach(e -> {
                        String value = csvRecord.get(e);
                        row.add(value);
                    });

                    csvPrinter.printRecord(row);
                }

                csvPrinter.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
