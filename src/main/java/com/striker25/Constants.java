package com.striker25;

import java.io.File;
import java.nio.file.Paths;

public class Constants {

    public static final String FILE_SEPARATOR = File.separator;
    public static final String WORKING_DIRECTORY = Paths.get("").toAbsolutePath() + FILE_SEPARATOR;
    public static final String CONFIGURATION_JSON_FILE_PATH = WORKING_DIRECTORY + "configuration.json";
}
