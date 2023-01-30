package com.striker25;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.striker25.models.Configuration;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import static com.striker25.Constants.FILE_SEPARATOR;
import static com.striker25.Constants.WORKING_DIRECTORY;

public class CsvTools {

    private static final ObjectMapper mapper = new ObjectMapper();


    public static void main(String[] args) throws IOException {


        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // read json
        String jsonStr = readFile();
        List<Configuration> configurations = mapper.readValue(jsonStr, new TypeReference<List<Configuration>>() {
        });

        configurations
                .stream()
                .filter(e -> !Objects.equals(e.getCsvFullPath(), ""))
                .forEach(configuration -> {

                    Reader in = getReader(configuration);

                    Iterable<CSVRecord> iterable = getCsvRecords(in);
                    Iterator<CSVRecord> csvRecordIterator = iterable.iterator();

                    List<CSVRecord> recordsForHeaders = new ArrayList<>();
                    recordsForHeaders.add(csvRecordIterator.next());

//                    List<CSVRecord> records = StreamSupport
//                            .stream(iterable.spliterator(), false)
//                            .collect(Collectors.toList());

                    List<String> headers = getCsvHeaders(recordsForHeaders);
                    List<String> updatedHeaders = getUpdatedCsvHeaders(headers, configuration);
                    List<String> updatedHeadersWithoutDeletedColumns = new ArrayList<>(List.copyOf(updatedHeaders));

                    // delete duplicated columns
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

        List<String> renamedHeaders = headers.stream().map(header -> {
            final String[] result = {null};

            configuration.getHeadersToRename().forEach(headerRename -> {
                if (header.equalsIgnoreCase(headerRename.getSource()) ||
                        header.contains(headerRename.getSource())
                ) {
                    result[0] = headerRename.getTarget();
                }
            });

            return result[0] != null ? result[0] : header;
        }).collect(Collectors.toList());



        return renamedHeaders;
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
            return new InputStreamReader(new FileInputStream(configuration.getCsvFullPath()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException(String.format("File %s was not found", configuration.getCsvFullPath()));
    }

    private static String getOutDirectoryPathStr(Configuration configuration) {

        String filePath = Paths.get(configuration.getCsvFullPath()).getParent().toString() + FILE_SEPARATOR;

        String outPath = configuration.getOutFolderName() != null ? configuration.getOutFolderName() : "out";
        return WORKING_DIRECTORY + outPath;
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
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);

        // make headers uppercase if requested.
        if (configuration.isHeadersToUpperCase() && updatedHeadersWithoutDeletedArray != null) {
            for (int i = 0; i < updatedHeadersWithoutDeletedArray.length; i++) {
                updatedHeadersWithoutDeletedArray[i] = updatedHeadersWithoutDeletedArray[i].toUpperCase(Locale.ROOT);
            }
        }

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

                        try {
                            if (e.contains("DATE") || e.contains("date")) {
                                LocalDate date = LocalDate.parse(value, inputFormatter);
                                String formattedDate = outputFormatter.format(date);
                                System.out.println("Date: " + value + " parsed: " + formattedDate);

                                value = formattedDate;
                            }
                        } catch (DateTimeException ex) {
                            System.out.println("La invalida es: " + value + " " + ex.getMessage());
                        }

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
