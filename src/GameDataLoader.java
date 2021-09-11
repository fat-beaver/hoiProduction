import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.HashMap;

public class GameDataLoader {
    //locations for state data
    private static final String STATE_INFRASTRUCTURE_PATH = "state/history/buildings/infrastructure";
    private static final String STATE_INDUSTRIAL_LEVEL_PATH = "state/state_category";
    private static final String STATE_BUILDING_SLOTS_PATH = "state/add_extra_state_shared_building_slots";
    private static final String STATE_DOCKYARDS_PATH = "state/history/buildings/dockyard";
    private static final String STATE_CIV_FACTORIES_PATH = "state/history/buildings/industrial_complex";
    private static final String STATE_MIL_FACTORIES_PATH = "state/history/buildings/arms_factory";
    //locations for country data
    private static final String COUNTRY_STABILITY_PATH = "set_stability";
    private static final String COUNTRY_WAR_SUPPORT_PATH = "set_war_support";

    public static HashMap<String, Country> getGameData(String historyFilesPath) {
        HashMap<String, Country> toReturn = new HashMap<>();
        File gameStatesDir = new File(historyFilesPath + "states/");
        File[] stateFiles = gameStatesDir.listFiles();
        FileSection[] stateRoots = new FileSection[0];
        if (stateFiles != null) {
            stateRoots = new FileSection[stateFiles.length];
            for (int i = 0; i < stateFiles.length; i++) {
                File stateFile = stateFiles[i];
                stateRoots[i] = processGameData(stateFile);
            }
            System.out.println("States loaded");
        } else {
            System.out.println("States could not be loaded");
        }
        File gameCountriesDir = new File(historyFilesPath + "countries/");
        File[] countryFiles = gameCountriesDir.listFiles();
        FileSection[] countryRoots = new FileSection[0];
        if (countryFiles != null) {
            countryRoots = new FileSection[countryFiles.length];
            for (int i = 0; i < countryFiles.length; i++) {
                File countryFile = countryFiles[i];
                countryRoots[i] = processGameData(countryFile);
            }
            System.out.println("Countries loaded");
        } else {
            System.out.println("Countries could not be loaded");
        }
        for (FileSection stateRaw : stateRoots) {
            State newState = stateFromRaw(stateRaw);
            for (FileSection countryRaw : countryRoots) {
                String countryCode = stateRaw.getValuesByPath("state/history/owner")[0];
                //first three characters of filename for country files are the country code
                if (countryCode.equals(countryRaw.getValuesByPath("fileName")[0].substring(0, 3))) {
                    if (toReturn.containsKey(countryCode)) {
                        toReturn.get(countryCode).addState(newState);
                    } else {
                        double stability = parseDoubleWithDefault(countryRaw.getValuesByPath(COUNTRY_STABILITY_PATH)[0]);
                        double warSupport = parseDoubleWithDefault(countryRaw.getValuesByPath(COUNTRY_WAR_SUPPORT_PATH)[0]);
                        Country newCountry = new Country(new State[0], stability, warSupport, countryCode);
                        toReturn.put(countryCode, newCountry);
                    }
                    break;
                }
            }
        }
        return toReturn;
    }
    private static State stateFromRaw(FileSection rawState) {
        int infrastructure = parseIntWithDefault(rawState.getValuesByPath(STATE_INFRASTRUCTURE_PATH)[0]);
        State.IndustrialLevel industrialLevel = State.IndustrialLevel.valueOf(rawState.getValuesByPath(STATE_INDUSTRIAL_LEVEL_PATH)[0]);
        int bonusBuildingSlots = parseIntWithDefault(rawState.getValuesByPath(STATE_BUILDING_SLOTS_PATH)[0]);
        int dockyards = parseIntWithDefault(rawState.getValuesByPath(STATE_DOCKYARDS_PATH)[0]);
        int civFactories = parseIntWithDefault(rawState.getValuesByPath(STATE_CIV_FACTORIES_PATH)[0]);
        int milFactories = parseIntWithDefault(rawState.getValuesByPath(STATE_MIL_FACTORIES_PATH)[0]);
        return new State(infrastructure, industrialLevel, bonusBuildingSlots, dockyards, civFactories, milFactories);
    }
    private static int parseIntWithDefault(String toParse) {
        int toReturn;
        try {
            toReturn = Integer.parseInt(toParse);
        } catch (NumberFormatException e) {
            toReturn = 0;
        }
        return toReturn;
    }
    private static double parseDoubleWithDefault(String toParse) {
        double toReturn;
        try {
            toReturn = Double.parseDouble(toParse);
        } catch (NumberFormatException e) {
            toReturn = 0.5;
        }
        return toReturn;
    }
    private static class FileSection {
        private final String name;
        private final ArrayList<FileSection> subSections = new ArrayList<>();
        private final ArrayList<String> values = new ArrayList<>();
        private final FileSection owner;

        private FileSection(String name, FileSection owner) {
            this.name = name;
            this.owner = owner;
        }
        private void addSubSection(FileSection newSection) {
            subSections.add(newSection);
        }
        private void addValue(String value) {
            values.add(value);
        }
        private FileSection getOwner() {
            return owner;
        }
        private String getName() {
            return name;
        }
        private String[] getValuesByPath(String path) {
            String[] sectionNames = path.split("/");
            FileSection currentSection = this;
            boolean pathDoesNotExist = false;
            for (String sectionName : sectionNames) {
                if (currentSection.getSubSection(sectionName) != null) {
                    currentSection = currentSection.getSubSection(sectionName);
                } else {
                    pathDoesNotExist = true;
                }
            }
            String[] toReturn;
            if (pathDoesNotExist || currentSection.getValues().length == 0) {
                toReturn = new String[1];
                toReturn[0] = "";
            } else {
                toReturn = currentSection.getValues();
            }
            return toReturn;
        }
        private FileSection getSubSection(String name) {
            FileSection toReturn = null;
            for (FileSection section : subSections) {
                if (section.getName().equals(name)) {
                    toReturn = section;
                    break;
                }
            }
            return toReturn;
        }
        private String[] getValues() {
            return values.toArray(new String[0]);
        }
    }
    private static FileSection processGameData(File dataFile) {
        FileSection rootSection = new FileSection("root", null);
        FileSection currentSection = rootSection;
        try {
            Scanner fileReader = new Scanner(dataFile);
            String lastLine = "";
            FileSection fileName = new FileSection("fileName", rootSection);
            fileName.addValue(dataFile.getName());
            rootSection.addSubSection(fileName);
            boolean firstLine = true;
            while (fileReader.hasNextLine()) {
                String line = fileReader.nextLine();
                //remove BOM which is present in some files (although it shouldn't be)
                if (firstLine && line.contains("\uFEFF")) {
                    line = line.substring(1);
                }
                firstLine = false;
                //strip comments
                if (line.contains("#")) {
                    line = line.substring(0, line.indexOf('#'));
                }
                //remove the quotes which are around some values
                if (line.contains("\"")) {
                    line = line.replaceAll("\"", "");
                }
                //remove trailing and leading whitespace
                line = line.trim();
                //ignore blank lines (and those which are just comments)
                if (line.equals("")) {
                    continue;
                }
                //create a new section if the line contains an "="
                if (line.contains("=")) {
                    String newSectionName = line.split("=")[0].trim();
                    if (currentSection.getSubSection(newSectionName) == null) {
                        FileSection newSection = new FileSection(newSectionName, currentSection);
                        currentSection.addSubSection(newSection);
                    }
                }
                if (line.contains("{")) {
                    if (line.contains("=")) {
                        String sectionName = line.split("=")[0].trim();
                        currentSection = currentSection.getSubSection(sectionName);
                    } else {
                        if (lastLine.contains("=")) {
                            String sectionName = lastLine.split("=")[0].trim();
                            currentSection = currentSection.getSubSection(sectionName);
                        }
                    }
                    if (line.indexOf("{") != line.length() - 1) {
                        String value = line.split("\\{")[1].replaceAll("}", "").trim();
                        String[] parts = value.split(" ");
                        for (String part : parts) {
                            currentSection.addValue(part);
                        }
                    }
                } else {
                    if (!line.contains("=")) {
                        if (!line.contains("}")) {
                            String[] parts = line.trim().split(" ");
                            for (String part : parts) {
                                currentSection.addValue(part);
                            }
                        }
                    } else {
                        //if there is an "=" and something after it, add the thing after it to the current section
                        if (line.indexOf("=") != line.length() - 1) {
                            String[] parts = line.split("=");
                            currentSection.getSubSection(parts[0].trim()).addValue(parts[1].trim());
                        }
                    }
                }
                if (line.contains("}")) {
                    //fix to replicate behaviour in the actual game where an extra closing bracket in the file for the british
                    // antilles causes some data not to be loaded
                    if (currentSection == rootSection) {
                        return rootSection;
                    } else {
                        assert currentSection != null;
                        currentSection = currentSection.getOwner();
                    }
                }
                lastLine = line;
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error reading data file");
            System.exit(1);
        }
        return rootSection;
    }
}
